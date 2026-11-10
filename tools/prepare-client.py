#!/usr/bin/env python3
"""Stage an isolated, checksum-verified copy of the pinned Vanilla Perfected pack."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import tempfile
from datetime import datetime, timezone
from zipfile import ZipFile
from runtime_lock import runtime_lock

ROOT = Path(__file__).resolve().parents[1]
RUN = ROOT / "run"
MANIFEST = RUN / "pack-provenance.json"
LOCK = Path(__file__).with_name("pack-lock.json")
DEFAULT_SOURCE = Path.home() / ".local/share/ModrinthApp/profiles/Vanilla Perfected"
ASSET_TREES = ("config", "resourcepacks", "defaultconfigs")
PRIVATE_WORDS = ("account", "credential", "secret", "session", "token", "auth")


def digest(path: Path, algorithm: str = "sha256") -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, algorithm).hexdigest()


def safe_destination(path: Path) -> None:
    """Never follow destination links into the user's profile or another world."""
    relative = path.relative_to(RUN)
    if ".." in relative.parts:
        raise ValueError(f"Destination path escapes runtime: {path}")
    for candidate in (RUN, *path.relative_to(RUN).parents):
        absolute = candidate if candidate == RUN else RUN / candidate
        if absolute.is_symlink():
            raise ValueError(f"Destination symlink is not allowed: {absolute}")
    if path.is_symlink():
        raise ValueError(f"Destination symlink is not allowed: {path}")


def atomic_copy(source: Path, target: Path) -> None:
    safe_destination(target)
    target.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary = tempfile.mkstemp(prefix=".pack-copy-", dir=target.parent)
    os.close(fd)
    try:
        shutil.copyfile(source, temporary)
        os.replace(temporary, target)
    finally:
        Path(temporary).unlink(missing_ok=True)


def prepare(args: argparse.Namespace) -> dict:
    if args.dry_run:
        return _prepare(args)  # Read-only preview cannot mutate a live runtime.
    with runtime_lock(RUN, create=True):
        return _prepare(args)


