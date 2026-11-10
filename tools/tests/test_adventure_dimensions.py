import importlib.util
from pathlib import Path
import struct
import unittest

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('adventure_dimensions', ROOT / 'tools/enable-adventure-dimensions.py')
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class AdventureDimensionsTest(unittest.TestCase):
    def root(self, generator='villagers:war'):
        return MODULE.from_json({'data': {'dimensions': {
            'minecraft:overworld': {'generator': {'type': generator}},
            'example:custom': {'type': 'example:existing'},
        }}})

    def definitions(self):
        import json
        return json.loads((ROOT / 'src/main/resources/data/villagers/worldgen/world_preset/four_biomes_war.json').read_text())['dimensions']

    def test_unknown_nbt_values_round_trip_without_numeric_or_text_changes(self):
        nodes = [(str(tag).encode(), (tag, bytes(range(size)))) for tag, size in [(1, 1), (2, 2), (3, 4), (4, 8), (5, 4), (6, 8)]]
        nodes += [(b'string', (8, b'\xc0\x80\xff')), (b'bytes', (7, struct.pack('>i', 2) + b'\x00\xff')),
                  (b'ints', (11, struct.pack('>iii', 2, -1, 2))), (b'longs', (12, struct.pack('>iq', 1, -9223372036854775808))),
                  (b'list', (9, (10, [(10, [(b'key', (8, b'value'))])])))]
        raw = MODULE.write_nbt(b'root', (10, nodes))
        self.assertEqual(raw, MODULE.write_nbt(*MODULE.read_nbt(raw)))

    def test_missing_dimensions_are_added_without_replacing_existing_data(self):
        root = self.root()
        MODULE.child(root, 'data')[1].append((b'seed', (4, struct.pack('>q', -123456789))))
        raw = MODULE.write_nbt(b'', root)
        upgraded, added = MODULE.add_dimensions(raw, self.definitions())
        self.assertEqual(['minecraft:the_nether', 'minecraft:the_end'], added)
        _, result = MODULE.read_nbt(upgraded)
        self.assertEqual(MODULE.child(MODULE.child(root, 'data'), 'seed'), MODULE.child(MODULE.child(result, 'data'), 'seed'))
        before = MODULE.child(MODULE.child(root, 'data'), 'dimensions')
        after = MODULE.child(MODULE.child(result, 'data'), 'dimensions')
        for key in ('minecraft:overworld', 'example:custom'):
            self.assertEqual(MODULE.child(before, key), MODULE.child(after, key))
        self.assertEqual((upgraded, []), MODULE.add_dimensions(upgraded, self.definitions()))

    def test_non_scenario_world_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'Four Biomes'):
            MODULE.add_dimensions(MODULE.write_nbt(b'', self.root('minecraft:noise')), self.definitions())

    def test_existing_nether_is_preserved(self):
        root = self.root()
        dims = MODULE.child(MODULE.child(root, 'data'), 'dimensions')
        custom = MODULE.from_json({'type': 'example:custom_nether'})
        dims[1].append((b'minecraft:the_nether', custom))
        result, added = MODULE.add_dimensions(MODULE.write_nbt(b'', root), self.definitions())
        self.assertEqual(['minecraft:the_end'], added)
        restored = MODULE.child(MODULE.child(MODULE.read_nbt(result)[1], 'data'), 'dimensions')
        self.assertEqual(custom, MODULE.child(restored, 'minecraft:the_nether'))
