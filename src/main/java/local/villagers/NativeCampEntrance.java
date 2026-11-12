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
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.pathfinder.*;
import net.minecraft.world.phys.*;

/** One small entrance in an owned camp, prompted by a real heard voice. No cell exits or free drops. */
final class NativeCampEntrance {
    private static final String JOB="villagers:camp_entrance:",COUNT="villagers:camp_entrance_blocks:",DONE="villagers:camp_entrance_done";
    record Plan(BlockPos outer,Direction inward,int width,BlockPos voice){
        Plan{if(width<1||width>2||inward.getAxis().isVertical())throw new IllegalArgumentException("Bounded horizontal entrance required");}
    }
    static final class State {
        Plan plan;boolean loaded,done;int removed,step,queryBatch;
        long scanAt,pathAt,until,started,lastWork;
        ItemStack tool=ItemStack.EMPTY;String activity="no camp entrance work";
        Map<String,Object> receipt=Map.of();
        Map<String,Object> status(){return Map.of("activity",activity,"removed",removed,"step",step,
            "outer",plan==null?"":plan.outer.toShortString(),"lastReceipt",receipt);}
    }
    static List<BlockPos> cells(Plan plan){
        var result=new ArrayList<BlockPos>();
        for(int i=0;i<plan.width;i++){
            var p=plan.outer.relative(plan.inward,i);result.add(p.above());result.add(p.above(2));result.add(p);
        }
        return List.copyOf(result);
    }
    private static boolean masonry(net.minecraft.world.level.block.state.BlockState b){
        return b.is(Blocks.COBBLESTONE)||b.is(Blocks.MOSSY_COBBLESTONE)||b.is(Blocks.STONE_BRICKS)||b.is(Blocks.MOSSY_STONE_BRICKS)
            ||b.is(Blocks.COBBLESTONE_STAIRS)||b.is(Blocks.MOSSY_COBBLESTONE_STAIRS);
    }
    private static boolean material(net.minecraft.world.level.block.state.BlockState b){return masonry(b)||b.is(Blocks.DIRT)||b.is(Blocks.GRASS_BLOCK);}
    private static boolean enabled(ServerLevel level,Pillager mob){return Villagers.config().nativeCampPassages()&&Villagers.config().nativeCampMeals()
        &&Villagers.LEARNING_MODE!=local.villagers.learning.LearningMode.FROZEN&&mob.isAlive()&&mob.level()==level
        &&CreatureAgents.managed(mob)&&!IllagerFamilies.juvenile(mob)&&NativeRegionsScenario.nativeRegions(level)
        &&NativeIllagers.member(mob)&&AdventureActions.PERMISSIONS.allows(Capability.OPEN_CAMP_PASSAGE);}
    private static double floor(ServerLevel level,BlockPos p){
        double y=WalkNodeEvaluator.getFloorLevel(level,p);var shape=level.getBlockState(p).getCollisionShape(level,p);
        return shape.isEmpty()?y:Math.max(y,p.getY()+shape.max(Direction.Axis.Y));
    }
    private static Vec3 exitFeet(ServerLevel level,Pillager mob,Plan plan){
        var p=plan.outer.relative(plan.inward,plan.width);if(!level.isLoaded(p.above(2))||!NativeCampWork.local(level,mob,p))return null;
        double y=WalkNodeEvaluator.getFloorLevel(level,p);var feet=new Vec3(p.getX()+.5,y,p.getZ()+.5);
        var body=mob.getBoundingBox().move(feet.subtract(mob.position()));
        return Math.abs(y-plan.outer.getY())<=1&&level.getFluidState(p).isEmpty()&&level.noBlockCollision(mob,body)
            &&!level.noBlockCollision(mob,body.move(0,-.03,0))?feet:null;
    }
    private static boolean shape(ServerLevel level,Pillager mob,Plan plan){
        for(int i=0;i<plan.width;i++){
            var footing=plan.outer.relative(plan.inward,i).below();
            if(!level.isLoaded(footing)||!level.getBlockState(footing).isFaceSturdy(level,footing,Direction.UP))return false;
        }
        for(var p:cells(plan)){
            var b=level.getBlockState(p);
            if(!NativeCampWork.local(level,mob,p)||!NativeIllagers.insideCamp(level,mob,p)||ConstructionSites.isProtected(level,p)
                ||level.getBlockEntity(p)!=null||!b.isAir()&&!material(b))return false;
            for(var d:Direction.values()){
                var q=p.relative(d);var next=level.getBlockState(q);var block=next.getBlock();
                if(!level.isLoaded(q)||!level.getFluidState(q).isEmpty()||block instanceof FallingBlock||block instanceof BedBlock
                    ||block instanceof DoorBlock||block instanceof FenceBlock||next.is(Blocks.IRON_BARS))return false;
            }
        }
        return exitFeet(level,mob,plan)!=null;
    }
    /** Native planning starts at a hypothetical far-side stand; no actor or block is changed. */
    static Path afterEntrance(ServerLevel level,Pillager mob,Plan plan){
        var feet=exitFeet(level,mob,plan);if(feet==null)return null;var start=BlockPos.containing(feet.x,Math.ceil(feet.y),feet.z);
        var evaluator=new WalkNodeEvaluator(){
            @Override public Node getStart(){var n=getNode(start);n.type=getPathTypeOfMob(currentContext,start.getX(),start.getY(),start.getZ(),this.mob);
                n.costMalus=this.mob.getPathfindingMalus(n.type);return n;}
        };
        evaluator.setCanOpenDoors(AdventureActions.PERMISSIONS.allows(Capability.OPEN_DOOR));evaluator.setCanPassDoors(true);
        evaluator.setCanFloat(mob.getNavigation().canFloat());var home=WarCombat.home(mob);int spread=NativeIllagers.radius(mob)+2;
        var center=new BlockPos(home.x(),start.getY(),home.z());
        var region=new PathNavigationRegion(level,center.offset(-spread,-8,-spread),center.offset(spread,16,spread));
        var finder=new PathFinder(evaluator,8192);
        var result=NativePathLimits.search(mob,2,()->finder.findPath(region,mob,Set.of(plan.voice),128f,Math.max(0,(int)NativeCampCalls.listeningRadius()-1),1f));
        return NativeIllagers.safePath(level,mob,result)?result:null;
    }
    static Plan find(ServerLevel level,Pillager mob,BlockPos voice,int batch){
        var options=new ArrayList<Plan>();var origin=mob.blockPosition();
        for(var at:BlockPos.betweenClosed(origin.offset(-16,-2,-16),origin.offset(16,2,16))){
            if(!NativeCampWork.local(level,mob,at)||!masonry(level.getBlockState(at))
                ||BlockContact.point(level,mob,mob.getEyePosition(),at.above(),16)==null)continue;
            for(var direction:Direction.Plane.HORIZONTAL)for(int width=1;width<=2;width++){
                if((mob.getX()-at.getX()-.5)*direction.getStepX()+(mob.getZ()-at.getZ()-.5)*direction.getStepZ()>=0)continue;
                var exit=at.relative(direction,width);if(exit.distSqr(voice)>=at.distSqr(voice)-4)continue;
                var p=new Plan(at.immutable(),direction,width,voice.immutable());if(shape(level,mob,p))options.add(p);
            }
        }
        options.sort(Comparator.comparingInt((Plan p)->(int)cells(p).stream().filter(at->!level.getBlockState(at).isAir()).count())
            .thenComparingDouble(p->p.outer.distSqr(origin)));
        for(int i=0;i<Math.min(6,options.size());i++){
            var p=options.get(Math.floorMod((long)batch*6+i,options.size()));
            if(outsidePath(level,mob,p)!=null&&afterEntrance(level,mob,p)!=null)return p;
        }
        return null;
    }
    private static Path outsidePath(ServerLevel level,Pillager mob,Plan plan){
        var outside=plan.outer.relative(plan.inward.getOpposite());
        var stand=BlockPos.containing(outside.getX()+.5,Math.ceil(floor(level,outside)),outside.getZ()+.5);
        return CampBeds.pathToStands(level,mob,List.of(stand));
    }
    private static void save(Pillager mob,State s){
        mob.entityTags().stream().filter(t->t.startsWith(JOB)||t.startsWith(COUNT)).toList().forEach(mob::removeTag);
        mob.addTag(COUNT+s.removed);if(s.done)mob.addTag(DONE);
        if(s.plan!=null)mob.addTag(JOB+s.plan.outer.asLong()+":"+s.plan.inward.name()+":"+s.plan.width+":"+s.plan.voice.asLong()+":"+s.step);
    }
    private static void load(Pillager mob,State s,long now){
        if(s.loaded)return;s.loaded=true;s.done=mob.entityTags().contains(DONE);
        for(var tag:mob.entityTags())try{
            if(tag.startsWith(COUNT))s.removed=Math.clamp(Integer.parseInt(tag.substring(COUNT.length())),0,6);
            if(tag.startsWith(JOB)){
                var a=tag.substring(JOB.length()).split(":");if(a.length!=5)continue;
                s.plan=new Plan(BlockPos.of(Long.parseLong(a[0])),Direction.valueOf(a[1]),Integer.parseInt(a[2]),BlockPos.of(Long.parseLong(a[3])));
                s.step=Math.clamp(Integer.parseInt(a[4]),0,cells(s.plan).size());s.until=now+2400;
            }
        }catch(IllegalArgumentException ignored){}
    }
    static boolean begin(ServerLevel level,Pillager mob,State s,BlockPos voice,long now){
        if(!enabled(level,mob))return false;load(mob,s,now);if(s.plan!=null||s.done||s.removed>=6||now<s.scanAt)return false;
        s.scanAt=now+200;s.plan=find(level,mob,voice,s.queryBatch++);if(s.plan==null)return false;
        s.step=0;s.until=now+2400;s.started=0;save(mob,s);return true;
    }
    static String work(ServerLevel level,Pillager mob,State s,long now){
        if(!enabled(level,mob))return null;load(mob,s,now);if(s.plan==null||s.done)return null;
        if(now>=s.until||!shape(level,mob,s.plan)){
            s.done=s.removed>0;s.plan=null;s.started=0;save(mob,s);return null;
        }
        var actor=CreatureAgents.loadedState(mob);
        if(actor==null||actor.mind.foodLevel()<13||actor.mind.fatigue()>.65||mob.getHealth()<mob.getMaxHealth()*.9||!mob.onGround()
            ||mob.isOnFire()||mob.isInWater()||mob.isInLava()||!mob.getActiveEffects().isEmpty())return null;
        var cells=cells(s.plan);
        while(s.step<cells.size()&&level.getBlockState(cells.get(s.step)).isAir())s.step++;
        if(s.step>=cells.size()){s.done=true;s.plan=null;save(mob,s);return null;}
        if(s.removed>=6)return null;
        var p=cells.get(s.step);var block=level.getBlockState(p);
        if(!level.getEntities(null,new AABB(p).expandTowards(0,2,0)).stream().allMatch(e->e instanceof ItemEntity||e instanceof net.minecraft.world.entity.ExperienceOrb)){
            s.started=0;return s.activity="waiting for a body to leave the camp entrance work";
        }
        if(BlockContact.point(level,mob,mob.getEyePosition(),p,3)==null){
            s.started=0;if(now>=s.pathAt){s.pathAt=now+40;var path=outsidePath(level,mob,s.plan);if(path!=null)mob.getNavigation().moveTo(path,.8);}
            return s.activity="approaching a visible camp entrance work face";
        }
        mob.getNavigation().stop();mob.swingForAttack(InteractionHand.MAIN_HAND);
        if(s.started==0||now-s.lastWork>20||!ItemStack.matches(s.tool,mob.getMainHandItem())){s.started=now;s.tool=mob.getMainHandItem().copy();}
        s.lastWork=now;boolean harvest=!block.requiresCorrectToolForDrops()||mob.getMainHandItem().isCorrectToolForDrops(block);
        int duration=Math.max(1,(int)Math.ceil(block.getDestroySpeed(level,p)*(harvest?30:100)/Math.max(1,mob.getMainHandItem().getDestroySpeed(block))));
        if(now-s.started<duration)return s.activity="working on the narrow camp entrance";
        var before=HarvestMaterials.snapshot(mob.getInventory());var next=new SimpleContainer(mob.getInventory().getContainerSize());
        for(int i=0;i<next.getContainerSize();i++)next.setItem(i,mob.getInventory().getItem(i).copy());
        var drops=harvest?Block.getDrops(block,level,p,null,mob,mob.getMainHandItem()):List.<ItemStack>of();var loose=new ArrayList<ItemStack>();
        for(var drop:drops){var left=next.addItem(drop.copy());if(!left.isEmpty())loose.add(left);}
        if(!shape(level,mob,s.plan)||!block.equals(level.getBlockState(p))||!ItemStack.matches(s.tool,mob.getMainHandItem())
            ||!HarvestMaterials.unchanged(mob.getInventory(),before)||BlockContact.point(level,mob,mob.getEyePosition(),p,3)==null||!level.destroyBlock(p,false,mob))return null;
        for(int i=0;i<next.getContainerSize();i++)mob.getInventory().setItem(i,next.getItem(i));for(var drop:loose)Block.popResource(level,p,drop);
        var tool=mob.getMainHandItem().get(DataComponents.TOOL);if(tool!=null)mob.getMainHandItem().hurtAndBreak(tool.damagePerBlock(),mob,EquipmentSlot.MAINHAND);
        s.step++;s.removed++;s.started=0;save(mob,s);actor.mind.exert(.005f);
        s.receipt=Map.of("gameTime",now,"position",p.toShortString(),"duration",duration,"tool",s.tool.toString(),"drops",drops.stream().map(ItemStack::toString).toList());
        actor.mind.remember("Worked a real camp entrance block at "+p.toShortString()+" while investigating a heard voice");
        return s.activity="removed a real block from the narrow camp entrance";
    }
    private NativeCampEntrance(){}
}
