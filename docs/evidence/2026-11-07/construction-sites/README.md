# Protected farm expansion — unreleased0.4.1-dev

An adopted farmer tilled and planted one explicitly approved bare-soil cell using
one held wheat seed and one point of real iron-hoe durability. Explicit protection
prevented construction until removed. The resulting farmland/wheat and unchanged
post-cost inventory survived a clean restart. This is a narrow cultivation check,
not evidence of general building, autonomous material acquisition or sustainability.

## Inputs and world custody

`setup.json` records an exact191-file copy of the stopped household-food fixture
into `runs/construction-sites/play`. No new food, tools, crops, terrain, villager
teleports, hunger or maturity setters were applied. Earlier supplied bread/tools/
stations and the earlier hunger/daylight inputs remain fixture background. Observer
teleports, explicit site grants/protection/removal and a declared same-state write
probe are listed in `commands.txt` and `approved/commands.txt`. The persistence run
only enabled read-only family observation. These copied histories never replace
default play or their source fixture.

`originalWorldUnchanged` means the household-food source named in setup, verified
against its full manifest. Each run separately verifies `source-before.json` pins.
The approved and persistence runs continue the same world without resetting it.

## Native outcomes

| Run | Loaded-world cutoff / final save | Result |
| --- | --- | --- |
| Protected, handle21924/exit0 |38927/38933| Native same-state outside write revoked the first grant. A second grant overlapped explicit protection; zero plots,12seeds,hoe damage6. |
| Protection removed, handle91129/exit0 |39734/39738| Exactly one plot,12→11seeds,hoe damage6→7,bread2 unchanged. Completed grant retired. |
| Read-only reopen, handle34816/exit0 |40139/40142| Same farmland/wheat,11seeds,hoe damage7,bread2; per-load plot counter0 and no duplicate charge. |

`native-revocation/outside-touch-38349.json` records authorizedBefore=true,
authorizedAfter=false and blockUnchanged=true. Native setBlock returned false for
the same-state attempt, demonstrating revocation at method entry, not only after
a successful block change. The probe was an explicit diagnostic intervention.

`sites-after-protected.json` and `approved/sites-before.json` retain the same farm
and protection across clean shutdown. Removal named only protection UUID
fca7a2d7-480b-43cb-b363-7cd6cd20ec76. The farmer then chose the real work operation;
no actor was teleported into reach. `approved/plot.json` and retained raw chunk NBT
show farmland at(-228,71,752),wheat at(-228,72,752),and clear headroom. The farmer's
mind records the paid expansion. `approved/sites-after.json` has no remaining grant.
`persistence/plot.json` independently reads the stopped reopened save. Its clean
metadata has no active-session marker. No post-protected raw chunk was archived;
that phase's negative result is supported by the native plot counter, inventory,
hoe status and persistent protection, not a retroactive terrain snapshot.

All runs retain5live villagers (4original adults plus the earlier ordinary child),
7historical deaths and0missing originals. Mean server tick samples6.678398,
3.340566,and3.48828ms are five-actor observations, not population or tail-latency
capacity evidence. The field now has one additional wheat plot; its renewable
output has not been measured.

## Checks and limits

`build-final.log`:237Java+10Python tests,zero failures/errors/skips. Added checks
exercise bounded/overlapping site validation, protection precedence, outside-write
retirement, clean restart, unclean grant revocation, corrupt/future/oversized/symlink
metadata refusal, failed writes, seed conservation and stale material plans. Native
runs verify the required mixin in the pinned pack. See `verification.json` for
assertions across the retained evidence and final artifact/source checks.

Generic block break/place remain disabled. Grants are explicit finite bare-ground
permissions, not ownership inferred from material. Existing crops and hay respect
explicit protected cuboids. Player edits remain possible; normal Level.setBlock
attempts revoke the affected grant before changing soil/crop/headroom. Mods that
mutate chunks directly are outside this hook; revoke grants before unsupported
bulk edits. Unknown or protected structures are never demolition resources.

Limits:8farm sites/81cells each,32protected cuboids/4096blocks each,128KiB metadata;
local8-block target search,100tick scan cache,40tick route retry,shared2path batches/
dimension/second,home-range and native path bounds,20work ticks. Reach, sight,
loaded cells, support, water, air, entity collision, actual hoe/seed and authority
are rechecked. Control writes are synchronous and bounded, including per completed
plot; no IO occurs during ordinary AI polling. Larger-site latency remains unmeasured.
Unclean restart conservatively revokes grants. Cross-chunk/entity/inventory crash
atomicity and arbitrary mod callbacks are not claimed.

## Process custody and reporting limits

Protected chain825817/13574736,825966/13574929,826129/13575827 and persistence chain
829992/13649611,830098/13649760,830266/13650086 were sampled live with exact metadata
and are terminal. The approved run ended before its late custody sample; jobs[]
does not supply a game PID. Its exec exit0, normal save/quit log and free runtime
lock establish terminal status without inventing tuples. No owned build/release
timer or new worker was created. No deployed0.4.0 update or service restart.

During this work the historical adaptive wrapper601856,daemon601946,game602326
were observed absent; this lead sent no stop/pause/signal and infers no cause or
replacement. The frozen601857/601990/602372 identities remain running at the latest
sample. Remaining clients/services are preserved. Head received one changed-custody
report; no PID exceptions or new coordination holds are inferred.

An initial reporting helper assumed all six historical processes existed and failed
on the missing adaptive wrapper. A dependent command then queued an invalid bare
`villagers site remove`; no game launched. That exact unused batch was removed under
the runtime lock before the approved continuation. The helper was corrected to
record absent identities honestly. This reporting error did not mutate the world.
