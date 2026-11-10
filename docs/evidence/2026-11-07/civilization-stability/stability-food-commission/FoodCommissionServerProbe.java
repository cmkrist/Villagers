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
import local.villagers.relations.FoodRequest;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelResource;

/** Isolates an actual paid proposal to a gift-willing producer; no scripted successful handoff or production. */
public final class FoodCommissionServerProbe implements ModInitializer {
    private Villager buyer,seller;
    private int ticks,foodAtPurchase=-1;
    private boolean reopen,heard,checkedGiftBoundary,purchased;
    private JsonObject previous;
    private final ArrayList<Map<String,Object>> events=new ArrayList<>();
    private static UUID founder(boolean selling) {
        for(long i=1;i<100000;i++) {
            var id=new UUID(0x434f4d4d49535349L,i);var mind=new AgentMind(id);
            if(selling?mind.has(Trait.GENEROUS) && mind.has(Trait.FRUGAL) && !mind.has(Trait.GREEDY):mind.has(Trait.GLUTTON))return id;
        }
        throw new AssertionError("Selected founder missing");
    }
    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server->{
            try {
                ticks++;
                if(ticks==1) {server.overworld().setChunkForced(0,0,true);server.overworld().getChunk(0,0);server.tickRateManager().setTickRate(80);}
                if(ticks==40) {
                    var file=server.getWorldPath(LevelResource.ROOT).resolve("food-commission-result.json");reopen=Files.exists(file);
                    if(reopen) {previous=JsonParser.parseString(Files.readString(file)).getAsJsonObject();check(previous.get("status").getAsString().equals("passed"),"only accepted fixture reopens");}
                    else terrain(server);
                }
                if(ticks==60)setup(server);
                if(buyer!=null) {
                    var b=Villagers.existingSession(buyer.getUUID());var s=Villagers.existingSession(seller.getUUID());
                    check(buyer.isAlive() && seller.isAlive() && buyer.getHealth()==buyer.getMaxHealth() && seller.getHealth()==seller.getMaxHealth(),"same two adults alive at full health every observed tick");
                    check(buyer.getInventory().countItem(Items.EMERALD)+seller.getInventory().countItem(Items.EMERALD)==1,"one actual coin conserved");
                    check(b.foodSharedReceived==0 && s.foodSharedGiven==0 && s.foodDeliveries==0,"paid proposal must not become a gift");
                    for(var v:List.of(buyer,seller))for(int slot=0;slot<v.getInventory().getContainerSize();slot++) {
                        var item=v.getInventory().getItem(slot);
                        if(item.is(Items.EMERALD))check(item.has(DataComponents.CUSTOM_NAME) && item.get(DataComponents.CUSTOM_NAME).getString().equals("Finite commission coin"),"original coin components retained");
                    }
                    if(reopen && ticks==160)restart(server);else if(!reopen)observe(server);
                }
                if(ticks>6000)throw new AssertionError("Paid food commission observation exhausted");
            }catch(Throwable failure) {failure.printStackTrace();try{write(server,"failed",failure.toString());}catch(Exception ignored){}server.halt(false);}
        });
    }
    private static void terrain(MinecraftServer server) {
        var level=server.overworld();
        for(int x=1;x<=10;x++)for(int z=1;z<=9;z++)for(int y=79;y<=84;y++)
            level.setBlock(new BlockPos(x,y,z),y==84?Blocks.GLOWSTONE.defaultBlockState():y<=80 || x==1 || x==10 || z==1 || z==9
                    ?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        for(int z:new int[]{3,6}) {
            var bed=Blocks.BED.white().defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH);
            level.setBlock(new BlockPos(3,81,z),bed.setValue(BedBlock.PART,BedPart.FOOT),3);
            level.setBlock(new BlockPos(3,81,z+1),bed.setValue(BedBlock.PART,BedPart.HEAD),3);
        }
        level.setBlock(new BlockPos(6,81,6),Blocks.CRAFTING_TABLE.defaultBlockState(),3);
    }
    private void setup(MinecraftServer server) {
        var level=server.overworld();
        if(reopen) {
            buyer=(Villager)level.getEntity(founder(false));seller=(Villager)level.getEntity(founder(true));
            check(buyer!=null && seller!=null,"same saved adults reloaded");Villagers.session(buyer,level);Villagers.session(seller,level);
            record(server,"reopen without supplies or actor/terrain/budget setters");return;
        }
        seller=new Villager(EntityTypes.VILLAGER,level);seller.setUUID(founder(true));seller.snapTo(6.5,81,5.5,0,0);
        buyer=new Villager(EntityTypes.VILLAGER,level);buyer.setUUID(founder(false));buyer.snapTo(5.5,81,5.5,0,0);
        seller.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_HOE));buyer.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.WOODEN_HOE));
        seller.getInventory().addItem(new ItemStack(Items.BREAD,3));seller.getInventory().addItem(new ItemStack(Items.WHEAT,6));
        level.addFreshEntity(seller);level.addFreshEntity(buyer);VillagerController.adopt(level,seller);VillagerController.adopt(level,buyer);
        var b=Villagers.existingSession(buyer.getUUID());var s=Villagers.existingSession(seller.getUUID());var language=Lexicon.invent(new Random(551));b.mind.lexicon(language);s.mind.lexicon(language);
        // Declared initial request budget isolates paid intent from a separate explicit charitable request.
        b.nextFoodRequest=Long.MAX_VALUE;
        var operator=new ServerPlayer(server,level,new GameProfile(new UUID(0x434f4d4d4f504552L,1),"CommissionOperator"),ClientInformation.createDefault());
        operator.snapTo(5.5,81,4.5,0,0);operator.setShiftKeyDown(true);
        var coin=new ItemStack(Items.EMERALD);coin.set(DataComponents.CUSTOM_NAME,Component.literal("Finite commission coin"));operator.setItemInHand(InteractionHand.MAIN_HAND,coin);
        check(Villagers.offerGift(level,buyer,operator,InteractionHand.MAIN_HAND) && operator.getMainHandItem().isEmpty(),"one finite operator-held coin supplied once");
        check(MarketGoods.offer(seller.getInventory(),seller.getMainHandItem(),true,MarketGoods.Good.FOOD)<0,"initial bread covers own reserve but cannot be sold");
        record(server,"finite setup; generous seller has three bread/six wheat; buyer one actual marked coin and no charitable request budget");
    }
    private void observe(MinecraftServer server) throws Exception {
        var b=Villagers.existingSession(buyer.getUUID());var s=Villagers.existingSession(seller.getUUID());long now=server.overworld().getGameTime();
        if(!heard && s.productionRequestsHeard>0) {
            check(s.lastProductionRequest.get("kind").equals("FOOD") && ((Number)s.lastProductionRequest.get("price")).intValue()==1,"understood affordable actual paid food request");
            heard=true;events.add(Map.of("phase","actual paid production request","request",s.lastProductionRequest));record(server,"paid request heard");
        }
        if(heard && !checkedGiftBoundary && buyer.getUUID().equals(s.foodDemand.requester(now))
                && FoodInventory.sharingSlot(seller.getInventory(),true)>=0 && Speech.willingToShare(s,buyer.getUUID())
                && FoodRequest.inReach(seller.distanceToSqr(buyer),Villagers.config().hearingRadius(),seller.hasLineOfSight(buyer) && buyer.hasLineOfSight(seller))) {
            check(FoodInventory.anySlot(buyer.getInventory(),true)<0,"customer still has unmet own food need");
            var before=HarvestMaterials.snapshot(seller.getInventory());
            check(!Speech.deliverPendingFood(server.overworld(),seller,s,buyer,now),"paid request alone cannot authorize an otherwise eligible native gift");
            check(HarvestMaterials.unchanged(seller.getInventory(),before),"denied gift leaves actual producer inventory untouched");
            checkedGiftBoundary=true;record(server,"eligible gift path denied for actual paid request");
        }
        if(!purchased && b.localPurchases>0) {
            check(heard && checkedGiftBoundary,"actual paid request and gift denial precede purchase");
            var r=b.lastTrade;check(r.get("item").equals("minecraft:bread") && ((Number)r.get("quantity")).intValue()==1 && ((Number)r.get("price")).intValue()==1,"one produced bread paid with one actual coin");
            check(s.breadCrafted>0 && seller.getInventory().countItem(Items.WHEAT)==6-3*s.breadCrafted,"real wheat pays every native bread recipe");
            check(s.foodDemand.requester(now)==null,"actual sale fulfills the paid production signal");
            purchased=true;foodAtPurchase=b.mind.foodLevel();events.add(Map.of("phase","actual paid exchange","receipt",r));record(server,"produced bread bought");
        }
        if(ticks%400==0)record(server,"observe");
        if(purchased && buyer.getInventory().countItem(Items.BREAD)==0 && b.mind.foodLevel()>foodAtPurchase) {
            check(b.localPurchases==1 && s.localSales==1 && seller.getInventory().countItem(Items.EMERALD)==1,"one sale earned actual income");
            check(FoodSupply.deficit(seller.getInventory(),true)==0 && foodAtPurchase>6,"seller reserve and timely buyer meal retained");
            record(server,"paid commission, denied gift, actual production/payment/meal passed");write(server,"passed",null);server.halt(false);
        }
    }
    private void restart(MinecraftServer server) throws Exception {
        var last=previous.getAsJsonArray("events").get(previous.getAsJsonArray("events").size()-1).getAsJsonObject();
        for(var v:List.of(buyer,seller)) {
            var s=Villagers.existingSession(v.getUUID());var expected=last.getAsJsonObject(v==buyer?"buyer":"seller");
            check(v.getInventory().countItem(Items.EMERALD)==expected.get("money").getAsInt() && v.getInventory().countItem(Items.BREAD)==expected.get("bread").getAsInt()
                    && v.getInventory().countItem(Items.WHEAT)==expected.get("wheat").getAsInt(),"actual food, grain and money persist without replay");
            check(s.localPurchases==0 && s.localSales==0 && s.breadCrafted==0 && s.foodDemand.requester(server.overworld().getGameTime())==null,"transient requests and counters do not replay on reload");
        }
        record(server,"clean restart retained food, inputs and earned money");write(server,"restart-passed",null);server.halt(false);
    }
    private void record(MinecraftServer server,String phase) {
        var out=new LinkedHashMap<String,Object>();out.put("phase",phase);out.put("tick",server.overworld().getGameTime());out.put("foodAtPurchase",foodAtPurchase);
        out.put("giftBoundaryChecked",checkedGiftBoundary);
        for(var v:List.of(buyer,seller)) {
            var s=Villagers.existingSession(v.getUUID());var row=new LinkedHashMap<String,Object>();
            row.put("food",s.mind.foodLevel());row.put("health",v.getHealth());row.put("money",v.getInventory().countItem(Items.EMERALD));row.put("bread",v.getInventory().countItem(Items.BREAD));row.put("wheat",v.getInventory().countItem(Items.WHEAT));
            row.put("crafted",s.breadCrafted);row.put("bought",s.localPurchases);row.put("sold",s.localSales);row.put("giftsReceived",s.foodSharedReceived);row.put("giftsGiven",s.foodSharedGiven);
            out.put(v==buyer?"buyer":"seller",row);
        }
        events.add(out);System.out.println("FOOD COMMISSION EVENT: "+new GsonBuilder().create().toJson(out));
    }
    private void write(MinecraftServer server,String status,String failure) throws Exception {
        var out=new LinkedHashMap<String,Object>();out.put("status",status);out.put("failure",failure);out.put("events",events);
        out.put("fixture","NEW isolated supplied peaceful lit chamber,beds,table,two adults with working hoes/common initial language; generous/frugal non-greedy seller starts three bread/six wheat; glutton buyer starts no food and receives one marked coin by real operator gift. Initial buyer charitable-request budget set beyond test window to isolate paid intent.80TPS,400base exhaustion/day. No post-setup supplies,body/health/clock/growth/position/budget setters or scripted successful trade/production/meal. One explicit native gift-denial attempt checks the paid-request boundary. Finite consent/income test, not renewable or sustainable survival.");
        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(reopen?"food-commission-restart-result.json":"food-commission-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out));
    }
    private static void check(boolean condition,String label) {if(!condition)throw new AssertionError(label);}
}
