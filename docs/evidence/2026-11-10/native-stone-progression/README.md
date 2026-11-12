# Native stone progression

**Primary stone pickaxe verified**, saved game time196344. Clover mined three
cobblestone with the actual wooden pickaxe (third wear2→3, native contact2.30),
then crafted a stone pickaxe from real inputs. The saved body inventory retains
both pickaxes. All179baseline humanoid UUIDs remain, all33managed villagers20HP,
and the original authored save's whole file hashes are unchanged. No births,
Nether/End progression or victory are verified.

See [focused proof](PRIMARY-STONE-PICKAXE.json),
[final save audit](native-primary-stair-paths-v1-result.json), and
[native milestone snapshot](native-primary-stair-paths-v1-milestone-native.json).
The preceding primary recovery also healed Fern12→20HP through actual grain/hay
work and four native bread crafts. Copied tests first exercised the same route
with all33managed villagers loaded.

The following entries document earlier copied and primary milestones. Their
claims that a primary pickaxe is unproven are historical.

A copied full desert food cycle finished24000ticks with five healthy residents,
one actual natural passage block dug by Fern, and20bread crafted from real inputs.
Sage reached the workbench and crafted three bread from his original9wheat.

Copied campv4 opened both five-block masonry entrances with ordinary crossbow
breaking effort and no stone drops. Juniper picked up one actual fallen cyan bed.
The actual saved continuation then placed it at107878 (item1→0), creating a normal
two-part bed and free HOME POI. No birth occurred. The continuation stopped at a
captive villager's4HP hunger guard; all179original bodies remain. Frozen paths
confirm that captive cannot reach the nearby barrel through the closed cage.
These copies are retained as evidence and were never promoted into the primary.

The resource copy from actual108141 ended132142 with real quarry movement and one
new soil drop after the planning-budget fix, but no stone pickaxe. The complete
stone-tool pipeline below remains separately verified copied gameplay evidence.

The following records the experiments and their state at each stage.

Initially the primary remained stopped at79821, all33managed villagers20HP,179baseline
humanoid UUIDs retained, and the authored save unchanged. Quarrying is still
an experiment disabled in that primary. All results below are separate copies.

The first continued workday stalled on the last stair. The controller thinks
once per10native ticks; its40tick fine-walking window did not allow enough
MoveControl requests to clear the support block. A bounded200tick window and
permission to center within the current validated stand let the subsequent
copy finish the stairs, skip sand bays, clear a real dirt bay, and mine the first
cobblestone with an actual wooden pickaxe (wear0→1). No sand was removed or
falling blocks neutralized. Actual unsupported/overhead columns still stop work.

Native ray aiming at actual visible faces then produced a second cobblestone.
The second receipt mined(-48258-360) at97540: native contact
(-481.5013424060389,58.50302425112806,-359),reach2.4196265706963653,
woodenpickaxe wear1→2. All normal three-block contact limits and structure,
fluid, occupied-support, real drops and bag-capacity checks remain.
The copied hero ended103819,20HP/food19, carrying2cobble,17dirt,1oaklog,
3bread,3wheat and an owned woodenhoe, holding the original pickaxe.

At that stage three stone and an actual stone pickaxe were still unproven. The next experiment
uses complete native dry paths to inspect cleared, supported lower work bays;
no hidden stone selection, position/velocity edits or supplied materials.
The observer's --until-stone-pickaxe option suppresses the early stop at3stone;
it remains bounded and an exhausted budget is not proof of an upgrade.

Bed recipes now support three matching wool of any native color and preserve
other stock. Mixed colors do not fund a bed.440Java tests and14Python tests
pass. No new bed or birth is claimed: all7nearby island beds remain claimed,
no sheep were saved within160blocks, and the island's actual surface chest
contains food/a sapling but no wool. Nearest saved sheep are around(-39568-91)
and(-41268-83), beyond the current local boat routes. Their locations are
operator diagnostics, not knowledge injected into villagers.

The stair-inspection continuation reached a third actual cobblestone at104824
from(-48358-359),woodpickwear2→3, but that was the original stair's support.
Although the miner stood beside it, removing it broke his exit. The preserved
copy was not replaced or teleported. In a new continuation of that actual saved
body, repairAccess placed one carried dirt back into that exact support at109823:
17dirt→16, native UP-face contact at(-482.49925971623406,58,-358.50006459055385),
reach2.0183093648695607. He walked out, returned home with the same boat, slept
normally, and crafted at a real workbench after dawn.

At120152 the continuation ended stone_pickaxe_prepared,20HP/food17, with one
stonepickaxe,2oakplanks,2sticks,16dirt,3bread, and his ownedhoe. His original
woodenpickaxe remains held. The actual log paid4planks;2planks paid4sticks;
3cobble+2sticks paid one stonepickaxe. No materials, health or actors were supplied.
This remains copied gameplay evidence, not a primary upgrade.

The first blanket support-exclusion test left him at2stone, which showed that
simply forbidding this resource is insufficient. The combined implementation
now allows a recorded stair support only if actual dirt is carried, its retained
floor is sturdy, and a native planned ray (ignoring only the already observed
stone to be removed) confirms the replacement's UP-face contact within3blocks.
After the actual drop transaction, normal placement immediately spends the dirt
and restores that specific support. Ordinary mining still cannot remove an
occupied support. Two fresh copied runs are checking this combined behavior,
including one from the current unchanged primary, before primary activation.

The combined test passed at106003 after2184ticks: source body's two real stone
became three, the third support was immediately restored (accessRepaired=true,
dirt17→16 at104844), and he walked out, returned with the same boat, and crafted
at a workbench. He ended20HP/food18 with a real stonepickaxe. Trace evidence showed
a further planning bug: unnecessary replanning during a jump returned no path,
causing a good43node route to be abandoned. Keeping the active matching route
lets the actual villager reach the lower bay without changing physics or falls.

That verified behavior was enabled in the primary, which then advanced normally
79821→91822 over12000native ticks at20TPS. All179baseline humanoid UUIDs were
retained,33managed villagers ended20HP, and the whole authored save was unchanged.
Clover gathered3actualdirt and ended20HP/food18, carrying2bread, the original
woodpick,1oaklog and3dirt, holding the originalhoe atdamage4. No primary stone
or new population is claimed. Nell physically collected an existing stored meal.
The world has reached night; the next observation will include dawn/worktime.
