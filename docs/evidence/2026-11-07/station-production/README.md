# Protected, paid station construction — unreleased0.4.1-dev

Dara crafted and placed a real table at an explicitly approved site, then used it
to make a wooden hoe. Two supplied oak logs became one new table, one hoe and two
spare sticks. Protection prevented any crafting/placement until explicitly removed.
Clean restart retained the block/tool/materials without duplicate work. Wood was
supplied: autonomous forestry and a sustainable material economy remain unproven.

## Controlled inputs and preserved world

`prepare-copy.py` copied the stopped construction fixture exactly (202files) into
`runs/station-production/play`. Source world and actor/terrain/inventory state were
unchanged during copying. Prior bread,tools,nursery and table remain labelled
background. The observer explicitly withdrew the prior OPERATOR SUPPLIED table at
(-226,71,744),retaining its native drop in backpack slot9. This isolates a missing
station; it is not villager demolition or permission to remove player structures.

`commands.txt` declares daylight,observer-only teleports,one finite station grant,
a same-state revocation probe,regrant+protection,and two real logs/one seed handed
to Dara through ordinary sneak-right-click gifting. No production call,villager
movement,hunger,age,mind or decision setters. The old table never entered Dara's
inventory. Native withdrawal and handoff reports are retained. A copied experiment
manifest had generic old one-log/no-terrain-input notes; actual inputs are in
setup/commands/native interventions. `preparation-note-correction.json` records
this explicitly without rewriting that historical manifest.

## Native sequence, including the failed continuation

| Phase | Handle/exit | Cutoff/final save | Outcome |
| --- | --- | --- | --- |
| Protected |95482/0|40743/40747| First native same-state AIR write revoked its station grant. Second grant persisted with protection.2logs/1seed,zero recipes/builds. |
| Protection removed |98862/0|41948/41952| Only protection removed. No build: Dara had moved beyond the original8block site lookup. Materials retained; failure preserved. |
| Assigned-job travel fixed |77189/0|43553/43557| Same world/materials/grant,no new inputs.2station recipes/1placement,3tool recipes/1hoe. |
| Read-only restart |11639/0|43958/43962| Same table/hoe/seed/two sticks/three bread; all new-work counters0. |

The successful1600tick continuation starts41953. Explicit job lookup now considers
at most8assigned sites in a32horizontal/8vertical broad phase,still constrained by
normal28block home range,loaded-site checks and shared bounded native paths. It
does not scan or discover distant unobserved natural resources. The previous failed
1200tick run is retained in `approved/`; no actor was moved to rescue it.

`job-range/production-observed.json` records the complete chain; native budget and
saved census independently confirm it. Raw stopped chunk NBT plus `plot.json` in
each phase show old table position AIR,new target(-233,72,746) AIR in denied/failed
runs,then CRAFTING_TABLE after the paid build and restart. Sand support/headroom
are unchanged. The actual table item was crafted from4planks,then consumed on
placement. Its grant621536da-071a-4578-8681-1c4a49edfe5d retired; no grants/protections
remain. The hoe is equipped at damage0/59; one seed and two sticks remain. One
ordinary bread was eaten across the longer observation,not used in a recipe.

Protected data and grant identities survive clean reopen. All runs retain5live
(4original adults+the earlier child),7historical deaths,0missing originals. Mean
tick samples4.046941/4.648985/4.666153/5.419747ms are five-actor snapshots,not scale
or tail-latency capacity evidence. The successful overview screenshot was viewed;
it is not a close-up building animation claim.

## Source binding and validation

251Java+10Python tests pass,zero failures/errors/skips. Seven added checks cover
station authority/protection/support/access retirement,overlap bounds,clean/dirty
restart,schema1migration without invented grants,real table recipe/cost/capacity,
stale placement materials and no replay. Existing hoe/bread/crop regressions pass.
`build-final.log` retains a diagnostic command-builder syntax failure; the corrected
`build-fixed.log` and final `build-job-range.log` pass. No failure was discarded.

`verify.py` and `verification.json` bind denied/paid terrain,inputs,counters,equipment,
grant retirement,restart,source world202-file manifest and artifact preservation.
The older protected/failed phases share one source pin; the final two share the
new pin. Only ConstructionSites.java differs for assigned-job reach. Its older
file was reconstructed and verified byte-for-byte against the original SHA in
`before-job-range.ConstructionSites.java`. `source-main.zip` archives the complete
current source binding for future audit. Do not compare older pins to current
source as if later intended edits were corruption.

Source/deployed0.4.0 JARs retain their historical hash.0.4.1-dev is unreleased;
no dedicated server install,restart or service change. Full pack warnings remain.
Current limits and schema2compatibility are in
[the design](../../../design/station-production.md). Cross-chunk/entity/inventory
crash atomicity and raw-chunk-writing mod hooks are not claimed.

## Exact transient custody

All sampled chains ended normally,worlds saved,and the known idle Animatica pool
drained. Exact metadata is in each custody-start/results pair:

- Protected854964/13859376,855075/13859437,855699/13859995.
- Failed continuation856587/13874498,856691/13874560,856809/13874834.
- Job-range858480/13893250,858581/13893315,858698/13893739.
- Persistence859422/13908892,859523/13908958,859640/13909273.

Historical adaptive601856/601946/602326 remain absent,no cause or replacement
inferred. Frozen601857/601990/602372 retain their exact identities at the latest
sample. This lead issued no stop/pause/signal to them. All other user clients/
services preserved. No active new job,pending command,worker,automated build/release
timer,hold,model grant or inherited PID exception.
