# Movement and coordination verification

## Confirmed failures

All eight villagers had converged on cabin thresholds around y=-59,z=-6.48 while repeatedly attempting to forage. `fill ... hollow` put cabin floors at y=-60, one block above the courtyard, but the door was cut at y=-60/-59. A villager standing on the raised floor needed headroom at y=-58, which remained solid. The initial unit tests did not cover physical world geometry, and the first short rendered check missed this later trap.

A separate mask excluded EXPLORE at extreme hunger. Healthy, foodless villagers therefore had only REST available and could remain in bed. The initial fixed-geometry run showed five villagers reaching food while three hungry foodless villagers stayed resting, which exposed this second failure.

## Changes

- New fixture cabins use floor y=-61, level with the courtyard.
- Existing fixture aisles/headroom are repaired idempotently when the lab resumes. Thirty-two blocks were cleared in the first repair, zero on the next resume. Beds, identities, food and villager positions were not reset; no lab teleport was issued to any villager.
- A five-block gate connects the courtyard to the surrounding world through `villagers openlab`.
- A healthy daytime villager may explore even when very hungry. Lack of known food increases authored search urgency. This is a control repair, not a newly learned skill or evidence of evolutionary adaptation.
- Reports now expose actual positions, actions, sleeping state, navigation completion and path reachability.
- The lab releases the mouse once after loading, allowing the user to click back in and control the camera normally.

## Rendered results

In the first repaired run, five villagers were observed in the courtyard during the30-second sample, and their hunger dropped after consuming existing finite food. After the mask repair, **all eight original identities** were observed outside their houses in the26-second sample. Two sampled positions were beyond the courtyard boundary at z>18, demonstrating movement into the surrounding world. All eight had20 health in these samples. These observations establish recovery of movement, not sustained survival or self-sufficiency.

Two normal development restarts retained all eight UUIDs, names, homes, personalities, cumulative learning counts and visit tables. `restart-continuity.json` checks the final restart against the completed prior save. The complete earlier lab world is backed up in the ignored runtime evidence directory. The separate human Modrinth world/profile and Buddy services were untouched.

The final full Gradle build passed all **12** unit tests, including the new regression for a hungry agent with no known food searching while retaining the danger override. Tests are supplementary to the visible movement checks.

## Coordination

The head policy SHA-256 was verified as `ee0dde2f40637894c35df76c95b9baed9eba3595b5ab1b0e69e8278699a74fe7`. It creates no quiet window, exclusive reservation or hold on Villagers. The earlier self-imposed wait was incorrect; see [current coordination](../../../coordination.md).

The original named jobs exited during an authorized development restart for the fixes, not to make space for an unscheduled model test. Current owned process tuples are recorded in `owned-jobs.json`. These are **new jobs** and must not inherit the old hash exceptions. The metadata is custody evidence only; no model/hash invocation or exclusive clearance is granted here. The final visible lab is left running for inspection.
