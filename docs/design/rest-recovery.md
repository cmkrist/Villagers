# Rest when a known bed cannot be reached

Unreleased0.4.1-dev preserves the existing ability to rest in place when a bed is
missing and extends it to a valid but unreachable or occupied bed. Bed ownership,
native reach/sight and sleep checks remain. A failed bed is not permission to take
another resident's or a player's bed.

Ground rest requires actual grounded,stationary,safe body state for80consecutive
ticks. Water,lava,fire,insufficient air and damaging/unsafe footing prevent credit.
Travel,more than quarter-block movement,another action's observation gap,bed changes
or clock rollback restart the interval. Finishing one interval resets it,so a new
REST attempt cannot replay immediate credit. The original0.12fatigue recovery per
rest bout and ordinary fatigue accumulation remain; no drive/rate multiplier changed.

Bed travel uses existing WorkJourney stall/deadline/backoff,40tick retry,shared
work-path admission and128node bound. Failed native paths back off rather than
prevent quiet rest forever. Ordinary survival planning now honors its40tick deadline
even when the last intent was NORMAL. Home/cover,danger and dusk changes still
invalidate the deadline. There is no new thread,IO,schema or external timer.

A calm resident already outside home range may follow a native partial path only
if it has2..128nodes and ends at least one block closer to home. Nodes must be
loaded and stay within max(28,current home distance)+8. This is a bounded return
detour,not broader work/exploration territory. Existing danger route selection is
preserved. The positive partial-path branch was not taken in the original rest
case. A subsequent [clipped-start fix](floor-centering.md) enabled three native
partial returns and restored ordinary movement inside home range; reaching the
actual bed remains unverified.

[Native evidence](../evidence/2026-11-07/rest-recovery/README.md) records real ground
recovery without new inputs,planning counts,persistence and the unresolved route.
See [forestry prerequisite](forestry.md): recovering fatigue alone does not prove
that the forest can be reached or harvested.
