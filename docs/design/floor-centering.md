# Recovering a clipped path start

Unreleased0.4.1-dev can recover a calm resident whose body already intersects
terrain and whose native navigation is idle. The intended destination is the
centre of the current horizontal block cell,over a clear floor at most1.25blocks
lower. It is not permission to move to another cell,climb through a ceiling,edit
terrain or broaden ordinary home territory.

Detection uses the resident's actual standing collision box,grounded state and
native block collisions. A clear destination box and sturdy,nonhazardous support
are required. Water,lava,fire,insufficient air,sleeping,trading,riding or danger
prevent the operation. Only two possible support levels beneath the current cell
are checked. Ordinary checks back off40ticks; no candidate backs off200,and a
finished/failed attempt400. No terrain scan,IO,thread,schema or external timer.

Movement uses normal MoveControl at0.35speed and native collision/gravity. Position,
velocity,pose,collision rules and blocks are never set. The support snapshot and
clear destination are rechecked during a maximum40tick attempt. Success requires
a grounded,collision-free body near the lower destination. Changing support,
obstruction,leaving the target cell or danger aborts it. Normal physics still owns
movement; a custom movement mod is not given a compatibility guarantee.

The motivating native case was Juniper standing partly on stairs under top slabs.
The body atY73extended to74.95,intersecting the roof beginning74.5. Every native
path contained only the blocked start node. Centring the feet within the existing
cell let Juniper step off the stair edge and drop naturally toY72. Native partial
return routes then became usable; ordinary movement continued after restart.

[Evidence](../evidence/2026-11-07/home-route/README.md) distinguishes leaving the
original trap,entering home range,reaching the actual bed and forestry access.
The latter two remain separate acceptance work.
