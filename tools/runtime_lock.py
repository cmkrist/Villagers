"""Cooperative runtime exclusion shared by preparation, repair, and the shell launcher."""
from contextlib import contextmanager
import fcntl
import os
from pathlib import Path
import stat
from runtime_config import live_clients


@contextmanager
def runtime_lock(runtime: Path, create: bool = False):
    runtime = runtime.absolute()
    if runtime.resolve() != runtime:
        raise ValueError(f"Runtime must be canonical, without symlink ancestors: {runtime}")
    if create:
        runtime.mkdir(parents=True, exist_ok=True)
    lock = runtime / ".villagers-client.lock"
    descriptor = os.open(lock, os.O_CREAT | os.O_RDWR | os.O_NOFOLLOW | os.O_NONBLOCK, 0o600)
    try:
        if not stat.S_ISREG(os.fstat(descriptor).st_mode):
            raise ValueError("Runtime lock must be a regular file")
        try:
            fcntl.flock(descriptor, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as error:
            raise ValueError(f"A client or preparation already owns {runtime}") from error
        if pids := live_clients(runtime):
            raise ValueError(f"A visible client still uses {runtime}: PID(s) {pids}")
        yield
    finally:
        os.close(descriptor)
