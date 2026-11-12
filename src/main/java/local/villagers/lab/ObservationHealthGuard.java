package local.villagers.lab;

/** Observer stop boundary, before further exposure; never changes an actor's body. */
public final class ObservationHealthGuard {
    public static boolean combatHit(float damage,boolean hasAttacker){
        return hasAttacker&&Float.isFinite(damage)&&damage>0;
    }
    public static boolean critical(float health,int air,boolean eyesInWater,int minimumHealth){
        return !Float.isFinite(health)||health<=0||minimumHealth>0&&health<=minimumHealth
            ||eyesInWater&&air<=0;
    }
    /** A bounded final recovery window; never allow the next lethal starvation interval. */
    public static boolean lastHealthRecovery(float health,int food,int timer){
        return health==1&&food>=0&&food<=20&&timer>=0&&(food>0||timer<60);
    }
    public static boolean criticalDuringRecovery(float health,int air,boolean eyesInWater,int minimumHealth,
            boolean allowRecovery,int food,int timer){
        return critical(health,air,eyesInWater,minimumHealth)
            &&!(allowRecovery&&minimumHealth==1&&!eyesInWater&&lastHealthRecovery(health,food,timer));
    }
    private ObservationHealthGuard(){}
}
