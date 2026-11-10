# Standing goals for an existing home

Unreleased0.4.1-dev. A native route to the bed can fail even when a resident has
a collision-free body and usable standing space inside the house. The observed
bed route ended outside the north wall; the bed target itself was one block above
the saved bed. A roof or a nearby path endpoint alone does not prove safe arrival.

`RefugeRoutes` preserves a working native bed route. When it cannot reach, it can
try a covered standing position within2.5blocks of the same existing bed. Candidates
need a full standing body box clear of collisions, sturdy nonhazardous dry support,
dry body cells, roof cover, native collision-ray sight to the bed and loaded chunks.
Player-bed reservations remain authoritative. No new bed claim, home, terrain
permission, construction, resources or body/velocity setter is introduced.

Each near-home plan uses at most three native home-path queries, including cached
goal validation and the bed fallback. At most75cells in a5×3×5box are considered;
only the remaining query budget is used after sorting valid standing candidates.
A floor path must reach its exact target with1..128nodes. A successful standing
goal is cached, geometrically revalidated and reused; changed homes/cover or a
coarse distant return discard it. Existing40tick planning cadence, progress stalls,
recovery, loaded/home-return bounds, threat-crossing checks, nearby danger rules,
normal navigation, door handling and stamina remain. Partial-route policy is
unchanged. This is a refuge goal, not permission for partial resource routes.

A failed floor search backs off200simulation ticks while the resident remains in
the same block cell. Moving to a different cell or changing home/cover makes it
eligible immediately. A valid cached floor goal remains usable during this backoff;
ordinary bed, threat, guardian and escape planning continue at their existing cadence.
This avoids repeating an unchanged interior scan for a resident trapped elsewhere.

Existing arrival checks still require actual roof cover, proximity and native
sight. A path receipt is not arrival. Sleep uses the real existing bed normally.
Geometry does not guarantee a monster-proof house, and the normal danger layer
can still reject an unsafe home or flee.

Development mode retains one actual planning receipt and one last assigned floor
route per resident, plus bounded counters. These are not growing histories or
persistent authority. The existing selected-actor watcher can retain a departure
transition; a disappearance alone means neither death nor conversion. Separate
native lifecycle evidence establishes the terminal cause. Release mode avoids
building the diagnostic maps. No new worker, external timer or schema.

The native comparison recorded four actual floor-route assignments; the retained
last path had34nodes and reached(-270,75,734). Juniper then actually sheltered and
slept. The diagnostic-only baseline also sheltered, so this is acceptance of a
specific fallback, not evidence of a general survival-rate increase. See
[native evidence](../evidence/2026-11-07/refuge-floor/README.md).

Scheduled refuge now follows the [native activity timeline](weather-refuge.md),
correcting a later-observed rainy-day sleep failure. Floor-route geometry and
permission rules remain unchanged.
