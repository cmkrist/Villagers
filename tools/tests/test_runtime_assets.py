import argparse
import fcntl
import hashlib
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))
import runtime_config
import runtime_lock


def module(name, filename):
    spec = importlib.util.spec_from_file_location(name, TOOLS / filename)
    loaded = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(loaded)
    return loaded


repair = module("resourcepack_repair", "repair-resourcepacks.py")
prepare = module("pack_prepare", "prepare-client.py")
parallel = module("parallel_prepare", "prepare-parallel.py")


class RuntimeAssetsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        for owner in (runtime_config, repair, parallel):
            guard = patch.object(owner, "ROOT", self.root)
            guard.start(); self.addCleanup(guard.stop)
        self.run = self.root / "runs/play"
        self.run.mkdir(parents=True)
        (self.run / "pack-provenance.json").write_text("{}")
        self.cache = self.root / "runs/.assets"
        self.source = self.cache / "verified.zip"
        self.cache.mkdir()
        self.source.write_bytes(b"verified resourcepack bytes")
        self.source.chmod(0o444)
        self.target = self.run / "resourcepacks/pack.zip"
        self.target.parent.mkdir()
        self.target.symlink_to(self.source)
        mod = self.run / "mods/mod.jar"
        mod.parent.mkdir()
        mod.symlink_to(self.source)
        checksum = hashlib.sha256(self.source.read_bytes()).hexdigest()
        self.manifest = {"role": "play", "requested_tps": 20, "asset_cache": str(self.cache),
                         "shared_assets": {"resourcepacks/pack.zip": {"sha256": checksum}, "mods/mod.jar": {"sha256": checksum}}}
        (self.run / "experiment.json").write_text(json.dumps(self.manifest))
        self.save = self.run / "saves/VillageTrial/level.dat"
        self.save.parent.mkdir(parents=True)
        self.save.write_bytes(b"untouched world")

    def test_repair_produces_regular_readonly_identical_assets_and_preserves_world_and_mods(self):
        result = repair.repair(self.run)
        self.assertEqual(1, result["resourcepacks_to_materialize"])
        self.assertFalse(self.target.is_symlink())
        self.assertTrue(self.target.is_file())
        self.assertEqual(self.source.read_bytes(), self.target.read_bytes())
        self.assertEqual(0, self.target.stat().st_mode & 0o222)
        self.assertTrue((self.run / "mods/mod.jar").is_symlink())
        self.assertEqual(b"untouched world", self.save.read_bytes())
        marker = json.loads((self.run / "experiment.json").read_text())
        runtime_config.validate_shared_assets(self.run, marker)
        self.assertEqual(0, repair.repair(self.run)["resourcepacks_to_materialize"])

    def test_launcher_rejects_legacy_resourcepack_links_then_accepts_repair(self):
        with self.assertRaisesRegex(ValueError, "require regular files"):
            runtime_config.validate_shared_assets(self.run, self.manifest)
        repair.repair(self.run)
        runtime_config.validate_shared_assets(self.run, json.loads((self.run / "experiment.json").read_text()))
        self.target.chmod(0o644)
        with self.assertRaisesRegex(ValueError, "writable"):
            runtime_config.validate_shared_assets(self.run, self.manifest)

    def test_bad_checksum_aborts_before_replacing_any_link(self):
        second = self.run / "resourcepacks/bad.zip"
        second.symlink_to(self.source)
        self.manifest["shared_assets"]["resourcepacks/bad.zip"] = {"sha256": "0" * 64}
        (self.run / "experiment.json").write_text(json.dumps(self.manifest))
        with self.assertRaisesRegex(ValueError, "differ"):
            repair.repair(self.run)
        self.assertTrue(self.target.is_symlink())
        self.assertTrue(second.is_symlink())

    def test_shell_launcher_lock_prevents_repair_and_refresh_before_source_access(self):
        with (self.run / ".villagers-client.lock").open("a+b") as owner:
            fcntl.flock(owner, fcntl.LOCK_EX | fcntl.LOCK_NB)
            with self.assertRaisesRegex(ValueError, "already owns"):
                repair.repair(self.run)
            with patch.object(prepare, "RUN", self.run):
                args = argparse.Namespace(dry_run=False, source=self.root / "nonexistent", refresh=True)
                with self.assertRaisesRegex(ValueError, "already owns"):
                    prepare.prepare(args)
        self.assertTrue(self.target.is_symlink())

    def test_manual_live_client_is_refused_even_without_launcher_lock(self):
        with patch.object(runtime_lock, "live_clients", return_value=[12345]):
            with self.assertRaisesRegex(ValueError, "visible client still uses"):
                repair.repair(self.run)
        self.assertTrue(self.target.is_symlink())

    def test_parallel_preparation_respects_existing_runtime_lock(self):
        pack = self.root / "pack"
        pack.mkdir()
        args = argparse.Namespace(dry_run=False, runs_dir=self.root / "runs", roles=["play"])
        with patch.object(parallel, "PACK_RUNTIME", pack):
            with runtime_lock.runtime_lock(self.run):
                with self.assertRaisesRegex(ValueError, "already owns"):
                    parallel.prepare(args)

    def test_fresh_parallel_preparation_is_regular_for_packs_linked_for_mods_and_independent_for_worlds(self):
        import shutil
        pack = self.root / "pack"
        files = {}
        for relative in ("mods/mod.jar", "resourcepacks/pack.zip"):
            target = pack / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(self.source, target)
            files[relative] = {"sha256": hashlib.sha256(target.read_bytes()).hexdigest(), "size": target.stat().st_size}
        (pack / "pack-provenance.json").write_text(json.dumps({"files": files}))
        snapshot = self.root / "snapshot"
        snapshot.mkdir()
        (snapshot / "level.dat").write_bytes(b"independent saved world bytes")
        args = argparse.Namespace(dry_run=False, source_snapshot=snapshot, runs_dir=self.root / "new-runs", roles=["play"], tps=20)
        with patch.object(parallel, "PACK_RUNTIME", pack), patch.object(parallel, "INTERNAL", self.root):
            result = parallel.prepare(args)
            self.assertEqual("create", result["clients"][0]["action"])
            fresh = args.runs_dir / "play"
            self.assertFalse((fresh / "resourcepacks/pack.zip").is_symlink())
            self.assertTrue((fresh / "mods/mod.jar").is_symlink())
            self.assertEqual((snapshot / "level.dat").read_bytes(), (fresh / "saves/VillageTrial/level.dat").read_bytes())
            manifest = json.loads((fresh / "experiment.json").read_text())
            runtime_config.validate_shared_assets(fresh, manifest)
            self.assertEqual("unchanged", parallel.prepare(args)["clients"][0]["action"])

    def test_empty_manifest_path_is_refused_without_touching_assets(self):
        self.manifest["shared_assets"][""] = {"sha256": "0" * 64}
        (self.run / "experiment.json").write_text(json.dumps(self.manifest))
        with self.assertRaisesRegex(ValueError, "Invalid managed asset path"):
            repair.repair(self.run)
        self.assertTrue(self.target.is_symlink())

    def test_dry_run_does_not_change_links_or_manifest(self):
        before = (self.run / "experiment.json").read_bytes()
        self.assertEqual(1, repair.repair(self.run, dry_run=True)["resourcepacks_to_materialize"])
        self.assertTrue(self.target.is_symlink())
        self.assertEqual(before, (self.run / "experiment.json").read_bytes())
        self.assertFalse((self.run / ".villagers-client.lock").exists())

    def test_lock_symlink_and_escape_paths_are_rejected(self):
        outside = self.root / "outside"
        outside.write_bytes(b"preserve")
        (self.run / ".villagers-client.lock").symlink_to(outside)
        with self.assertRaises(OSError):
            repair.repair(self.run)
        self.assertEqual(b"preserve", outside.read_bytes())
        (self.run / ".villagers-client.lock").unlink()
        self.manifest["shared_assets"]["../outside"] = {"sha256": "0" * 64}
        (self.run / "experiment.json").write_text(json.dumps(self.manifest))
        with self.assertRaisesRegex(ValueError, "Invalid managed asset path"):
            repair.repair(self.run)
        self.assertTrue(self.target.is_symlink())


if __name__ == "__main__":
    unittest.main()
