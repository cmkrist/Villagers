package local.villagers;

import java.util.Comparator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.illager.AbstractIllager;

/** A fed, lonely resident may seek visible company before another elective patrol. */
final class CampCompany {
    static String seek(ServerLevel level,Mob mob,CreatureAgents.State actor,long now){
        if(!NativeIllagers.member(mob)||IllagerFamilies.juvenile(mob)||actor.mind.foodLevel()<14
            ||mob.getHealth()<mob.getMaxHealth()*.9||actor.mind.fatigue()>.6||actor.mind.loneliness()<.5
            ||mob.isInWater()||mob.isOnFire()){
            actor.companyPeer=null;return null;
        }
        if(now<actor.nextCompany)return null;
        if(actor.companyPeer!=null&&now>=actor.companyUntil){
            actor.companyPeer=null;actor.nextCompany=now+200;return null;
        }
        var peer=level.getEntitiesOfClass(AbstractIllager.class,mob.getBoundingBox().inflate(24),
            other->other!=mob&&other.isAlive()&&CreatureAgents.managed(other)&&NativeIllagers.member(other)
                &&!IllagerFamilies.juvenile(other)&&java.util.Objects.equals(WarCombat.home(other),WarCombat.home(mob))
                &&mob.hasLineOfSight(other)&&other.hasLineOfSight(mob))
            .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        if(peer==null){actor.companyPeer=null;actor.nextCompany=now+100;return null;}
        if(!peer.getUUID().equals(actor.companyPeer)){
            actor.companyPeer=peer.getUUID();actor.companyUntil=now+400;actor.nextCompanyPath=0;
        }
        if(mob.distanceToSqr(peer)<=9){
            mob.getNavigation().stop();mob.getLookControl().setLookAt(peer,30,30);
            actor.mind.socialize();actor.companyContacts++;
            // The same ordinary physical meeting used by elective SOCIALIZE.
            // Consent, food payment, nursery and all birth checks remain separate.
            IllagerFamilies.consider(level,mob,actor,peer);
            return "spending time with visible camp company";
        }
        if(now>=actor.nextCompanyPath){
            actor.nextCompanyPath=now+80;
            if(!NativeIllagers.approach(level,mob,peer,.7)){
                actor.companyPeer=null;actor.nextCompany=now+200;return null;
            }
        }
        return "walking to visible camp company";
    }
    private CampCompany(){}
}
