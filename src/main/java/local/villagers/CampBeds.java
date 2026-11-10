package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.*;

/** Move only free, cramped upper bunks within the enrolled camp; never collect an occupied or claimed bed. */
final class CampBeds {
    static final class State {BlockPos target;long scanAt,pathAt,started,lastWork,until;int moved;Map<String,Object> receipt=Map.of();final Map<BlockPos,Long> blocked=new HashMap<>();}
    static boolean bedItem(ItemStack stack){return !stack.isEmpty()&&Items.BED.asList().contains(stack.getItem());}
    private static BlockPos foot(ServerLevel level,BlockPos head){return head.relative(level.getBlockState(head).getValue(BedBlock.FACING).getOpposite());}
    static boolean reusable(ServerLevel level,Pillager mob,BlockPos head){
        if(!NativeCampWork.local(level,mob,head)||!NativeIllagers.insideCamp(level,mob,head)||!level.isLoaded(head.above(2)))return false;
        var b=level.getBlockState(head);
        if(!(b.getBlock() instanceof BedBlock)||b.getValue(BedBlock.PART)!=BedPart.HEAD||b.getValue(BedBlock.OCCUPIED)
                ||level.getBlockState(head.above()).isAir()&&level.getBlockState(head.above(2)).isAir()
                ||!(level.getBlockState(head.below()).getBlock() instanceof BedBlock)||!TraderRecruitment.allowedBed(level,head))return false;
        var foot=foot(level,head);var f=level.getBlockState(foot);
        if(!NativeCampWork.local(level,mob,foot)||!NativeIllagers.insideCamp(level,mob,foot)||!f.is(b.getBlock())||f.getValue(BedBlock.PART)!=BedPart.FOOT
                ||f.getValue(BedBlock.FACING)!=b.getValue(BedBlock.FACING)||f.getValue(BedBlock.OCCUPIED)||!TraderRecruitment.allowedBed(level,foot)
                ||!level.getEntities(null,new AABB(head).minmax(new AABB(foot)).expandTowards(0,2,0)).isEmpty())return false;
        return level.getPoiManager().findAll(t->t.is(PoiTypes.HOME),head::equals,head,1,PoiManager.Occupancy.HAS_SPACE).findAny().isPresent();
    }
    static String work(ServerLevel level,Pillager mob,CreatureAgents.State agent,State w,long now){
        if(!AdventureActions.PERMISSIONS.allows(Capability.RELOCATE_CAMP_BED))return null;
        if(w.target!=null&&(now>=w.until||!reusable(level,mob,w.target))){w.target=null;w.started=0;}
        if(w.target==null&&now>=w.scanAt){
            w.scanAt=now+200;w.blocked.entrySet().removeIf(e->e.getValue()<=now);var origin=mob.blockPosition();
            // Use the same native HOME discovery as nursery claims. These are
            // existing free camp beds, not remotely inspected storage contents.
            var beds=level.getPoiManager().findAll(t->t.is(PoiTypes.HOME),p->!w.blocked.containsKey(p)&&reusable(level,mob,p),origin,64,PoiManager.Occupancy.HAS_SPACE)
                .sorted(Comparator.comparingDouble(p->p.distSqr(origin))).limit(3).toList();
            for(var p:beds){
                // The manipulation route targets a supported adjacent stand,
                // not the upper bed's solid, low-headroom block itself.
                w.target=p.immutable();break;
            }
            w.until=now+1200;
        }
        if(w.target==null)return null;
        var head=w.target;var foot=foot(level,head);
        if(mob.getEyePosition().distanceToSqr(Vec3.atCenterOf(head))>9||!NativeCampWork.visible(level,mob,head)){
            w.started=0;
            if(now>=w.pathAt){w.pathAt=now+40;if(!IllagerFood.approachNearby(level,mob,head)){w.blocked.put(head,now+1200);w.target=null;w.scanAt=now+200;return null;}}
            return "visiting an unclaimed cramped upper bunk";
        }
        mob.getNavigation().stop();mob.swingForAttack(InteractionHand.MAIN_HAND);
        if(w.started==0||now-w.lastWork>20)w.started=now;w.lastWork=now;
        var original=level.getBlockState(head);var originalFoot=level.getBlockState(foot);
        if(now-w.started<Math.max(1,(int)Math.ceil(original.getDestroySpeed(level,head)*30)))return "dismantling an unclaimed cramped upper bunk";
        // Read native drops, reserve bag space, then remove both exact halves
        // without neighbour-generated duplicate drops. The lower bunk is retained.
        var drops=Block.getDrops(original,level,head,level.getBlockEntity(head),mob,mob.getMainHandItem());
        if(drops.size()!=1||!bedItem(drops.getFirst())||drops.getFirst().getCount()!=1||!mob.getInventory().canAddItem(drops.getFirst())){
            w.target=null;w.scanAt=now+400;return null;
        }
        if(!reusable(level,mob,head)||!level.getBlockState(head).equals(original)||!level.getBlockState(foot).equals(originalFoot))return null;
        if(!level.setBlock(foot,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS))return null;
        if(!level.setBlock(head,Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS)){
            level.setBlock(foot,originalFoot,Block.UPDATE_CLIENTS);return null;
        }
        var remaining=mob.getInventory().addItem(drops.getFirst().copy());
        if(!remaining.isEmpty()){
            level.setBlock(foot,originalFoot,Block.UPDATE_CLIENTS);level.setBlock(head,original,Block.UPDATE_CLIENTS);return null;
        }
        level.updateNeighborsAt(foot,originalFoot.getBlock());level.updateNeighborsAt(head,original.getBlock());
        level.levelEvent(2001,head,Block.getId(original));level.gameEvent(mob,GameEvent.BLOCK_DESTROY,head);
        w.moved++;w.receipt=Map.of("gameTime",now,"head",head.toShortString(),"foot",foot.toShortString(),"item",drops.getFirst().toString(),"removedHalves",2,"collectedBeds",1);
        agent.mind.exert(.005f);agent.mind.remember("Collected one real unclaimed cramped upper bunk for relocation at "+head.toShortString());
        w.target=null;w.started=0;return "carrying a real unclaimed bed to usable shelter";
    }
    private CampBeds(){}
}
