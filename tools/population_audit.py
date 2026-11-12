"""Compare actual saved bodies and living identities, including corpses still present in NBT."""
import math


def compare_population(before, after):
    def living(body):
        health = body.get('health')
        return isinstance(health, (int, float)) and math.isfinite(health) and health > 0

    original = {body['id'] for body in before['bodies']}
    actual = {body['id'] for body in after['bodies']}
    living_before = {body['id'] for body in before['bodies'] if living(body)}
    living_after = {body['id'] for body in after['bodies'] if living(body)}
    return {'beforeHumanoids': len(original), 'afterHumanoids': len(actual),
            'missingOriginalIds': sorted(original - actual), 'newHumanoidIds': sorted(actual - original),
            'beforeLivingHumanoids': len(living_before), 'afterLivingHumanoids': len(living_after),
            'newlyDeadOrMissingIds': sorted(living_before - living_after),
            'nonlivingAfterIds': sorted(actual - living_after)}
