#!/usr/bin/env python3
"""Atomically queue quoted commands or stdin lines for the rendered VillagerLab."""
from __future__ import annotations

import argparse
import os
from pathlib import Path
import re
import sys
import tempfile

RUN = Path(__file__).resolve().parents[1] / "run"
NUMBER = r"-?[0-9]{1,6}(\.[0-9]{1,4})?"
ALLOWED = (
    r"villagers [A-Za-z0-9_ -]+",
    r"glossary",
    r"teach [a-z_]{1,32} [a-z]{2,12}",
    r"time (set|add) (day|night|noon|midnight|[0-9]{1,8})",
    r"weather (clear|rain|thunder)( [0-9]{1,6}[stm]?)?",
    r"gamemode (creative|spectator) @a",
    rf"tp @a ({NUMBER} ){{2,4}}{NUMBER}",
    r"screenshot [A-Za-z0-9_-]{1,64}",
    r"tick query",
    r"tick rate (20|40|80)",
    r"quit",
    r"resume",
)


def validate(lines: list[str]) -> list[str]:
    commands = [line.strip() for line in lines if line.strip() and not line.strip().startswith("#")]
    if not commands:
        raise ValueError("No commands supplied")
    if len(commands) > 32:
        raise ValueError("A batch may contain at most 32 commands")
    for command in commands:
        if len(command) > 512 or not any(re.fullmatch(pattern, command) for pattern in ALLOWED):
            raise ValueError(f"Unsupported lab command: {command!r}")
    if "quit" in commands[:-1]:
        raise ValueError("quit must be the final command in its batch")
    if len(("\n".join(commands) + "\n").encode("utf-8")) > 8_192:
        raise ValueError("Command batch exceeds the client's 8192-byte limit")
    return commands


def enqueue(commands: list[str], run: Path = RUN) -> Path:
    commands = validate(commands)
    # Resolve neither the runtime nor its ancestors through a symlink.
    if run.resolve() != run.absolute() or not run.is_dir():
        raise ValueError(f"Expected a prepared regular runtime directory: {run}")
    queue = run / "lab-command.txt"
    processing = run / "lab-command.processing.txt"
    if queue.exists() or queue.is_symlink() or processing.exists() or processing.is_symlink():
        raise ValueError("A lab command batch is pending; wait for consumption before queuing another")
    temporary: Path | None = None
    try:
        descriptor, name = tempfile.mkstemp(prefix=".lab-command-", suffix=".tmp", dir=run)
        temporary = Path(name)
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as stream:
            stream.write("\n".join(commands) + "\n")
            stream.flush()
            os.fsync(stream.fileno())
        # A hard link publishes the completed file atomically and fails if another
        # producer has queued a batch. os.replace() would silently lose that batch.
        os.link(temporary, queue)
        return queue
    except FileExistsError as error:
        raise ValueError("Another producer queued a batch; existing commands were preserved") from error
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run-dir", type=Path, default=Path(os.environ.get("VILLAGERS_RUN_DIR", str(RUN))),
                        help="Prepared client directory (default: VILLAGERS_RUN_DIR or run/)")
    parser.add_argument("commands", nargs="*", help="One complete quoted command per argument; omit to read stdin")
    args = parser.parse_args()
    try:
        if args.commands:
            lines = args.commands
        elif sys.stdin.isatty():
            parser.error("Supply quoted commands or pipe command lines on stdin")
        else:
            text = sys.stdin.read(8_193)
            if len(text.encode("utf-8")) > 8_192:
                raise ValueError("Command batch exceeds the client's 8192-byte limit")
            lines = text.splitlines()
        commands = validate(lines)
        requested_run = args.run_dir.expanduser().absolute()
        root = Path(__file__).resolve().parents[1]
        if not requested_run.is_relative_to(root):
            raise ValueError("The selected client must live inside the canonical project on the internal drive")
        path = enqueue(commands, requested_run)
        print(f"Queued {len(commands)} command(s) at {path}; execution is asynchronous.")
    except (OSError, ValueError) as error:
        sys.exit(str(error))


if __name__ == "__main__":
    main()
