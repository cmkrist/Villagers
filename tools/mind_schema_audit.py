"""Reject a simulation artifact that cannot restore its source world's minds."""
import json
import re
import subprocess


def audit_world_minds(world, supported_schema):
    files = sorted((world / 'villagers').glob('*/mind.json'))
    versions = {}
    incompatible = []
    for path in files:
        data = json.loads(path.read_text())
        version = data.get('schemaVersion')
        if type(version) is not int or not 1 <= version <= supported_schema:
            incompatible.append(str(path.relative_to(world)))
        else:
            versions[str(version)] = versions.get(str(version), 0) + 1
    if incompatible:
        raise ValueError(f'Production JAR supports mind schemas 1..{supported_schema}, '
                         f'but {len(incompatible)} saved minds are incompatible; '
                         f'first: {incompatible[0]}. Refusing fallback minds or reset food.')
    return {'supportedSchema': supported_schema, 'savedMinds': len(files),
            'savedSchemaCounts': versions, 'incompatibleMinds': []}


def audit_jar_minds(world, production_jar, javap):
    output = subprocess.run([str(javap), '-classpath', str(production_jar), '-constants',
                             'local.villagers.mind.AgentMind'], check=True,
                            text=True, capture_output=True).stdout
    match = re.search(r'public static final int SCHEMA_VERSION = (\d+);', output)
    if match is None:
        raise ValueError('Cannot verify the production JAR mind schema; refusing simulation')
    return audit_world_minds(world, int(match.group(1)))
