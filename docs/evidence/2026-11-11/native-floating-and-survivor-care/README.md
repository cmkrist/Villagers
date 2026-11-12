# Native floating and guarded survivor care

Actual primary outcome: one death in v108, followed by a clean guarded v116.
At gameTime287173 there are186saved humanoids and56managed villagers, all alive.
No copied world was promoted, no body was replaced or resurrected, and the
historical founder record still contains Juniper-e9b7, who drowned in v108.

Actual v108 ran4,000ticks. Pip-4c3c healed6→20HP and Pip-460a delivered four real
meals, but founder Juniper-e9b7 drowned at(-603.47,47.75,-613.47). The normal save
completed and the post-save UUID audit caught187→186. `v108-death-log.txt`
contains the native death message. Source/authored-save hashes were unchanged.
This is a failed population-preservation outcome, retained honestly.

Root cause: our managed goal replacement disabled the existing native FloatGoal.
The code now runs that existing goal when its native condition holds, preserving
ordinary JumpControl/physics/air. Managed creatures prioritize finding visible
dry shore over work/rest. Their reports include actual air and native float
ticks. Actual and copied observation guards now cover all loaded villagers and
illagers, with a shared health/depleted-air boundary checked every tick. Guards
freeze and save; they do not alter health, damage, air or movement.

535Java tests pass, including recovery-call and health/air boundary regressions.
`build-tests.json` pins the verified copied-runtime JAR. Actual clients used
recorded development classes, not that JAR. No live water encounter occurred in
the successful windows; zero nativeFloatTicks means these runs do NOT prove an
underwater rescue. This limitation remains explicit.

Copied v107 used the earlier recovery-call fix:12real gifts and full recovery
of three previously critical camp residents, then health_stop7640 for Dara-f13a.
Dara is in the SAME camp (an earlier narrative misidentified her settlement).
v110 with the new guard stopped at3638 for Dara4HP and retained every186UUID.
The guard prevented a blind continuation, but did not solve food access.

Copied v112, from a retained PRE-LOSS diagnostic source, passed6k with all187
identities and nine real gifts. It is not a restored primary. v114 from actual
v108 passed1k with186→186 and three gifts; its full source-hash check completed
before actual v116 resumed.

Actual v116 passed1k with186→186, two real beetroot gifts, minimum managedHP7
(up from6), no new UUIDs, source/authored-save hashes unchanged and simulation16
verified. Birch-67f3 reached7HP/18food through normal eating and metabolism.
Current Pip has24HP/17food, bag1beetroot/6wheat/1bread and held1bread.
Dara-f13a remains20HP/2food and urgently needs actual food access.

Frozen v115 confirms Dara and both historical donors share camp membership and
understand the actual urgent request words when their saved minds are loaded.
An earlier diagnostic false result came from not waiting for the saved donor
mind. Twelve bounded native listening-route batches each found no complete
safe route. Hearing approach now targets a real listening range, with a
one-block margin around the public sound block center, and waits220ticks;
actual gifts still require visible hand contact3. This change alone does not
prove Dara can be fed.

Frozen v117/v118 inspected525 physically supported listening stands. Eight
nearest target queries even WITHOUT body avoidance end after1–3nodes near
donor start. Raising only the diagnostic native search drop bound2→3 also
failed. Production drop bounds were not loosened. Further geometry/start/route
diagnosis is needed; do not assume language or other bodies are the cause.

No primary births, new armor, diamonds, Nether/End entry or dragon victory.
Renewable camp farming was postponed for this survival defect and is not yet
implemented. The long-running goal remains active.
