package local.villagers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
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
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelResource;

/** Supplied finite-stock fixture; ordinary hunger, market negotiation and meals after setup. */
public final class FoodBundleServerProbe implements ModInitializer {
    private final boolean root=System.getProperty("villagers.bundle.kind","carrot").equals("carrot");
    private final Item food=root?Items.CARROT:Items.BEETROOT;
    private final int quantity=root?2:5,remaining=root?1:0;
    private Villager buyer,seller;
    private int ticks,foodAtPurchase=-1;
    private boolean reopen,purchased;
    private JsonObject previous;
    private final ArrayList<Map<String,Object>> events=new ArrayList<>();
    private static UUID founder(boolean selling) {
        for(long i=1;i<100000;i++) {
            var id=new UUID(0x42554e444c45464fL,i);var m=new AgentMind(id);
            if(selling?m.has(Trait.GREEDY) && m.has(Trait.FRUGAL):m.has(Trait.GLUTTON))return id;
        }
        throw new AssertionError("No selected founder");
    }
    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                ticks++;
                if(ticks==1) {server.overworld().setChunkForced(0,0,true);server.overworld().getChunk(0,0);server.tickRateManager().setTickRate(80);}
                if(ticks==40) {
                    var file=server.getWorldPath(LevelResource.ROOT).resolve("food-bundle-result.json");reopen=Files.exists(file);
                    if(reopen) {previous=JsonParser.parseString(Files.readString(file)).getAsJsonObject();check(previous.get("status").getAsString().equals("passed"),"only passed fixture may reopen");}
                    else terrain(server.overworld());
                }
                if(ticks==60)setup(server);
                if(buyer!=null) {
                    check(buyer.isAlive() && seller.isAlive(),"same two managed adults remain alive");
                    check(buyer.getInventory().countItem(Items.EMERALD)+seller.getInventory().countItem(Items.EMERALD)==2,"both actual coins conserved");
                    for(var v:List.of(buyer,seller))for(int slot=0;slot<v.getInventory().getContainerSize();slot++) {
                        var stack=v.getInventory().getItem(slot);
                        if(stack.is(food) || stack.is(Items.EMERALD))check(stack.has(DataComponents.CUSTOM_NAME)
                                && stack.get(DataComponents.CUSTOM_NAME).getString().equals(stack.is(food)?"Finite market ration":"Finite fixture coin"),
                                "real food and coin components retained, including reload");
                    }
                    if(reopen && ticks==160)restart(server);else if(!reopen)observe(server);
                }
                if(ticks>6000)throw new AssertionError("Ordinary food bundle observation exhausted");
            }catch(Throwable failure) {
                failure.printStackTrace();try{write(server,"failed",failure.toString());}catch(Exception ignored){}server.halt(false);
            }
        });
    }
    private static void terrain(ServerLevel level) {
        for(int x=1;x<=10;x++)for(int z=1;z<=9;z++)for(int y=79;y<=84;y++)
            level.setBlock(new BlockPos(x,y,z),y==84?Blocks.GLOWSTONE.defaultBlockState():y<=80 || x==1 || x==10 || z==1 || z==9
                    ?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        for(int z:new int[]{3,6}) {
            var bed=Blocks.BED.white().defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH);
            level.setBlock(new BlockPos(3,81,z),bed.setValue(BedBlock.PART,BedPart.FOOT),3);
            level.setBlock(new BlockPos(3,81,z+1),bed.setValue(BedBlock.PART,BedPart.HEAD),3);
        }
    }
    private void setup(MinecraftServer server) {
        var level=server.overworld();
        if(reopen) {
            buyer=(Villager)level.getEntity(founder(false));seller=(Villager)level.getEntity(founder(true));
            check(buyer!=null && seller!=null,"same saved bodies reloaded");Villagers.session(buyer,level);Villagers.session(seller,level);
            record(server,"reopen without supplies or actor/terrain setters");return;
        }
        seller=new Villager(EntityTypes.VILLAGER,level);seller.setUUID(founder(true));seller.snapTo(6.5,81,5.5,0,0);
        buyer=new Villager(EntityTypes.VILLAGER,level);buyer.setUUID(founder(false));buyer.snapTo(5.5,81,5.5,0,0);
        seller.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_HOE));buyer.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.WOODEN_HOE));
        var stock=new ItemStack(food,32);stock.set(DataComponents.CUSTOM_NAME,Component.literal("Finite market ration"));seller.getInventory().addItem(stock);
        level.addFreshEntity(seller);level.addFreshEntity(buyer);VillagerController.adopt(level,seller);VillagerController.adopt(level,buyer);
        var language=Lexicon.invent(new Random(551));Villagers.existingSession(seller.getUUID()).mind.lexicon(language);Villagers.existingSession(buyer.getUUID()).mind.lexicon(language);
        var operator=new ServerPlayer(server,level,new GameProfile(new UUID(0x42554e444c454f50L,1),"BundleOperator"),ClientInformation.createDefault());
        operator.snapTo(5.5,81,4.5,0,0);operator.setShiftKeyDown(true);
        var coins=new ItemStack(Items.EMERALD,2);coins.set(DataComponents.CUSTOM_NAME,Component.literal("Finite fixture coin"));operator.setItemInHand(InteractionHand.MAIN_HAND,coins);
        for(int i=0;i<2;i++)check(Villagers.offerGift(level,buyer,operator,InteractionHand.MAIN_HAND),"one actual finite coin gift");
        check(operator.getMainHandItem().isEmpty() && buyer.getInventory().countItem(food)==0,"no buyer ration or extra coin supplied");
        record(server,"declared supplied seller stock32 and two gifted actual buyer coins; ordinary AI thereafter");
    }
    private void observe(MinecraftServer server) throws Exception {
        var b=Villagers.existingSession(buyer.getUUID());var s=Villagers.existingSession(seller.getUUID());
        if(!purchased && b.localPurchases>0) {
            var trade=b.lastTrade;purchased=true;foodAtPurchase=b.mind.foodLevel();
            check(((Number)trade.get("quantity")).intValue()==quantity && ((Number)trade.get("price")).intValue()==2,"whole actual bundle bought for one quoted payment");
            check(((Number)trade.get("buyerGoodsAfter")).intValue()-((Number)trade.get("buyerGoodsBefore")).intValue()==quantity,"whole bundle received");
            check(((Number)trade.get("sellerGoodsBefore")).intValue()-((Number)trade.get("sellerGoodsAfter")).intValue()==quantity,"same bundle removed from seller");
            check(((Number)trade.get("buyerMoneyBefore")).intValue()-((Number)trade.get("buyerMoneyAfter")).intValue()==2
                    && ((Number)trade.get("sellerMoneyAfter")).intValue()-((Number)trade.get("sellerMoneyBefore")).intValue()==2,"two existing coins change hands once");
            check(buyer.getInventory().countItem(food)==quantity,"bundle observed before its first ordinary meal");
            for(int i=0;i<buyer.getInventory().getContainerSize();i++) {
                var item=buyer.getInventory().getItem(i);
                if(item.is(food))check(item.has(DataComponents.CUSTOM_NAME) && item.get(DataComponents.CUSTOM_NAME).getString().equals("Finite market ration"),"actual food components preserved");
            }
            events.add(Map.of("phase","actual bundle exchange","receipt",trade));record(server,"paid bundle received");
        }
        if(ticks%400==0)record(server,"observe");
        if(purchased && buyer.getInventory().countItem(food)==remaining && b.mind.foodLevel()>foodAtPurchase) {
            check(b.localPurchases==1 && s.localSales==1 && b.foodSharedReceived==0 && b.foodSharedGiven==0,"one paid bundle, no charitable transfer counted as a meal");
            check(buyer.getInventory().countItem(Items.EMERALD)==0 && seller.getInventory().countItem(Items.EMERALD)==2,"same finite coins preserved");
            check(FoodSupply.deficit(seller.getInventory(),true)==0,"seller retains its own nutritional reserve");
            if(root)check(FoodInventory.anySlot(buyer.getInventory(),true)<0,"buyer ate one root and retained the actual last planting carrot");
            record(server,"native bundle payment, owned meals and retained reserve passed");write(server,"passed",null);server.halt(false);
        }
    }
    private void restart(MinecraftServer server) throws Exception {
        var last=previous.getAsJsonArray("events").get(previous.getAsJsonArray("events").size()-1).getAsJsonObject();
        for(var v:List.of(buyer,seller)) {
            var s=Villagers.existingSession(v.getUUID());var expected=last.getAsJsonObject(v==buyer?"buyer":"seller");
            check(v.getInventory().countItem(Items.EMERALD)==expected.get("money").getAsInt(),"paid money persists without replay");
            check(v.getInventory().countItem(food)==expected.get("foodItems").getAsInt(),"bundle and consumed/retained food persist without replay");
            check(s.localPurchases==0 && s.localSales==0,"no repeated transaction in reopened session");
        }
        check(buyer.getInventory().countItem(food)==remaining,"retained planting root or consumed bundle preserved");
        record(server,"clean restart preserved payment, meals and retained roots");write(server,"restart-passed",null);server.halt(false);
    }
    private void record(MinecraftServer server,String phase) {
        var row=new LinkedHashMap<String,Object>();row.put("phase",phase);row.put("tick",server.overworld().getGameTime());row.put("kind",root?"carrot":"beetroot");
        row.put("quotedQuantity",quantity);row.put("foodAtPurchase",foodAtPurchase);
        for(var v:List.of(buyer,seller)) {
            var s=Villagers.existingSession(v.getUUID());row.put(v==buyer?"buyer":"seller",Map.of("food",s.mind.foodLevel(),"money",v.getInventory().countItem(Items.EMERALD),
                    "foodItems",v.getInventory().countItem(food),"bought",s.localPurchases,"sold",s.localSales,"health",v.getHealth()));
        }
        events.add(row);System.out.println("FOOD BUNDLE EVENT: "+new GsonBuilder().create().toJson(row));
    }
    private void write(MinecraftServer server,String status,String failure) throws Exception {
        var out=new LinkedHashMap<String,Object>();out.put("status",status);out.put("failure",failure);out.put("events",events);
        out.put("fixture","NEW isolated supplied peaceful lit chamber,beds,two adults with working hoes,common initial language; greedy/frugal seller starts32marked food items,glutton buyer starts no food and receives2finite marked coins through actual player gifts.80TPS,400base exhaustion/day. No post-setup supplies, actor/body/clock/growth setters or scripted trade/meal. Finite bundle integration, not renewable production or sustained survival.");
        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(reopen?"food-bundle-restart-result.json":"food-bundle-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out));
    }
    private static void check(boolean condition,String label) {if(!condition)throw new AssertionError(label);}
}
