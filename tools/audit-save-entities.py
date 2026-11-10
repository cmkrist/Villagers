#!/usr/bin/env python3
"""Read actual stopped-world humanoid bodies (including boat passengers), inventories and their mind needs."""
import argparse
import fcntl
import gzip
import importlib.util
import json
from pathlib import Path
import struct
import uuid
import zlib


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('world', type=Path)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    world = args.world.resolve()
    spec = importlib.util.spec_from_file_location('typed_nbt', Path(__file__).with_name('enable-adventure-dimensions.py'))
    nbt = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(nbt)

    def plain(node):
        tag, value = node
        if tag == 10:
            return {key.decode(): plain(child) for key, child in value}
        if tag == 9:
            return [plain(child) for child in value[1]]
        if tag == 8:
            return value.decode()
        if tag in (1, 2, 3, 4, 5, 6):
            return struct.unpack('>' + {1: 'b', 2: 'h', 3: 'i', 4: 'q', 5: 'f', 6: 'd'}[tag], value)[0]
        count = struct.unpack('>i', value[:4])[0]
        if tag == 7:
            return list(value[4:])
        return list(struct.unpack('>' + {11: 'i', 12: 'q'}[tag] * count, value[4:]))

    def passengers(entity, vehicle=None):
        yield entity, vehicle
        for passenger in entity.get('Passengers', entity.get('passengers', [])):
            yield from passengers(passenger, {'type': entity.get('id'), 'position': entity.get('Pos', entity.get('pos'))})

    rows = []
    with (world / 'session.lock').open('rb') as lock:
        fcntl.lockf(lock, fcntl.LOCK_SH | fcntl.LOCK_NB)
        folders = list((world / 'dimensions').glob('*/*/entities'))
        if (world / 'entities').exists():
            folders.append(world / 'entities')
        for folder in folders:
            for region in sorted(folder.glob('*.mca')):
                data = region.read_bytes()
                for index in range(1024):
                    offset = int.from_bytes(data[index * 4:index * 4 + 4], 'big') >> 8
                    if not offset:
                        continue
                    start = offset * 4096
                    length = int.from_bytes(data[start:start + 4], 'big')
                    compression = data[start + 4]
                    payload = data[start + 5:start + 4 + length]
                    if compression == 2:
                        raw = zlib.decompress(payload)
                    elif compression == 1:
                        raw = gzip.decompress(payload)
                    elif compression == 3:
                        raw = payload
                    else:
                        raise ValueError(f'Unsupported region compression {compression}: {region}')
                    chunk = plain(nbt.read_nbt(raw)[1])
                    for root in chunk.get('Entities', chunk.get('entities', [])):
                        for entity, vehicle in passengers(root):
                            kind = entity.get('id')
                            if kind not in ('minecraft:villager', 'minecraft:pillager', 'minecraft:vindicator', 'minecraft:evoker'):
                                continue
                            identity = entity.get('UUID', entity.get('uuid'))
                            if isinstance(identity, list):
                                identity = str(uuid.UUID(int=sum((n & 0xffffffff) << (96 - 32 * i) for i, n in enumerate(identity))))
                            row = {'id': identity, 'type': kind, 'position': entity.get('Pos', entity.get('pos')),
                                   'health': entity.get('Health', entity.get('health')), 'tags': entity.get('Tags', entity.get('tags', [])),
                                   'noAI': bool(entity.get('NoAI', False)), 'invulnerable': bool(entity.get('Invulnerable', False)),
                                   'age': entity.get('Age', entity.get('age', 0)),
                                   'vehicle': vehicle,
                                   'inventory': entity.get('Inventory', entity.get('inventory', [])), 'equipment': entity.get('equipment', {}),
                                   'meetingPoint': entity.get('Brain', {}).get('memories', {}).get('minecraft:meeting_point'),
                                   'dimensionFolder': str(folder.relative_to(world))}
                            mind = world / 'villagers' / identity / 'mind.json'
                            if mind.exists():
                                memory = json.loads(mind.read_text())
                                row.update({'name': memory.get('name'), 'food': memory.get('food'), 'home': memory.get('home')})
                            rows.append(row)
        managed = [row for row in rows if 'villagers:managed' in row['tags']]
        trial = world / 'villagers/village-trial.json'
        originals = set(json.loads(trial.read_text()).get('cohort', {})) if trial.exists() else set()
        result = {'source': 'Stopped-world native entity NBT; bodies and nested passengers, not mind-file existence.',
                  'world': str(world), 'savedHumanoids': len(rows), 'managedVillagers': len(managed),
                  'originalCohortBodies': len(originals & {row['id'] for row in rows}),
                  'managedAtZeroFood': [row['id'] for row in managed if (row.get('food') or {}).get('foodLevel') == 0],
                  'bodies': rows}
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2) + '\n')
        print(json.dumps({key: value for key, value in result.items() if key not in ('bodies', 'source')}))
    else:
        print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
