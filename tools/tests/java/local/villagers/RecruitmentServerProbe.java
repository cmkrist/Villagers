package local.villagers;

import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;

/** Isolated dedicated-server acceptance fixture; packaged as a separate test mod, never in the release. */
public final class RecruitmentServerProbe implements ModInitializer {
    private static final BlockPos PLAYER_BED = new BlockPos(4, 81, 4), SPARE = new BlockPos(8, 81, 4);
    private Villager recruit;
    private int ticks;
    private int checks;
    private void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
        System.out.println("RECRUITMENT CHECK: " + label);
    }
    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                ticks++;
                if (ticks == 1) { server.overworld().setChunkForced(0, 0, true); server.overworld().getChunk(0, 0); }
                if (ticks == 40) setup(server);
                if (ticks == 100 && recruit != null) finish(server);
            } catch (Throwable failure) {
                failure.printStackTrace();
                System.out.println("RECRUITMENT FAILED");
                server.halt(false);
            }
        });
    }
    private void setup(MinecraftServer server) throws Exception {
        ServerLevel level = server.overworld();
        level.getChunk(0, 0);
        var marker = server.getWorldPath(LevelResource.ROOT).resolve("recruitment-probe-id.txt");
        if (Files.exists(marker)) {
            UUID id = UUID.fromString(Files.readString(marker));
            var saved = level.getEntity(id);
            check(saved instanceof Villager, "converted entity survives restart");
            recruit = (Villager) saved;
            check(Villagers.isManaged(recruit), "managed tag survives restart");
            check(TraderRecruitment.anchor(recruit) != null, "household anchor survives restart");
            check(!TraderRecruitment.allowedBed(level, PLAYER_BED), "offline player's bed remains protected after restart");
            check(!TraderRecruitment.allowedBed(level, PLAYER_BED.south()), "foot remains protected after restart");
            check(TraderRecruitment.allowedBed(level, SPARE), "spare bed remains available");
            System.out.println("RECRUITMENT RESTART PASS: " + checks);
            recruit = null;
            server.halt(false);
            return;
        }
        for (int x=0;x<16;x++) for (int z=0;z<16;z++) {
            level.setBlock(new BlockPos(x,80,z), Blocks.STONE.defaultBlockState(),3);
            level.setBlock(new BlockPos(x,84,z), Blocks.STONE.defaultBlockState(),3);
        }
        bed(level, PLAYER_BED);
        bed(level, SPARE);
        ServerPlayer owner = new ServerPlayer(server, level,
                new GameProfile(UUID.randomUUID(), "RecruitmentProbe"), ClientInformation.createDefault()) {
            @Override public void sendSystemMessage(Component message) { System.out.println("TRADER: " + message.getString()); }
        };
        owner.snapTo(2,81,2,0,0);
        owner.setShiftKeyDown(true);
        WanderingTrader trader = new WanderingTrader(EntityTypes.WANDERING_TRADER, level);
        trader.snapTo(2,81,2,0,0);
        level.addFreshEntity(trader);
        click(owner,level,trader);
        check(!trader.entityTags().contains("villagers:recruitment_rolled"), "missing home does not consume chance");
        owner.setRespawnPosition(new ServerPlayer.RespawnConfig(new LevelData.RespawnData(GlobalPos.of(level.dimension(), PLAYER_BED),0,0),false),false);
        owner.setShiftKeyDown(false);
        check(click(owner,level,trader) == InteractionResult.PASS, "ordinary trading remains available");
        owner.setShiftKeyDown(true);
        click(owner,level,trader);
        check(trader.isAlive() && !trader.isRemoved(), "first willing interaction only offers recruitment");
        check(trader.entityTags().contains("villagers:recruitment_willing"), "100 percent config makes a willing trader");
        click(owner,level,trader);
        check(trader.isRemoved(), "accepted trader is removed exactly once");
        recruit = level.getEntitiesOfClass(Villager.class,trader.getBoundingBox().inflate(4),Villagers::isManaged).stream().findFirst().orElseThrow();
        check(TraderRecruitment.anchor(recruit).x() == PLAYER_BED.getX(), "recruit uses player home as household anchor");
        check(recruit.isPersistenceRequired(), "recruit no longer has wandering trader despawn");
        check(!TraderRecruitment.allowedBed(level, PLAYER_BED), "player bed head is protected");
        check(!TraderRecruitment.allowedBed(level, PLAYER_BED.south()), "player bed foot is protected");
        check(TraderRecruitment.canClaim(level,recruit,SPARE), "nearby spare is eligible");
        check(!TraderRecruitment.canClaim(level,recruit,new BlockPos(40,81,4)), "recruit cannot settle away from household");
        VillagerController.setHome(level,recruit,Villagers.session(recruit,level),PLAYER_BED);
        check(Villagers.session(recruit,level).mind.home() == null || Villagers.session(recruit,level).mind.home().x() != PLAYER_BED.getX(), "explicit home assignment cannot steal player bed");
        WanderingTrader declined = new WanderingTrader(EntityTypes.WANDERING_TRADER, level);
        declined.snapTo(2,81,2,0,0);
        declined.addTag("villagers:recruitment_rolled");
        level.addFreshEntity(declined);
        for (int i=0;i<5;i++) click(owner,level,declined);
        check(!declined.entityTags().contains("villagers:recruitment_willing") && !declined.isRemoved(), "repeated clicks cannot reroll a declined trader");
        Files.writeString(marker,recruit.getUUID().toString());
    }
    private void finish(MinecraftServer server) {
        ServerLevel level = server.overworld();
        VillagerController.setHome(level,recruit,Villagers.session(recruit,level),SPARE);
        check(Villagers.session(recruit,level).mind.home() != null && Villagers.session(recruit,level).mind.home().x() == SPARE.getX(), "recruit can actually reserve spare bed POI");
        check(!recruit.isSleeping() || recruit.getSleepingPos().orElseThrow().equals(SPARE), "recruit never sleeps in player bed");
        Villagers.persistAll();
        System.out.println("RECRUITMENT PASS: " + checks);
        server.halt(false);
    }
    private static InteractionResult click(ServerPlayer player,ServerLevel level,WanderingTrader trader) {
        return UseEntityCallback.EVENT.invoker().interact(player,level,InteractionHand.MAIN_HAND,trader,null);
    }
    private static void bed(ServerLevel level, BlockPos head) {
        var state = Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH);
        level.setBlock(head.south(),state.setValue(BedBlock.PART,BedPart.FOOT),3);
        level.setBlock(head,state.setValue(BedBlock.PART,BedPart.HEAD),3);
    }
}
