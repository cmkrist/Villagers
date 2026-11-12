package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;

/** One small entrance to a known free bunk in the actor's own native camp. */
final class NativeCampAccess {
    private static final String JOB="villagers:camp_access:",COUNT="villagers:camp_access_blocks:",DONE="villagers:camp_access_done";
    private record Plan(BlockPos bed,BlockPos outer,Direction outward){}
    static final class State {
        Plan plan;int step,removed;boolean loaded,done;
        long scanAt,pathAt,until,started,lastWork;
        ItemStack tool=ItemStack.EMPTY;
        String activity="no bunkhouse access work";
        Map<String,Object> receipt=Map.of();
        Map<String,Object> failure=Map.of();
    }
    static Map<String,Object> status(State s){return Map.of("activity",s.activity,"blocks",s.removed,"step",s.step,
        "bed",s.plan==null?"":s.plan.bed.toShortString(),"lastReceipt",s.receipt,"lastFailure",s.failure);}
    /** Three-high clearance through both wall cells permits a jump onto a raised native bed. */
    static List<BlockPos> cells(BlockPos outer,Direction outward){
        var inner=outer.relative(outward.getOpposite());
        return List.of(outer.above(),outer.above(2),outer,inner.above(),inner,inner.above(2));
    }
    private static boolean masonry(net.minecraft.world.level.block.state.BlockState b){
        return b.is(Blocks.COBBLESTONE)||b.is(Blocks.MOSSY_COBBLESTONE)||b.is(Blocks.STONE_BRICKS)||b.is(Blocks.MOSSY_STONE_BRICKS)
            ||b.is(Blocks.COBBLESTONE_STAIRS)||b.is(Blocks.MOSSY_COBBLESTONE_STAIRS);
    }
    private static boolean occupied(ServerLevel level,BlockPos p){
        return level.getEntities(null,new AABB(p).expandTowards(0,2,0)).stream()
            .anyMatch(e->!(e instanceof ItemEntity)&&!(e instanceof net.minecraft.world.entity.ExperienceOrb));
    }
    private static boolean masonryCell(ServerLevel level,Pillager mob,BlockPos p){
        if(!NativeCampWork.local(level,mob,p)||!NativeIllagers.insideCamp(level,mob,p)||level.getBlockEntity(p)!=null
            ||!masonry(level.getBlockState(p)))return false;
        for(var d:Direction.values()){
            var next=p.relative(d);
            if(!level.isLoaded(next)||!level.getFluidState(next).isEmpty()||level.getBlockState(next).getBlock() instanceof FallingBlock)return false;
        }
        return true;
    }
    private static boolean editable(ServerLevel level,Pillager mob,BlockPos p){return masonryCell(level,mob,p)&&!occupied(level,p);}
    private static boolean shape(ServerLevel level,Pillager mob,Plan p){
        if(p.outward.getAxis().isVertical())return false;
        var b=level.getBlockState(p.bed);
        if(!CampBeds.reusable(level,mob,p.bed)){
            // A native neighbor update can remove a generated upper bed and
            // expose a usable lower bunk. Revalidate that actual remaining bed.
            var lower=p.bed.below();b=level.getBlockState(lower);
            if(!level.getBlockState(p.bed).isAir()||!(b.getBlock() instanceof BedBlock)
                ||b.getValue(BedBlock.PART)!=net.minecraft.world.level.block.state.properties.BedPart.HEAD
                ||!NativeIllagers.insideCamp(level,mob,lower)||!TraderRecruitment.allowedBed(level,lower)
                ||level.getPoiManager().findAll(t->t.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME),lower::equals,lower,1,
                    net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.HAS_SPACE).findAny().isEmpty())return false;
        }
        var foot=p.bed.relative(b.getValue(BedBlock.FACING).getOpposite());
        if(List.of(p.bed.below(),foot.below()).stream().noneMatch(end->end.relative(p.outward,2).equals(p.outer)))return false;
        var inner=p.outer.relative(p.outward.getOpposite());
        for(var floor:List.of(p.outer.below(),inner.below()))
            if(!NativeCampWork.local(level,mob,floor)||!level.getBlockState(floor).isFaceSturdy(level,floor,Direction.UP))return false;
        // A body passing the opening pauses actual digging; it does not invalidate
        // the observed geometry or permanently abandon this paid work already done.
        return cells(p.outer,p.outward).stream().allMatch(cell->level.getBlockState(cell).isAir()||masonryCell(level,mob,cell));
    }
    private static void save(Pillager mob,State s){
        mob.entityTags().stream().filter(t->t.startsWith(JOB)||t.startsWith(COUNT)).toList().forEach(mob::removeTag);
        mob.addTag(COUNT+s.removed);if(s.done)mob.addTag(DONE);
        if(s.plan!=null)mob.addTag(JOB+s.plan.bed.asLong()+":"+s.plan.outer.asLong()+":"+s.plan.outward.name()+":"+s.step);
    }
    private static void load(Pillager mob,State s,long now){
        if(s.loaded)return;s.loaded=true;s.done=mob.entityTags().contains(DONE);
        for(var tag:mob.entityTags())try{
            if(tag.startsWith(COUNT))s.removed=Math.clamp(Integer.parseInt(tag.substring(COUNT.length())),0,6);
            if(tag.startsWith(JOB)){
                var parts=tag.substring(JOB.length()).split(":");if(parts.length!=4)continue;
                var direction=Direction.valueOf(parts[2]);int step=Integer.parseInt(parts[3]);
                if(direction.getAxis().isVertical()||step<0||step>6)continue;
                s.plan=new Plan(BlockPos.of(Long.parseLong(parts[0])),BlockPos.of(Long.parseLong(parts[1])),direction);s.step=step;s.until=now+4800;
            }
        }catch(IllegalArgumentException ignored){}
    }
    private static void finish(Pillager mob,State s,long now){
        if(s.removed>0)s.done=true;s.plan=null;s.step=0;s.started=0;s.scanAt=now+600;save(mob,s);
    }
    private static net.minecraft.world.level.pathfinder.Path outsidePath(ServerLevel level,Pillager mob,Plan p,BlockPos target){
        var stands=new ArrayList<BlockPos>();
        for(var stand:BlockPos.betweenClosed(p.outer.offset(-2,-1,-2),p.outer.offset(2,0,2))){
            if(!NativeCampWork.local(level,mob,stand)||!level.isLoaded(stand.above())||!level.getBlockState(stand).isAir()
                ||!level.getBlockState(stand.above()).isAir()||!level.getBlockState(stand.below()).isFaceSturdy(level,stand.below(),Direction.UP))continue;
            var eye=Vec3.atBottomCenterOf(stand).add(0,mob.getEyeHeight(),0);
            if(BlockContact.point(level,mob,eye,target,2.75)!=null)stands.add(stand.immutable());
        }
        stands.sort(Comparator.comparingDouble(stand->stand.distSqr(mob.blockPosition())));
        for(var stand:stands.stream().limit(6).toList()){
            var path=mob.getNavigation().createPath(stand,0,64);if(NativeIllagers.safePath(level,mob,path))return path;
        }
        return null;
    }
    static String work(ServerLevel level,Pillager mob,CreatureAgents.State actor,CampBeds.State bunk,State s,long now){
        if(!Villagers.config().nativeCampPassages()||!NativeRegionsScenario.nativeRegions(level)||!NativeIllagers.member(mob)
            ||!AdventureActions.PERMISSIONS.allows(Capability.OPEN_CAMP_PASSAGE))return null;
        load(mob,s,now);if(s.done||s.removed>=6)return null;
        if(s.plan!=null&&(!shape(level,mob,s.plan)||now>=s.until)){
            s.failure=Map.of("gameTime",now,"expired",now>=s.until,"bed",level.getBlockState(s.plan.bed).toString(),
                "lower",level.getBlockState(s.plan.bed.below()).toString(),"cells",cells(s.plan.outer,s.plan.outward).stream()
                    .map(p->Map.of("position",p.toShortString(),"block",level.getBlockState(p).toString(),"occupied",occupied(level,p))).toList());
            finish(mob,s,now);return null;
        }
        if(s.plan==null){
            var path=mob.getNavigation().getPath();
            if(path!=null&&!path.isDone()&&bunk.blocked.containsKey(path.getTarget()))return s.activity="surveying access to a known free bunk";
            if(now<s.scanAt||!mob.onGround())return null;s.scanAt=now+100;
            var beds=bunk.blocked.keySet().stream().filter(p->CampBeds.reusable(level,mob,p))
                .sorted(Comparator.comparingDouble(p->p.distSqr(mob.blockPosition()))).limit(3).toList();
            for(var bed:beds){
                var b=level.getBlockState(bed);var foot=bed.relative(b.getValue(BedBlock.FACING).getOpposite());
                outer:for(var end:List.of(bed.below(),foot.below()))for(var direction:Direction.Plane.HORIZONTAL){
                    var candidate=new Plan(bed,end.relative(direction,2),direction);
                    if(!shape(level,mob,candidate)||!cells(candidate.outer,candidate.outward).stream().allMatch(p->masonry(level.getBlockState(p))))continue;
                    if(BlockContact.point(level,mob,mob.getEyePosition(),cells(candidate.outer,candidate.outward).getFirst(),16)==null)continue;
                    var route=outsidePath(level,mob,candidate,cells(candidate.outer,candidate.outward).getFirst());if(route==null)continue;
                    s.plan=candidate;s.step=0;s.until=now+4800;save(mob,s);break outer;
                }
                if(s.plan!=null)break;
                if(NativeIllagers.returnHome(level,mob,bed))return s.activity="walking a useful native route toward the blocked bunkhouse";
            }
            if(s.plan==null){s.activity="no observed small masonry entrance to a free bunk";return null;}
        }
        var targets=cells(s.plan.outer,s.plan.outward);
        while(s.step<targets.size()&&level.getBlockState(targets.get(s.step)).isAir())s.step++;
        if(s.step>=targets.size()){finish(mob,s,now);return s.activity="opened a native bunkhouse entrance";}
        var p=targets.get(s.step);if(!editable(level,mob,p)){s.started=0;return null;}
        if(!mob.onGround()||!mob.getActiveEffects().isEmpty()){s.started=0;return s.activity="waiting to work on steady ground";}
        var hit=BlockContact.point(level,mob,mob.getEyePosition(),p,3);
        if(hit==null){
            s.started=0;
            if(now>=s.pathAt&&mob.onGround()){
                // The first wall block may already be air. Approach the current
                // remaining block rather than trying to make contact with air.
                s.pathAt=now+40;var route=outsidePath(level,mob,s.plan,p);
                if(route!=null){if(mob.getNavigation().isDone())mob.getNavigation().stop();mob.getNavigation().moveTo(route,.7);}
            }
            return s.activity="approaching the observed bunkhouse masonry";
        }
        mob.getNavigation().stop();mob.swingForAttack(InteractionHand.MAIN_HAND);var block=level.getBlockState(p);
        if(s.started==0||now-s.lastWork>20||!ItemStack.matches(s.tool,mob.getMainHandItem())){s.started=now;s.tool=mob.getMainHandItem().copy();}
        s.lastWork=now;
        int duration=(int)Math.ceil(block.getDestroySpeed(level,p)*(mob.getMainHandItem().isCorrectToolForDrops(block)?30:100)
            /Math.max(1,mob.getMainHandItem().getDestroySpeed(block)));
        if(now-s.started<Math.max(1,duration))return s.activity="opening the camp masonry with ordinary block-breaking effort";
        var before=HarvestMaterials.snapshot(mob.getInventory());var next=new SimpleContainer(mob.getInventory().getContainerSize());
        for(int i=0;i<next.getContainerSize();i++)next.setItem(i,mob.getInventory().getItem(i).copy());
        boolean harvest=!block.requiresCorrectToolForDrops()||mob.getMainHandItem().isCorrectToolForDrops(block);
        var drops=harvest?Block.getDrops(block,level,p,null,mob,mob.getMainHandItem()):List.<ItemStack>of();
        var loose=new ArrayList<ItemStack>();
        for(var drop:drops){var remaining=next.addItem(drop.copy());if(!remaining.isEmpty())loose.add(remaining);}
        if(!editable(level,mob,p)||!block.equals(level.getBlockState(p))||!ItemStack.matches(s.tool,mob.getMainHandItem())
            ||!HarvestMaterials.unchanged(mob.getInventory(),before)||BlockContact.point(level,mob,mob.getEyePosition(),p,3)==null
            ||!level.destroyBlock(p,false,mob))return null;
        for(int i=0;i<next.getContainerSize();i++)mob.getInventory().setItem(i,next.getItem(i));
        for(var drop:loose)Block.popResource(level,p,drop);
        var tool=mob.getMainHandItem().get(DataComponents.TOOL);if(tool!=null)mob.getMainHandItem().hurtAndBreak(tool.damagePerBlock(),mob,EquipmentSlot.MAINHAND);
        s.step++;s.removed++;s.started=0;save(mob,s);actor.mind.exert(.005f);
        s.receipt=Map.of("gameTime",now,"position",p.toShortString(),"bed",s.plan.bed.toShortString(),"duration",duration,
            "tool",s.tool.toString(),"drops",drops.stream().map(ItemStack::toString).toList(),"loose",loose.stream().map(ItemStack::toString).toList());
        actor.mind.remember("Opened a real camp wall block at "+p.toShortString()+" to reach an available bunk");
        return s.activity="removed actual camp masonry for bunk access";
    }
    private NativeCampAccess(){}
}
