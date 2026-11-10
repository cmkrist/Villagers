# Autonomous social visits

Unreleased 0.4.1-dev adds deliberate company seeking alongside existing food,
family and safety behavior. Lonely villagers can initiate a visit even when the
small action learner would otherwise choose another elective action. Their
expressed sociability, gossip/loner traits and current needs affect initiative;
experience can therefore change future willingness to seek company.

A visitor considers at most sixteen actually visible managed neighbours within
sixteen blocks and its ordinary home range. Selection uses its own opinions,
distance and curiosity: familiar friends attract visits, while curiosity makes
unknown acquaintances more appealing. A selected partner stays stable through
the trip. Selection never reads another villager's stock, needs or private memories.

Food production, delivery and help, family company, refuge and physical needs
retain their existing priority. Work-path admission is shared; the visitor needs
a complete loaded path inside its normal home range. Queue waiting and actual
travel use the existing separate WorkJourney bounds. The visit moves only the
visitor. It neither commandeers the recipient's movement nor freezes its routine.

At contact, the recipient evaluates its own availability, current regard and
attention budget. Both bodies need mutual sight and separation under three
blocks for forty uninterrupted ticks. Travel, an unwilling or busy neighbour,
and interrupted contact earn no social relief. A completed visit relieves both
villagers' loneliness and invokes the existing conversation system, which can
spread words, gossip, change opinions and influence mood/personality. Family
consideration retains its ordinary separate funding and consent guards.

Each body has a four-hundred-tick break between accepted encounters and a
twelve-hundred-tick break with the same peer. Recent contacts occupy at most
sixteen entries. A failed approach backs off without inventing a negative social
opinion. The activity label shows the current companion's name, and each body
remembers visiting or welcoming the other.

Opinions, experience, language and bounded mind memories use the existing schema8
persistence. Trip state and recent-contact cooldowns are transient, so reopening
cancels the trip and clears those cooldowns. No persisted schema or terrain
permission changes are introduced.

## Validation

The isolated native fixture supplies three adults with initial bread and declares
one lonely visitor's prior favourable opinion of a farther neighbour. Thereafter
the ordinary controller chooses and moves without body, need, clock or decision
setters. The accepted run demonstrates selecting that friend, actual walking,
continuous contact, social relief, relationship change, and save/reopen retention.
It does not establish village-scale social variety, travel around obstacles,
long-term social balance or survival against hostile mobs.

See [native evidence](../evidence/2026-11-07/social-company/README.md).
