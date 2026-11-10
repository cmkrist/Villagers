#!/usr/bin/env python3
"""Add missing vanilla Nether/End definitions to a stopped FourBiomesWar save, retaining raw NBT values."""
import argparse
import fcntl
import gzip
import hashlib
import io
import json
from pathlib import Path
import struct
import time


def read_nbt(raw):
    stream = io.BytesIO(raw)

    def take(n):
        if n < 0 or n > len(raw):
            raise ValueError('Invalid NBT length')
        result = stream.read(n)
        if len(result) != n:
            raise ValueError('Truncated NBT')
        return result

    def string():
        return take(struct.unpack('>H', take(2))[0])

    def node(tag):
        if tag in (1, 2, 3, 4, 5, 6):
            return tag, take({1: 1, 2: 2, 3: 4, 4: 8, 5: 4, 6: 8}[tag])
        if tag in (7, 11, 12):
            header = take(4)
            count = struct.unpack('>i', header)[0]
            return tag, header + take(count * {7: 1, 11: 4, 12: 8}[tag])
        if tag == 8:
            return tag, string()
        if tag == 9:
            subtype = take(1)[0]
            count = struct.unpack('>i', take(4))[0]
            if count < 0 or count > len(raw):
                raise ValueError('Invalid list length')
            return tag, (subtype, [node(subtype) for _ in range(count)])
        if tag == 10:
            values = []
            while (subtype := take(1)[0]) != 0:
                key = string()
                values.append((key, node(subtype)))
            return tag, values
        raise ValueError(f'Unsupported NBT tag {tag}')

    tag = take(1)[0]
    name = string()
    root = node(tag)
    if stream.read(1):
        raise ValueError('Trailing NBT data')
    return name, root


def write_nbt(name, root):
    def string(value):
        return struct.pack('>H', len(value)) + value

    def value(node):
        tag, data = node
        if tag in (1, 2, 3, 4, 5, 6, 7, 11, 12):
            return data
        if tag == 8:
            return string(data)
        if tag == 9:
            subtype, elements = data
            return bytes([subtype]) + struct.pack('>i', len(elements)) + b''.join(value(item) for item in elements)
        if tag == 10:
            return b''.join(bytes([item[0]]) + string(key) + value(item) for key, item in data) + b'\0'
        raise ValueError(tag)

    return bytes([root[0]]) + string(name) + value(root)


def child(node, key):
    if node[0] != 10:
        raise ValueError('Expected compound')
    return next(item for name, item in node[1] if name == key.encode('ascii'))


def from_json(value):
    if isinstance(value, str):
        return 8, value.encode('utf-8')
    if isinstance(value, dict):
        return 10, [(key.encode('ascii'), from_json(item)) for key, item in value.items()]
    raise ValueError('Dimension definitions must contain only strings and objects')


def add_dimensions(raw, definitions):
    name, root = read_nbt(raw)
    if write_nbt(name, root) != raw:
        raise ValueError('NBT round trip changed original values')
    settings = child(root, 'data')
    dimensions = child(settings, 'dimensions')
    if child(child(child(dimensions, 'minecraft:overworld'), 'generator'), 'type') != (8, b'villagers:war'):
        raise ValueError('Only the Four Biomes War generator can be upgraded')
    added = []
    for key in ('minecraft:the_nether', 'minecraft:the_end'):
        if not any(name == key.encode('ascii') for name, _ in dimensions[1]):
            dimensions[1].append((key.encode('ascii'), from_json(definitions[key])))
            added.append(key)
    return write_nbt(name, root), added


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run-dir', type=Path, required=True)
    args = parser.parse_args()
    project = Path(__file__).resolve().parents[1]
    run = args.run_dir.resolve()
    if not run.is_relative_to(project / 'runs'):
        raise ValueError('Use a prepared runtime inside runs/')
    world = run / 'saves/FourBiomesWar'
    if json.loads((world / 'four-biomes-war.json').read_text())['state'] != 'active':
        raise ValueError('The existing scenario must be active')
    file = world / 'data/minecraft/world_gen_settings.dat'
    if file.is_symlink():
        raise ValueError('World settings must be a regular file')
    with (run / '.villagers-client.lock').open('a+b') as client, (world / 'session.lock').open('r+b') as session:
        fcntl.flock(client, fcntl.LOCK_EX | fcntl.LOCK_NB)
        fcntl.lockf(session, fcntl.LOCK_EX | fcntl.LOCK_NB)
        original = file.read_bytes()
        definitions = json.loads((project / 'src/main/resources/data/villagers/worldgen/world_preset/four_biomes_war.json').read_text())['dimensions']
        upgraded, added = add_dimensions(gzip.decompress(original), definitions)
        if not added:
            print('Standard adventure dimensions already present; no changes.')
            return
        archive = project / 'runs/archives' / ('adventure-dimensions-' + str(time.time_ns()))
        archive.mkdir(parents=True)
        (archive / file.name).write_bytes(original)
        output = gzip.compress(upgraded, mtime=0)
        temporary = file.with_name(file.name + '.adventure.tmp')
        with temporary.open('xb') as stream:
            stream.write(output)
            stream.flush()
            import os
            os.fsync(stream.fileno())
        if file.read_bytes() != original:
            raise ValueError('World settings changed during preparation')
        temporary.replace(file)
        receipt = {'world': str(world), 'backup': str(archive / file.name), 'added': added,
                   'beforeSha256': hashlib.sha256(original).hexdigest(), 'afterSha256': hashlib.sha256(output).hexdigest()}
        (archive / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
        print(json.dumps(receipt, indent=2))


if __name__ == '__main__':
    main()
