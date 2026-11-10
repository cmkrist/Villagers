package local.villagers;

import com.mojang.authlib.GameProfile;
import java.util.*;
import local.villagers.block.KingSizeBedBlock;
import local.villagers.family.Courtship;
import local.villagers.mind.AgentMind;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.entity.ai.village.poi.*;

/** Disposable native fixture: never operates on a player's save. */
public final class KingSizeBedServerProbe implements ModInitializer {
    int ticks, checks;
    Villager a, b;
    Session first, second;
    final BlockPos foot = new BlockPos(5,81,6), nursery = new BlockPos(8,81,5);
    void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++; System.out.println("KING BED CHECK: " + label);
    }
    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                ticks++;
                ServerLevel level = server.overworld();
                if (ticks == 1) { level.setChunkForced(0,0,true); level.getChunk(0,0); }
                if (ticks == 40) setup(level);
                if (ticks == 41) start(level);
                if (ticks > 40 && a != null) {
                    VillageFamilies.tickVisit(level, a, level.getGameTime(), false);
                    if (ticks == 42) check(a.isSleeping() && b.isSleeping(), "both adults use independent sleeping positions");
                    if (ticks == 80) {
                        check(children(level) == 0 && xp(level) == 0, "no birth or XP before the courtship pause completes");
                        VillageFamilies.interrupt(a.getUUID());
                        check(!a.isSleeping() && !b.isSleeping() && children(level)==0 && xp(level)==0,
                                "interruption wakes both adults without birth or XP");
                        check(a.getInventory().countItem(Items.BREAD)==3 && b.getInventory().countItem(Items.BREAD)==3,
                                "interruption leaves both food payments untouched");
                        start(level);
                        VillageFamilies.tickVisit(level,a,level.getGameTime(),true);
                        check(!a.isSleeping() && children(level)==0 && xp(level)==0, "danger cancels a pending visit");
                        start(level);
                        level.destroyBlock(foot,true);
                        VillageFamilies.tickVisit(level,a,level.getGameTime(),false);
                        check(!a.isSleeping() && children(level)==0 && xp(level)==0, "broken bed cancels without birth or XP");
                        var rebuilt=KingSizeBeds.BLOCK.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH);
                        level.setBlock(foot,rebuilt,2|16);
                        KingSizeBeds.BLOCK.setPlacedBy(level,foot,rebuilt,null,new ItemStack(KingSizeBeds.BLOCK));
                    }
                    if (ticks == 82) start(level);
                    if (ticks == 190) {
                        check(children(level)==1, "completed sequence produces exactly one baby");
                        check(xp(level)>=1 && xp(level)<=7, "completed birth awards one to seven XP");
                        check(a.getInventory().countItem(Items.BREAD)==0 && b.getInventory().countItem(Items.BREAD)==0,
                                "successful birth charges each parent's food exactly once");
                        check(!a.isSleeping() && !b.isSleeping(), "successful sequence wakes both adults");
                        for (int i=0;i<10;i++) VillageFamilies.tickVisit(level,a,level.getGameTime(),false);
                        check(children(level)==1, "completed visit cannot spawn a duplicate baby");
                        System.out.println("KING BED PASS: " + checks); server.halt(false);
                    }
                }
            } catch (Throwable failure) { failure.printStackTrace(); System.out.println("KING BED FAILED"); server.halt(false); }
        });
    }
    int children(ServerLevel level) { return level.getEntitiesOfClass(Villager.class,new AABB(0,79,0,16,87,16), Villager::isBaby).size(); }
    int xp(ServerLevel level) { return level.getEntitiesOfClass(ExperienceOrb.class,new AABB(0,79,0,16,87,16)).stream().mapToInt(ExperienceOrb::getValue).sum(); }
    void setup(ServerLevel level) throws Exception {
        for (int x=0;x<16;x++) for (int z=0;z<16;z++) {
            level.setBlock(new BlockPos(x,80,z), Blocks.STONE.defaultBlockState(),3);
            level.setBlock(new BlockPos(x,84,z), Blocks.STONE.defaultBlockState(),3);
        }
        ServerPlayer player = new ServerPlayer(level.getServer(),level,new GameProfile(UUID.randomUUID(),"KingBedProbe"),ClientInformation.createDefault()) {
            @Override public boolean preventsBlockDrops() { return true; }
        };
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            player.setYRot(facing.toYRot());
            BlockPos place = new BlockPos(11,81,11);
            ItemStack stack = new ItemStack(KingSizeBeds.BLOCK);
            BlockItem bedItem = (BlockItem) stack.getItem();
            var context = new BlockPlaceContext(player,InteractionHand.MAIN_HAND,stack,
                    new BlockHitResult(Vec3.atCenterOf(place.below()),Direction.UP,place.below(),false));
            check(bedItem.place(context).consumesAction(), "item places four parts facing " + facing);
            for (int lane=0;lane<2;lane++) for (int part=0;part<2;part++) {
                BlockPos p=place.relative(facing.getClockWise(),lane).relative(facing,part);
                check(level.getBlockState(p).is(KingSizeBeds.BLOCK),"intact placed part " + p);
            }
            BlockPos head=place.relative(facing);
            check(PoiTypes.forState(level.getBlockState(head)).orElseThrow().is(PoiTypes.HOME),"custom head registers as a native home POI");
            // Every possible broken part must collapse the entire bed and drop only one item.
            for (int lane=0;lane<2;lane++) for (int part=0;part<2;part++) {
                if (lane!=0 || part!=0) bedItem.place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(KingSizeBeds.BLOCK),
                    new BlockHitResult(Vec3.atCenterOf(place.below()),Direction.UP,place.below(),false)));
                level.destroyBlock(place.relative(facing.getClockWise(),lane).relative(facing,part),true);
                for (int l=0;l<2;l++) for (int p=0;p<2;p++)
                    check(level.getBlockState(place.relative(facing.getClockWise(),l).relative(facing,p)).isAir(), "breaking any part removes the full bed");
                var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(7,80,7,15,84,15));
                check(drops.stream().mapToInt(e->e.getItem().getCount()).sum()==1 && drops.getFirst().getItem().is(KingSizeBeds.BLOCK.asItem()),"one bed item drops when any part breaks");
                drops.forEach(Entity::discard);
            }
        }
        BlockPos creative = new BlockPos(11,81,11);
        player.setYRot(Direction.NORTH.toYRot());
        for (int lane=0;lane<2;lane++) for (int part=0;part<2;part++) {
            var item=new ItemStack(KingSizeBeds.BLOCK);
            ((BlockItem)item.getItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,item,
                    new BlockHitResult(Vec3.atCenterOf(creative.below()),Direction.UP,creative.below(),false)));
            BlockPos broken=creative.east(lane).north(part);
            KingSizeBeds.BLOCK.playerWillDestroy(level,broken,level.getBlockState(broken),player);
            level.destroyBlock(broken,false);
            check(level.getEntitiesOfClass(ItemEntity.class,new AABB(7,80,7,15,84,15)).isEmpty(), "creative removal has no bed drops");
        }
        var state=KingSizeBeds.BLOCK.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH);
        level.setBlock(foot,state,2|16);
        KingSizeBeds.BLOCK.setPlacedBy(level,foot,state,null,new ItemStack(KingSizeBeds.BLOCK));
        var babyBed=Blocks.BED.white().defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH);
        level.setBlock(nursery.south(),babyBed,2|16);
        level.setBlock(nursery,babyBed.setValue(BedBlock.PART,BedPart.HEAD),3);
        var door=Blocks.OAK_DOOR.defaultBlockState();
        level.setBlock(new BlockPos(7,81,8),door,2|16);
        level.setBlock(new BlockPos(7,82,8),door.setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),3);
        a=parent(level,5.5); b=parent(level,6.5);
        first=Villagers.existingSession(a.getUUID()); second=Villagers.existingSession(b.getUUID());
        long now=level.getGameTime();
        first.courtship=new Courtship.Agreement(b.getUUID(),now,now+1200);
        second.courtship=new Courtship.Agreement(a.getUUID(),now,now+1200);
    }
    Villager parent(ServerLevel level,double x) {
        Villager v=new Villager(EntityTypes.VILLAGER,level); v.snapTo(x,81,4.5,0,0); v.setNoAi(true); v.setOnGround(true);
        v.addTag(Villagers.MANAGED); v.getInventory().addItem(new ItemStack(Items.BREAD,3)); level.addFreshEntity(v);
        Villagers.registerNewborn(new AgentMind(v.getUUID()),level.getGameTime()); return v;
    }
    void start(ServerLevel level) throws Exception {
        // Retry only the random decision; all production readiness and nursery checks still execute.
        var search=VillageFamilies.class.getDeclaredField("NEXT_SEARCH"); search.setAccessible(true);
        var visits=VillageFamilies.class.getDeclaredField("VISITS"); visits.setAccessible(true);
        for (int i=0;i<300 && ((Map<?,?>)visits.get(null)).isEmpty();i++) {
            ((Map<?,?>)search.get(null)).clear(); first.nextFamilyAttempt=second.nextFamilyAttempt=0;
            VillageFamilies.consider(level,a,first,b);
        }
        if (((Map<?,?>)visits.get(null)).isEmpty()) {
            var king=VillageFamilies.class.getDeclaredMethod("kingBed",ServerLevel.class,Villager.class,Villager.class); king.setAccessible(true);
            var path=a.getNavigation().createPath(nursery,1);
            System.out.println("KING DEBUG: king="+king.invoke(null,level,a,b)+" path="+path+" reaches="+(path!=null && path.canReach())+" age="+a.getAge());
        }
        check(!((Map<?,?>)visits.get(null)).isEmpty(),"ordinary checks start a King Size Bed visit; status=" + first.familyStatus + "; nurseries=" + VillageFamilies.nurseries(level,a));
    }
}
