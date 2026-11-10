# Native soil supply and paid farm expansion

Unreleased 0.4.1-dev; the installed 0.4.0 and user world were untouched. User policy:
limited autonomous building with real materials and protection for player structures.
This is an owner-assigned prototype, not autonomous land discovery or general mining.

The stopped current society was copied exactly, preserving Juniper's death at 66151
and the offspring's starvation at 81487. No actor, food, tool, terrain, growth, weather,
clock or body setters were used. The operator granted one soil job and protected its
destination; later only that protection was removed. Observer movement and read-only
probes were the other inputs. Older supplied fixtures remain historical background;
this is not proof of survival from an unsupplied baseline.

| Phase | Native outcome | Final save | Mean server tick |
|---|---|---:|---:|
| Protected, 12000 ticks | No soil income or placement; source grass and target sand unchanged | 98067 | 3.35 ms |
| Approved, 2400 ticks | Fern gathered/carried one dirt, replaced sand, kept sand and planted | 100473 | 3.14 ms |
| Reopen, 400 ticks | Source remained consumed; crop and paid inventory persisted; no replay | 100878 | 3.78 ms |

Job `e817f6cf-921a-487f-8a22-625b650b34c8` approved grass at (-247,65,713) and sand
at (-232,71,753). Target protection `efde2bfa-96ae-4441-98fd-0546afcd3d3f` covered
the soil and two headroom cells. It excluded the whole job: all soil queries and
income counters were zero. This does not claim an attempted protected commit.
Ordinary food work continued: Fern harvested/replanted three wheat crops and crafted
one bread. The first grant attempt was refused when startup moved the observer too
far away; returning the observer allowed the same guarded command, without relaxing it.

After protection removal, Fern completed both physical stages. Her native delivery
receipt shows one dirt removed and one actual native sand added; the iron hoe was
stored intact for bare-hand work and later re-equipped. Soil counters were 1 gathered,
1 delivered, with 18 path queries and zero rejected full batches. After paid planting,
wheat seeds fell from 22 to 21 and `plotsCultivatedThisLoad` was 1. The source became
air; the destination became moist farmland with wheat above. The job and single-cell
farm grant both retired. The same source/crop/inventory survived reopening; soil and
planting counters reset to zero and no authority reappeared.

Dara entered the approved phase with food 0 and health 9 and **starved at 98739**.
That death remains in the current continuation, alongside the two earlier deaths.
There are now **two living adults**, Fern and Nell: original cohort 2 alive / 9 dead /
0 missing, with the additional offspring death tracked separately. An old child mind
file does not imply a living child. This trial proves one paid expansion chain, not
adequate farm output, reliable food access/sharing or population sustainability.

`source-main.zip` binds the protected cohort. `source-approved.zip` binds the later
approved/reopen cohort (safe exposed support, stale-trip reset and native block
events added after protected collection). Each phase has its own source manifest,
exact world-copy manifest, launch/terminal records, native snapshots and census.
Saved endpoint reports include retained raw chunks. All original worlds remained
unchanged during their child trials. All three captured process chains exited 0;
no client or service was stopped to create a resource window.

296 Java and 10 Python checks pass (`build-approved.log`, build handle 66017 exit 0).
New coverage exercises real loot/payment/capacity, stale/replayed receipts,
protection/revocation, schema migration, quota reservation and finite-trip bounds.
Verify recorded native outcomes with `python3 docs/evidence/2026-11-07/soil-transfer/verify-recorded.py`.
General BREAK/PLACE remain disabled. Exact source/destination grants, real materials,
complete loaded paths, native reach/sight, readiness, bounded budgets and player
protection remain required. Crash atomicity across chunk/entity/metadata saves is
not claimed. No release or deployment was performed.
