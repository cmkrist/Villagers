package local.villagers.economy;

/** Provisional small local quotes, based only on each participant's own need and observed relationship. */
public final class TradePolicy {
    public static final long COOLDOWN=600;
    private TradePolicy() {}
    /** Hearing a nonbinding proposal is separate from the three-block physical transfer rule. */
    public static boolean canHearProposal(double distanceSquared,double hearingRadius,boolean mutualSight) {
        double radius=Math.min(16,hearingRadius);
        return mutualSight && Double.isFinite(distanceSquared) && distanceSquared>=0
                && Double.isFinite(hearingRadius) && radius>0 && distanceSquared<=radius*radius;
    }
    public static int ask(boolean greedy,boolean lowMood,double regard,double fear) {
        if(lowMood || !Double.isFinite(regard) || !Double.isFinite(fear) || regard<-.25 || regard>1 || fear>=.6 || fear<0 || fear>1)return 0;
        return greedy?2:1;
    }
    public static int bid(boolean tool,int food) {
        if(food<0 || food>20)return 0;
        // The caller already requires unmet own hunger. Leave time for real production and travel.
        return tool || food<=13?2:1;
    }
    public static boolean sellerSafe(int food,float health,float maxHealth,double fatigue) {
        return food>=13 && food<=20 && Float.isFinite(health) && Float.isFinite(maxHealth) && maxHealth>0
                && health>=maxHealth*.9 && health<=maxHealth && Double.isFinite(fatigue) && fatigue>=0 && fatigue<.75;
    }
}
