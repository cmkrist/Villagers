# Player impressions and schema migration — November 7, 2026

The dedicated rendered **play** copy demonstrated useful-item handoff, accepted
player-attributed damage, attentive episodes, and one subsequent avoidance
episode. The same villager/player impression survived a client restart.
The adaptive/frozen comparison worlds were not used for these interventions.

The final build passed **44 tests**, with no failures, errors or skips. Its SHA-256
is `b979fbae8944aeea9d529c7725d033cd6c2fe77ad371835a5e1e9b28c92d36f5`;
all **54** reloaded play-client classes matched that JAR. See
[final-unit-tests.json](final-unit-tests.json) and
[final-build-artifact.json](final-build-artifact.json). First-session native events
were collected with the earlier 0.3.0 candidate recorded in
[build-artifact.json](build-artifact.json). The final candidate corrected
server-source report targeting to use the actual observer and supplied eleven
navigation rows after reload.

## Native events and reactions

The observer was `6609fea5-c92a-359b-acb7-1ab63051aa33`, and the selected original
resident was **Nell-e358**, `e358e923-f9d2-4d5b-a04b-b2531c8f02d9`. The first
[start record](first-social-events/start-1794031935143-eb853e55.json) shows the
unfamiliar-player prior: awe **0.85**, trust **0.10**, fear **0.15**, confidence
**0.05**, with no encounter notes. Normal exposure subsequently produced attention
episodes and small familiarity updates.

The [gift record](first-social-events/gift-1794031951631-b7e656a5.json) records an
explicit fixture intervention: one bread was supplied to the observer, then
passed through the actual `Villagers.offerGift` logic. Transfer succeeded,
resident bread increased from zero to one, and no bread remained in the observer's
hand. Trust changed from **0.11 to 0.29**, fear from **0.14 to 0.04**, and confidence
from **0.08 to 0.21**. This demonstrates a useful real transfer and its associated
update; it is not untouched natural food acquisition or proof of a person's
manual click.

The resident moved under its own control from a roof down to ground level before
the [harm intervention](first-social-events/harm-1794032008152-6660f72e.json).
Only the observer was repositioned. One native `player.attack` accepted **one
health point** of damage, leaving the resident at **19/20** health. Trust changed
from **0.29 to -0.36**, fear from **0.03 to 0.68**, and confidence from **0.24 to
0.59**. The trace contains `PLAYER_HURT`; no direct health or belief setter was
used. This was a controlled nonlethal play-world intervention, not a normal
survival outcome.

At the later
[re-encounter status](first-social-events/status-1794032068146-4f4e4585.json),
the same player retained negative trust and high fear. The runtime had recorded
four attention episodes and one accepted avoidance episode. These counters show
the social controller acting on the learned estimate. They do not attribute the
resident's entire travel distance to social avoidance; ordinary navigation and
immediate hurt response also ran.

## Retained legacy state

Before first launch, all **eleven** schema-1 play minds were copied byte-for-byte
into [legacy-minds](legacy-minds/). The
[legacy manifest](legacy-manifest.json) contains individual SHA-256 hashes,
identities, homes and policy comparison fields. Its aggregate source-set hash is
`dc60f158bdd523150b7f93aedc14d57a9597c1e7339052ccf3feb75d87d8e47d`.

The [migration audit](migration-audit.json) compares those originals with
independently frozen schema-2 records in [migrated-minds](migrated-minds/):

- All eleven UUIDs, names and personalities match exactly.
- Ten homes match exactly. Previously unhomed **Juniper-231e** acquired an existing
  generated bed at **(-269, 75, 733)**. That bed appears in the original terrain
  baseline, and the retained memory records the assignment. No original home was
  replaced in this exception.
- All learning-update counters and existing visit entries are nondecreasing.
  No complete action-value table reset or value change without new visits was
  found.
- Every event history remains within its bound. Drives, new outcomes, and bounded
  memory contents naturally change during play, so exact whole-file equality is
  not required after simulation.

This is a post-runtime retention audit, rather than instrumentation of the exact
conversion instant. The saved schema-2 bytes are archived with hashes so the
comparison remains reproducible as the play world develops.

## Restart and shutdown boundaries

The first client exited with **143**. Its queued save/quit commands were not
consumed, no clean shutdown was recorded, and the cause was not established.
[first-session-exit.json](first-session-exit.json) preserves that result. Autosaved
social state existed on disk; do not call this a successful graceful save.

The final candidate then reopened the same play world. At game time **9697**,
[after-reload-status.json](after-reload-status.json) matched the selected
player impression in [before-reload-mind.json](before-reload-mind.json), including
its seven typed notes, negative trust, high fear, and the same UUIDs. Health was
still **19**. Transient episode counters restarted independently; they are not the
persisted relationship record. The second session finished cleanly with
`BUILD SUCCESSFUL` and exit **0**. Rendered evidence, native events and logs are
retained in [rendered](rendered/), [final-social-events](final-social-events/),
and [reloaded-play-client.log](reloaded-play-client.log).

## Limits

Witnessed protection has an implemented visibility/threat hook but was **not
exercised in this visible run**. Witnessed harm is not implemented. The positive
approach threshold was unit-tested, rather than demonstrated by this single gift.
This test does not establish multi-player discrimination, many-agent capacity,
survival improvement, resource self-sufficiency, or personhood. The first exit
remains an unresolved negative result alongside the successful second shutdown.
