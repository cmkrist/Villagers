# A stable villager economy: is an emerald standard worth it?

Research date: 2026-11-07. This is a research and design document; nothing in it
is implemented. Claims are labelled:

- **[Econ]** established or mainstream economics, with sources;
- **[Game]** documented game-design practice or post-mortems, with sources;
- **[MC 26.3]** behavior checked in the decompiled Minecraft 26.3 sources in this
  project's Loom cache (`minecraft-merged-…-26.3-sources.jar`);
- **[Wiki]** taken from the Minecraft Wiki and not checked in source;
- **[Proposal]** this project's own design inference. A proposal can be wrong
  even when the sources it draws on are sound.

Real monetary history doesn't map neatly onto a village of about 20 agents. The economics
gives us directions of effect and known failure modes. It does not give
calibrated numbers.

## Short answer

An emerald standard makes sense here in one narrow form. Emeralds should be
**conserved inside the mod**: every emerald a managed villager pays out must be
one it actually holds, and every emerald it receives stays in its wallet until it
spends it. That single rule makes the money supply bounded and auditable. It
also removes the vanilla exploit in which a trading hall mints emeralds without
limit. Vanilla does not work this way. Vanilla villagers have no balance sheet:
buy trades create emeralds from nothing, sell trades destroy them, and the only
limits are stock and the twice-a-day restock (see §3).

A *fractional-reserve* note-issuing bank is not worth building. It adds
bank-run dynamics and a lender-of-last-resort problem, and it solves a credit
problem the mod doesn't have. Value should come from the villagers' own needs
(option c). Emeralds act as the conserved medium of exchange and unit of account
(option a). A fully backed "currency board" treasury is an optional later
extension, not a foundation.

## 1. Real-world background

### Commodity money and the classical gold standard

