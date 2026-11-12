package local.villagers;

import java.util.Map;
import local.villagers.capability.CapabilityPermissions.Capability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** A one-block feeding opening in a three-high native fence cage. */
final class CampFoodHatch {
    static final class State {
        BlockPos target;long started,lastWork;ItemStack tool=ItemStack.EMPTY;int opened;
        Map<String,Object> receipt=Map.of();
        Map<String,Object> status(){return Map.of("opened",opened,"target",target==null?"":target.toShortString(),"lastReceipt",receipt);}
    }
    static boolean column(BlockState lower,BlockState middle,BlockState upper){
        return lower.is(BlockTags.WOODEN_FENCES)&&middle.is(lower.getBlock())&&upper.is(lower.getBlock());
    }
    private static boolean enabled(){return Villagers.config().nativeCampPassages()
        &&AdventureActions.PERMISSIONS.allows(Capability.OPEN_CAMP_FOOD_HATCH);}
    static Vec3 point(Villager receiver,double height){return receiver.position().add(0,Math.min(height,receiver.getBbHeight()*.8),0);}
    /** A hand may reach the visible side of a body leaning beside a door jamb. */
    static java.util.List<Vec3> contactPoints(AABB body){
        var points=new java.util.ArrayList<Vec3>();
        double x=(body.minX+body.maxX)/2,z=(body.minZ+body.maxZ)/2;
        double dx=body.getXsize()*.4,dz=body.getZsize()*.4;
        for(double h:new double[]{.5,.8,1.1,body.getYsize()*.8}){
            double y=body.minY+Math.min(h,body.getYsize()*.8);
            points.add(new Vec3(x,y,z));
            for(double ox:new double[]{-dx,dx})for(double oz:new double[]{-dz,dz})points.add(new Vec3(x+ox,y,z+oz));
        }
        return points;
    }
    /** Actual empty block ray to a point on the recipient's body, not through
     * a closed gate or fence. An audible request already supplies consent. */
    static Vec3 contact(ServerLevel level,Pillager donor,Villager receiver,Vec3 eye){
        for(var p:contactPoints(receiver.getBoundingBox())){
            var ray=level.clip(new ClipContext(eye,p,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,donor));
            if(ray.getType()==HitResult.Type.MISS&&eye.distanceToSqr(p)<=9)return p;
        }
        return null;
    }
    private static boolean target(ServerLevel level,Pillager donor,Villager receiver,BlockPos p){
        if(!enabled()||p.getY()!=receiver.getBlockY()||!level.isLoaded(p.above(2))
            ||!NativeCampWork.local(level,donor,p)||!NativeIllagers.insideCamp(level,donor,p)
            ||ConstructionSites.isProtected(level,p)||ConstructionSites.isProtected(level,p.above())
            ||!level.getFluidState(p).isEmpty()||!level.getFluidState(p.above()).isEmpty()
            ||!column(level.getBlockState(p),level.getBlockState(p.above()),level.getBlockState(p.above(2)))
            ||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),net.minecraft.core.Direction.UP)
            ||!level.getEntities(null,new AABB(p)).isEmpty())return false;
        var s=Villagers.existingSession(receiver.getUUID());
        if(s==null||!NativeFoodStorage.homeStructure(level,receiver,s,p))return false;
        var building=level.structureManager().getStructureWithPieceAt(p,h->h.getRegisteredName().equals("nova_structures:illager_barracks"));
        return building.isValid();
    }
    static BlockPos proposed(ServerLevel level,Pillager donor,Villager receiver,Vec3 eye){
        if(!enabled())return null;
        for(double h:new double[]{.5,.8,1.1}){
            var end=point(receiver,h);var ray=level.clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,donor));
            if(ray.getType()!=HitResult.Type.BLOCK||!target(level,donor,receiver,ray.getBlockPos())||eye.distanceToSqr(ray.getLocation())>9)continue;
            var fence=ray.getBlockPos();
            var after=level.clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,donor){
                @Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(BlockState state,net.minecraft.world.level.BlockGetter world,BlockPos at){
                    return at.equals(fence)?net.minecraft.world.phys.shapes.Shapes.empty():super.getBlockShape(state,world,at);
                }
            });
            if(after.getType()==HitResult.Type.MISS)return fence.immutable();
        }
        return null;
    }
    static String work(ServerLevel level,Pillager donor,Villager receiver,State w,long now){
        if(donor.getEyePosition().distanceToSqr(receiver.getBoundingBox().getCenter())>16)return null;
        var p=proposed(level,donor,receiver,donor.getEyePosition());
        if(p==null){w.target=null;w.started=0;return null;}
        var block=level.getBlockState(p);var tool=donor.getMainHandItem();
        if(w.started==0||!p.equals(w.target)||now-w.lastWork>20||!ItemStack.matches(tool,w.tool)){
            w.target=p;w.started=now;w.tool=tool.copy();
        }
        w.lastWork=now;donor.getNavigation().stop();donor.swingForAttack(InteractionHand.MAIN_HAND);
        int duration=Math.max(1,(int)Math.ceil(block.getDestroySpeed(level,p)*30/Math.max(1,tool.getDestroySpeed(block))));
        if(now-w.started<duration)return "opening a one-block feeding hatch with the actual held tool";
        if(!target(level,donor,receiver,p)||!ItemStack.matches(tool,w.tool)||!level.getBlockState(p).equals(block)
            ||BlockContact.point(level,donor,donor.getEyePosition(),p,3)==null)return null;
        if(!level.destroyBlock(p,true,donor))return null;
        var data=tool.get(DataComponents.TOOL);if(data!=null)tool.hurtAndBreak(data.damagePerBlock(),donor,EquipmentSlot.MAINHAND);
        w.opened++;w.receipt=Map.of("gameTime",now,"position",p.toShortString(),"duration",now-w.started,
            "tool",w.tool.toString(),"upperFenceRetained",level.getBlockState(p.above()).is(block.getBlock()),"nativeWorldDrops",true);
        w.target=null;w.started=0;CreatureAgents.mind(donor).remember("Opened a one-block food hatch in the actual fence at "+p.toShortString());
        return "opened an actual feeding hatch while retaining the upper cage";
    }
    private CampFoodHatch(){}
}
