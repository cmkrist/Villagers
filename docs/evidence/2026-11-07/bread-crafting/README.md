# Visible bread crafting — November 7, 2026

A visible isolated play-copy check verified one ordinary forage action crafting
and eating bread. The complete stopped play save was backed up before launch;
its path is in backup.txt. Existing adaptive/frozen/user clients were preserved.

## Result and supporting evidence

The selected resident was Nell-e358, UUID e358e923-f9d2-4d5b-a04b-b2531c8f02d9.
An explicitly logged operator fixture transferred three real wheat through the
regular sneak-gift path and placed one crafting table into an eligible air block.
No resident was teleported and no hunger setter or forced crafting action ran.
The normal controller subsequently found the table, resolved the server recipe,
consumed the three wheat, produced one bread and ate it. Food rose from16 to20
(the bread's native nutrition was capped by the food-bar maximum). The inventory
then contained neither wheat nor bread. The table remained in the world. A bounded
mind-history entry, retained after clean shutdown, records the craft location.

- results.json summarizes the grounded outcome.
- crafting-reports/craft-1794037626520.json records provisioning: wheat3, food16,
  craftedDelta0. craft-1794037643016.json records wheat0, food20, craftedDelta1.
- crafted-mind.json retains the saved craft memory.
- commands.jsonl and client-final.log retain native results under the actual
  observer UUID. `/villagers status`, `/villagers language`, `/glossary`, and
  `/teach bread mora` executed as the observer. The lesson was reinforced but
  did not immediately replace the resident's inherited word; that is expected.
- bread-setup and bread-after PNGs were rendered by the real client. The after
  image was inspected: daytime village, fixture station and empty observer hand.
  It does not independently show the inventory transaction; server reports and
  the saved memory supply that evidence.

The first approach/provisioning attempts were rejected before any fixture items
or table were created, because the observer was out of reach or lacked sight.
The existing social probe then safely moved only the observer near its selected
resident. Additional explicit intervention: time set day for observability. These
commands and supplies make this a capability check, not natural-supplies survival
or a sustainable farming economy result.

The provisioning report also directly observed several nearby wheat crops at
age0. This is a post-harvest block observation, but it was not synchronized to the
specific harvest that reset each plant. Immediate matched harvest/replant evidence
remains pending; the earlier smoke established native harvest counters and wear.

## Scope and limitations

The source passed146 tests with no failures/errors/skips before launch. The final
play build compiled the additional preflight range check and loaded the same
crafting implementation. Third-party JOML emits a runtime Unsafe deprecation
warning in engine-backed inventory tests; no compiler warning appeared. Source
hashes are recorded in source-sha256.json. No release artifact was published.

Bread crafting requires real wheat, a nearby loaded crafting table, sight/reach,
a matching native recipe, supported bread output/byproducts and sufficient
inventory capacity. It is a supplied capability, not learned recipe discovery.
Missing table access, sustained crop supply, worn-tool replacement, starvation
recovery, parenting/breeding, and population scaling remain open. The existing
resourcepack-symlink warnings still prevent a full pack-parity claim.

## Closure and custody

Handle7110 ended with exit0 after consumed save/quit commands. Wrapper690512,
daemon690622 and client690768 are all absent; exact uid/start/executable/cwd/scope
are in owned-launch.json and terminal-custody.json. Queue empty, play lock free.
The six original historical jobs retained their exact start identities. No new
coordination workers or automated build/release timers were created.

The head received launch/change-of-readiness and closure notices. No hash hold,
execution or exception transfer was inferred. The earlier07:42–07:52 proposal
was explicitly not active; future coordination needs fresh mutual endpoints.
