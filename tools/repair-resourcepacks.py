#!/usr/bin/env python3
"""Materialize verified resourcepacks in one stopped prepared runtime; never alter saves or mods."""
from __future__ import annotations
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import tempfile
from runtime_config import ROOT, settings
from runtime_lock import runtime_lock


def digest(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def repair(runtime: Path, dry_run: bool = False) -> dict:
    selected = settings({"VILLAGERS_RUN_DIR": str(runtime)})
    runtime = Path(selected["run_dir"])
    if dry_run:
        return _repair(runtime, True)
    with runtime_lock(runtime):
        return _repair(runtime, False)


def _repair(runtime: Path, dry_run: bool) -> dict:
    marker = runtime / "experiment.json"
    if marker.is_symlink() or not marker.is_file():
        raise ValueError("Only a managed independent runtime can be repaired")
    manifest = json.loads(marker.read_text())
    cache = Path(manifest["asset_cache"]).resolve(strict=True)
    if not cache.is_relative_to(ROOT):
        raise ValueError("Asset cache must stay inside the canonical project")
    replacements = []
    changed = False
    for relative, record in manifest.get("shared_assets", {}).items():
        path = Path(relative)
        if not path.parts or path.is_absolute() or ".." in path.parts:
            raise ValueError(f"Invalid managed asset path: {relative}")
        if path.parts[0] != "resourcepacks":
            continue
        target = runtime / path
        if target.parent.resolve() != target.parent:
            raise ValueError(f"Resourcepack parent must not be linked: {target.parent}")
        source = target.resolve(strict=True)
        if not source.is_file() or source.stat().st_mode & 0o222 or digest(source) != record["sha256"]:
            raise ValueError(f"Resourcepack bytes/permissions differ from the verified manifest: {relative}")
        if target.is_symlink():
            if not source.is_relative_to(cache):
                raise ValueError(f"Resourcepack link escapes its verified asset cache: {relative}")
            replacements.append((source, target, record["sha256"]))
        changed |= record.get("storage") != "regular"
        record["storage"] = "regular"
    result = {"runtime": str(runtime), "dry_run": dry_run, "resourcepacks_to_materialize": len(replacements),
              "manifest_update": changed, "saves_or_mods_changed": False}
    if dry_run or not (replacements or changed):
        return result
    staged = []
    try:
        # Verify and stage every asset before replacing any existing runtime entry.
        for source, target, checksum in replacements:
            fd, name = tempfile.mkstemp(prefix=".resourcepack-", dir=target.parent)
            os.close(fd)
            temporary = Path(name)
            staged.append((temporary, target))
            temporary.unlink()
            try:
                os.link(source, temporary)
            except OSError:
                shutil.copyfile(source, temporary)
                temporary.chmod(0o444)
            if digest(temporary) != checksum:
                raise ValueError(f"Resourcepack changed during repair: {source}")
        backup = runtime / ".pack-backups" / ("resourcepacks-" + datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S.%fZ") + ".json")
        backup.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(marker, backup)
        for temporary, target in staged:
            os.replace(temporary, target)  # Replace the link itself, never write through it.
        fd, name = tempfile.mkstemp(prefix=".experiment-", dir=runtime)
        temporary = Path(name)
        staged.append((temporary, marker))
        with os.fdopen(fd, "w") as stream:
            json.dump(manifest, stream, indent=2)
            stream.write("\n")
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, marker)
        result["manifest_backup"] = str(backup)
        return result
    finally:
        for temporary, unused in staged:
            temporary.unlink(missing_ok=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run-dir", type=Path, required=True)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    try:
        print(json.dumps(repair(args.run_dir, args.dry_run), indent=2))
    except (OSError, ValueError, KeyError) as error:
        sys.exit(str(error))


if __name__ == "__main__":
    main()
