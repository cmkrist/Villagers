# Four-biome world foundation

First native creation and reopening passed on Minecraft26.3/Fabric Loader0.19.5,
using JDK25 in the isolated `runs/four-biomes-world-v1` runtime. Both server
processes are terminal. No earlier trial or external server world was opened.

`war-world-before-restart.json` records the first check at game time4.
`war-world-result.json` records reopening at game time24. Both checks find
40tagged actors: eight residents, one trader and one Enderman in each of the four
towns. Each town has16bed blocks (eight complete beds), its expected actual biome,
and a1024-block world border. The probe also checks remote generated terrain and
biomes. The restart log contains no construction messages. Artifact binding and
logs are retained alongside these reports.

The initial build passed396Java tests. A later full test run on JDK25 passed the
10Python checks but failed the unrelated existing child-language inheritance
test after concurrent language-source changes. The three new world tests pass
on JDK25. A subsequent shared-workspace suite contains409tests and has two failures in
`LanguageDevelopmentTest`; its child-vocabulary test now passes. Test status
must be rechecked after shared-workspace changes settle.

The v1 native artifact predates explicit structure suppression and the Greenhaven
player spawn; those additions also pass in the subsequent v2 runtime, whose first/reopen
reports and pinned artifacts are retained in `v2/`. The v2 restart check verifies
the saved Greenhaven spawn and absence of native structure starts in sampled
remote chunks. All40founders remain present at game time40; both v2processes
exited successfully. This is
world-generation evidence only. It does not establish expanded creature AI,
warfare, casualty persistence, economy adequacy or rendered appearance.
