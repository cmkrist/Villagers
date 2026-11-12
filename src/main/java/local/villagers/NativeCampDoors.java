package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.*;

/** Native paths can plan through wood doors; the actual worker must open them. */
final class NativeCampDoors {
    private record Opened(BlockState before,double side,long at){}
    static final class State {
        final Map<BlockPos,Opened> opened=new LinkedHashMap<>();
        long checkAt;int opens,closes;Map<String,Object> receipt=Map.of();
    }
    static Map<String,Object> status(State w){return Map.of("opened",w.opens,"closed",w.closes,"pending",w.opened.size(),"lastReceipt",w.receipt);}
    static double side(Vec3 feet,BlockPos p,net.minecraft.core.Direction facing){
        return (feet.x-p.getX()-.5)*facing.getStepX()+(feet.z-p.getZ()-.5)*facing.getStepZ();
    }
    static boolean passed(double before,double after){return before*after<0&&Math.abs(after)>=1;}
    static void tick(ServerLevel level,Pillager mob,State w,long now){
        if(!NativeIllagers.member(mob)||!(Villagers.config().nativeCampMeals()||Villagers.config().nativeCampPassages())||now<w.checkAt)return;
        mob.getNavigation().setCanOpenDoors(AdventureActions.PERMISSIONS.allows(Capability.OPEN_DOOR));
        w.checkAt=now+2;
        if(AdventureActions.PERMISSIONS.allows(Capability.CLOSE_DOOR)){
            var it=w.opened.entrySet().iterator();
            while(it.hasNext()){
                var entry=it.next();var p=entry.getKey();if(!level.isLoaded(p))continue;
                var state=level.getBlockState(p);var old=entry.getValue();
                if(!state.is(old.before.getBlock())||!state.getValue(DoorBlock.OPEN)||state.getValue(DoorBlock.POWERED)){it.remove();continue;}
                double here=side(mob.position(),p,state.getValue(DoorBlock.FACING));
                if(!passed(old.side,here)&&now-old.at<200)continue;
                if(BlockContact.interactionPoint(level,mob,mob.getEyePosition(),p,3)==null
                    ||!level.getEntities(null,new AABB(p).expandTowards(0,1,0).inflate(.2)).stream().allMatch(e->e instanceof ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb))continue;
                ((DoorBlock)state.getBlock()).setOpen(mob,level,state,p,false);w.closes++;it.remove();
                w.receipt=Map.of("gameTime",now,"action","closed actual camp wooden door","position",p.toShortString());
            }
        }
        if(!AdventureActions.PERMISSIONS.allows(Capability.OPEN_DOOR)||w.opened.size()>=4)return;
        var candidates=new LinkedHashSet<BlockPos>();var path=mob.getNavigation().getPath();
        if(path!=null)for(int i=Math.max(0,path.getNextNodeIndex()-1);i<Math.min(path.getNodeCount(),path.getNextNodeIndex()+3);i++)candidates.add(path.getNodePos(i));
        // Native navigation may time out while a body is already at the closed
        // threshold. Opening that touched door lets its next real route proceed.
        candidates.add(mob.blockPosition());
        for(var at:candidates){
            var state=level.getBlockState(at);
            if(!state.is(BlockTags.WOODEN_DOORS)||!(state.getBlock() instanceof DoorBlock door)||!door.type().canOpenByHand())continue;
            var p=state.getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER?at.below():at;state=level.getBlockState(p);
            if(state.getValue(DoorBlock.OPEN)||state.getValue(DoorBlock.POWERED)||!NativeIllagers.insideCamp(level,mob,p)
                ||!NativeCampWork.local(level,mob,p)||ConstructionSites.isProtected(level,p)
                ||BlockContact.interactionPoint(level,mob,mob.getEyePosition(),p,3)==null)continue;
            double entry=side(mob.position(),p,state.getValue(DoorBlock.FACING));
            door.setOpen(mob,level,state,p,true);w.opened.put(p.immutable(),new Opened(state,entry,now));w.opens++;
            w.receipt=Map.of("gameTime",now,"action","opened actual camp wooden door","position",p.toShortString(),"reach",mob.getEyePosition().distanceTo(Vec3.atCenterOf(p)));
            return;
        }
    }
    private NativeCampDoors(){}
}
