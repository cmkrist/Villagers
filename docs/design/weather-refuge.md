# Native rest schedule and waking after load

Unreleased0.4.1-dev reads Minecraft26.3's adult/baby villager activity attribute at
the resident's position. REST owns scheduled refuge. The old skyDarken>=2 condition
confused rain with dusk: actual rainy daylight had skyDarken3, and sheltered actors
stayed asleep instead of producing food. An offspring starved in the observed failure.

Native timelines define the schedule; vanilla starts REST at clock tick12000,
retaining a sunset lead before full darkness. Work/idle/play/meeting do not become
night because rain dims the sky. Actual threats still enter the same defense layer.
Existing food-work body, daylight, fatigue, stock, recipe, reach and path checks remain.

On a calm non-REST day inside ordinary home range, an existing sleeper is woken even
if session-local survival intent starts NORMAL. Native sleep persists across load;
intent does not. The prior intent cannot be the wake-up gate. This uses normal
stopSleeping, never a position/velocity, weather, clock or nutrition setter.
Home/bed protection, terrain authority, metabolism and recipes are unchanged.

Use the native environment activity/day clock for scheduling. Monotonic gameTime
is different: the observed clock differed by94578ticks. Reports now expose both,
actual activity, weather and sky darkness, preventing guesses from gameTime%24000.

Native acceptance recorded awake WORK during rain/skyDarken3 after daytime reload,
paid harvests, real bread crafting/eating, sunset return before full darkness and
reopen without replay. Both recorded deaths remain. No worker, external timer,
schema or free item was added. Adequate yield and offspring survival remain open.
[Evidence](../evidence/2026-11-07/dawn-food/README.md).
