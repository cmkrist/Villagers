package local.villagers.mind;

/** Owned-food reflex: efficient nutrition, healing threshold, and urgent hunger; no invented food/healing. */
public final class MealPolicy {
    public static final int INTERVAL = 32;
    private MealPolicy() {}
    public static boolean needsRecoveryFood(int food,float health,float maxHealth){
        return food>=0&&food<18&&Float.isFinite(health)&&Float.isFinite(maxHealth)
            &&health>0&&maxHealth>0&&health<maxHealth;
    }
    public static boolean shouldEat(int food, int nutrition, float health, float maxHealth) {
        if (food < 0 || food >= Metabolism.MAX_FOOD || nutrition <= 0 || !Float.isFinite(health)
                || !Float.isFinite(maxHealth) || health <= 0 || maxHealth <= 0 || health > maxHealth) return false;
        return food <= Metabolism.SPRINT_FOOD_LEVEL || health < maxHealth && food < 18
                || Metabolism.MAX_FOOD - food >= nutrition;
    }
}
