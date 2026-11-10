package local.villagers;

import java.util.Comparator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;

/** Collect actual unclaimed fallen wood/sticks before cutting another tree. No generated materials. */
final class LooseWood {
    static String collect(ServerLevel level,Villager v,Session s,long now){
        if(now<s.nextLooseWoodScan)return null;
        var item=level.getEntitiesOfClass(ItemEntity.class,v.getBoundingBox().inflate(12),i->i.isAlive()&&!i.hasPickUpDelay()
            &&i.getOwner()==null&&WoodCrafting.material(i.getItem())&&v.hasLineOfSight(i)
            &&VillageSurvival.withinHomeRange(level,s.mind,i.position())&&!ConstructionSites.isProtected(level,i.blockPosition()))
            .stream().min(Comparator.comparingDouble(v::distanceToSqr)).orElse(null);
        if(item==null){s.nextLooseWoodScan=now+100;return null;}
        if(v.distanceToSqr(item)>4){
            if(now>=s.nextLooseWoodPath&&WorkNavigation.admit(level,now,v)){
                s.nextLooseWoodPath=now+40;var path=v.getNavigation().createPath(item,1);
                if(!WorkNavigation.staysHome(level,v,s,path)){s.nextLooseWoodScan=now+100;return null;}
                v.getNavigation().moveTo(path,.6);
            }
            return "walking to fallen preparation materials";
        }
        var offered=item.getItem().copyWithCount(Math.min(4,item.getItem().getCount()));
        int count=offered.getCount();var remaining=v.getInventory().addItem(offered);
        int moved=count-remaining.getCount();
        if(moved<=0){s.nextLooseWoodScan=now+100;return null;}
        v.take(item,moved);item.getItem().shrink(moved);if(item.getItem().isEmpty())item.discard();
        s.looseWoodCollected+=moved;s.nextLooseWoodScan=now+10;
        s.mind.remember("Collected "+moved+" actual fallen wood materials for preparation");
        return "collected fallen preparation materials";
    }
    private LooseWood(){}
}
