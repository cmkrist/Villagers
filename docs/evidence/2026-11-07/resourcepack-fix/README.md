# Resourcepack preparation fix — November7,2026

No new Java, Gradle, javap, game, release or local-model job was started in this
pass. This was Python/source work while keeping the proposed coordination interval
usable; no coordination hold/hash invocation was inferred.

The visible tests had reported that Minecraft rejected resourcepack symlinks.
Preparation now places verified read-only regular files into resourcepacks/,
using hardlinks to the immutable cache on the same filesystem and a checked copy
fallback otherwise. Mod JAR sharing remains symlink-based. Launcher validation
understands the storage distinction and refuses unresolved legacy pack links
with a repair instruction. Byte verification and read-only checks remain.

`tools/repair-resourcepacks.py` repairs only an explicitly selected stopped,
managed runtime. It validates all resourcepack entries before replacing any,
preserves the old experiment manifest, and atomically replaces the links rather
than writing through them. Preparation and repair share the launcher's flock
and check for manually launched live clients. Default pack refresh and parallel
preparation use this exclusion too. Dry-run previews do not mutate asset entries
or manifests.

The stopped `runs/play` copy was repaired:30 packs are now regular read-only
files, each matching its original verified SHA256. World file metadata and mod
entries matched their pre-repair state. The manifest backup path and exact results
are in results.json; world metadata before repair is retained separately. Original
adaptive/frozen/user runtimes were not repaired or stopped.

Ten Python regressions passed: successful/idempotent repair; unchanged world and
mod data; launch validation before/after repair; checksum-failure atomic refusal;
launcher-lock and live-client exclusion; parallel-preparer exclusion; dry-run
preservation; symlink/traversal/empty-key refusal; and full fresh parallel creation
with independent world bytes, regular packs and shared mod links. Shell syntax
checks passed. `tools/test.sh` now includes these Python checks in future builds;
it was not run here because that wrapper starts Java.

Rendered resourcepack loading/selection and full pack visual parity remain pending
after the next explicitly coordinated visible launch. Regular files and hashes
are necessary preparation evidence, not proof that the engine loaded every pack.
