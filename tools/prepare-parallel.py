#!/usr/bin/env python3
"""Prepare independent visible adaptive/frozen/play clients from one stopped world snapshot."""
from __future__ import annotations

import argparse
from contextlib import contextmanager, ExitStack
from datetime import datetime, timezone
import fcntl
import hashlib
import json
import os
from pathlib import Path
import shutil
import sys
import tempfile

from runtime_config import ROOT, ROLES, TPS, live_clients
from runtime_lock import runtime_lock

PACK_RUNTIME = ROOT / "run"
INTERNAL = Path("/home/niko/.internal")


def digest(path: Path) -> str:
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def tree_files(root: Path) -> dict[str, dict]:
    if root.is_symlink():
        raise ValueError(f"Mutable data source must not be a symlink: {root}")
    result = {}
    for path in sorted(root.rglob("*")):
        # Session locks are process coordination, not world state. Reading and
        # closing another descriptor for this inode would release our lockf lock.
        if path.relative_to(root).as_posix() == "session.lock":
            continue
        if path.is_symlink():
            raise ValueError(f"A mutable data tree contains a symlink: {path}")
        if path.is_dir():
            continue
        if not path.is_file():
            raise ValueError(f"A mutable data tree contains a special file: {path}")
        result[path.relative_to(root).as_posix()] = {"sha256": digest(path), "size": path.stat().st_size}
    return result


def tree_digest(files: dict) -> str:
    return hashlib.sha256(json.dumps(files, sort_keys=True, separators=(",", ":")).encode()).hexdigest()


@contextmanager
def stopped_world(snapshot: Path):
    # Java FileChannel's session lock uses POSIX record locks; flock alone would
    # not detect it on Linux. Hold this lock throughout hashing and copying.
    lock = snapshot / "session.lock"
    if lock.is_symlink():
        raise ValueError("Snapshot session.lock must not be a symlink")
    stream = lock.open("r+b") if lock.exists() else None
    try:
        if stream is not None:
            try:
                fcntl.lockf(stream.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError as failure:
                raise ValueError("Snapshot session.lock is live; stop and save the client before copying") from failure
        yield
    finally:
        if stream is not None:
            stream.close()


def canonical_child(path: Path, root: Path) -> Path:
    requested = path.expanduser().absolute()
    if requested.resolve() != requested or not requested.is_relative_to(root) or requested == root:
        raise ValueError(f"Expected a canonical child directory below {root}: {requested}")
    return requested


def verified_assets(pack: Path) -> tuple[dict, dict]:
    provenance_file = pack / "pack-provenance.json"
    if not provenance_file.is_file() or provenance_file.is_symlink():
        raise ValueError("Prepare the official pack in run/ before preparing parallel clients")
    provenance = json.loads(provenance_file.read_text())
    assets = {}
    expected = provenance["files"]
    for relative, record in expected.items():
        path = Path(relative)
        immutable = path.parts[0] == "mods" and path.suffix == ".jar" or path.parts[0] == "resourcepacks" and path.suffix == ".zip"
        if not immutable:
            continue
        source = pack / path
        if not source.is_relative_to(pack) or ".." in path.parts or source.is_symlink() or not source.is_file():
            raise ValueError(f"Missing or unsafe verified pack asset: {source}")
        actual = digest(source)
        if actual != record["sha256"]:
            raise ValueError(f"Pack asset changed since verification: {source}")
        assets[relative] = {"sha256": actual, "size": source.stat().st_size}
    actual_mods = {p.relative_to(pack).as_posix() for p in (pack / "mods").glob("*.jar")}
    if actual_mods != {relative for relative in assets if relative.startswith("mods/")}:
        raise ValueError("Runtime mods differ from its verified pack manifest")
    return assets, provenance


def settings_files(pack: Path) -> dict[str, dict]:
    result = {}
    for tree in ("config", "defaultconfigs", "resourcepacks"):
        folder = pack / tree
        if folder.exists():
            for relative, record in tree_files(folder).items():
                if tree == "resourcepacks" and Path(relative).suffix == ".zip":
                    continue
                result[f"{tree}/{relative}"] = record
    options = pack / "options.txt"
    if options.exists():
        if options.is_symlink() or not options.is_file():
            raise ValueError("Mutable options.txt must be a regular file")
        result["options.txt"] = {"sha256": digest(options), "size": options.stat().st_size}
    return result


def copy_files(source: Path, destination: Path, records: dict) -> None:
    for relative, record in records.items():
        source_file, target = source / relative, destination / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source_file, target)
        if digest(target) != record["sha256"]:
            raise ValueError(f"File changed while copying: {source_file}")


