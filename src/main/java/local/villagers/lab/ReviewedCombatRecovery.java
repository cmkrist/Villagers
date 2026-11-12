package local.villagers.lab;

import java.util.Objects;
import java.util.UUID;

/** One observed encounter may continue through retained native damage, above a fixed health floor. */
public record ReviewedCombatRecovery(UUID actor,UUID attacker) {
    public static final int MIN_HEALTH=8;
    public ReviewedCombatRecovery { Objects.requireNonNull(actor);Objects.requireNonNull(attacker); }
    public boolean permits(UUID body,UUID source,float health){
        return actor.equals(body)&&attacker.equals(source)&&Float.isFinite(health)&&health>MIN_HEALTH;
    }
    public boolean critical(UUID body,float health){
        return actor.equals(body)&&(!Float.isFinite(health)||health<=MIN_HEALTH);
    }
}
