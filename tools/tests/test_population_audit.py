import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from population_audit import compare_population


class PopulationAuditTest(unittest.TestCase):
    def test_a_corpse_with_the_same_uuid_is_a_lost_survivor(self):
        result = compare_population({'bodies': [{'id': 'resident', 'health': 20}]},
                                    {'bodies': [{'id': 'resident', 'health': 0}]})
        self.assertEqual(result['missingOriginalIds'], [])
        self.assertEqual(result['newlyDeadOrMissingIds'], ['resident'])
        self.assertEqual(result['afterLivingHumanoids'], 0)

    def test_a_new_adult_cannot_replace_an_original_identity(self):
        result = compare_population({'bodies': [{'id': 'original', 'health': 20}]},
                                    {'bodies': [{'id': 'replacement', 'health': 20}]})
        self.assertEqual(result['newlyDeadOrMissingIds'], ['original'])
        self.assertEqual(result['newHumanoidIds'], ['replacement'])

    def test_historical_death_and_real_healing_are_not_new_losses(self):
        result = compare_population({'bodies': [{'id': 'dead', 'health': 0}, {'id': 'alive', 'health': 7}]},
                                    {'bodies': [{'id': 'dead', 'health': 0}, {'id': 'alive', 'health': 20}]})
        self.assertEqual(result['newlyDeadOrMissingIds'], [])
        self.assertEqual(result['nonlivingAfterIds'], ['dead'])