**[Econ]** Under the classical gold standard (about 1880–1914), currencies were
convertible into gold at a fixed price, which fixed exchange rates between
member countries. Because new gold production was small relative to the existing
stock, the money supply grew slowly. US–UK average inflation was about 0.1% a
year from 1880 to 1914, against 4.1% from 1946 to 2003. Short-run stability was
much worse. The coefficient of variation of annual US price changes was 17.0
for 1879–1913 against 0.88 for 1946–1990, and real output was also more
variable. Gold discoveries in California in 1848 and in Australia caused real price
shocks ([Bordo, Econlib, "Gold Standard"](https://www.econlib.org/library/Enc/GoldStandard.html)).
Bordo and Kydland note that the long-run, but not short-run, stability claim is
the standard view, and that comparisons with later managed regimes are "mixed"
([Cleveland Fed WP 9205](https://webarchive.library.unt.edu/web/20121021114704/http://www.clevelandfed.org/research/workpaper/1992/wp9205.pdf)).

**[Econ] Price–specie flow.** Hume's mechanism (1752; first sketched in a 1749
letter): a country whose prices are high imports more and exports less, loses
gold, and so its money supply and prices fall until trade balances
([Wikipedia summary](https://en.wikipedia.org/wiki/Price%E2%80%93specie_flow_mechanism);
[Bordo 1984, NBER](https://www.nber.org/system/files/chapters/c11126/c11126.pdf)).
The empirical record is weaker than the theory. Central banks often "sterilised"
gold flows, and only some followed the "rules of the game" (Bordo, Econlib).

**[Econ] Deflationary bias and rigidity.** When real output grows faster than
the commodity money stock, the price level must fall. Falling prices plus
sticky wages and debts is the dangerous combination. Comparative evidence from the
Great Depression is strong: countries that left gold earlier reflated and
recovered sooner ([Eichengreen & Sachs 1985, as cited in Bernanke & James, NBER](https://www.nber.org/system/files/chapters/c11482/c11482.pdf);
[Ellison, Lee & O'Rourke, CEPR DP15061](https://repec.cepr.org/repec/cpr/ceprdp/DP15061.pdf), who argue the effect is causal).

**[Econ] Bretton Woods (1946/1958–1971)** pegged currencies to the dollar and
the dollar to gold. Triffin's dilemma: the world needed a growing dollar
supply for liquidity, but more dollars outstanding against a fixed US gold stock
eroded confidence in convertibility. US inflation after 1965 contributed to the
end. Nixon closed the gold window on 15 August 1971
([Bordo, NBER w23189](https://www.nber.org/papers/w23189); Econlib). How much
weight the dilemma itself deserves, as against US policy choices, is debated.

### Fiat and representative money

**[Econ]** *Representative* money is a claim on a commodity (a gold
certificate). *Fiat* money has no convertibility; its supply is a policy choice.
In modern economies most money is bank deposits created by lending, and the
central bank controls the total mainly through interest rates, not a fixed
reserve multiplier ([McLeay, Radia & Thomas, Bank of England QB 2014 Q1](https://www.bankofengland.co.uk/quarterly-bulletin/2014/q1/money-creation-in-the-modern-economy)).

### Quantity theory: MV = PQ

**[Econ]** As written, MV = PQ is an identity: V is defined as PQ/M. It becomes a
theory of prices only with added assumptions, roughly that V is stable and Q is
set by real factors. Then changes in M show up mainly in P
([Humphrey, Richmond Fed Economic Review 1984](https://www.richmondfed.org/-/media/richmondfedorg/publications/research/economic_review/1984/pdf/er700502.pdf)).
Velocity is unstable in crises, which is why central banks no longer target
money aggregates directly.

**[Proposal]** For a village, the useful reading is diagnostic, not predictive.
Log M (emeralds held), PQ (emerald value of trades per day), and so V. If M
doubles and the price index doesn't move, either V fell (hoarding) or Q rose.
The logs should be able to tell these apart.

### Gresham's law

**[Econ]** "Bad money drives out good" when both must be accepted at the same
face value (legal tender acts as a price control): people spend the debased coin
and hoard the full-weight coin. Without forced equal acceptance, the good money
instead trades at a premium (Rolnick & Weber's refinement;
[overview](https://en.wikipedia.org/wiki/Gresham%27s_law)).

**[Proposal]** This matters only if the mod adds a second money: notes, a
"village coin", or emerald blocks at a non-9:1 rate. If villagers must accept a
note at par while emeralds are scarcer, players will hold emeralds and spend
notes. Either avoid a second money, or let villagers value each one separately.

### Currency boards, full-reserve and fractional banking

**[Econ]** An orthodox currency board issues local money only against 100%
anchor reserves at the margin, at a fixed rate, with full convertibility. It
has no discretion and no lender-of-last-resort role
([Hanke, Cato, "Currency boards"](https://cato.org/articles/currency-boards);
[Nguyen, JHU IAE WP 168](https://ideas.repec.org/p/ris/jhisae/0168.html)).
Most historical boards weren't fully orthodox in law, and Argentina's
1991–2002 regime is disputed as a "board" at all.

**[Econ]** Fractional-reserve deposit contracts provide liquidity but have
multiple equilibria, one of which is a run. Runs do real damage, and deposit
insurance or a lender of last resort can remove the run equilibrium
([Diamond & Dybvig 1983, JPE 91(3), Minneapolis Fed reprint](https://minneapolisfed.org/research/quarterly-review/bank-runs-deposit-insurance-and-liquidity)).
Full-reserve proposals (the 1930s Chicago Plan) remove runs by construction. One
IMF working paper models large benefits, but that is a single DSGE calibration,
not consensus ([Benes & Kumhof, IMF WP/12/202](https://imf.org/external/pubs/ft/wp/2012/wp12202.PDF)).

## 2. Lessons from game economies

**[Game] Faucets and drains (Ultima Online).** Simpson's 1999 UO paper
([archive copy](https://gwern.net/doc/economics/1999-04-07-simpson-theingameconomicsofultimaonline.html))
describes an original closed "resource system": items were made from a fixed
resource pool and returned to it on destruction, and NPC shopkeepers could only
stock goods the pool could supply. It failed for reasons that apply directly here:

- Players hoarded at massive scale, until the database became too large.
- Skill-by-repetition gave players "a non-demand incentive to produce",
  overproducing basic goods and crashing their prices. Players expected to
  profit regardless of demand and reported unsold goods as bugs.
- A gold-and-reagent duplication bug caused inflation estimated at hundreds of
  thousands to millions of times: "counterfeiting destroyed the 'experiment'."
  The gold economy collapsed into barter.
- "Hacker's Law": exploits are found faster than fixes.

UO moved to a faucet/drain model with NPC buying as a gold faucet and NPC
selling, rent and decay as drains. Koster later wrote that the drains were never
sufficient ([Koster, "The evolution of UO's economy"](https://www.raphkoster.com/?p=16263)).

**[Game] EVE Online** publishes Monthly Economic Reports with ISK faucets (NPC
bounties, NPC commodity buy orders, incursion payouts), sinks (NPC skill books,
taxes, fees) and money supply. A payment is only a sink if it goes to an NPC;
insurance payouts make ship loss partly a faucet. Even CCP's charts have needed
corrections against the CSV data
([MER Aug 2024](https://eveonline.com/news/view/monthly-economic-report-august-2024)).
CCP employed an economist, Eyjólfur Guðmundsson, who reported OPEC-like supply
manipulation by large alliances
([Massively interview](https://www.engadget.com/2009-02-19-massively-interviews-eve-online-lead-economist-dr-eyjog.html)).
Lesson: instrument faucets and sinks by source from the start, and audit the
totals.

**[Game] WoW** relies on ever-larger optional gold sinks (repairs, reforging,
transmog, multi-million-gold mounts) against faucets that never stop: quests
and vendor sales create gold
([Engadget, history of gold sinks](https://www.engadget.com/2012-07-06-wow-archivist-an-expensive-history-of-gold-sinks.html)).
The sinks are described as "a losing but necessary war".

**[Game] Diablo III** closed its gold and real-money auction houses in March
2014. Blizzard said they "ultimately undermine[d] Diablo's core game play: kill
monsters to get cool loot" ([PC Gamer](https://www.pcgamer.com/diablo-3-closing-real-money-gold-auction-houses-blizzard-says)).
Lesson: an efficient market can replace the game loop. Villagers that are too
easy to trade with in bulk could replace the social loop this mod is built around.

**[Game] RuneScape**'s Grand Exchange (Nov 2007) came with trade limits and
exchange price bands, aimed at gold sellers. Price limits were removed in
February 2011 ([RuneScape Wiki: Grand Exchange](https://runescape.wiki/w/Ge),
[Trade limit](https://runescape.wiki/w/Trade_limit)). Hard price bands stop
some abuse but push trade off-exchange. Here that would be dropped items and
gifts.

**[Game] RimWorld** scales raid strength with colony wealth, a deliberate sink
for accumulation run by its storyteller ([RimWorld Wiki: Raid points](https://rimworldwiki.com/wiki/Raid_points)).
This mod has no storyteller, so no wealth-scaled threat exists to drain hoards.
**Dwarf Fortress** removed its dwarven coin economy in v0.31; community accounts
say dwarves spent their time hauling coins
([DF Wiki: Tax collector](https://dwarffortresswiki.org/index.php/v0.34:Tax_collector),
[Crooked Timber](https://crookedtimber.org/2014/01/07/dwarf-fortress-a-marxist-analysis/)).
That reason is secondhand. Lesson: physical money handling costs agent time and
simulation budget.

**[Game] General reference:** Lehdonvirta & Castronova, *Virtual Economies:
Design and Analysis* (MIT Press, 2014) ([OII listing](https://www.oii.ox.ac.uk/?p=9173)).
It covers faucet/sink design, inflation and market design in textbook form.

## 3. Vanilla Minecraft 26.3: emerald faucets and sinks

### How vanilla trade pricing actually works **[MC 26.3]**

- `MerchantOffer.getModifiedCostCount`: price = `clamp(base + max(0, floor(base·demand·m)) + specialPriceDiff, 1, maxStack)`.
  Only the first cost item is adjusted. For "sell 20 wheat → 1 emerald" trades,
  the *wheat count* moves, not the emerald.
- `updateDemand`: `demand += uses − (maxUses − uses)`, run at each restock.
  Demand has **no lower clamp** in this method; only its positive part raises
  prices. An offer left unused builds negative demand, which then absorbs later
  heavy use.
- Restock: at most twice per day, at least 2400 ticks apart. It resets uses
  and catches up missed demand updates. Most offers have `maxUses` 12 or 16.
  `m` is 0.05 for most trades and 0.2 for some high-value ones.
- `updateSpecialPrices`: discount `floor(reputation·m)` per player. Hero of the
  Village subtracts `max(1, floor((0.3 + 0.0625·amplifier)·base))`.
- Gossip (`GossipType`, as weight/max/decay per day): major_negative −5/100/10,
  minor_negative −1/200/20, minor_positive +1/25/1, major_positive +5/20/0,
  trading +1/25/2. Curing adds major_positive +20 and minor_positive +25. Both
  are capped, so in 26.3 **a second cure cannot push the permanent part past
  the cap**. Older claims of unlimited stacking don't apply to this version's
  code. Reputation from these positive types is at most 100 + 25 + 25 = 150, a
  7-emerald discount at m = 0.05.
- Trades are data-driven since 26.1 (`VillagerTrade`, `TradeSet` registries)
  ([26.1-snapshot-1 notes](https://minecraft.wiki/w/Java_Edition_26.1-snapshot-1)).
  Vanilla base prices can be read from the registry as anchors, not hard-coded.
- When a villager has no offers, the "no" head shake (`setUnhappy`,
  `VILLAGER_NO`) already exists. It can serve as a refusal cue.
- **Project fact:** adopted villagers bypass `Brain.tick`, so they currently do
  **not restock** (README). Their vanilla offers simply run out.

The key structural fact: **vanilla villagers hold no emeralds.** A buy trade
creates the emerald it pays, and a sell trade consumes the emerald it receives.
Each villager is both a mint and an incinerator, rate-limited only by stock and
restocks. One farmer's wheat offer can pay out up to 16 × 2 = 32 emeralds a day
(before demand-driven quantity increases). A hall of N villagers with k buy
offers each therefore has a faucet of about 32·N·k emeralds a day. Breeding raises
N cheaply. The resulting emerald inflow is limited only by the player's need for
emeralds, because sell trades are finite but renewable. Villager XP plays the same
role as UO's "non-demand incentive to produce": players trade to level
villagers, not because they need the goods.

### Faucets (emeralds entering the world)

| Source | Status |
| --- | --- |
| Villager buy trades (crops, string, coal, paper, cod, etc. → emerald) | [MC 26.3] registry `VillagerTrades` |
| Wandering trader buy trades (water bottle, water bucket, milk bucket, baked potato, hay bale, fermented spider eye → emerald) | [MC 26.3] trade keys; water bottle is nearly free to produce |
| Emerald ore (1 per ore, Fortune up to 4; mountain biomes) | [Wiki](https://minecraft.wiki/w/Emerald) |
| Loot chests: buried treasure, ocean ruins, desert/jungle pyramids, desert wells, end cities, igloos, shipwrecks, trail ruins, trial chamber vaults/ominous vaults/pots, villages | [Wiki]; exact tables not checked |
| Suspicious sand/gravel (archaeology) | [Wiki] |
| Vindicator and evoker drops (player or tamed-wolf kill), so raids are an indirect faucet | [Wiki]; loot tables not checked |
| Foxes spawning with/holding an emerald | [Wiki] |
| Bedrock-only: wandering trader drops; bonus raid drops | [Wiki]; not Java |
| Hero of the Village gifts | **None are emeralds** ([Wiki](https://minecraft.wiki/w/Hero_of_the_Village)); `GiveGiftToHero`/`Raid` contain no emerald reference [MC 26.3] |

### Sinks (emeralds leaving the world)

| Sink | Status |
| --- | --- |
| Villager and wandering-trader sell trades (emerald → goods) | [MC 26.3] |
| Beacon payment (one item consumed per activation change) | [Wiki] |
| Armor-trim material in smithing | [Wiki] |
| Block of emerald (reversible storage, not a sink) | [Wiki] |
| Item despawn, lava, void | general game behavior |

**Not verified for 26.3:** exact chest and archaeology loot probabilities,
vindicator/evoker loot-table values, whether any 26.x drop added new emerald
loot (the wiki excerpts showed no 26.x emerald history entries), Bedrock
behavior, and anything in 26.4 snapshots. The wiki mentions upcoming wandering-trader
trades (ice crystals, icicles) but these don't affect 26.3.

## 4. Design options

Common assumptions: managed villagers coexist with vanilla ones; players can
mine emeralds without limit over time; the mod controls only adopted villagers.

### (a) Pure commodity economy: emeralds as conserved money

Each managed villager has an emerald wallet. Trades move emeralds between real
holders and never mint or burn them. Village money supply M_v changes only by
net player flow, the founding endowment, wandering traders, and explicit
documented exceptions.

- **Stability:** M_v is bounded by what has entered it, which is the strongest
  guarantee available. A player who sells wheat drains the village's emeralds;
  once villagers are poor they stop buying. That outcome is price–specie flow at
  village scale: the trading-hall faucet becomes self-limiting.
- **Inflation/deflation:** a player dumping mined emeralds into the village is
  a gold rush, and with wealth-sensitive prices (§5) the village price level
  rises. The opposite risk is real: population growth with fixed M forces
  deflation, and if agents' asks are sticky, trade freezes (the gold-standard
  failure mode).
- **Exploits:** trading halls and breeding gain nothing because newborns carry
  no money. AFK farms can only harvest what villagers hold. Duplication becomes
  detectable by audit. New exploit: killing a villager for its wallet (see §5.7).
- **Traits and social state:** these plug in as price terms and refusal rules.
- **Cost:** moderate. Wallet persistence, a trade adapter over `MerchantOffer`,
  and an audit ledger.

### (b) Emerald-backed notes from a village treasury with reserve ratio r

A treasury holds emeralds and issues paper "notes" (custom items). With r < 1
it can lend notes to villagers or players.

- **Stability:** with r = 1, this is a currency board: safe, but it adds
  nothing that (a) doesn't already do, except a lighter or more divisible token.
  With r < 1, Diamond–Dybvig applies: a player who collects notes and redeems them
  together can run the treasury. Nothing here can act as a lender of last
  resort except a mint, which reintroduces the unbounded faucet.
- **Inflation:** credit expansion raises the note supply while emeralds stay fixed.
  Gresham's law applies if notes must be accepted at par.
- **Exploits:** note duplication or forging (another item to protect), run
  timing, and arbitrage between par notes and scarce emeralds. Debt needs
  enforcement: what happens when a debtor dies or wanders off?
- **Traits:** a "greedy treasurer" or trust-based lending is interesting, but it
  needs institutions (who runs the treasury, and who audits it) that the mod doesn't have.
- **Cost:** high. New items, a treasury entity or block, debt records, default
  semantics, UI, and run tests.
- **Verdict:** only worth it as a fully backed warehouse receipt, and only if
  emerald indivisibility becomes a real problem (§6). Do not build fractional reserves.

### (c) Barter and needs-based value, with emeralds as one good

Each villager values items by how much they reduce its drives or enable its work.
Emeralds are valued for what they can later buy.

- **Stability:** prices are grounded in needs. When needs are met, demand
  falls without hard caps, and a hungry villager simply won't sell its last
  bread. Without a medium of exchange, barter needs a "double coincidence of
  wants", so trade volume is low and player trades become fiddly.
- **Inflation:** with no money there is no monetary inflation, but goods can still
  flood (UO's overproduction).
- **Exploits:** players flood villagers with cheap goods they "need"
  (seeds). Diminishing marginal value is the guard.
- **Traits:** traits are the most natural fit here, since valuation *is* personality times drives.
- **Cost:** valuation functions per item class are moderate. Full multi-good
  barter matching between villagers is expensive and hard to explain to players.

### Comparison

| | (a) commodity | (b) notes, r<1 | (c) needs/barter |
| --- | --- | --- | --- |
| Money supply bounded | yes, by conservation | no (credit) | n/a |
| Trading-hall faucet | self-limiting | self-limiting until runs | limited by needs |
| Main failure | deflation lock-up, hoarding | runs, forgery | low liquidity, UX |
| Trait/mood fit | via price terms | weak | strong |
| Cost | moderate | high | moderate–high |

## 5. Recommended design [Proposal]

**Combine (a) and (c):** a conserved emerald money, with every price derived from
an individual villager's needs, wealth and traits. Add a small village-scarcity term,
and no bank.

### 5.1 State per managed villager (persisted in `mind.json`)

- `wallet`: integer emeralds, at least 0. Emerald items picked up go into it.
- `inventory`: the existing villager inventory of real items.
- `capacity_g`: per-good production budget for goods the villager can't
  physically make yet (enchanted books, maps). It refills each day from real
  inputs where available (paper, books) and is otherwise capped. Label it in
  the UI and docs as an abstraction.
- `lastProposalDay`, a bounded trade-history list (shares the existing
  32-player cap), and refusal counters by reason.

Village-level (a small per-village record keyed by meeting point or bell):
`stock_g`, an observed sum of managed villagers' holdings of good g. Also an
audit ledger of every mint/burn event, by reason code.

### 5.2 Where emeralds enter and leave

Within the managed economy, emeralds move only by:

1. **Player ↔ villager trades.** Each is a transfer. A villager cannot pay more
   than `wallet`.
2. **Villager ↔ villager trades and gifts.** Transfers.
3. **Founding endowment (mint, once).** On first adoption, a villager receives
   `W0 = w_base + w_level·level` emeralds (defaults 4 + 4·level, so a master
   gets 20). The amount is logged as `MINT_ADOPT`. **Re-adoption after release does not re-endow**
   (record a flag). Villagers born to managed parents get 0 and receive any money
   as parental gifts.
4. **Wandering trader:** left vanilla; it is an external economy. A player who
   sells it water bottles imports emeralds, as mining does.
5. **Burn (optional, off by default):** villagers may spend emeralds on
   "upkeep" (for example, a bell or golem fund), at a rate `β·M_v` per day with β ≤ 1%.
   It's there to test whether a slow drain prevents end-state hoards. It is
   modelled on demurrage, an idea this document does not evaluate.
6. **Death:** wallet goes to living kin, otherwise split among nearby managed villagers by
   opinion, otherwise to the village record. It never drops as items (§5.7).

So `M_v(t+1) = M_v(t) + player_in − player_out + mint_adopt − burn`, and the audit
must show zero unexplained residual.

### 5.3 Valuation and reservation prices

All values are in "need units" and converted to emeralds through the villager's
marginal value of money:

- **Value of good g to villager i:** `v_i(g) = base_g · (1 + Σ_d α_{g,d} · drive_d) · scarcity_v(g)`.
  - `base_g` is the vanilla trade price from the 26.3 registry, in emerald terms.
  - α are authored need links; for example, food links to hunger.
  - `scarcity_v(g) = clamp((S*_g / (stock_g + 1))^σ, 0.5, 2)`, with σ = 0.3 by default.
- **Marginal value of an emerald:** `λ_i = k / (wallet_i + w0)`, with w0 = 8.
  Poor villagers value each emerald more. At village scale this term turns
  extra money into a higher price level, the direction QTM predicts.
- **Seller's ask:** `ask_i(g) = ceil(v_i(g) / λ_i · (1 + μ_i) · O_i(p))`.
- **Buyer's bid:** `bid_i(g) = floor(v_i(g) / λ_i · (1 − μ_i/2) · O_i(p))`, and never above `wallet_i`.
- Normalise k so that a median villager with `wallet = W_ref` reproduces vanilla
  base prices. This keeps prices familiar on day 0.
- Indivisibility: as in vanilla, adjust **item quantity** rather than emeralds
  when the price is below about 3 emeralds. Never let a price fall below 1.
- Selling the last units that cover a current need raises v sharply (with
  α·drive). That makes "won't sell my last bread" emerge from valuation, not a rule.

**Traits and social state** (bounded, multiplicative, logged):

- `μ_i` markup: greedy +0.25, generous −0.10, otherwise 0, plus a small
  heritable continuous component.
- `O_i(p)`: player opinion term `1 − δ·clamp(trust − 0.5, 0, 0.5)·2`, with
  δ ≤ 0.2. Friends get up to 20% off.
  - Fear does **not** lower prices; otherwise intimidation becomes the best
    strategy. Fear raises refusal and avoidance instead.
  - Awe gives no discount.
- Villager–villager trades use the same terms, with villager opinions in place of player trust.
- Mood: low mood widens the spread (raises μ by up to +0.15) and raises
  refusal probability (§5.5). It never changes `v`, which keeps "what I need"
  separate from "how I feel about trading today".
- Vanilla reputation and Hero of the Village: map cure and Hero events into
  trust evidence (cure is strong positive evidence) rather than stacking a
  separate discount. The total discount stays capped by δ.

**Village price index** (for logs, not pricing): a Laspeyres index over a fixed
basket (bread, wheat, paper, iron ingot, an enchanted book, …) at median asks,
normalised to 100 on adoption day.

### 5.4 Player trading screen

Keep the vanilla merchant UI. Before opening it, rebuild the villager's
`MerchantOffers` from its current inventory, capacity, wallet and prices:

- Buy offers are hidden or marked out of stock when `bid > wallet`.
- Sell offers are limited by real stock or capacity.

This reuses `MerchantOffer` pricing fields rather than vanilla demand. Vanilla
demand and restock are replaced by scarcity and real stock for managed villagers.
The datapack-driven trade sets still decide *which kinds* of good a profession
deals in.

### 5.5 Refusals

Check the reasons in order, and record the first that applies:

1. `UNAVAILABLE`: danger, sleep, eating, or an essential commitment. This matches the
   existing interruption priorities.
2. `NO_FUNDS` / `NO_STOCK`: hard budget constraints.
3. `NO_SURPLUS`: the villager would sell below its own value. This emerges from
   valuation, so no separate rule is needed.
4. `DISTRUST`: trust < τ_trust (0.15), or fear > τ_fear (0.6), for this player.
5. `MOOD`: probability `ρ·max(0, 0.5 − mood)·2`, with ρ = 0.3. It is drawn once per
   villager per player per 1200 ticks, so re-clicking can't reroll it.

Show refusals with the vanilla head shake and `VILLAGER_NO`. A label or
inspection command gives the reason code. A refusal never changes trust. A
player who attacks after a refusal produces ordinary harm evidence.

### 5.6 Once-a-day trade proposal

- **Eligible:** at most one proposal per villager per in-game day (`proposalsPerDay`,
  default 1). Also a village-wide cap (`villageProposalsPerDay`, default 3), so 20
  villagers don't swarm the player. The proposer:
  - is not in danger, asleep or committed;
  - is within 16 blocks with line of sight;
  - has trust in that player ≥ 0.3;
  - has not been declined by this player in the past 3 days.
- **What to propose:** the good with the largest expected surplus,
  `max(ask_player_est − v_i)` for a surplus good or `v_i − cost` for a need. This
  could be buying food while hungry and solvent, or selling surplus crops when
  the wallet is low. The price is the villager's own ask or bid; greedy villagers
  open higher.
- **Mechanics:** the villager walks up and shows the offer, using a single-offer
  merchant screen on interaction. The offer expires after 1200 ticks.
  - Accepting executes a normal transfer and is weak positive trust evidence, once.
  - Declining or ignoring lowers the villager's estimate of that player's interest.
    It is not harm evidence.
- **Persistence:** `lastProposalDay` is saved before the proposal shows, so a
  save/reload cannot produce a second proposal that day.

### 5.7 Exploit handling

- **Trading halls:** payouts are capped by M_v.
  - Confinement should be legible: a villager unable to reach bed or food
    loses mood and refuses more. It is not punished by fiat.
  - Vanilla-style XP and levelling for managed villagers should be tied to the
    *volume of real goods* produced, or left disabled at first.
- **Breeding farms:** newborns have no wallet and no endowment, so there is no free mint.
- **AFK farms selling to villagers:** limited by wallets and by diminishing
  scarcity value. Expect prices for the farmed good to fall toward the 0.5
  scarcity floor.
- **Arbitrage with vanilla villagers:** a player can buy from or sell to
  unmanaged villagers (a mint) and trade with managed ones. This is like
  importing gold, and it is acceptable. Log it as `player_in`. The mod cannot bound
  world emeralds.
- **Murder for wallets:** wallets never drop on death (§5.2.6). Killing a villager
  is never profitable.
- **Duplication:** every transfer is recorded as a pair of decrement and
  increment, as in the impressions design. A nightly audit checks that
  `Σ wallets + village record = ledger total`, and any residual is quarantined
  and reported. UO shows why this matters: one dupe can invalidate an experiment.
- **Unloaded chunks:** no fictional trades while unloaded. This is consistent with
  "unloaded time creates no fictional help or harm". If agents are later kept
  loaded and fully simulated, villager–villager trade runs on the decision
  scheduler at 1–2 Hz, not per tick.

### 5.8 Tunable parameters (defaults)

| Parameter | Default | Meaning |
| --- | --- | --- |
| `w_base`, `w_level` | 4, 4 | founding endowment |
| `w0`, `W_ref`, `k` | 8, 12, normalised | money's marginal value |
| `σ`, scarcity clamp | 0.3, [0.5, 2] | village scarcity strength |
| μ greedy / generous | +0.25 / −0.10 | trait markup |
| δ | 0.2 | max friendship discount |
| τ_trust, τ_fear | 0.15, 0.6 | social refusal thresholds |
| ρ | 0.3 | max mood refusal probability |
| `proposalsPerDay`, `villageProposalsPerDay` | 1, 3 | proposal caps |
| β | 0 (off) | optional upkeep burn per day |

### 5.9 Metrics to log per village and day

- M_v (wallets + village record); `player_in`, `player_out`, mint and burn by reason
- Ledger residual (must be 0)
- Price index (basket of median asks); PQ (emerald trade value); V = PQ / M_v
- Gini coefficient of managed-villager wallets
- Refusal rate overall and by reason; proposal count, acceptance and decline
- Stock-outs per good; unmet-need time (hunger above threshold while solvent)
- Villager–villager trade count

### 5.10 Falsifiable acceptance tests

These are proposals, not results. Run each test in the rendered lab with
recorded seeds and several repeats, per [modern-solutions.md](modern-solutions.md).
Unit tests cover only the arithmetic.

| # | Experiment | Pass criterion |
| --- | --- | --- |
| E1 conservation | 7 in-game days of scripted player and villager trades, deaths and a save/reload | Ledger residual exactly 0; M_v identical across reload |
| E2 bounded faucet | 10 managed farmers vs 10 vanilla farmers; player supplies unlimited wheat for 5 days | Managed payout ≤ initial M_v + emeralds the player paid in. Vanilla control exceeds it, documenting the faucet. |
| E3 price response | Same village, three arms: no intervention, +2·M_v emeralds gifted, M_v drained by sales | Price index ordering drained < control < injected by day 3 in most seeds. Tests direction only. |
| E4 trait effect | Matched greedy vs neutral villagers, same needs and wallet | Mean ask ratio within ±5% of 1.25. Trait ablation removes the difference. |
| E5 opinion, not fear | Trusted vs stranger vs feared player, same villager | Trusted price ≤ stranger price; discount ≤ δ; feared price not below stranger price |
| E6 grounded refusal | Hungry farmer holding its last bread; neutral-mood control | Last bread not sold. Every refusal has a reason code. Refusal rate in neutral conditions below 10%. |
| E7 proposal cap | 3 days with 20 eligible villagers, including a reload mid-day | ≤ 1 per villager per day; ≤ village cap; none during danger or sleep; no double after reload |
| E8 deflation lock-up | Double the managed population by breeding, M_v fixed, no player | Villager–villager trade volume and need satisfaction don't collapse: V stays above 50% of baseline. If it fails, enable β or adoption-linked mint and retest. |
| E9 inequality | 20 days, no player | Report the Gini trajectory. Flag if it exceeds 0.8, or if any villager has unmet need while another holds over half of M_v. |

## 6. Risks and open questions

- **Deflation lock-up (most likely failure).** A fixed M with a growing
  population and integer prices could stall trade, which is the gold standard's
  documented weakness. E8 tests it. Fallbacks: quantity-based price adjustment, a
  small mint per *adopted* newborn drawn from a capped village allowance, or barter.
- **Emerald indivisibility.** Prices below 1 emerald need quantity bundles.
  If that proves clumsy, a 1:1 fully backed note or "emerald shard" is the
  only form of (b) worth considering, and Gresham's law must be checked if it's added.
- **Player flood.** Mined emeralds are an unbounded external faucet.
  Wealth-sensitive asks mean prices rise, which is honest behavior but may feel
  bad. Is a capped wallet (excess goes to the village record) acceptable?
- **Hoarding without sinks.** With no storyteller and β = 0, wealth may pool in
  a few greedy villagers (E9). The faucet/drain literature says drains are usually
  under-provided.
- **Vanilla expectations.** Players expect restocks and cure discounts.
  Changing these for managed villagers needs clear in-game signals.
  Unmanaged villagers remain a vanilla mint next door.
- **Abstract capacity.** Enchanted books and maps from `capacity_g` are not real
  production. Keep the abstraction visible and don't describe it as an emergent economy.
- **Calibration.** Every default above is a guess anchored to vanilla prices.
  None comes from data. Tune from trial logs, and report the changes.
- **Performance.** Rebuilding offers on interaction is cheap. Villager–villager
  matching with many fully simulated agents must be measured against the 2 ms
  budget in modern-solutions.md.
- **Open:** should villagers lend to each other, which would need debt and default
  rules? Should prices and opinions spread by gossip between villagers? Should
  a villager's trade history with a player count as trust evidence beyond the
  once-per-proposal rule?
