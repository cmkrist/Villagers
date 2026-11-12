# Owned-grain priority and calm nighttime refuge

The dragon goal remains incomplete. No iron or dimension progression is proved
here. Actual primary remains the205346 checkpoint; a fresh body audit retained
179 humanoids and33 managed villagers, all20HP. No copied population was promoted.

## Implemented behavior

An urgent owner of at least three wheat first attempts its real workbench route,
retains that task while waiting for navigation admission, and crafts at actual
reach before selecting another supply trip or waiting for a food-request reply.
Explicitly blocked stations still permit food access/help fallbacks. Nearby real
meal handoffs and owned-food consumption retain their earlier resolution.

Bedless villagers seeking a golem during calm nighttime conditions walk rather
than sprint. On supported ground, within eight blocks of a visible friendly
guardian, they can stop and use the existing rest behavior. Actual threats/fire
still take the existing danger branch; no hunger, health, damage or clock setters
were introduced. The policy boundary has a regression test covering danger,
visibility and invalid distances.

## Actual native observations

- Frozen Willow station audit: complete native routes exist to supported stands
  with real reach and sight to the remembered bench at(644,99,296). No world
  simulation before the snapshot; source hashes unchanged.
- Old artifact baseline: Willow eventually crafted/ate three bread at2641ticks,
  ending5HP/15food. This artifact differs elsewhere from the later stack, so this
  is not a controlled timing benchmark.
- First priority attempt: still stopped240ticks later at4HP while waiting for a
  nearby food reply. That upstream wait preceded the new owned-grain branch.
- `native-willow-grain-before-reply-v3`: existing saved Willow starts7HP/0food
  with9wheat. It reaches the actual bench, crafts three bread and consumes all
  three by188ticks, ending6HP/15food. No resource/body setters; source unchanged.
- Full population before the calm-rest correction: native all-resident mining
  rehearsal stops around15360ticks with Willow7HP/0food, empty inventory atY91.
  A focused trace then shows prolonged bedless nighttime golem pursuit.
- `native-willow-calm-guardian-cycle-v5`: all33 residents loaded. Willow ends
  21480ticks at20HP/18food with9wheat+2bread and three bread crafted. However the
  run FAILS its full-population guard: Fern-d349 ends8HP/0food, carrying9wheat
  and10sand at(657.288,86,306.654), remembering the same bench13blocks higher.
  All179 humanoid identities remain in that copy. This is improvement for Willow,
  not sustained feeding proof for all settlements. Do not deploy/promote the copy.

## Verification and next action

Isolated Java build/tests and14 Python checks pass. The native runtime JAR hashes,
actor snapshots and terminal outcomes are retained in [results](results.json).
Each runtime also retains its copied production JAR and `artifact-pins.json`.

`food-access-audit.py --focus-resident <UUID>` now supports frozen bench/stand
queries or bounded normal-AI observation, with a JSONL history. Operator station
queries do not populate the actor's station cache. `--focus-cycle --focus-ticks`
keeps observing after the first meal and applies the all-resident health guard;
the full window alone does not establish sustainable production.

Next inspect Fern's actual route/passage from the lower floor to its remembered
bench, using the healthy primary for longer acceptance and stopped copies for
diagnosis. The damaged copy must never replace the primary or receive recovery
resources/body setters.
