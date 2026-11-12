package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Bounded real headroom clearing beneath a natural tree's low canopy. */
final class ForestryCanopy {
    static final class State {
        BlockPos tree,leaf;long until,pathAt,started,lastWork;ItemStack tool=ItemStack.EMPTY;
        final Map<BlockPos,Integer> cleared=new HashMap<>();
        int blocks;Map<String,Object> receipt=Map.of();
        void clear(){tree=leaf=null;started=0;}
    }
    static Map<String,Object> status(Session s){var w=s.forestryCanopy;return Map.of("blocks",w.blocks,"target",w.leaf==null?"":w.leaf.toShortString(),"lastReceipt",w.receipt);}
    private static boolean leaf(ServerLevel level,Villager v,Session s,BlockPos tree,BlockPos p){
        if(!level.isLoaded(p)||!level.isLoaded(p.below(2))||ConstructionSites.isProtected(level,p)
            ||!level.getWorldBorder().isWithinBounds(p)||p.distSqr(tree)>32
            ||!VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p)))return false;
        var block=level.getBlockState(p);
        return block.getBlock() instanceof LeavesBlock&&!block.getValue(LeavesBlock.PERSISTENT)
            &&level.getFluidState(p).isEmpty()&&level.getBlockState(p.below()).getCollisionShape(level,p.below()).isEmpty()
            &&level.getBlockState(p.below(2)).isFaceSturdy(level,p.below(2),Direction.UP)
            &&level.getEntities(null,new AABB(p)).isEmpty();
    }
    static boolean plan(ServerLevel level,Villager v,Session s,BlockPos tree,long now){
        var w=s.forestryCanopy;
        if(!NativeRegionsScenario.nativeRegions(level)||!AdventureActions.PERMISSIONS.allows(Capability.CLEAR_PREPARATION_CANOPY)
            ||w.cleared.getOrDefault(tree,0)>=12||!BoatPreparation.naturalTrunk(level,tree)
            ||!TreeClaims.available(level,tree,v.getUUID(),now))return false;
        var candidates=new ArrayList<BlockPos>();
        for(var p:BlockPos.betweenClosed(tree.offset(-4,0,-4),tree.offset(4,2,4)))if(leaf(level,v,s,tree,p))candidates.add(p.immutable());
        // Prefer the corridor between the worker and the trunk over unrelated
        // leaves around the far perimeter of the same crown.
        candidates.sort(Comparator.comparingDouble(p->Math.sqrt(p.distSqr(v.blockPosition()))+Math.sqrt(p.distSqr(tree))));
        for(var p:candidates.stream().limit(6).toList()){
            var contact=BlockContact.point(level,v,v.getEyePosition(),p,3);
            var path=contact==null?WoodBootstrap.approachPath(level,v,s,p):null;
            if(contact==null&&(path==null||!BoatTravel.dryPath(level,path)))continue;
            if(!TreeClaims.claim(level,tree,v.getUUID(),now))return false;
            w.tree=tree;w.leaf=p;w.until=now+600;w.pathAt=now+40;
            if(path!=null)v.getNavigation().moveTo(path,.6);return true;
        }
        return false;
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        var w=s.forestryCanopy;if(w.leaf==null)return null;
        if(now>=w.until||!WoodBootstrap.safe(level,v,s)||!AdventureActions.PERMISSIONS.allows(Capability.CLEAR_PREPARATION_CANOPY)
            ||!BoatPreparation.naturalTrunk(level,w.tree)||!leaf(level,v,s,w.tree,w.leaf)||!TreeClaims.claim(level,w.tree,v.getUUID(),now)){
            w.clear();return null;
        }
        var point=BlockContact.point(level,v,v.getEyePosition(),w.leaf,3);
        if(point==null){
            w.started=0;
            if(now>=w.pathAt&&WorkNavigation.admit(level,now,v)){
                w.pathAt=now+40;var path=WoodBootstrap.approachPath(level,v,s,w.leaf);
                if(path==null||!BoatTravel.dryPath(level,path)){w.clear();return null;}
                v.getNavigation().moveTo(path,.6);
            }
            return "approaching low natural leaves that block preparation wood";
        }
        var block=level.getBlockState(w.leaf);var tool=v.getMainHandItem();
        if(w.started==0||now-w.lastWork>20||!ItemStack.matches(tool,w.tool)){w.started=now;w.tool=tool.copy();}
        w.lastWork=now;v.getNavigation().stop();v.swingForAttack(InteractionHand.MAIN_HAND);
        int duration=Math.max(1,(int)Math.ceil(block.getDestroySpeed(level,w.leaf)*30/Math.max(1,tool.getDestroySpeed(block))));
        if(now-w.started<duration)return "clearing actual leaf headroom beside natural wood";
        var drops=Block.getDrops(block,level,w.leaf,null,v,tool);var p=w.leaf;
        if(!level.getBlockState(p).equals(block)||!leaf(level,v,s,w.tree,p)||BlockContact.point(level,v,v.getEyePosition(),p,3)==null
            ||!ItemStack.matches(tool,w.tool)||!level.destroyBlock(p,false,v))return null;
        for(var drop:drops){var loose=v.getInventory().addItem(drop.copy());if(!loose.isEmpty())Block.popResource(level,p,loose);}
        block.spawnAfterBreak(level,p,tool,true);
        var data=tool.get(net.minecraft.core.component.DataComponents.TOOL);if(data!=null)tool.hurtAndBreak(data.damagePerBlock(),v,EquipmentSlot.MAINHAND);
        w.blocks++;w.cleared.merge(w.tree,1,Integer::sum);
        w.receipt=Map.of("gameTime",now,"position",p.toShortString(),"tree",w.tree.toShortString(),"drops",drops.stream().map(ItemStack::toString).toList(),
            "toolBefore",w.tool.toString(),"toolDamageBefore",w.tool.getDamageValue(),"toolDamageAfter",v.getMainHandItem().getDamageValue(),"reach",v.getEyePosition().distanceTo(point));
        s.mind.exert(.005f);s.mind.remember("Cleared actual nonpersistent leaf headroom beside my observed preparation tree at "+p.toShortString());
        w.clear();return "cleared a real low canopy block for tree access";
    }
    private ForestryCanopy(){}
}