def asset_cache(pack: Path, runs: Path, assets: dict) -> dict[str, Path]:
    cache = {}
    for relative, record in assets.items():
        target = runs / ".assets" / record["sha256"] / Path(relative).name
        canonical_child(target, runs)
        if target.exists():
            if target.is_symlink() or not target.is_file() or digest(target) != record["sha256"]:
                raise ValueError(f"Read-only asset cache was changed: {target}")
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            fd, name = tempfile.mkstemp(prefix=".asset-", dir=target.parent)
            os.close(fd)
            temporary = Path(name)
            try:
                shutil.copyfile(pack / relative, temporary)
                if digest(temporary) != record["sha256"]:
                    raise ValueError(f"Asset changed during staging: {relative}")
                temporary.chmod(0o444)
                os.replace(temporary, target)
            finally:
                temporary.unlink(missing_ok=True)
        if target.stat().st_mode & 0o222:
            raise ValueError(f"Shared asset must be read-only: {target}")
        cache[relative] = target
    return cache


def existing_runtime(target: Path, source_digest: str, role: str, tps: int) -> bool:
    if not target.exists():
        return False
    if not target.is_dir() or target.is_symlink():
        raise ValueError(f"Runtime destination is not an independent directory: {target}")
    marker = target / "experiment.json"
    if marker.is_symlink() or not marker.is_file():
        raise ValueError(f"Runtime already exists without a managed experiment manifest: {target}")
    manifest = json.loads(marker.read_text())
    required = {"source_world_sha256": source_digest, "role": role, "requested_tps": tps}
    if any(manifest.get(key) != value for key, value in required.items()):
        raise ValueError(f"Existing runtime belongs to a different experiment or settings: {target}; archive it explicitly first")
    if not (target / "saves/VillageTrial/level.dat").is_file():
        raise ValueError(f"Existing runtime has no saved VillageTrial: {target}")
    for folder in (target / "config", target / "saves", target / "saves/VillageTrial/villagers"):
        if folder.is_symlink():
            raise ValueError(f"Runtime mutable state must not be linked: {folder}")
    return True


def prepare(args: argparse.Namespace) -> dict:
    if args.dry_run:
        return _prepare(args)
    runs = canonical_child(args.runs_dir, ROOT)
    with ExitStack() as guards:
        guards.enter_context(runtime_lock(PACK_RUNTIME))
        for role in sorted(set(args.roles)):
            if role not in ROLES:
                raise ValueError("Unknown runtime role")
            target = canonical_child(runs / role, runs)
            if target.exists():
                guards.enter_context(runtime_lock(target))
        return _prepare(args)


