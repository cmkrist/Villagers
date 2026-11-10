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
public final class MarketProductionServerProbe implements ModInitializer {
    private Villager buyer,seller;
    private int ticks,harvests,foodAtPurchase=-1;
    private long lastTrade=-1,lastHarvest=-1;
    private boolean boughtHoe,boughtFood,atePurchase,reopen;
    private JsonObject previous;
    private final ArrayList<Map<String,Object>> events=new ArrayList<>();
    private static UUID founder(boolean selling) {
        for(long i=1;i<100000;i++) {
            var id=new UUID(0x50524f444d41524bL,i);var mind=new AgentMind(id);
            if(selling?mind.has(Trait.GREEDY) && mind.has(Trait.GLUTTON):mind.has(Trait.GREEDY) && mind.has(Trait.FRUGAL))return id;
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
                    check(buyer.getX()>7 && seller.getX()<7,"native counter compartments remain separate; never correct actors by moving them");
                    check(buyer.getInventory().countItem(Items.EMERALD)+seller.getInventory().countItem(Items.EMERALD)==2,"both physical emeralds conserved");
                    for(var v:List.of(buyer,seller))for(int slot=0;slot<v.getInventory().getContainerSize();slot++) {
                        var coin=v.getInventory().getItem(slot);
                        if(coin.is(Items.EMERALD))check(coin.has(DataComponents.CUSTOM_NAME)
                                && coin.get(DataComponents.CUSTOM_NAME).getString().equals("Finite fixture coin"),"original coin components retained, including reload");
                    }
                    if(reopen && ticks==160)finishRestart(server);
                    else if(!reopen)observe(server);
                }
                if(ticks>12000)throw new AssertionError("Native local exchange observation exhausted");
            }catch(Throwable failure) {
                failure.printStackTrace();try{write(server,"failed",failure.toString());}catch(Exception ignored){}server.halt(false);
            }
        });
    }
    private static void terrain(ServerLevel level) {
        for(int x=4;x<=14;x++)for(int z=2;z<=7;z++)for(int y=79;y<=84;y++)
            level.setBlock(new BlockPos(x,y,z),y==84?Blocks.GLOWSTONE.defaultBlockState():y<=80 || y<=83 && (x==4 || x==14 || z==2 || z==7)
                    ?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        for(int z=2;z<=7;z++)for(int y=81;y<=83;y++)
            level.setBlock(new BlockPos(7,y,z),z>=3 && z<=6
                    ?(y==81?Blocks.OAK_FENCE:Blocks.AIR).defaultBlockState():Blocks.STONE.defaultBlockState(),3);
        for(int x:new int[]{5,8}) {
            var bed=Blocks.BED.white().defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH);
            level.setBlock(new BlockPos(x,81,3),bed.setValue(BedBlock.PART,BedPart.FOOT),3);
            level.setBlock(new BlockPos(x,81,4),bed.setValue(BedBlock.PART,BedPart.HEAD),3);
            level.setBlock(new BlockPos(x,81,6),Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        }
        for(int x=10;x<=12;x++)for(int z=3;z<=6;z++) {
            level.setBlock(new BlockPos(x,80,z),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7),3);
            level.setBlock(new BlockPos(x,81,z),((CropBlock)Blocks.WHEAT).getStateForAge(7),3);
        }
        level.setBlock(new BlockPos(13,80,5),Blocks.WATER.defaultBlockState(),3);
        level.setBlock(new BlockPos(13,81,5),Blocks.STONE.defaultBlockState(),3);
    }
    private void setup(MinecraftServer server) {
        var level=server.overworld();
        if(reopen) {
            buyer=(Villager)level.getEntity(founder(false));seller=(Villager)level.getEntity(founder(true));
            check(buyer!=null && seller!=null,"same saved actors reloaded");Villagers.session(buyer,level);Villagers.session(seller,level);
            record(server,"reopened without supplies or resident/terrain setters");return;
        }
        seller=new Villager(EntityTypes.VILLAGER,level);seller.setUUID(founder(true));seller.snapTo(6.5,81,5.5,0,0);
        buyer=new Villager(EntityTypes.VILLAGER,level);buyer.setUUID(founder(false));buyer.snapTo(8.5,81,5.5,0,0);
        var ownHoe=new ItemStack(Items.WOODEN_HOE);ownHoe.setDamageValue(17);seller.setItemSlot(EquipmentSlot.MAINHAND,ownHoe);
        seller.getInventory().addItem(new ItemStack(Items.OAK_LOG));seller.getInventory().addItem(new ItemStack(Items.BREAD,3));
        buyer.getInventory().addItem(new ItemStack(Items.WHEAT_SEEDS,16));buyer.getInventory().addItem(new ItemStack(Items.CARROT,6));
        level.addFreshEntity(seller);level.addFreshEntity(buyer);VillagerController.adopt(level,seller);VillagerController.adopt(level,buyer);
        // Explicit initial common language, not a post-setup lesson or forced understanding of a trade.
        var language=Lexicon.invent(new Random(551));Villagers.existingSession(seller.getUUID()).mind.lexicon(language);
        Villagers.existingSession(buyer.getUUID()).mind.lexicon(language);
        var operator=new ServerPlayer(server,level,new GameProfile(new UUID(0x4d41524b45544f50L,1),"MarketOperator"),ClientInformation.createDefault());
        operator.snapTo(8.5,81,4.5,0,0);operator.setShiftKeyDown(true);
        var coins=new ItemStack(Items.EMERALD,2);coins.set(DataComponents.CUSTOM_NAME,Component.literal("Finite fixture coin"));
        operator.setItemInHand(InteractionHand.MAIN_HAND,coins);
        for(int i=0;i<2;i++)check(Villagers.offerGift(level,buyer,operator,InteractionHand.MAIN_HAND),"one actual operator-held emerald handed over");
        check(operator.getMainHandItem().isEmpty() && buyer.getInventory().countItem(Items.EMERALD)==2,"finite external two-coin supply transferred exactly once");
        check(MarketGoods.offer(seller.getInventory(),seller.getMainHandItem(),true,MarketGoods.Good.HOE)<0,"no supplied spare tool");
        record(server,"finite setup: farmer two marked coins/carrot reserve/seeds; maker one owned log, working used hoe and three bread; no spare tool; real fence counter");
    }
    private void observe(MinecraftServer server) throws Exception {
        var farmer=Villagers.existingSession(buyer.getUUID());var maker=Villagers.existingSession(seller.getUUID());
        for(var actor:List.of(buyer,seller)) {
            var session=Villagers.existingSession(actor.getUUID());var trade=session.lastTrade;
            if(!trade.isEmpty() && ((Number)trade.get("gameTime")).longValue()!=lastTrade) {
                lastTrade=((Number)trade.get("gameTime")).longValue();int price=((Number)trade.get("price")).intValue();
                check(((Number)trade.get("buyerMoneyBefore")).intValue()-((Number)trade.get("buyerMoneyAfter")).intValue()==price,"buyer pays exact quoted money");
                check(((Number)trade.get("sellerMoneyAfter")).intValue()-((Number)trade.get("sellerMoneyBefore")).intValue()==price,"seller receives the same money");
                check(((Number)trade.get("buyerGoodsAfter")).intValue()-((Number)trade.get("buyerGoodsBefore")).intValue()==1,"buyer receives one real item");
                check(((Number)trade.get("sellerGoodsBefore")).intValue()-((Number)trade.get("sellerGoodsAfter")).intValue()==1,"seller spends one real item");
                events.add(Map.of("phase","actual paid exchange","receipt",trade));
                if(trade.get("item").equals("minecraft:wooden_hoe")) {
                    check(trade.get("seller").equals(seller.getUUID().toString()) && maker.hoesCrafted==1 && maker.woodRecipesCrafted==3,"tool sale is funded by actual customer-triggered manufacture");
                    boughtHoe=true;
                }
                if(trade.get("item").equals("minecraft:bread") && trade.get("buyer").equals(seller.getUUID().toString())) {
                    check(farmer.breadCrafted>0,"farmer sold actually produced bread, with no initial farmer bread");
                    boughtFood=true;foodAtPurchase=maker.mind.foodLevel();
                }
                record(server,"manufactured tool sale or earned food purchase observed");
            }
        }
        var harvest=AdventureActions.lastHarvest(buyer.getUUID());
        if(!harvest.isEmpty() && ((Number)harvest.get("gameTime")).longValue()!=lastHarvest) {
            lastHarvest=((Number)harvest.get("gameTime")).longValue();harvests++;
            check(boughtHoe,"native farming uses a paid, actually manufactured tool");
            events.add(Map.of("phase","native harvest using purchased hoe","receipt",harvest));
        }
        if(boughtFood && seller.getInventory().countItem(Items.BREAD)==0 && maker.mind.foodLevel()>foodAtPurchase)atePurchase=true;
        if(ticks%400==0)record(server,"observe");
        if(boughtHoe && boughtFood && atePurchase) {
            check(maker.hoesCrafted==1 && maker.woodRecipesCrafted==3 && seller.getInventory().countItem(Items.OAK_LOG)==0
                    && seller.getInventory().countItem(Items.OAK_PLANKS)==0 && seller.getInventory().countItem(Items.STICK)==2,"one actual log funded one spare hoe and two remaining sticks");
            check(farmer.localPurchases==1 && farmer.localSales==1 && maker.localPurchases==1 && maker.localSales==1,"money returns via distinct useful goods, not gifts");
            check(buyer.getInventory().countItem(Items.EMERALD)==2 && seller.getInventory().countItem(Items.EMERALD)==0,"same finite money circulated back to farmer");
            check(seller.getMainHandItem().is(Items.WOODEN_HOE) && seller.getMainHandItem().getDamageValue()==17,"maker retained actual working tool rather than selling it");
            check(maker.toolDemand.requester(server.overworld().getGameTime())==null,"actual customer sale fulfilled the tool signal");
            check(buyer.getX()>7 && seller.getX()<7 && farmer.tradePathStarts+maker.tradePathStarts>0,"accepted physical contact routes work across the fence counter without crossing it");
            record(server,"request, paid native manufacture, sale, native farming, earned bread sale, returned currency and meal passed");
            write(server,"passed",null);server.halt(false);
        }
    }
    private void finishRestart(MinecraftServer server) throws Exception {
        var last=previous.getAsJsonArray("events").get(previous.getAsJsonArray("events").size()-1).getAsJsonObject();
        for(var v:List.of(buyer,seller)) {
            var s=Villagers.existingSession(v.getUUID());var expected=last.getAsJsonObject(v==buyer?"buyer":"seller");
            check(s.mind.foodLevel()<=expected.get("food").getAsInt() && s.mind.foodLevel()>=expected.get("food").getAsInt()-1,"saved nutrition continues normal bounded metabolism across restart observation");
            check(v.getInventory().countItem(Items.EMERALD)==expected.get("money").getAsInt(),"actual money persists without replay");
            check(v.getInventory().countItem(Items.BREAD)==expected.get("bread").getAsInt(),"paid/consumed bread not replayed");
            check(s.localPurchases==0 && s.localSales==0 && s.breadCrafted==0,"no transaction or recipe replay");
        }
        check(buyer.getMainHandItem().is(Items.WOODEN_HOE) && buyer.getMainHandItem().getDamageValue()==last.getAsJsonObject("buyer").get("damage").getAsInt(),"purchased tool and actual wear persist");
        check(Villagers.existingSession(seller.getUUID()).hoesCrafted==0 && Villagers.existingSession(seller.getUUID()).woodRecipesCrafted==0,"no repeated tool manufacture after restart");
        record(server,"saved real money, meal, tool wear and no replay passed");write(server,"restart-passed",null);server.halt(false);
    }
    private void record(MinecraftServer server,String phase) {
        var row=new LinkedHashMap<String,Object>();row.put("phase",phase);row.put("tick",server.overworld().getGameTime());
        for(var v:List.of(buyer,seller)) {
            var s=Villagers.existingSession(v.getUUID());var state=new LinkedHashMap<String,Object>();
            state.put("id",v.getUUID().toString());state.put("food",s.mind.foodLevel());state.put("money",v.getInventory().countItem(Items.EMERALD));
            state.put("bread",v.getInventory().countItem(Items.BREAD));state.put("wheat",v.getInventory().countItem(Items.WHEAT));state.put("crafted",s.breadCrafted);
            state.put("toolDemand",s.toolDemand.requester(server.overworld().getGameTime())==null?"none":"known customer");state.put("hoesCrafted",s.hoesCrafted);state.put("woodRecipes",s.woodRecipesCrafted);state.put("tradePaths",s.tradePathQueries);state.put("tradeRejections",s.tradePathRejections);state.put("x",v.getX());state.put("z",v.getZ());state.put("toolStatus",s.toolStatus);state.put("foodStatus",s.foodStatus);state.put("foodDemand",s.foodDemand.requester(server.overworld().getGameTime())==null?"none":"known customer");
            state.put("tradePathStarts",s.tradePathStarts);state.put("lastTradePath",s.lastTradePath);
            state.put("productionRequests",s.productionRequestsHeard);state.put("lastProductionRequest",s.lastProductionRequest);
            state.put("bought",s.localPurchases);state.put("sold",s.localSales);state.put("foodRequests",s.foodHelpRequests);
            state.put("held",v.getMainHandItem().toString());state.put("damage",v.getMainHandItem().getDamageValue());row.put(v==buyer?"buyer":"seller",state);
            state.put("y",v.getY());state.put("eye",v.getEyePosition().toString());state.put("seesPeer",v.hasLineOfSight(v==buyer?seller:buyer));
        }
        row.put("harvestsThisLoad",harvests);row.put("meanServerTickMillis",server.getAverageTickTimeNanos()/1e6);events.add(row);
        System.out.println("MARKET PRODUCTION EVENT: "+new GsonBuilder().create().toJson(row));
    }
    private void write(MinecraftServer server,String status,String failure) throws Exception {
        var out=new LinkedHashMap<String,Object>();out.put("status",status);out.put("failure",failure);out.put("events",events);
        out.put("fixture","NEW isolated supplied peaceful two-room platform with real fence counter,open upper window,lit low ceiling preventing bed-assisted fence climbing,beds,tables,water,twelve mature wheat; common initial language,greedy/glutton toolmaker and greedy/frugal farmer. Maker starts one owned log,one used working woodenhoe,three bread,no spare tool or money. Farmer starts six carrots,sixteen seeds and two marked coins via actual finite operator gifts,no tool or bread.80TPS,400base exhaustion/day. No post-setup supplies, body/clock/growth/position setters or scripted production,exchange,meal. Finite request-driven production/currency circulation, not renewable production or normal survival.");
        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(reopen?"local-trade-restart-result.json":"local-trade-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out));
    }
    private static void check(boolean condition,String label) {if(!condition)throw new AssertionError(label);}
}
