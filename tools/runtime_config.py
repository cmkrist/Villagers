#!/usr/bin/env python3
"""Validate a selected visible-client runtime without exposing account/session arguments."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]
ROLES = ("adaptive", "frozen", "play")
TPS = (20, 40, 80)


def live_clients(runtime: Path) -> list[int]:
    """Inspect only game-directory identity; never print full process arguments."""
    found = []
    for proc in Path("/proc").iterdir():
        if not proc.name.isdigit():
            continue
        try:
            args = (proc / "cmdline").read_bytes().decode(errors="replace").split("\0")
            if not args or Path(args[0]).name != "java":
                continue
            cwd = (proc / "cwd").resolve(strict=True)
            if "--gameDir" in args:
                index = args.index("--gameDir")
                game = Path(args[index + 1])
                game = game if game.is_absolute() else cwd / game
                if game.resolve() == runtime:
                    found.append(int(proc.name))
            elif cwd == runtime and any("devlaunchinjector.Main" in arg or "KnotClient" in arg for arg in args):
                found.append(int(proc.name))
        except (OSError, ValueError, IndexError):
            continue
    return found


def settings(environment: dict[str, str] | None = None, check_live: bool = True) -> dict:
    env = dict(os.environ if environment is None else environment)
    runtime = Path(env.get("VILLAGERS_RUN_DIR", str(ROOT / "run"))).expanduser()
    if not runtime.is_absolute():
        runtime = ROOT / runtime
    runtime = runtime.absolute()
    if runtime.resolve() != runtime or not runtime.is_relative_to(ROOT) or runtime == ROOT:
        raise ValueError("VILLAGERS_RUN_DIR must be a canonical directory inside this internal-drive project")
    if "\n" in str(runtime) or not runtime.is_dir() or not (runtime / "pack-provenance.json").is_file():
        raise ValueError(f"Client directory is not prepared: {runtime}")
    manifest_path = runtime / "experiment.json"
    manifest = json.loads(manifest_path.read_text()) if manifest_path.exists() else {}
    role = env.get("VILLAGERS_ROLE", manifest.get("role", "adaptive"))
    if role not in ROLES:
        raise ValueError("VILLAGERS_ROLE must be adaptive, frozen, or play")
    if manifest.get("role", role) != role:
        raise ValueError("Selected role does not match this runtime's experiment manifest")
    tps = int(env.get("VILLAGERS_TPS", str(manifest.get("requested_tps", 20))))
    if tps not in TPS:
        raise ValueError("VILLAGERS_TPS must be 20, 40, or 80")
    if role == "play":
        tps = 20
    if check_live and (pids := live_clients(runtime)):
        raise ValueError(f"A visible client already uses {runtime}: PID(s) {pids}")
    return {"run_dir": str(runtime), "role": role, "requested_tps": tps,
            "learning": "frozen" if role == "frozen" else "adaptive",
            "separate_build": runtime != ROOT / "run"}


def validate_shared_assets(runtime: Path, manifest: dict) -> None:
    for relative, record in manifest.get("shared_assets", {}).items():
        path = Path(relative)
        if not path.parts or path.is_absolute() or ".." in path.parts:
            raise ValueError(f"Invalid shared asset path: {relative}")
        asset = runtime / path
        resourcepack = path.parts[0] == "resourcepacks"
        regular = resourcepack or record.get("storage") == "regular"
        if regular:
            if asset.is_symlink() or not asset.is_file() or asset.resolve() != asset.absolute():
                raise ValueError(f"Resourcepacks require regular files; repair this stopped runtime: {asset}")
        elif not asset.is_symlink():
            raise ValueError(f"Invalid shared asset link: {asset}")
        target = asset.resolve(strict=True)
        if not target.is_relative_to(ROOT) or not target.is_file():
            raise ValueError(f"Shared asset escapes canonical project: {asset}")
        if target.stat().st_mode & 0o222:
            raise ValueError(f"Shared asset is writable: {target}")
        with target.open("rb") as stream:
            actual = hashlib.file_digest(stream, "sha256").hexdigest()
        if actual != record["sha256"]:
            raise ValueError(f"Shared asset checksum changed: {target}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--shell-lines", action="store_true", help="Print run directory, role and TPS as three safe lines")
    parser.add_argument("--allow-live", action="store_true", help="Read settings even while a client uses the directory")
    parser.add_argument("--record-launch", action="store_true", help="Validate shared assets and record actual requested launch settings")
    args = parser.parse_args()
    try:
        result = settings(check_live=not args.allow_live)
        if args.record_launch:
            runtime = Path(result["run_dir"])
            manifest_path = runtime / "experiment.json"
            if manifest_path.exists():
                manifest = json.loads(manifest_path.read_text())
                validate_shared_assets(runtime, manifest)
            record = dict(result, requested_at_utc=datetime.now(timezone.utc).isoformat(),
                          status="launcher requested; not proof of client startup or achieved TPS")
            fd, temporary = tempfile.mkstemp(prefix=".launch-", dir=runtime)
            try:
                with os.fdopen(fd, "w") as stream:
                    json.dump(record, stream, indent=2)
                    stream.write("\n")
                os.replace(temporary, runtime / "launch-settings.json")
            finally:
                Path(temporary).unlink(missing_ok=True)
            with (runtime / "launches.jsonl").open("a") as stream:
                stream.write(json.dumps(record) + "\n")
        if args.shell_lines:
            print(result["run_dir"]); print(result["role"]); print(result["requested_tps"])
        else:
            print(json.dumps(result, indent=2))
    except (OSError, ValueError) as error:
        sys.exit(str(error))


if __name__ == "__main__":
    main()