def _prepare(args: argparse.Namespace) -> dict:
    snapshot = args.source_snapshot.expanduser().resolve(strict=True)
    if not snapshot.is_relative_to(INTERNAL) or "saves" in snapshot.parts:
        raise ValueError("Use a stopped world snapshot on the internal drive outside every active saves/ directory")
    if not snapshot.is_dir() or not (snapshot / "level.dat").is_file():
        raise ValueError("--source-snapshot must name one saved world directory containing level.dat")
    runs = canonical_child(args.runs_dir, ROOT)
    if snapshot == runs or snapshot.is_relative_to(runs) or runs.is_relative_to(snapshot):
        raise ValueError("Snapshot and runtime destinations must not overlap")
    if args.tps not in TPS:
        raise ValueError("Training TPS must be 20, 40, or 80")
    roles = list(dict.fromkeys(args.roles))
    if not roles or any(role not in ROLES for role in roles):
        raise ValueError("Choose adaptive, frozen, and/or play roles")
    for candidate in (PACK_RUNTIME, *(runs / role for role in roles)):
        if pids := live_clients(candidate):
            raise ValueError(f"Stop the client using {candidate} before preparation; PID(s) {pids}")
    with stopped_world(snapshot):
        world_files = tree_files(snapshot)
        source_hash = tree_digest(world_files)
        assets, pack_provenance = verified_assets(PACK_RUNTIME)
        settings = settings_files(PACK_RUNTIME)
        plans = []
        for role in roles:
            target = canonical_child(runs / role, runs)
            tps = 20 if role == "play" else args.tps
            exists = existing_runtime(target, source_hash, role, tps)
            plans.append({"role": role, "run_dir": str(target), "requested_tps": tps,
                          "learning": "frozen" if role == "frozen" else "adaptive",
                          "action": "unchanged" if exists else "create"})
        result = {"dry_run": args.dry_run, "source_snapshot": str(snapshot),
                  "source_world_sha256": source_hash, "world_files": len(world_files),
                  "world_bytes_per_client": sum(record["size"] for record in world_files.values()),
                  "shared_verified_assets": len(assets), "independent_settings_files": len(settings), "clients": plans}
        if args.dry_run:
            return result
        runs.mkdir(parents=True, exist_ok=True)
        lock_path = runs / ".prepare.lock"
        if lock_path.is_symlink():
            raise ValueError("Preparation lock must not be a symlink")
        with lock_path.open("a+b") as preparation_lock:
            try:
                fcntl.flock(preparation_lock.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
            except BlockingIOError as failure:
                raise ValueError("Another preparation is in progress") from failure
            cache = asset_cache(PACK_RUNTIME, runs, assets)
            for plan in plans:
                if plan["action"] == "unchanged":
                    continue
                target = Path(plan["run_dir"])
                # Recheck after the lock: a racing preparer must never replace a client.
                if existing_runtime(target, source_hash, plan["role"], plan["requested_tps"]):
                    plan["action"] = "unchanged"
                    continue
                staging = Path(tempfile.mkdtemp(prefix=f".{plan['role']}-prepare-", dir=runs))
                try:
                    copy_files(snapshot, staging / "saves/VillageTrial", world_files)
                    copy_files(PACK_RUNTIME, staging, settings)
                    for relative, cached in cache.items():
                        link = staging / relative
                        link.parent.mkdir(parents=True, exist_ok=True)
                        if Path(relative).parts[0] == "resourcepacks":
                            # Minecraft rejects resourcepack symlinks. Keep verified bytes regular and read-only.
                            try:
                                os.link(cached, link)
                            except OSError:
                                shutil.copyfile(cached, link)
                                link.chmod(0o444)
                        else:
                            link.symlink_to(cached)
                    (staging / "evidence").mkdir()
                    provenance = dict(pack_provenance)
                    provenance["runtime"] = str(target)
                    provenance["parallel_pack_source"] = str(PACK_RUNTIME)
                    provenance["parallel_source_provenance_sha256"] = digest(PACK_RUNTIME / "pack-provenance.json")
                    provenance["shared_readonly_assets"] = assets
                    provenance["independently_copied_settings"] = settings
                    (staging / "pack-provenance.json").write_text(json.dumps(provenance, indent=2) + "\n")
                    manifest = {"schema_version": 1, "prepared_at_utc": datetime.now(timezone.utc).isoformat(),
                                "runtime": str(target), "role": plan["role"], "requested_tps": plan["requested_tps"],
                                "learning": plan["learning"], "source_snapshot": str(snapshot),
                                "source_world_sha256": source_hash, "source_world_files": world_files,
                                "initial_settings_sha256": tree_digest(settings), "initial_settings_files": settings,
                                "vm_properties": {"villagers.role": plan["role"], "villagers.learning": plan["learning"],
                                                  "villagers.training.tps": str(plan["requested_tps"]), "villagers.trial": "village"},
                                "world_name": "VillageTrial", "mutable_data_shared": False,
                                "asset_cache": str(runs / ".assets"), "shared_assets": {relative: dict(record, storage="regular" if Path(relative).parts[0] == "resourcepacks" else "symlink")
                                          for relative, record in assets.items()},
                                "notes": "Common saved starting world; process session.lock is omitted and recreated independently. No worlds, configurations, evidence or minds are symlinked. TPS is requested, not proof of achieved throughput."}
                    (staging / "experiment.json").write_text(json.dumps(manifest, indent=2) + "\n")
                    # Refuse a changing source; publish only complete verified clients.
                    if tree_files(snapshot) != world_files:
                        raise ValueError("Source snapshot changed during preparation; staged client was not published")
                    if target.exists():
                        raise ValueError(f"Destination appeared during staging: {target}")
                    staging.rename(target)
                finally:
                    if staging.exists():
                        shutil.rmtree(staging)
            summary = {key: value for key, value in result.items() if key != "dry_run"}
            (runs / "experiment-summary.json").write_text(json.dumps(summary, indent=2) + "\n")
        return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-snapshot", type=Path, required=True,
                        help="Stopped archived world directory outside saves/, containing level.dat")
    parser.add_argument("--runs-dir", type=Path, default=ROOT / "runs")
    parser.add_argument("--roles", nargs="+", choices=ROLES, default=list(ROLES))
    parser.add_argument("--tps", type=int, choices=TPS, default=40, help="Adaptive/frozen requested TPS; play always uses 20")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    try:
        print(json.dumps(prepare(args), indent=2))
    except (OSError, ValueError, KeyError) as error:
        sys.exit(str(error))


if __name__ == "__main__":
    main()
