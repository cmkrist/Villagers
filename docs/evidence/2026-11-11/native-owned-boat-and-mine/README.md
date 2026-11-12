# Existing owned boat and recorded mine recovery

Primary remains actual v205 until the separately recorded v213 run completes. These are copied-world results; no copied save was promoted.

- v206 audited the existing boat 02ee2e79-0b8f-4d23-8bbe-5e504d5cabc6 at (-464.929,62.523,-369.184). The old launch bank was outside normal boarding reach after the boat drifted.
- v207 preserved all192 residents but did not board; it exposed the lack of a route to an actually remembered shore when the hull was out of sight.
- v208 physically approached a replacement reachable bank, boarded the same owned hull, paddled320ticks and landed once. No boat recipe or new deployment occurred. It then crashed in MineStair.save because a reached recorded junction had no direction chosen yet. Saved bodies retained all192 alive; the run is not a successful rehearsal.
- The mine codec now explicitly persists pending direction selection, rejects invalid active directionless passages, and waits for a real selected direction before digging. Two regression tests cover the real crash and an active passage roundtrip.
- v209 resumed the actual saved v208 copy, completed4000ticks and physically walked home. All192 survived, no new/missing/dead UUIDs.
- v210 completed24000ticks/all192 alive, but did not board. The new shore search was waiting on a duplicated navigation admission. This was fixed by admitting exactly once inside the survey helper.
- v211 completed4000ticks from the stopped v209 world: one landing,380paddle ticks and a physical dry-ground return. All192 remained alive; no armor earned. Its final shore-search receipt is empty, so this is physical boat-cycle evidence, not a claim that a remembered-shore search necessarily executed in that replay.
- v212 completed4000ticks from v206 with two existing managed camp workers and experimental farming enabled. All192 remained alive; zero grass/logs/recipes/plants. No camp-farm success or births are claimed. Farming stays disabled in primary.

Each completed tool run also verified its source world unchanged. Per-run artifacts retain the exact observed receipts. Original runtime worlds/logs remain under runs/.
