# Creatures AI: evidence, limitations, and lessons for Villagers

Research date: 2026-11-06. Scope: the Creatures series' artificial-life architecture, with attention to differences between the original Creatures, Creatures 2, and Creatures 3 / Docking Station. This document separates source-supported descriptions from proposed Villagers engineering. It does not claim that personification demonstrates consciousness.

## Findings that should shape this project

Creatures is useful because its inhabitants learned while living, coupled behavior to internal needs, and could inherit variation. Those mechanisms offer a stronger starting point than dialogue alone. Its limitations also show why increasing neural-network size is insufficient: attention, action affordances, credit assignment, survival constraints, identity, and observability each need deliberate treatment.

Our design recommendation is a layered agent: reliable survival and movement; needs and temperament; bounded contextual learning; explicit memories of individuals and places; persistent identity; and, later, planning and constrained heredity. This is an engineering proposal derived from the evidence below, not a finding that Creatures implemented all those layers.

## What the original system actually did

Grand, Cliff, and Malhotra's 1997 paper describes a genetically configured network coupled to artificial biochemistry. Drives rise and fall through biochemical reactions; drive changes produce reinforcement signals. Synaptic short-term weights react quickly, while slower long-term weights support statistical retention. Recent active connections remain susceptible to delayed reinforcement. Chemistry uses concentrations, emitters, reactions, and receptors rather than physically realistic molecules. Genetics can change structure and physiology, with crossover, duplication, deletion, and mutation; headers restrict operations on critical genes. Gene expression can change with age. Importantly, the authors described this as an engineering project and warned that apparently social emergence can reflect observers' anthropomorphism. These statements establish mechanisms and author interpretation, not comparative evidence of human-like cognition. [Grand, Cliff & Malhotra, Agents '97, preserved author paper](https://eem.foo/ccarchive/Sites/fp.cyberlifersrch.plus.com/articles/agents_%2797_paper.htm), [publication DOI](https://doi.org/10.1145/267658.267663).

The expanded 1998 paper describes the original model as approximately 1,000 neurons, nine lobes, and 5,000 synapses. Symbolic sensory approximations activate inputs when relevant objects are perceived. Attention selects an object; a decision selects an action concerning it. Concept neurons recognize combinations of sensory features; reinforcement, atrophy, and dendritic migration reuse finite representational capacity. The authors explicitly acknowledge limited cognition, problems with this approach, and a generalization-related migration mechanism left disconnected after problems. Processing only one attended object reduces cost while constraining thought to verb–object form. Around ten simultaneous creatures caused serious response degradation on target hardware. That historical limit is not a modern benchmark, and it includes the entire game workload. [Grand & Cliff, 1998, sections 3.1–3.2](https://www.cp.eng.chula.ac.th/~vishnu/gameResearch/AI/creatures.pdf), [publication DOI](https://doi.org/10.1023/A:1010042522104).

## Version distinctions matter

Chris Double's engineering notes distinguish the standard C1 nine-lobe genome from C2's ten-lobe genome. They describe the added regulator as biochemical control rather than another decision layer. The documented C1/C2 drive-to-perception arrangement allows only one drive in an individual concept, so simultaneous hot-and-hungry conditions cannot be represented by that mechanism. The documentation includes executable-controlled lobe interfaces alongside genetically defined processing. This is primary community engineering evidence; statements explicitly marked uncertain in those notes should remain uncertain here. [Double, Brain Lobes](https://double.nz/creatures/brainlobes/lobes.htm).

Creatures 3 changes the action-selection picture. Original developer documentation describes at most one representative object per category, across forty categories. Selection considers accessibility, current drives, and expected drive reduction; learnt drive-to-action associations influence the weighting. Chosen actions trigger scripts that handle approach, pickup, and use. Thus even Creatures delegated movement mechanics to conventional engine code. Drive-specific learning and broader reinforcement coexist, and genetically specified instincts are trained at hatching or during dreaming. C3 therefore already addresses aspects of motivation-sensitive selection: it should not be criticized as if it retained the original C1 attention system unchanged. Category representatives still compress distinct objects and can lose useful instance-level information. The final sentence is our inference from the documented representation. [Creatures Development Network, How Creatures Sense Their Environment, preserved original documentation](https://lisdude.com/cdn/18.html).

The C3 Genetics Kit manual distinguishes neuron collections, called lobes, from connection collections, called tracts; initialization and update rules control both. It also describes organs and irreversible organ death. This reinforces that changing a genome can change multiple interacting mechanisms, rather than merely changing a list of personality sliders. [Creature Labs, Genetics Kit Help File, preserved manual](https://device.report/m/1f1d24e95501c4def74d4d970983a5621edb2b3ccd9fa319eefea3c1d2e70772).

## Demonstrated engineering shortcomings

### Attention can defeat survival even when action learning exists

Double's analysis describes an early attention system dominated by the most stimulating object and spoken nouns. A nearby active toy or creature can win attention while food remains available farther away. Since action selection concerns the attended object, knowing that eating reduces hunger does not ensure that food will be considered. His proposed repair couples attention to learnt drive relevance. This is an architectural diagnosis by a developer, not a controlled survival-rate experiment. [Double, Papers and Discussions](https://double.nz/creatures/papers.htm).

Double and Lis Morris subsequently built C2 Canny Norns with three additional working lobes. Their release notes say these learn which objects affect drives and look for relevant objects when those drives are high. Version 1.2 adds lifespan and concept-retention adjustments. This provides a concrete historical intervention supporting the attention diagnosis, although the page supplies no controlled population benchmark or confidence interval. It also demonstrates that users changed the cognitive architecture through genomes. [Double & Morris, Canny Norns release notes](https://double.nz/creatures/norns/index.htm).

### Reactive learning is not the same as prospective planning

Steve Grand's retrospective says Norns learned, generalized, and reacted, while contrasting those abilities with the thinking he wanted in his next project. He emphasizes online lifetime learning, short- and long-term memory, useful retention, and instincts. His proposed next step centers on representing situations absent from current perception. This is valuable primary testimony about design intent and limitations; his broader assertions about thought and consciousness are opinions, not settled results. [Grand, Introduction to an artificial mind, 2011](https://stevegrand.wordpress.com/2011/03/06/introduction-to-an-artificial-mind/).

### Attractive anecdotes are weak evidence of general capability

Claims that a Norn loved someone, became depressed, invented cooperation, or understood a conversation need operational definitions. A plausible trace is insufficient: the behavior may arise from reinforcement, current drives, scripts, or a player's interpretation. Our project should retain readable reactions while measuring changed decisions and outcomes. This is our research-method recommendation, informed by the original authors' stated caution, rather than an accusation that their experiments were fraudulent.

### Reported C2 failure syndromes require version-specific reproduction

“One Hour Stupidity Syndrome” is a commonly reported label for C2 survival/retention failures. We have not reproduced it or established a universal root cause from primary experimental evidence. Do not present reward-chemical saturation, neuron scarcity, or every feeding failure as the single proven explanation for the whole series. The stronger evidence available here is the Canny authors' specific architecture changes and claimed retention improvements. A rigorous follow-up would pin the original genome and engine versions, inspect chemical trajectories, and compare outcomes with each repair independently.

## Shortfall → proposed design → falsifiable test

Everything in this table is a Villagers proposal. The evidence above motivates the problems; these are not claims about implemented functionality.

| Limitation or risk | Proposed Villagers response | Visible-client test and measurement |
| --- | --- | --- |
| Attention ignores critical needs | Form a bounded candidate set from needs, perceived hazards, and remembered resources; rank before committing attention | Place a hungry villager near food and a distracting social group; measure time to eat versus starvation and distraction time |
| Immediate learned reactions cannot prepare for absence | Explicit short plans with preconditions, resource checks, and interruption; later learn transition outcomes | Remove the usual food source, leave an alternate remembered source; measure recovery time and successful replanning |
| Coarse categories erase individual identity | Stable UUID relationships and object/place records, with perception provenance | Have player A help and player B attack; compare responses to each in the same context |
| Diffuse reward assigns credit incorrectly | Attribute rewards to actual completed actions and elapsed intervals; separate damage, satiety, safety, and social outcomes | Reward once after an unrelated intervening action; inspect trace for the responsible action and reject duplicate reinforcement |
| Retention competes with new learning | Bounded episodic memory plus compact skill values; preserve critical experience, decay stale evidence, and track confidence | Teach a food route, introduce new experiences, save and reload, then repeat the original task |
| Exploration can kill immature agents | Survival reflexes and action validity gates constrain learning; expand exploration only inside safe affordances | Put lava, a drop, water, and unreachable food nearby; record chosen candidates, rejected hazards, and survival |
| Learned habits can outlive their usefulness | Detect repeated failure, lower confidence, blacklist temporarily, and seek alternatives | Block a learned route; count failed retries before switching and time to reach an alternate destination |
| Unbounded evolution breaks viability | Separate inherited bounded traits from lifetime knowledge; record parentage and mutation; validate structural invariants | Breed several generations; inspect trait distributions, minimum viability, and preservation of child identities |
| Believable reactions may hide no learning | Compare enabled and frozen-learning agents under matching conditions and seeds | Train repeated encounters; inspect changed values and subsequent choices, then repeat with values frozen |
| Population load grows unpredictably | Stagger decisions, cache perception, bound searches, avoid pairwise all-population relationships, profile loaded agents | Run 10/50/100/200 agents in a visible client; report hardware, mod set, median/p95 server tick time, memory, and survivor counts |
| Disk identity can diverge from world identity | World-scoped UUID homes, versioned schemas, atomic writes, bounded journals, and recovery policy | Save, reload, unload/reload chunks, and interrupt a save; verify no duplicated or cross-world identity and readable recovery |

## Implementation priority

1. Give existing villagers stable world-scoped identities and inspectable state on the internal drive. Preserve Minecraft's robust movement and emergency behavior while integration is incomplete.
2. Add bounded needs, temperament, explicit outcome records, and learnt preferences. Ensure a changed value can alter a later decision before calling this learning.
3. Integrate a small, interruptible action set: retreat, find shelter, approach known food, eat available food, rest, and socialize. Add actions only with verified preconditions and completion evidence.
4. Compare with frozen-learning and vanilla baselines. A prototype that merely raises artificial hunger without providing obtainable food is not a survival improvement.
5. Add individual relationships, then planning, teaching, and constrained heredity. Keep those milestones separate; learning within a lifetime does not demonstrate generational evolution.

The target experience is recognizable individuals whose histories affect what they do. The target evidence is retained, useful behavioral change under uncertainty at a measured population cost. Neither target requires claiming sentience.
