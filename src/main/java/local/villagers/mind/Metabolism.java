package local.villagers.mind;

/**
 * The player's hunger rules ({@code net.minecraft.world.food.FoodData}) for a villager body: a food level of
 * 0 to 20, saturation that is spent first, and exhaustion that costs one saturation or food point per 4.0.
 * Regeneration and starvation use the player's timers. Two deliberate differences: starvation damage ignores
 * difficulty, so it is always lethal, and the food level also falls on Peaceful.
 * Pure state; the controller applies the returned healing and damage to the entity.
 */
public final class Metabolism {
    public static final int MAX_FOOD = 20;
    /** At or below this food level a player cannot sprint; a villager cannot run either. */
    public static final int SPRINT_FOOD_LEVEL = 6;
    public static final float START_SATURATION = 5;
    public static final float MAX_EXHAUSTION = 40;
    public static final float EXHAUSTION_PER_POINT = 4;
    // Player exhaustion costs, from FoodConstants.
    public static final float EXHAUSTION_SPRINT_PER_BLOCK = 0.1f;
    public static final float EXHAUSTION_SWIM_PER_BLOCK = 0.01f;
    public static final float EXHAUSTION_ATTACK = 0.1f;
    public static final float EXHAUSTION_HEAL = 6;

    private int foodLevel;
    private float saturation;
    private float exhaustion;
    private int timer;

    /** Saved state, as written to {@code mind.json}. */
    public record State(int foodLevel, float saturation, float exhaustion, int timer) {}

    /** Effects for the controller to apply to the body: health to restore and 1-point starvation hits. */
    public record Vitals(float healing, int starvationHits) {
        public static final Vitals NONE = new Vitals(0, 0);
    }

    Metabolism(int foodLevel, float saturation) { this(new State(foodLevel, saturation, 0, 0)); }

    Metabolism(State state) {
        if (!Float.isFinite(state.saturation()) || !Float.isFinite(state.exhaustion()))
            throw new IllegalArgumentException("Invalid food state");
        foodLevel = Math.clamp(state.foodLevel(), 0, MAX_FOOD);
        saturation = Math.clamp(state.saturation(), 0, foodLevel);
        exhaustion = Math.clamp(state.exhaustion(), 0, MAX_EXHAUSTION);
        timer = Math.max(0, state.timer());
    }

    /** Saves from before the food model stored only a 0..1 hunger drive; that maps onto the food bar. */
    static Metabolism fromLegacyHunger(double hunger) {
        int food = (int) Math.round(MAX_FOOD * (1 - Observation.normalized(hunger)));
        return new Metabolism(food, Math.min(START_SATURATION, food));
    }

    public int foodLevel() { return foodLevel; }
    public float saturation() { return saturation; }
    public float exhaustion() { return exhaustion; }
    public State state() { return new State(foodLevel, saturation, exhaustion, timer); }
    /** The empty part of the food bar, 0 (full) to 1 (empty). */
    public double hunger() { return (MAX_FOOD - foodLevel) / (double) MAX_FOOD; }
    public boolean canEat() { return foodLevel < MAX_FOOD; }
    public boolean canSprint() { return foodLevel > SPRINT_FOOD_LEVEL; }

    /** As {@code FoodData.eat(FoodProperties)}: saturation is capped by the new food level. */
    public void eat(int nutrition, float saturationGain) {
        if (nutrition < 0 || !Float.isFinite(saturationGain)) throw new IllegalArgumentException("Invalid food");
        foodLevel = Math.clamp(foodLevel + nutrition, 0, MAX_FOOD);
        saturation = Math.clamp(saturation + saturationGain, 0, foodLevel);
    }

    public void exert(float amount) {
        if (!Float.isFinite(amount) || amount <= 0) return;
        // In point-sized steps, so a long interval is not lost to the 40.0 cap meant for a single tick.
        while (amount > 0) {
            float step = Math.min(amount, EXHAUSTION_PER_POINT);
            exhaustion = Math.min(exhaustion + step, MAX_EXHAUSTION);
            amount -= step;
            burn();
            if (foodLevel == 0 && saturation == 0) { exhaustion = Math.min(exhaustion + amount, MAX_EXHAUSTION); return; }
        }
    }

    /**
     * Runs the player's per-tick regeneration and starvation timers for {@code ticks} ticks. {@code health} is
     * the body's current health; healing during the interval counts toward it, so a full body stops healing.
     */
    public Vitals tick(int ticks, float health, float maxHealth, boolean naturalRegeneration) {
        float healed = 0;
        int hits = 0;
        for (int i = 0; i < ticks; i++) {
            burn();
            boolean hurt = health + healed - hits < maxHealth;
            if (naturalRegeneration && saturation > 0 && hurt && foodLevel >= MAX_FOOD) {
                if (++timer >= 10) {
                    float spent = Math.min(saturation, 6);
                    healed += spent / 6;
                    exhaustion = Math.min(exhaustion + spent, MAX_EXHAUSTION);
                    timer = 0;
                }
            } else if (naturalRegeneration && foodLevel >= 18 && hurt) {
                if (++timer >= 80) {
                    healed += 1;
                    exhaustion = Math.min(exhaustion + EXHAUSTION_HEAL, MAX_EXHAUSTION);
                    timer = 0;
                }
            } else if (foodLevel <= 0) {
                // Players are spared at 10 HP on Easy and 1 HP on Normal; villagers are never spared.
                if (++timer >= 80) {
                    hits++;
                    timer = 0;
                }
            } else {
                timer = 0;
            }
        }
        return healed == 0 && hits == 0 ? Vitals.NONE : new Vitals(healed, hits);
    }

    private void burn() {
        while (exhaustion > EXHAUSTION_PER_POINT) {
            exhaustion -= EXHAUSTION_PER_POINT;
            if (saturation > 0) saturation = Math.max(saturation - 1, 0);
            else foodLevel = Math.max(foodLevel - 1, 0);
        }
    }
}
