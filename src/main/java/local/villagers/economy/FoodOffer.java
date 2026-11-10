package local.villagers.economy;

/** Each actor answers using its own state. A remembered request never grants remote access to peer needs. */
public final class FoodOffer {
    private FoodOffer() {}
    public static boolean mayDeferOwnMeal(int food,float health,float maxHealth) {
        return food>=13 && food<=20 && Float.isFinite(health) && Float.isFinite(maxHealth)
                && maxHealth>0 && health>=maxHealth;
    }
    public static boolean wantsFood(double ownHunger,boolean glutton,boolean hasUsableRation) {
        return !hasUsableRation && Double.isFinite(ownHunger) && ownHunger<=1 && ownHunger>=(glutton?.35:.5);
    }
    public static boolean wantsMeal(int food,float health,float maxHealth,boolean glutton,boolean hasUsableRation){
        return !hasUsableRation&&(wantsFood((20-food)/20.0,glutton,false)
            ||local.villagers.mind.MealPolicy.needsRecoveryFood(food,health,maxHealth));
    }
}
