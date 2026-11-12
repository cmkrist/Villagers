# Native gate meals and repeat supply investigation

Main remains stopped at actual v213. These are copied-world observations, not a
main-world rescue or population-growth claim.

Frozen audits v250/v251 identified an18-node block-safe contact route to Dara's
lever-side stand. An actual ravager occupied a node of that route. Nearby lever
stands alone did not establish handoff reach. Current delivery planning prefers
actual contact stands before the wider lever meeting area.

v252 stopped at582ticks on Dara1HP, all192 humanoids retained alive. v253 reached
1971ticks before stopping on Pip1HP at zero food. In v253 the courier physically
transferred2 potatoes toPip at304787,2 potatoes through Dara's iron gate at305037,
and1 bread through that gate toOak at305727. Both gate receipts confirm native
lever restoration and closed doors. Actual food/health snapshots show the roots
were eaten, with one planting root retained. Oak ate the bread. No healing claimed.
All192 humanoids remained alive; no new/missing/dead UUIDs.

An experimental native crowd-walking fallback was included in v253, but no saved
40-tick planning receipt demonstrated its use. It has been removed; do not credit
that speculative feature for the successful gate handoff.

The run exposed the next supply issue: a1HP recipient with1food ceased making the
critical-volume call, and a provisioner left a real bench after making one bread
while six gathered wheat remained. Current source retains a critical call at<=2
food and batches up to3 real recipes for heard emergencies, conserving actual
wheat and output capacity.606 integrated Java tests pass. Full integrated v254 missed its first-food deadline;
its changed initialstorage route remains unverified for actual rescue.

v258 uses the isolated pinned courier/supply chain. It completed4000native ticks
with all192humanoids alive and12 realhandoffs, actualmeals and3breadrecipes.
Saved pair-history tags persist. EndPip1HP/3food,Dara1HP/8food,Oak5HP/11food;
starvationtimers0. No healing, births or mainworld progress claimed. A further
8000tick continuation v259 stopped on Sage in a different village after873ticks;
all192alive. This exposed an overbroad gate-wait condition. Restricting camp requests
and waits to residents whose ownhome is in an illager barracks yielded v260PASS:
4000native ticks after savedv258,all192alive,Sageactualstoredmeals and12→14HP.
Successful branch258→260 totals8000native ticks acrosssave/reopen. EndPip7food,
Dara8,Oak11; Pip/Dara remain1HP, so no wholecohort healing claimed. Fullintegrated
artifact still has a first-supply integration failure; primary remainsstopped.
Files link to complete run evidence.


Stable integrated snapshot v265 (346source/build/resource files, unchanged through
build) passes606Java tests and a4000tick native care run fromfrozen214, all192alive.
Its JARsha is f88f8f26e4cfbc060efbbe3e84d7cb48e4464e5082e7dc19dbedb8ec670d20e6.
This resolves the prior stale first-supply discovery filter at a fixed source point.
Actual main body state still exactlymatchesv213; deployment, population growth and
game completion are not established. See v265-integrated-snapshot-summary.json.
