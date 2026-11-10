# Villagers 0.3.0 — experimental Fabric social update

For **Minecraft 26.3**, Fabric Loader **0.19.5+**, matching Fabric API, and the
pinned **Vanilla Perfected 1.0.0+26.3** pack. This build adds remembered impressions
of individual players: high initial awe **0.85** with low confidence **0.05**,
then changing trust, fear and confidence after grounded encounters. Respect is
not obedience.

## Play on this machine

From `/home/niko/.internal/source/Villagers`, run `./tools/play.sh`. It opens the
independent adaptive play copy at normal 20 TPS. Training worlds and the user's
Modrinth profile remain separate. The play copy begins from an earlier saved
checkpoint; it does not silently import the adaptive trial's latest learning.

- Sneak-right-click a managed villager with a sword, hoe, allowed food or planting
  item to transfer one real item. Your held stack decreases only on success.
- `/villagers impression` inspects the nearest managed villager's view of you.
  Awe, trust, fear and confidence are separate, player-specific estimates.
- `/villagers status` inspects activity; `/villagers adopt` enables the prototype
  for the nearest resident; `/villagers adoptnearby` adopts within 48 blocks.
- `/villagers release` restores vanilla AI while retaining the identity file.
- `/villagers play` stops acceleration in a development session; a frozen control
  retains its fixed policy. Use this separate play copy for adaptive interaction.

Existing tool, wooden-door, shelter and bounded crop/replant behavior remain.
Harvesting requires an actual hoe, matching planting material, a reachable ripe
supported crop and immediate replanting. General breaking and placement remain
disabled. Naturally present golems retain vanilla AI.

## Install or upgrade

With the destination instance closed, back up the complete world, including its
`villagers/` directory. Install only `villagers-0.3.0.jar`, replacing the older
Villagers JAR; keep Fabric API and the matching pack installed. Ordinary launches
do not start or accelerate a development trial. Adoption requires commands or
server operator permission.

The new **schema 2** preserves schema-1 identity, homes, personality, needs,
learned values/counters and history while adding bounded player impressions.
Old files start without fabricated encounters. **The 0.2.0 JAR cannot read schema
2**; restore the pre-upgrade world backup for a downgrade. The retained 0.2.0
release artifacts are separate from this update.

## Evidence and limits

Native successful useful gifts and actual player-attributed damage provide
evidence; failed interactions or debug belief edits do not. Witnessed protection
has its own visibility/threat checks; witnessed harm is not implemented.
A development probe's supplied
bread/nonlethal hit is an explicit intervention in the play copy, separate from
the untouched comparison. All **44 unit tests pass**. Visible full-pack testing
verified a real bread transfer, accepted unarmed damage, attention, a later
avoidance path, and exact per-player impression persistence after reopening.
The second session closed cleanly. The first exited 143 before its queued close;
its cause remains unconfirmed. [Evidence and limits](../../docs/evidence/2026-11-07/player-impressions/README.md).

Trusted approach and witnessed protective kills have automated/model or source
coverage, but have not yet been exercised in this visible smoke test. These
bounded scores are authored estimates, not calibrated probabilities.

This remains experimental. Survival improvement, a complete resource economy,
crafting, genetic evolution, language, and personhood are not established. The
paired accelerated run is a small controlled experiment, not a trained-model or
many-agent capability claim.