def _prepare(args: argparse.Namespace) -> dict:
    source = args.source.expanduser().resolve(strict=True)
    if source == RUN or RUN in source.parents or source in RUN.parents:
        raise ValueError("Source profile and isolated run directory must be separate")
    safe_destination(MANIFEST)
    lock = json.loads(LOCK.read_text())
    files: dict[str, dict] = {}
    excluded: list[dict] = []
    expected_mods = {item["path"]: item for item in lock["mods"]}
    ids: dict[str, str] = {}

    for relative, expected in expected_mods.items():
        path = source / relative
        if path.is_symlink() or not path.is_file():
            raise ValueError(f"Pinned pack mod is missing or a symlink: {path}")
        actual = digest(path, "sha512")
        if actual != expected["hashes"]["sha512"]:
            raise ValueError(f"Pinned Modrinth SHA-512 mismatch: {path}")
        with ZipFile(path) as archive:
            # A pack jar contains literal controls in a description; Loader accepts it.
            metadata = json.loads(archive.read("fabric.mod.json"), strict=False)
        mod_id = metadata["id"]
        if mod_id in ids:
            raise ValueError(f"Duplicate mod ID {mod_id}: {ids[mod_id]} and {relative}")
        ids[mod_id] = relative
        if mod_id == "fabric-api":
            excluded.append({"path": relative, "mod_id": mod_id,
                             "reason": "Provided by the pinned Gradle dependency"})
            continue
        files[relative] = {"sha256": digest(path), "sha512": actual,
                           "size": path.stat().st_size, "mod_id": mod_id,
                           "version": metadata.get("version")}

    for path in sorted((source / "mods").glob("*.jar")):
        relative = path.relative_to(source).as_posix()
        if relative not in expected_mods:
            mod_id = None
            if not path.is_symlink():
                with ZipFile(path) as archive:
                    if "fabric.mod.json" in archive.namelist():
                        mod_id = json.loads(archive.read("fabric.mod.json"), strict=False).get("id")
            excluded.append({"path": relative, "mod_id": mod_id,
                             "reason": "Not in the pinned Modrinth pack"})

    for tree in ASSET_TREES:
        folder = source / tree
        if folder.is_symlink():
            raise ValueError(f"Asset source directory must not be a symlink: {folder}")
        if not folder.exists():
            continue
        for path in sorted(folder.rglob("*")):
            relative = path.relative_to(source).as_posix()
            if path.is_symlink():
                excluded.append({"path": relative, "reason": "Symlink; external data is never followed"})
                continue
            if not path.is_file():
                continue
            # Copy settings/assets, not generated logs, personal auth, or crash/session data.
            if path.suffix == ".log" or any(word in path.name.lower() for word in PRIVATE_WORDS):
                excluded.append({"path": relative, "reason": "Generated or potentially private configuration"})
                continue
            files[relative] = {"sha256": digest(path), "size": path.stat().st_size}

    previous = json.loads(MANIFEST.read_text()) if MANIFEST.exists() else {}
    managed = previous.get("files", {})
    copies: list[str] = []
    backups: list[str] = []
    conflicts: list[str] = []
    for relative, item in files.items():
        target = RUN / relative
        safe_destination(target)
        if target.exists():
            if not target.is_file():
                conflicts.append(f"Not a regular file: {relative}")
            elif digest(target) != item["sha256"]:
                if args.refresh and relative in managed:
                    copies.append(relative)
                    backups.append(relative)
                else:
                    conflicts.append(f"Existing file differs: {relative}")
        else:
            copies.append(relative)

    removed = sorted(set(managed) - set(files))
    for relative in removed:
        target = RUN / relative
        safe_destination(target)
        if target.exists():
            if args.refresh and target.is_file():
                backups.append(relative)
            else:
                conflicts.append(f"Previously managed file is no longer in source: {relative}")
    # Unknown jars can change dependency resolution. Never mix an old mod setup silently.
    if (RUN / "mods").exists():
        for path in (RUN / "mods").glob("*.jar"):
            relative = path.relative_to(RUN).as_posix()
            if relative not in files and relative not in removed:
                conflicts.append(f"Unmanaged runtime mod: {relative}")
    if conflicts:
        raise ValueError("\n".join(conflicts) +
                         "\nNo files copied. --refresh explicitly replaces only previously managed files; "
                         "move untracked conflicts aside yourself.")

    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ")
    backup_root = RUN / ".pack-backups" / stamp
    if not args.dry_run:
        for relative in backups:
            atomic_copy(RUN / relative, backup_root / relative)
        for relative in copies:
            atomic_copy(source / relative, RUN / relative)
        for relative in removed:
            (RUN / relative).unlink(missing_ok=True)
        record = {"schema_version": 1, "prepared_at_utc": stamp,
                  "source_profile": str(source), "runtime": str(RUN),
                  "pack": {key: value for key, value in lock.items() if key != "mods"},
                  "pack_lock_sha256": digest(LOCK), "files": files, "excluded": excluded,
                  "copied_trees": ["mods", *ASSET_TREES],
                  "omitted": ["saves", "accounts", "launcher state", "options.txt", "servers.dat",
                              "logs", "screenshots", "shaderpacks", "data", ".fabric"],
                  "backup_directory": str(backup_root) if backups else None}
        RUN.mkdir(parents=True, exist_ok=True)
        # Keep the manifest on the same filesystem and replace atomically.
        fd, temporary = tempfile.mkstemp(prefix=".pack-provenance-", dir=RUN)
        with os.fdopen(fd, "w") as stream:
            json.dump(record, stream, indent=2)
            stream.write("\n")
        os.replace(temporary, MANIFEST)
    return {"dry_run": args.dry_run, "copied": len(copies), "unchanged": len(files) - len(copies),
            "mods": sum("mod_id" in item for item in files.values()),
            "managed_files": len(files), "excluded": excluded,
            "backed_up": len(backups), "removed": len(removed), "runtime": str(RUN)}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--refresh", action="store_true", help="Back up and replace changed managed files")
    parser.add_argument("--dry-run", action="store_true", help="Validate and report without writing")
    args = parser.parse_args()
    try:
        print(json.dumps(prepare(args), indent=2))
    except (OSError, ValueError, KeyError) as error:
        sys.exit(str(error))


if __name__ == "__main__":
    main()
