# Hay food bootstrap — source verification

November 7, 2026, after head explicitly released the 006 resource commitment.
No hash/model invocation, new worker, game/reopen, runtime configuration change,
world mutation or published release replacement occurred.

Added `HayForaging`, two narrowly named capabilities, a default-off
`hayForaging` setting, and integration with normal forage/family provisioning.
[Behavior and limits](../../../design/food-bootstrap.md).

- Final normal build/test handle 78041 exited 0 in seven seconds:
  [console](build-final.log), [counts and process preservation](verification.json).
- 173 Java tests: zero failures/errors/skips. All 10 Python checks passed.
- Earlier initial build handle 22134 also exited 0, before adding explicit opt-in.
- Both short builds finished before process sampling. The empty custody receipts
  describe those sampling instants; they do not establish that no JVM ran or
  retroactively pin the transient build PIDs. No inherited job exemption claimed.
- Original six exact identities, user Modrinth 516527 and external Buddy 707822
  matched prior UID/start/executable/cwd/cgroup at final verification. Shared
  cgroups are not ownership evidence. No build/release timer was created.

Inventory tests use native ItemStack/SimpleContainer, but pass explicit recipe
outputs to transaction code. They do not prove live native recipe availability,
block loot/removal, pathfinding, survival or throughput. Those remain pending.
Existing third-party JOML Unsafe runtime warnings remain; no compiler warning.
