package local.villagers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import local.villagers.genetics.Trait;
import local.villagers.language.Lexicon;
import local.villagers.mind.AgentMind;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelResource;

/** Declared finite currency/stocks; no scripted trade, meal, harvest, movement or recipe after initial setup. */
public final class LocalTradeServerProbe implements ModInitializer {
    private Villager buyer,seller;
    private int ticks,harvests,foodAtPurchase=-1;
    private long lastTrade=-1,lastHarvest=-1;
    private boolean boughtHoe,boughtFood,atePurchase,reopen;
    private JsonObject previous;
    private final ArrayList<Map<String,Object>> events=new ArrayList<>();
    private static UUID founder(boolean selling) {
        for(long i=1;i<100000;i++) {
            var id=new UUID(0x4c4f43414c545241L,i);var mind=new AgentMind(id);
            if(selling?mind.has(Trait.GREEDY) && mind.has(Trait.FRUGAL):mind.has(Trait.GLUTTON))return id;
        }
        throw new AssertionError("No selected founder");
    }
    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                ticks++;
                if(ticks==1) {server.overworld().setChunkForced(0,0,true);server.overworld().getChunk(0,0);server.tickRateManager().setTickRate(80);}
                if(ticks==40) {
                    var file=server.getWorldPath(LevelResource.ROOT).resolve("local-trade-result.json");reopen=Files.exists(file);
                    if(reopen) {previous=JsonParser.parseString(Files.readString(file)).getAsJsonObject();check(previous.get("status").getAsString().equals("passed"),"only passed fixture reopens");}
                    else terrain(server.overworld());
                }
                if(ticks==60)setup(server);
                if(buyer!=null) {
                    check(buyer.isAlive() && seller.isAlive(),"same two adults remain alive");
                    check(buyer.getInventory().countItem(Items.EMERALD)+seller.getInventory().countItem(Items.EMERALD)==4,"all four physical emeralds conserved");
                    if(reopen && ticks==160)finishRestart(server);
                    else if(!reopen)observe(server);
                }
                if(ticks>9000)throw new AssertionError("Native local exchange observation exhausted");
            }catch(Throwable failure) {
                failure.printStackTrace();try{write(server,"failed",failure.toString());}catch(Exception ignored){}server.halt(false);
            }
        });
    }
    private static void terrain(ServerLevel level) {
        for(int x=1;x<=10;x++)for(int z=1;z<=9;z++)for(int y=79;y<=84;y++)
            level.setBlock(new BlockPos(x,y,z),y<=80 || y<=83 && (x==1 || x==10 || z==1 || z==9)
                    ?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        for(int z:new int[]{3,6}) {
            var bed=Blocks.BED.white().defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH);
            level.setBlock(new BlockPos(2,81,z),bed.setValue(BedBlock.PART,BedPart.FOOT),3);
            level.setBlock(new BlockPos(2,81,z+1),bed.setValue(BedBlock.PART,BedPart.HEAD),3);
        }
        level.setBlock(new BlockPos(4,81,3),Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        for(int z=4;z<=6;z++) {
            level.setBlock(new BlockPos(6,80,z),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7),3);
            level.setBlock(new BlockPos(6,81,z),((CropBlock)Blocks.WHEAT).getStateForAge(7),3);
        }
        level.setBlock(new BlockPos(7,80,5),Blocks.WATER.defaultBlockState(),3);
        level.setBlock(new BlockPos(7,81,5),Blocks.STONE.defaultBlockState(),3);
    }
    private void setup(MinecraftServer server) {
        var level=server.overworld();
        if(reopen) {
            buyer=(Villager)level.getEntity(founder(false));seller=(Villager)level.getEntity(founder(true));
            check(buyer!=null && seller!=null,"same saved actors reloaded");Villagers.session(buyer,level);Villagers.session(seller,level);
            record(server,"reopened without supplies or resident/terrain setters");return;
        }
        seller=new Villager(EntityTypes.VILLAGER,level);seller.setUUID(founder(true));seller.snapTo(4.5,81,5.5,0,0);
        buyer=new Villager(EntityTypes.VILLAGER,level);buyer.setUUID(founder(false));buyer.snapTo(5.5,81,5.5,0,0);
        seller.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_HOE));
        seller.getInventory().addItem(new ItemStack(Items.WOODEN_HOE));seller.getInventory().addItem(new ItemStack(Items.BREAD,6));
        buyer.getInventory().addItem(new ItemStack(Items.WHEAT_SEEDS,8));
        level.addFreshEntity(seller);level.addFreshEntity(buyer);VillagerController.adopt(level,seller);VillagerController.adopt(level,buyer);
        // Explicit initial common language, not a post-setup lesson or forced understanding of a trade.
        var language=Lexicon.invent(new Random(551));Villagers.existingSession(seller.getUUID()).mind.lexicon(language);
        Villagers.existingSession(buyer.getUUID()).mind.lexicon(language);
        var operator=new ServerPlayer(server,level,new GameProfile(new UUID(0x4d41524b45544f50L,1),"MarketOperator"),ClientInformation.createDefault());
        operator.snapTo(5.5,81,4.5,0,0);operator.setShiftKeyDown(true);
        var coins=new ItemStack(Items.EMERALD,4);coins.set(DataComponents.CUSTOM_NAME,Component.literal("Finite fixture coin"));
        operator.setItemInHand(InteractionHand.MAIN_HAND,coins);
        for(int i=0;i<4;i++)check(Villagers.offerGift(level,buyer,operator,InteractionHand.MAIN_HAND),"one actual operator-held emerald handed over");
        check(operator.getMainHandItem().isEmpty() && buyer.getInventory().countItem(Items.EMERALD)==4,"finite external four-coin supply transferred exactly once");
        record(server,"declared finite setup: four marked coins via actual operator handoff, seller spare hoe and six bread, buyer seeds; ordinary controller thereafter");
    }
    private void observe(MinecraftServer server) throws Exception {
        var b=Villagers.existingSession(buyer.getUUID());var s=Villagers.existingSession(seller.getUUID());
        var trade=b.lastTrade;
        if(!trade.isEmpty() && ((Number)trade.get("gameTime")).longValue()!=lastTrade) {
            lastTrade=((Number)trade.get("gameTime")).longValue();int price=((Number)trade.get("price")).intValue();
            check(((Number)trade.get("buyerMoneyBefore")).intValue()-((Number)trade.get("buyerMoneyAfter")).intValue()==price,"buyer pays actual quoted money");
            check(((Number)trade.get("sellerMoneyAfter")).intValue()-((Number)trade.get("sellerMoneyBefore")).intValue()==price,"seller receives same money");
            check(((Number)trade.get("buyerGoodsAfter")).intValue()-((Number)trade.get("buyerGoodsBefore")).intValue()==1,"buyer receives one real item");
            check(((Number)trade.get("sellerGoodsBefore")).intValue()-((Number)trade.get("sellerGoodsAfter")).intValue()==1,"seller spends same real item");
            events.add(Map.of("phase","actual paid exchange","receipt",trade));
            if(trade.get("item").equals("minecraft:wooden_hoe"))boughtHoe=true;
            if(trade.get("item").equals("minecraft:bread")) {boughtFood=true;foodAtPurchase=b.mind.foodLevel();}
            record(server,"paid exchange observed");
        }
        var harvest=AdventureActions.lastHarvest(buyer.getUUID());
        if(!harvest.isEmpty() && ((Number)harvest.get("gameTime")).longValue()!=lastHarvest) {
            lastHarvest=((Number)harvest.get("gameTime")).longValue();harvests++;
            check(boughtHoe,"ordinary harvest used a paid tool, never supplied buyer equipment");
            events.add(Map.of("phase","native harvest using purchased hoe","receipt",harvest));
        }
        if(boughtFood && buyer.getInventory().countItem(Items.BREAD)==0 && b.mind.foodLevel()>foodAtPurchase)atePurchase=true;
        if(ticks%400==0)record(server,"observe");
        if(boughtHoe && boughtFood && atePurchase) {
            check(harvests==3 && b.breadCrafted==1,"three native paid-tool harvests funded one actual bread recipe");
            check(b.localPurchases==2 && s.localSales==2 && b.foodSharedReceived==0,"two paid exchanges, no invented charitable transfer");
            check(buyer.getInventory().countItem(Items.EMERALD)==0 && seller.getInventory().countItem(Items.EMERALD)==4,"no mint, burn or virtual wallet");
            for(int i=0;i<seller.getInventory().getContainerSize();i++) {
                var stack=seller.getInventory().getItem(i);
                if(stack.is(Items.EMERALD))check(stack.has(DataComponents.CUSTOM_NAME)
                        && stack.get(DataComponents.CUSTOM_NAME).getString().equals("Finite fixture coin"),"actual coin components survive both payments");
            }
            check(seller.getMainHandItem().is(Items.IRON_HOE),"seller retained its working tool");
            check(seller.getInventory().countItem(Items.BREAD)>=3,"seller retained personal food reserve after sale");
            record(server,"ordinary paid tool, production, paid meal and currency conservation passed");write(server,"passed",null);server.halt(false);
        }
    }
    private void finishRestart(MinecraftServer server) throws Exception {
        var last=previous.getAsJsonArray("events").get(previous.getAsJsonArray("events").size()-1).getAsJsonObject();
        for(var v:List.of(buyer,seller)) {
            var s=Villagers.existingSession(v.getUUID());var expected=last.getAsJsonObject(v==buyer?"buyer":"seller");
            check(s.mind.foodLevel()==expected.get("food").getAsInt(),"meal nutrition retained");
            check(v.getInventory().countItem(Items.EMERALD)==expected.get("money").getAsInt(),"actual money persists without replay");
            check(v.getInventory().countItem(Items.BREAD)==expected.get("bread").getAsInt(),"paid/consumed bread not replayed");
            check(s.localPurchases==0 && s.localSales==0 && s.breadCrafted==0,"no transaction or recipe replay");
        }
        check(buyer.getMainHandItem().is(Items.WOODEN_HOE) && buyer.getMainHandItem().getDamageValue()==3,"purchased tool and actual wear persist");
        record(server,"saved real money, meal, tool wear and no replay passed");write(server,"restart-passed",null);server.halt(false);
    }
    private void record(MinecraftServer server,String phase) {
        var row=new LinkedHashMap<String,Object>();row.put("phase",phase);row.put("tick",server.overworld().getGameTime());
        for(var v:List.of(buyer,seller)) {
            var s=Villagers.existingSession(v.getUUID());var state=new LinkedHashMap<String,Object>();
            state.put("id",v.getUUID().toString());state.put("food",s.mind.foodLevel());state.put("money",v.getInventory().countItem(Items.EMERALD));
            state.put("bread",v.getInventory().countItem(Items.BREAD));state.put("wheat",v.getInventory().countItem(Items.WHEAT));state.put("crafted",s.breadCrafted);
            state.put("bought",s.localPurchases);state.put("sold",s.localSales);state.put("foodRequests",s.foodHelpRequests);
            state.put("held",v.getMainHandItem().toString());state.put("damage",v.getMainHandItem().getDamageValue());row.put(v==buyer?"buyer":"seller",state);
        }
        row.put("harvestsThisLoad",harvests);row.put("meanServerTickMillis",server.getAverageTickTimeNanos()/1e6);events.add(row);
        System.out.println("LOCAL TRADE EVENT: "+new GsonBuilder().create().toJson(row));
    }
    private void write(MinecraftServer server,String status,String failure) throws Exception {
        var out=new LinkedHashMap<String,Object>();out.put("status",status);out.put("failure",failure);out.put("events",events);
        out.put("fixture","NEW isolated supplied peaceful platform,beds,table,water,three mature wheat; initially common language,selected greedy/frugal seller and glutton buyer; seller ironhoe,spare woodenhoe,sixbread; buyer eightseeds,no tool or food,receives four marked coins from finite operator hand via real gifts during setup.80TPS,400base exhaustion/day. No post-setup supplies, body/clock/growth/actor-position setters or scripted exchange,recipe,harvest,meal. Finite currency/goods integration, not normal survival or a sustainable economy.");
        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(reopen?"local-trade-restart-result.json":"local-trade-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out));
    }
    private static void check(boolean condition,String label) {if(!condition)throw new AssertionError(label);}
}
