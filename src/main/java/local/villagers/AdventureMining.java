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

/** Three real drops from naturally exposed stone; no tunnels or structural quarrying. */
final class AdventureMining {
    static boolean naturalRock(net.minecraft.world.level.block.state.BlockState block){
        return block.is(Blocks.STONE)||block.is(Blocks.DEEPSLATE)||block.is(Blocks.ANDESITE)
            ||block.is(Blocks.DIORITE)||block.is(Blocks.GRANITE)||block.is(Blocks.TUFF);
    }
    static final class State {
        BlockPos target,working;
        long nextScan,nextPath,started,lastWork,blocks;
        ItemStack tool=ItemStack.EMPTY;
        String status="not quarrying";
        Map<String,Object> receipt=Map.of();
    }
    static Map<String,Object> status(Session s){return Map.of("activity",s.mining.status,"blocks",s.mining.blocks,"lastReceipt",s.mining.receipt);}
    static boolean surface(ServerLevel level,Villager v,Session s,BlockPos p){
        return VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p))&&exposed(level,v,p);
    }
    private static boolean visible(ServerLevel level,Villager v,BlockPos p){return BlockContact.point(level,v,v.getEyePosition(),p,64)!=null;}
    static net.minecraft.world.level.pathfinder.Path approach(ServerLevel level,Villager v,Session s,BlockPos target){
        var stands=new ArrayList<BlockPos>();var danger=new AABB(target).expandTowards(0,2,0);
        for(var p:BlockPos.betweenClosed(target.offset(-2,-3,-2),target.offset(2,1,2))){
            if(!level.isLoaded(p.below())||!level.isLoaded(p.above())||!level.getBlockState(p).isAir()||!level.getBlockState(p.above()).isAir()
                ||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
                ||BlockContact.point(level,v,ForestryWork.standEye(v,p),target,2.75)==null)continue;
            var body=v.getBoundingBox().move(Vec3.atBottomCenterOf(p).subtract(v.position()));
            if(!body.intersects(danger))stands.add(p.immutable());
        }
        stands.sort(Comparator.comparingDouble(p->p.distSqr(v.blockPosition())));
        for(var p:stands.stream().limit(6).toList()){
            var path=NativePathLimits.search(v,1,()->WorkNavigation.pathTo(level,v,s,p,0));if(!WorkNavigation.staysHome(level,v,s,path))continue;
            int y=v.getBlockY();boolean dry=true;
            for(int i=0;i<path.getNodeCount();i++){
                var node=path.getNodePos(i);
                if(node.getY()<y-1||!level.getFluidState(node).isEmpty()||!level.getFluidState(node.below()).isEmpty()){dry=false;break;}
                y=node.getY();
            }
            if(dry)return path;
        }
        return null;
    }
    static boolean exposed(ServerLevel level,Villager v,BlockPos p){return exposed(level,v,p,false);}
    static boolean exposed(ServerLevel level,Villager v,BlockPos p,boolean excavation){
        return exposedAt(level,v,p,excavation,v.getBlockY());
    }
    /** A recorded junction is inspected relative to that standing floor.
     * Actual mining always calls exposed() again at the body's real height. */
    static boolean exposedAt(ServerLevel level,Villager v,BlockPos p,boolean excavation,int standY){
        return exposedAt(level,v,p,excavation,standY,Set.of());
    }
    /** Prediction for a paid sealing job; actual mining never ignores fluids. */
    static boolean afterWaterSeals(ServerLevel level,Villager v,BlockPos face,int standY,Set<BlockPos> seals){
        if(seals.isEmpty()||seals.stream().anyMatch(p->p.distManhattan(face)!=1||!level.isLoaded(p)||!level.getBlockState(p).is(Blocks.WATER)))return false;
        return exposedAt(level,v,face,true,standY,seals);
    }
    private static boolean exposedAt(ServerLevel level,Villager v,BlockPos p,boolean excavation,int standY,Set<BlockPos> seals){
        if(!level.isLoaded(p)||!level.isLoaded(p.above(2))||p.getY()<standY-1
                ||!level.getWorldBorder().isWithinBounds(p)||ConstructionSites.isProtected(level,p)||MineStair.exitFloor(v,p)||MineWaterSeal.retained(v,p)
                ||AdventureQuarry.accessSupport(v,p)&&!AdventureQuarry.canReplaceAccess(level,v,p)
                )return false;
        var state=level.getBlockState(p);
        if(!(naturalRock(state)||AdventurePreparation.stonePickaxe(v)&&AdventureIron.ore(state)&&(!AdventureIron.diamondOre(state)||AdventurePreparation.reserveIronPickaxe(v))
            ||excavation&&(state.is(Blocks.DIRT)||state.is(Blocks.GRASS_BLOCK)||state.is(Blocks.COARSE_DIRT))))return false;
        boolean open=level.getBlockState(p.above()).isAir()||level.getBlockState(p.below()).isAir();
        for(var d:Direction.Plane.HORIZONTAL)if(level.isLoaded(p.relative(d))&&level.getBlockState(p.relative(d)).isAir())open=true;
        if(!open)return false;
        if(level.getEntities(null,new AABB(p).expandTowards(0,2,0)).stream().anyMatch(e->!(e instanceof net.minecraft.world.entity.item.ItemEntity)&&!(e instanceof net.minecraft.world.entity.ExperienceOrb)))return false;
        for(Direction d:Direction.values()){
            var n=p.relative(d);if(!level.isLoaded(n)||!level.getFluidState(n).isEmpty()&&!seals.contains(n)||level.getBlockState(n).getBlock() instanceof FallingBlock)return false;
        }
        for(BlockPos n:BlockPos.betweenClosed(p.offset(-2,-1,-2),p.offset(2,2,2))){
            if(!level.isLoaded(n))return false;
            var block=level.getBlockState(n);
            if(block.getBlock() instanceof BedBlock||block.getBlock() instanceof DoorBlock||block.is(net.minecraft.tags.BlockTags.PLANKS)
                    ||level.getBlockEntity(n)!=null)return false;
        }
        return NativeTerrainWork.structuralCover(level,p)==null;
    }
    static BlockPos observed(ServerLevel level,Villager v,Session s){
        var origin=v.blockPosition();BlockPos best=null;double nearest=Double.MAX_VALUE;
        for(var p:BlockPos.betweenClosed(origin.offset(-32,-1,-32),origin.offset(32,6,32))){
            if(!AdventureIron.wants(v,s,level.getBlockState(p))||!exposed(level,v,p)||!visible(level,v,p))continue;
            double d=p.distSqr(origin);if(d<nearest){best=p.immutable();nearest=d;}
        }
        return best;
    }
    static String work(ServerLevel level,Villager v,Session s,long now){
        var m=s.mining;
        if(!AdventurePreparation.nominated(v)||!AdventureActions.PERMISSIONS.allows(Capability.MINE_SURFACE_STONE)
                ||!WoodBootstrap.safe(level,v,s)||!AdventureIron.needsMining(v,s)){m.started=0;return null;}
        if(m.target!=null&&(!surface(level,v,s,m.target)||!AdventureIron.wants(v,s,level.getBlockState(m.target)))){m.target=null;m.started=0;}
        if(m.target==null){
            // This bounded perception scan makes no path queries. Taking the
            // actor's scarce navigation slot here starves an active quarry when
            // no surface stone is visible, especially with many other workers.
            if(now<m.nextScan)return null;
            m.nextScan=now+100;var origin=v.blockPosition();double nearest=Double.MAX_VALUE;
            for(BlockPos p:BlockPos.betweenClosed(origin.offset(-12,-1,-12),origin.offset(12,5,12))){
                if(!AdventureIron.wants(v,s,level.getBlockState(p))||!surface(level,v,s,p)||!visible(level,v,p))continue;
                double d=p.distSqr(origin);if(d<nearest){m.target=p.immutable();nearest=d;}
            }
            if(m.target==null){m.status="needs observed exposed natural stone";return null;}
        }
        return mineAt(level,v,s,m.target,now,false);
    }
    static String mineAt(ServerLevel level,Villager v,Session s,BlockPos p,long now,boolean excavation){
        var m=s.mining;var block=level.getBlockState(p);
        if(!AdventureActions.preparePickaxeForMining(level,v,block)){m.status="needs an actual suitable pickaxe";return null;}
        var held=v.getMainHandItem();
        if(excavation&&held.isDamageableItem()&&held.getMaxDamage()-held.getDamageValue()<=8)return m.status="retaining the last usable pickaxe for withdrawal";
        var contact=BlockContact.point(level,v,v.getEyePosition(),p,3);
        if(contact==null){
            m.started=0;
            if(now>=m.nextPath&&WorkNavigation.admit(level,now,v)){
                m.nextPath=now+40;var path=approach(level,v,s,p);
                if(path==null){m.target=null;m.nextScan=now+200;return null;}
                if(v.getNavigation().isDone())v.getNavigation().stop();
                v.getNavigation().moveTo(path,.6);
            }
            return m.status="walking to exposed stone";
        }
        v.getNavigation().stop();v.swingForAttack(InteractionHand.MAIN_HAND);
        if(m.started==0||!p.equals(m.working)||now-m.lastWork>20||!ItemStack.matches(m.tool,v.getMainHandItem())){m.started=now;m.working=p;m.tool=v.getMainHandItem().copy();}
        m.lastWork=now;
        int ticks=(int)Math.ceil(block.getDestroySpeed(level,p)*30/Math.max(1,v.getMainHandItem().getDestroySpeed(block)));
        if(now-m.started<Math.max(1,ticks))return m.status="mining real surface stone";
        var before=HarvestMaterials.snapshot(v.getInventory());
        var drops=Block.getDrops(block,level,p,null,v,v.getMainHandItem());
        var next=new SimpleContainer(v.getInventory().getContainerSize());
        for(int i=0;i<next.getContainerSize();i++)next.setItem(i,v.getInventory().getItem(i).copy());
        if(drops.isEmpty()){
            m.target=null;m.nextScan=now+200;return m.status="stone output or bag capacity blocked";
        }
        var loose=new ArrayList<ItemStack>();
        for(var drop:drops){
            // After the furnace is paid for, a fresh pick does not need a
            // spare common-stone stack occupying the last ore slot. Collect
            // replacement ingredients before the actual tool approaches wear.
            int reserve=s.iron.furnace==null?11:!AdventurePreparation.reservePickaxe(v)||AdventurePreparation.toolUses(v.getMainHandItem())<=32?3:0;
            if(excavation&&(NativeWorkInventory.decorativeRock(drop)||drop.is(net.minecraft.world.item.Items.DIRT)&&next.countItem(net.minecraft.world.item.Items.DIRT)>=64||WoodCrafting.stoneMaterial(drop)&&WoodCrafting.stoneCount(next)>=reserve)){loose.add(drop.copy());continue;}
            var overflow=next.addItem(drop.copy());if(!overflow.isEmpty())loose.add(overflow);
        }
        if((!excavation||AdventureIron.ore(block))&&!loose.isEmpty()){
            var room=NativeWorkInventory.makeRoom(level,v,s);return m.status=room==null?"stone output needs real bag space":room;
        }
        if(!exposed(level,v,p,excavation)||!VillageSurvival.withinHomeRange(level,s.mind,Vec3.atCenterOf(p))||!level.getBlockState(p).equals(block)||!ItemStack.matches(m.tool,v.getMainHandItem())
                ||!HarvestMaterials.unchanged(v.getInventory(),before)||block.requiresCorrectToolForDrops()&&!v.getMainHandItem().isCorrectToolForDrops(block)
                ||BlockContact.point(level,v,v.getEyePosition(),p,3)==null)return null;
        if(!level.destroyBlock(p,false,v))return null;
        for(int i=0;i<next.getContainerSize();i++)v.getInventory().setItem(i,next.getItem(i));
        for(var drop:loose)Block.popResource(level,p,drop);
        block.spawnAfterBreak(level,p,v.getMainHandItem(),true);
        var tool=v.getMainHandItem().get(DataComponents.TOOL);
        if(tool!=null)v.getMainHandItem().hurtAndBreak(tool.damagePerBlock(),v,EquipmentSlot.MAINHAND);
        boolean accessRepaired=AdventureQuarry.accessSupport(v,p)&&AdventureQuarry.repairAccess(level,v,s,now,p)!=null;
        Villagers.exert(v,.005f);m.blocks++;s.quarry.inspectedAll=false;
        m.receipt=Map.of("gameTime",now,"position",p.toShortString(),"drops",drops.stream().map(ItemStack::toString).toList(),
            "toolBefore",m.tool.toString(),"toolDamageBefore",m.tool.getDamageValue(),"toolDamageAfter",v.getMainHandItem().getDamageValue(),
            "contact",contact.toString(),"reach",v.getEyePosition().distanceTo(contact),"accessRepaired",accessRepaired);
        s.mind.remember("Mined actual surface stone at "+p.toShortString()+" using my pickaxe");
        m.target=null;m.started=0;m.nextScan=now+20;return m.status="gathered native stone drops";
    }
    private AdventureMining(){}
}
