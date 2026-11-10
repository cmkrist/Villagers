package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** Three real surface-stone drops for the nominated villager's first upgrade. No tunnels or structural quarrying. */
final class AdventureMining {
    static final class State {
        BlockPos target;
        long nextScan,nextPath,started,lastWork,blocks;
        ItemStack tool=ItemStack.EMPTY;
        String status="not quarrying";
        Map<String,Object> receipt=Map.of();
    }
    static Map<String,Object> status(Session s){return Map.of("activity",s.mining.status,"blocks",s.mining.blocks,"lastReceipt",s.mining.receipt);}
    static boolean surface(ServerLevel level,Villager v,Session s,BlockPos p){
        if(!level.isLoaded(p)||!level.isLoaded(p.above(2))||p.getY()<v.blockPosition().getY()
                ||!level.getWorldBorder().isWithinBounds(p)||ConstructionSites.isProtected(level,p)
                ||!VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p)))return false;
        var state=level.getBlockState(p);
        if(!(state.is(Blocks.STONE)||state.is(Blocks.DEEPSLATE))||!level.getBlockState(p.above()).isAir())return false;
        if(!level.getEntities(null,new AABB(p).expandTowards(0,2,0).inflate(.1)).isEmpty())return false;
        for(Direction d:Direction.values()){
            var n=p.relative(d);if(!level.isLoaded(n)||!level.getFluidState(n).isEmpty()||level.getBlockState(n).getBlock() instanceof FallingBlock)return false;
        }
        for(BlockPos n:BlockPos.betweenClosed(p.offset(-2,-1,-2),p.offset(2,2,2))){
            if(!level.isLoaded(n))return false;
            var block=level.getBlockState(n);
            if(block.getBlock() instanceof BedBlock||block.getBlock() instanceof DoorBlock||block.is(net.minecraft.tags.BlockTags.PLANKS)
                    ||level.getBlockEntity(n)!=null)return false;
        }
        return !level.structureManager().getStructureWithPieceAt(p,holder->true).isValid();
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        var m=s.mining;
        if(!AdventurePreparation.nominated(v)||!AdventureActions.PERMISSIONS.allows(Capability.MINE_SURFACE_STONE)
                ||!WoodBootstrap.safe(level,v,s)||WoodCrafting.stoneCount(v.getInventory())>=3){m.started=0;return null;}
        if(m.target!=null&&!surface(level,v,s,m.target)){m.target=null;m.started=0;}
        if(m.target==null){
            if(now<m.nextScan||!WorkNavigation.admit(level,now,v))return null;
            m.nextScan=now+100;var origin=v.blockPosition();double nearest=Double.MAX_VALUE;
            for(BlockPos p:BlockPos.betweenClosed(origin.offset(-12,0,-12),origin.offset(12,5,12))){
                if(!surface(level,v,s,p)||!BoatRoutes.visible(level,v,Vec3.atCenterOf(p)))continue;
                double d=p.distSqr(origin);if(d<nearest){m.target=p.immutable();nearest=d;}
            }
            if(m.target==null){m.status="needs observed exposed natural stone";return null;}
        }
        var p=m.target;var block=level.getBlockState(p);
        if(!AdventureActions.preparePickaxeForMining(level,v,block)){m.status="needs an actual suitable pickaxe";return null;}
        if(v.getEyePosition().distanceToSqr(Vec3.atCenterOf(p))>9||!BoatRoutes.visible(level,v,Vec3.atCenterOf(p))){
            m.started=0;
            if(now>=m.nextPath&&WorkNavigation.admit(level,now,v)){
                m.nextPath=now+40;var path=WoodBootstrap.approachPath(level,v,s,p);
                if(path==null){m.target=null;m.nextScan=now+200;return null;}
                v.getNavigation().moveTo(path,.6);
            }
            return m.status="walking to exposed stone";
        }
        v.getNavigation().stop();v.swingForAttack(InteractionHand.MAIN_HAND);
        if(m.started==0||now-m.lastWork>20||!ItemStack.matches(m.tool,v.getMainHandItem())){m.started=now;m.tool=v.getMainHandItem().copy();}
        m.lastWork=now;
        int ticks=(int)Math.ceil(block.getDestroySpeed(level,p)*30/Math.max(1,v.getMainHandItem().getDestroySpeed(block)));
        if(now-m.started<Math.max(1,ticks))return m.status="mining real surface stone";
        var before=HarvestMaterials.snapshot(v.getInventory());
        var drops=Block.getDrops(block,level,p,null,v,v.getMainHandItem());
        var next=new SimpleContainer(v.getInventory().getContainerSize());
        for(int i=0;i<next.getContainerSize();i++)next.setItem(i,v.getInventory().getItem(i).copy());
        if(drops.isEmpty()||drops.stream().anyMatch(a->!WoodCrafting.stoneMaterial(a)||!next.addItem(a.copy()).isEmpty())){
            m.target=null;m.nextScan=now+200;return m.status="stone output or bag capacity blocked";
        }
        if(!surface(level,v,s,p)||!level.getBlockState(p).equals(block)||!ItemStack.matches(m.tool,v.getMainHandItem())
                ||!HarvestMaterials.unchanged(v.getInventory(),before)||!v.getMainHandItem().isCorrectToolForDrops(block))return null;
        if(!level.destroyBlock(p,false,v))return null;
        for(int i=0;i<next.getContainerSize();i++)v.getInventory().setItem(i,next.getItem(i));
        var tool=v.getMainHandItem().get(DataComponents.TOOL);
        if(tool!=null)v.getMainHandItem().hurtAndBreak(tool.damagePerBlock(),v,EquipmentSlot.MAINHAND);
        Villagers.exert(v,.005f);m.blocks++;
        m.receipt=Map.of("gameTime",now,"position",p.toShortString(),"drops",drops.stream().map(ItemStack::toString).toList(),
            "toolBefore",m.tool.toString(),"toolDamageBefore",m.tool.getDamageValue(),"toolDamageAfter",v.getMainHandItem().getDamageValue());
        s.mind.remember("Mined actual surface stone at "+p.toShortString()+" using my pickaxe");
        m.target=null;m.started=0;m.nextScan=now+20;return m.status="gathered native stone drops";
    }
    private AdventureMining(){}
}
