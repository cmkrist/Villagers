import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from mind_schema_audit import audit_world_minds


class MindSchemaAuditTest(unittest.TestCase):
    def test_compatible_starving_mind_is_read_without_mutation(self):
        with tempfile.TemporaryDirectory() as directory:
            world = Path(directory)
            path = world / 'villagers/resident/mind.json'
            path.parent.mkdir(parents=True)
            original = json.dumps({'schemaVersion': 10, 'food': {'foodLevel': 0}})
            path.write_text(original)
            result = audit_world_minds(world, 10)
            self.assertEqual(result['savedSchemaCounts'], {'10': 1})
            self.assertEqual(path.read_text(), original)

    def test_newer_mind_rejects_older_artifact(self):
        with tempfile.TemporaryDirectory() as directory:
            world = Path(directory)
            path = world / 'villagers/resident/mind.json'
            path.parent.mkdir(parents=True)
            path.write_text(json.dumps({'schemaVersion': 10}))
            with self.assertRaisesRegex(ValueError, 'Refusing fallback minds'):
                audit_world_minds(world, 9)


if __name__ == '__main__':
    unittest.main()
