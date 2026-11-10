# First rendered checkpoint — November 6, 2026

This is an initial implementation checkpoint, not completion of the survival/evolution project.

- Visible Minecraft 26.3 client with the pinned Vanilla Perfected pack and eight opted-in villagers. The existing player profile/world was untouched.
- Eleven unit tests passed in a serial full Gradle build. Tests include a learned action preference versus a frozen baseline, action masks, attribution, bounded memory, persistence, corrupt data, unsupported versions and detached snapshots.
- Actual rendered evidence shows named villagers moving, food present, eight houses, and a villager occupying its assigned bed. The world has eight per-UUID mind.json folders with homes, needs, learned values and bounded memories.
- The final saved checkpoint reports 571 decisions, 404 completed authored skills and eight active sessions. These are instrumentation counts, not proof of intelligent planning, better survival or learning convergence.
- Both hostile-mob exercises recorded **zero safety interrupts**. They did not establish the intended close-range exposure, so the danger-response integration test is **inconclusive**, not passed. All eight client-tracked villagers retained 20 health in sampled observations. No sustained survival claim follows.
- During this live run, code review prompted fixes for interrupted navigation/trading, bed reclamation and POI reservations, real rest duration, and coalesced asynchronous saves with reload ordering. The revised source builds and its unit tests pass, but this already-running client still uses the earlier classes. A fresh rendered run is required to validate those fixes.
- Save/reload continuity is unit-tested, but a full visible client restart comparison is still pending. No population load or genetic evolution experiment has run.

## Historical resource coordination checkpoint

**Superseded:** the head later clarified that there is no quiet window or hold on Villagers. See [current coordination](../../coordination.md). The text below records what was believed at this checkpoint, not a current restriction.

The user requested ownership/resource coordination for Niko. PIDs527215 (Gradle launcher),527263 (Gradle daemon),527599 (visible research client) belong to this thread's runClient launch, confirmed by recorded launch and parent chain with project working directories. Gradle heap cap2GiB; client heap cap4GiB; these are not total RSS guarantees.

The current scenario was calmed and identities were checkpointed. No further build, game or model process will be started automatically before resource coordination is cleared. The notice did not authorize or trigger a stop/restart/pause of any existing job. The current visible client remains active; the exclusive model window is **not clear**. Its normal exit and disappearance of the three owned jobs must be verified before declaring the heavy-job conflict resolved.

Next work after coordination: restart the current build normally, compare persisted identities/weights/homes, construct a controlled close-range danger exposure, verify recovery from missing beds, then evaluate learned versus frozen agents in equal seeded conditions. Fix unreachable-target handling and test32/100-agent load before broad adoption. Farming, recurring resources, relationships and evolutionary cohorts remain roadmap work.
