# Forestry prototype: native seed/clearance verified; log/plant cycle still open

This historical checkpoint binds273tests. Later [food-help work](../food-help/README.md)
adds hungry-work suspension and actual food sharing; current source pins now differ.
Its recorded starvation case remains unchanged evidence.

Current unreleased0.4.1-dev has finite protected mangrove jobs, native loot/real
inventory costs and paid planting code. **Native logs harvested=0, planted=0.**
Actual native evidence verifies one wild propagule retained, three explicitly
approved leaves removed, clean partial persistence and player-write revocation.
Read [rules](../../../design/forestry.md) and [verification](verification.json).

`tools/test.sh --no-daemon --max-workers=2` passes273Java+10Python, no failures,
errors or skips. Latest build: build-route-candidates.log. The508 recorded evidence
checks are mostly source/archive hashes plus native assertions, not508 new unit
tests. Pure material tests cover paid seed/log/plant conservation; native full
cycle, natural growth and second income cycle remain unverified.

## Worlds and inputs

The first runtime runs/forestry/play is an exact244-file stopped-world copy of
runs/home-route/play. The source was unchanged through its native trials. Earlier
food/tool/station/nursery interventions remain historical fixture inputs. This
forestry work supplied no new food, seed, log, tool, terrain, actor movement, age,
fatigue, health, growth or RNG setter. Observer teleports affect only the player.
Operator commands create/revoke exact finite authority and protection. One explicit
`time set day` in lower-logs/ is a labelled daylight fixture; it changes daytime,
not gameTime/age/cooldown. These are not sustainable day/night economy tests.

After the first continued fixture reached starvation, an independent exact copy
of the unchanged home-route world was made at runs/forestry-refresh/play. It was
not a reset of the first experiment. It supplied no resources or actor setters.
Its approved/ continuation used one further labelled `time set day`, plus explicit
three-leaf authority. Its next route-candidates/ continuation changed source only,
with read-only watch/status and no new grants, clock or supplies. Its final
revocation/ test is a separate257-file copy at runs/forestry-leaf-revocation/play;
the ongoing forestry-refresh source world remained unchanged during that copy's
test. No user world histories were merged or residents revived.

## Exact resource positions

Mature hanging seed(-269,74,721), supporting leaf(-269,75,721). Original logs
(-270,76,722)/(-270,77,722); explicit reassignment to lower logs
(-271,74,722)/(-271,75,722) retained the outstanding quota2 and real planting duty.
Plant cell(-269,72,723) with captured mud soil(-269,71,723) and air headroom.

Explicit clearance selected three leaves at(-270,73,723),(-269,74,723),
(-270,74,723). The fourth cell in the tiny approved cuboid was AIR and granted no
cut. Jobs and fresh identities are recorded in sites-before/after, command files
and launch logs; ordinary progress cannot grow authority. No roots were approved.

## Retained native runs

- Initial protected trial41336exit0: cutoff49397/final49400,600ticks. A direct
  same-state AIR write revoked the first grant. Protection over an approved log
  blocked the entire replacement job: no seed/log/plant cost or income.
- approved/91062exit0:2400ticks,cutoff51801/final51804. Removed only protection;
  one actual wild seed collected and retained. Original upper logs unreachable;
  native dusk correctly interrupted work. No log/plant completion.
- lower-logs/18317exit0:2400ticks,cutoff54205/final54208. Labelled daylight and
  explicit fresh lower-log authority. Actual seed retained; leaves occluded access.
- clearance/60532:2400ticks,cutoff56609/final56612. One approved leaf cut, then
  authority disappeared. Vanilla leaf scheduled ticks write even identical states;
  the prior broad outside-write hook cancelled adjacent clearance. Actor finished
  food0/health7. Evidence is preserved, not repaired. Native budget stop, normal
  save, BUILD SUCCESSFUL and exact captured identities absent establish terminal
  completion; numeric exec exit was unavailable after context recovery and is
  recorded NULL, not invented. Its unconsumed read-only status batch was retained
  and removed under the runtime lock.
- leaf-refresh/49873exit0:1000ticks,cutoff49797/final49800. Independent244-file
  source copy and native same-state AIR revocation/regrant. Another real wild seed
  collected. First clearance command failed loaded/proximity checks; its late
  retry was not consumed and is retained, not counted as an executed intervention.
- leaf-refresh/approved/66587exit0:1500ticks,cutoff51301/final51305. Labelled day,
  explicit3leaf clearance. All3 removed using native empty loot; seed1 retained,
  food4/health20, logs0/planted0. Pending metadata survived actual unchanged native
  leaf refreshes. No distance-changing or player-write exemption was added.
- leaf-refresh/route-candidates/61964exit0:1000ticks,cutoff52306/final52309. Clean
  restart retained real seed1, completed clearance and quota2. Cursor prevents
  retrying only the nearest3 stands when alternatives exist. Native diagnostic
  found3 admitted planning batches,0standing candidates,0paths and0log/plant work.
  Actor food3/health20. Rotation cannot create a safe visible stand. The late
  read-only command failed to produce a fresh report; old status is excluded and
  its exact unconsumed batch was archived/removed under lock.
- leaf-refresh/revocation/39742exit0:200ticks,cutoff52510/final52514. In a separate
  copy, explicitly added the actual seed-support leaf to clearance, then directly
  wrote its identical state through native Level.setBlock. Report52325 proves
  before=true,after=false,blockUnchanged=true. The fresh grant was retired, with
  no physical leaf/resource change. This validates that the native-refresh wrapper
  does not exempt direct player-style leaf no-ops. No screenshot was generated
  because the budget ended before the15second screenshot interval; collector now
  records that absence instead of assuming a picture exists.

All runs retained4original survivors+1previously born child,7old known deaths,
0missing. `newSelectedParentChildren=[]` describes the watched pair only; it does
not mean the existing child is absent. Family files include historical reports;
use budget-native.json for each exact cutoff. In particular,
approved/route-inspection.json is a stale copied HOME-route diagnostic, not a new
forestry inspection: its queued idle query was refused. Do not use it as proof.

## Safety, performance and custody

The native leaf wrapper only retains unchanged pending approved mangrove leaves
at LeavesBlock.tick's exact write call. Changed distance/persistence/waterlogging,
AIR/decay, other cells and direct same-state writes still revoke. Tests and actual
leaf no-op revocation cover this distinction. No global no-op relaxation exists.

Production budgets remain2planning batches/dimension/second,3paths/batch,128nodes,
100candidate positions,100tick lookup,80tick physical stall,1200tick job and400tick
blocked-route backoff. No force-load, route excavation, teleport or timer added.
Longer native runs sampled roughly2.81–3.20ms mean server ticks with five actors;
the200tick hook run sampled5.07ms. These snapshots do not establish population
capacity or tail latency. Existing pack/model/JOML warnings remain in raw logs.

Exact launch tuples/custody are in every custody-start/results pair, all terminal.
Historical adaptive3identities remain absent; frozen3identities remained exact in
the final results. They were neither stopped nor restarted by this lead. No active
build/release timer or resource hold. Existing005/006/007 evidence is untouched.
The source and installed0.4.0 JARs still hash to
4da3ac37044f5a3fa6ef77f70a27d91d1333748176aa483ff1d735b132afecb0.
No commit/PR/release/deployment/service change was made.

Current source archive source-main.zip matches route-candidate source pins;
clearance/source-main.zip binds272tests and leaf-refresh/approved/source-main.zip
binds273tests before route diagnostics. Earlier source-before pins/build logs and
failures are retained. build-lower-logs.log includes a mistaken test fixture UUID,
corrected before build-reassigned.log passed; this was not a native failure.

## Next focused work

Diagnose why the selected lower logs have zero admitted stands: record actual
standing eye height/pose, AIR/headroom/support rejects and the first native clip
obstruction. Do not assume the three removed leaves exposed either log, clear
unapproved roots, widen home range or repeatedly run the hungry resident waiting
for success. Choose a genuinely accessible finite resource set only with fresh
explicit authority. Address optional wood work while starving and actual food
supply/sharing; the earlier starvation is an observed survival gap. Then verify
native log loot, one paid plant, clean restart, natural growth and another cycle.
Keep the broader society goal active and carry this full limitation into handoff.
