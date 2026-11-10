package local.villagers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
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

/** Two real customer orders can fund repeated input sales, then actual food purchases. Finite supplied fixture. */
public final class InputMarketServerProbe implements ModInitializer {
    private enum Role { MAKER, SUPPLIER, FARMER_A, FARMER_B }
    private final Map<Role,Villager> actors=new LinkedHashMap<>();
    private final ArrayList<Map<String,Object>> events=new ArrayList<>();
    private final HashSet<String> trades=new HashSet<>(),harvests=new HashSet<>();
    private final int makerRations=Integer.getInteger("villagers.input.makerRations",4);
    private int ticks,woodSales,toolSales,minimumFood=20;
    private boolean reopen,makerBoughtFood,supplierBoughtFood,makerAte,supplierAte;
    private int makerFoodAtPurchase=-1,supplierFoodAtPurchase=-1;
    private JsonObject previous;
    private static UUID founder(Role role) {
        for(long n=1;n<100000;n++) {
            var id=new UUID(0x494e5055544d4b00L+role.ordinal(),n);var m=new AgentMind(id);
            boolean match=switch(role) {
                case MAKER->m.has(Trait.GREEDY) && m.has(Trait.GLUTTON);
                case SUPPLIER->!m.has(Trait.GREEDY) && m.has(Trait.GLUTTON);
                default->m.has(Trait.GREEDY) && m.has(Trait.FRUGAL) && !m.has(Trait.GLUTTON);
            };
            if(match)return id;
        }
        throw new AssertionError("Selected founder missing");
    }
    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server->{
            try {
                ticks++;
                if(ticks==1) {for(int x=0;x<=1;x++) {server.overworld().setChunkForced(x,0,true);server.overworld().getChunk(x,0);}server.tickRateManager().setTickRate(80);}
                if(ticks==40) {
                    var f=server.getWorldPath(LevelResource.ROOT).resolve("input-market-result.json");reopen=Files.exists(f);
                    if(reopen) {previous=JsonParser.parseString(Files.readString(f)).getAsJsonObject();check(previous.get("status").getAsString().equals("passed"),"only accepted fixture reopens");}
                    else terrain(server);
                }
                if(ticks==60)setup(server);
                if(!actors.isEmpty()) {
                    int money=0,woodUnits=0,recipes=0;
                    for(var entry:actors.entrySet()) {
                        var v=entry.getValue();var s=Villagers.existingSession(v.getUUID());
                        if(ticks>80)check(server.overworld().getGameTime()-s.lastTick<=20,"every declared actor keeps thinking across both fixture chunks");
                        check(v.isAlive() && v.getHealth()==v.getMaxHealth(),"same four adults alive at full health every observed tick");
                        minimumFood=Math.min(minimumFood,s.mind.foodLevel());money+=v.getInventory().countItem(Items.EMERALD);
                        woodUnits+=8*v.getInventory().countItem(Items.OAK_LOG)+2*v.getInventory().countItem(Items.OAK_PLANKS)+v.getInventory().countItem(Items.STICK);recipes+=s.hoesCrafted;
                        check(entry.getKey().ordinal()<2?v.getX()<7.5:v.getX()>7.5,"actors remain on their side of the actual fence center plane without position correction");
                        for(int i=0;i<v.getInventory().getContainerSize();i++) {
                            var item=v.getInventory().getItem(i);
                            if(item.is(Items.EMERALD))check(item.has(DataComponents.CUSTOM_NAME) && item.get(DataComponents.CUSTOM_NAME).getString().equals("Finite input coin"),"original currency components preserved");
                            if(item.is(Items.OAK_LOG))check(item.has(DataComponents.CUSTOM_NAME) && item.get(DataComponents.CUSTOM_NAME).getString().equals("Finite input log"),"actual input components retained until native recipe consumption");
                        }
                    }
                    check(money==5,"all five physical coins conserved");
                    check(actors.get(Role.SUPPLIER).getInventory().countItem(Items.OAK_LOG)>=1,"supplier retains real maintenance input");
                    if(!reopen)check(woodUnits+6*recipes==24,"actual logs, plank/stick intermediates and paid native hoe inputs conserve all24 half-plank units");
                    if(reopen && ticks==160)restart(server);else if(!reopen)observe(server);
                }
                if(ticks>16000)throw new AssertionError("Native input-market observation exhausted");
            }catch(Throwable failure) {failure.printStackTrace();try{if(!actors.isEmpty())record(server,"failure snapshot");write(server,"failed",failure.toString());}catch(Exception ignored){}server.halt(false);}
        });
    }
    private static void terrain(MinecraftServer server) {
        var l=server.overworld();
        for(int x=1;x<=18;x++)for(int z=1;z<=12;z++)for(int y=79;y<=84;y++)
            l.setBlock(new BlockPos(x,y,z),y==84?Blocks.GLOWSTONE.defaultBlockState():y<=80 || x==1 || x==18 || z==1 || z==12?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        for(int z=1;z<=12;z++)for(int y=81;y<=83;y++)l.setBlock(new BlockPos(7,y,z),z>1 && z<12?(y==81?Blocks.OAK_FENCE:Blocks.AIR).defaultBlockState():Blocks.STONE.defaultBlockState(),3);
        for(int x:new int[]{5,8})for(int z:new int[]{3,9}) {
            var bed=Blocks.BED.white().defaultBlockState().setValue(BedBlock.FACING,Direction.SOUTH);
            l.setBlock(new BlockPos(x,81,z),bed.setValue(BedBlock.PART,BedPart.FOOT),3);l.setBlock(new BlockPos(x,81,z+1),bed.setValue(BedBlock.PART,BedPart.HEAD),3);
        }
        for(int x:new int[]{6,9})l.setBlock(new BlockPos(x,81,6),Blocks.CRAFTING_TABLE.defaultBlockState(),3);
        for(int x=11;x<=16;x++)for(int z=3;z<=10;z++) {
            l.setBlock(new BlockPos(x,80,z),Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE,7),3);
            l.setBlock(new BlockPos(x,81,z),((CropBlock)Blocks.WHEAT).getStateForAge(7),3);
        }
        for(int z:new int[]{4,8}) {l.setBlock(new BlockPos(17,80,z),Blocks.WATER.defaultBlockState(),3);l.setBlock(new BlockPos(17,81,z),Blocks.STONE.defaultBlockState(),3);}
    }
    private void setup(MinecraftServer server) {
        var l=server.overworld();var language=Lexicon.invent(new Random(551));
        for(var role:Role.values()) {
            Villager v;
            if(reopen) {v=(Villager)l.getEntity(founder(role));check(v!=null,"same saved actor reloaded");Villagers.session(v,l);}
            else {
                v=new Villager(EntityTypes.VILLAGER,l);v.setUUID(founder(role));
                v.snapTo(role==Role.MAKER?6.5:role==Role.SUPPLIER?5.5:9.5,81,role==Role.FARMER_B || role==Role.SUPPLIER?8.5:5.5,0,0);
                if(role==Role.MAKER) {var tool=new ItemStack(Items.WOODEN_HOE);tool.setDamageValue(17);v.setItemSlot(EquipmentSlot.MAINHAND,tool);v.getInventory().addItem(new ItemStack(Items.BREAD,makerRations));}
                else if(role==Role.SUPPLIER) {
                    var logs=new ItemStack(Items.OAK_LOG,3);logs.set(DataComponents.CUSTOM_NAME,Component.literal("Finite input log"));v.getInventory().addItem(logs);v.getInventory().addItem(new ItemStack(Items.BREAD,3));
                } else {v.getInventory().addItem(new ItemStack(Items.WHEAT_SEEDS,16));v.getInventory().addItem(new ItemStack(Items.CARROT,6));}
                int cash=role==Role.MAKER?1:role==Role.SUPPLIER?0:2;
                if(cash>0) {var coin=new ItemStack(Items.EMERALD,cash);coin.set(DataComponents.CUSTOM_NAME,Component.literal("Finite input coin"));v.getInventory().addItem(coin);}
                l.addFreshEntity(v);VillagerController.adopt(l,v);var s=Villagers.existingSession(v.getUUID());s.mind.lexicon(language);s.nextFoodRequest=Long.MAX_VALUE;
            }
            actors.put(role,v);
        }
        record(server,reopen?"reopened without resources, positions, bodies or budgets changed":"declared initial stocks: maker working hoe/"+makerRations+" bread/one coin; supplier three marked logs/three bread; two farmers seeds/carrots/two coins each; no supplied spare hoe");
    }
    private void observe(MinecraftServer server) throws Exception {
        for(var v:actors.values()) {
            var s=Villagers.existingSession(v.getUUID());var r=s.lastTrade;
            if(!r.isEmpty() && trades.add(r.get("gameTime")+":"+r.get("buyer")+":"+r.get("seller"))) {
                int price=((Number)r.get("price")).intValue(),q=((Number)r.get("quantity")).intValue();
                check(((Number)r.get("buyerMoneyBefore")).intValue()-((Number)r.get("buyerMoneyAfter")).intValue()==price && ((Number)r.get("sellerMoneyAfter")).intValue()-((Number)r.get("sellerMoneyBefore")).intValue()==price,"exact same real currency changes hands");
                check(((Number)r.get("buyerGoodsAfter")).intValue()-((Number)r.get("buyerGoodsBefore")).intValue()==q && ((Number)r.get("sellerGoodsBefore")).intValue()-((Number)r.get("sellerGoodsAfter")).intValue()==q,"exact quoted quantity conserved");
                events.add(Map.of("phase","actual exchange","receipt",r));
                if(r.get("item").equals("minecraft:oak_log")) {
                    check(r.get("seller").equals(founder(Role.SUPPLIER).toString()) && price==1 && q==1,"one actual surplus input bought from supplier for its actual one-coin quote");
                    var buyer=(Villager)server.overworld().getEntity(UUID.fromString((String)r.get("buyer")));var customer=Villagers.existingSession(buyer.getUUID()).toolDemand.requester(server.overworld().getGameTime());
                    check(customer!=null,"input purchase remains backed by an actual heard tool customer");woodSales++;
                } else if(r.get("item").equals("minecraft:wooden_hoe")) {
                    check(woodSales>toolSales && q==1 && price==2,"each actual customer tool has an earlier paid raw input");toolSales++;
                } else if(r.get("item").equals("minecraft:bread")) {
                    var buyerId=r.get("buyer");
                    if(buyerId.equals(founder(Role.MAKER).toString())) {makerBoughtFood=true;makerFoodAtPurchase=Villagers.existingSession(founder(Role.MAKER)).mind.foodLevel();}
                    if(buyerId.equals(founder(Role.SUPPLIER).toString())) {supplierBoughtFood=true;supplierFoodAtPurchase=Villagers.existingSession(founder(Role.SUPPLIER)).mind.foodLevel();}
                }
                record(server,"useful paid exchange observed");
            }
            var h=AdventureActions.lastHarvest(v.getUUID());
            if(!h.isEmpty() && harvests.add(v.getUUID()+":"+h.get("gameTime")))events.add(Map.of("phase","native paid harvest","receipt",h));
        }
        var m=actors.get(Role.MAKER);var p=actors.get(Role.SUPPLIER);var ms=Villagers.existingSession(m.getUUID());var ps=Villagers.existingSession(p.getUUID());
        if(makerBoughtFood && m.getInventory().countItem(Items.BREAD)==0 && ms.mind.foodLevel()>makerFoodAtPurchase)makerAte=true;
        if(supplierBoughtFood && p.getInventory().countItem(Items.BREAD)==0 && ps.mind.foodLevel()>supplierFoodAtPurchase)supplierAte=true;
        if(ticks%400==0)record(server,"observe");
        if(woodSales>=2 && toolSales>=2 && makerAte && supplierAte) {
            check(woodSales==2 && toolSales==2 && p.getInventory().countItem(Items.OAK_LOG)==1,"two distinct actual input sales, two customer tools, one retained log");
            check(makerFoodAtPurchase>6 && supplierFoodAtPurchase>6 && minimumFood>0,"earned meals arrive before emergency hunger with no zero-food interval");
            for(var role:List.of(Role.FARMER_A,Role.FARMER_B))check(AdventureActions.canHarvest(actors.get(role)),"both initially tool-less farmers obtained working equipment");
            check(ps.localSales==2 && ps.localPurchases>=1,"supplier earned income repeatedly and spent actual proceeds");
            record(server,"two paid input orders, manufactured tools, farming, income-funded meals and survival passed");write(server,"passed",null);server.halt(false);
        }
    }
    private void restart(MinecraftServer server) throws Exception {
        var array=previous.getAsJsonArray("events");var last=array.get(array.size()-1).getAsJsonObject();
        for(var role:Role.values()) {
            var v=actors.get(role);var s=Villagers.existingSession(v.getUUID());var old=last.getAsJsonObject(role.name());
            for(var item:List.of(Items.EMERALD,Items.OAK_LOG,Items.BREAD,Items.WHEAT)) {
                String key=item==Items.EMERALD?"money":item==Items.OAK_LOG?"logs":item==Items.BREAD?"bread":"wheat";
                check(v.getInventory().countItem(item)==old.get(key).getAsInt(),"native saved balances persist without replay");
            }
            check(optional(s,"toolInputCustomer")==null && number(s,"toolReserveWork")==0 && knownSupplier(s)==null && s.localPurchases==0 && s.localSales==0 && s.hoesCrafted==0 && s.foodDemand.requester(server.overworld().getGameTime())==null && s.toolDemand.requester(server.overworld().getGameTime())==null,"transient work and trades do not replay");
            check(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(v.getMainHandItem().getItem()).toString().equals(old.get("held").getAsString())
                    && v.getMainHandItem().getDamageValue()==old.get("damage").getAsInt(),"actual retained and purchased tools preserve equipment and native wear");
        }
        record(server,"clean restart preserved inputs, proceeds and consumed food");write(server,"restart-passed",null);server.halt(false);
    }
    private void record(MinecraftServer server,String phase) {
        var out=new LinkedHashMap<String,Object>();out.put("phase",phase);out.put("tick",server.overworld().getGameTime());out.put("woodSales",woodSales);out.put("toolSales",toolSales);out.put("minimumFood",minimumFood);out.put("makerFoodAtPurchase",makerFoodAtPurchase);out.put("supplierFoodAtPurchase",supplierFoodAtPurchase);
        for(var entry:actors.entrySet()) {
            var v=entry.getValue();var s=Villagers.existingSession(v.getUUID());var a=new LinkedHashMap<String,Object>();
            a.put("id",v.getUUID().toString());a.put("food",s.mind.foodLevel());a.put("money",v.getInventory().countItem(Items.EMERALD));a.put("bread",v.getInventory().countItem(Items.BREAD));a.put("wheat",v.getInventory().countItem(Items.WHEAT));a.put("logs",v.getInventory().countItem(Items.OAK_LOG));
            a.put("hoesCrafted",s.hoesCrafted);a.put("recipes",s.woodRecipesCrafted);a.put("toolReserveWork",number(s,"toolReserveWork"));a.put("breadCrafted",s.breadCrafted);a.put("bought",s.localPurchases);a.put("sold",s.localSales);a.put("foodDemand",s.foodDemand.requester(server.overworld().getGameTime())!=null);a.put("toolDemand",s.toolDemand.requester(server.overworld().getGameTime())!=null);a.put("toolStatus",s.toolStatus);a.put("foodStatus",s.foodStatus);a.put("tradeStatus",s.tradeStatus);a.put("lastPath",s.lastTradePath);a.put("x",v.getX());a.put("y",v.getY());a.put("z",v.getZ());a.put("health",v.getHealth());a.put("paidToolDeliveries",number(s,"paidToolDeliveries"));a.put("paidWoodDeliveries",number(s,"paidWoodDeliveries"));a.put("lastStockInquiry",optional(s,"lastStockInquiry"));a.put("paidFoodDeliveries",s.paidFoodDeliveries);a.put("knownToolSupplier",String.valueOf(knownSupplier(s)));a.put("supplierMemoryAvailable",optional(s,"toolSupplier")!=null);a.put("lastProductionRequest",s.lastProductionRequest);
            a.put("action",String.valueOf(s.action));a.put("sleeping",v.isSleeping());a.put("inWater",v.isInWater());a.put("air",v.getAirSupply());a.put("scheduledRest",VillageSurvival.scheduledRest(server.overworld(),v));
            a.put("fatigue",s.mind.fatigue());a.put("mood",s.mind.mood().band().name());a.put("storedDeficit",FoodSupply.deficit(v.getInventory(),AdventureActions.canHarvest(v)));
            a.put("nextTradeAttempt",s.nextTradeAttempt);a.put("nextTradeInquiry",number(s,"nextTradeInquiry"));a.put("tradePeer",String.valueOf(s.tradePeer));a.put("tradeGood",String.valueOf(s.tradeGood));
            a.put("inventory",HarvestMaterials.snapshot(v.getInventory()).stream().map(Object::toString).toList());
            var seen=new LinkedHashMap<String,Boolean>();for(var other:actors.entrySet())if(other.getValue()!=v)seen.put(other.getKey().name(),v.hasLineOfSight(other.getValue()));a.put("sight",seen);
            a.put("lastThinkGameTime",s.lastTick);a.put("held",net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(v.getMainHandItem().getItem()).toString());a.put("damage",v.getMainHandItem().getDamageValue());out.put(entry.getKey().name(),a);
        }
        out.put("meanServerTickMillis",server.getAverageTickTimeNanos()/1_000_000.0);events.add(out);System.out.println("INPUT MARKET EVENT: "+new GsonBuilder().create().toJson(out));
    }
    private void write(MinecraftServer server,String status,String failure) throws Exception {
        var out=new LinkedHashMap<String,Object>();out.put("status",status);out.put("failure",failure);out.put("events",events);
        out.put("fixture","NEW isolated supplied peaceful four-adult chamber covering two initially forced/entity-ticking chunks with fence separating factory/supplier from two farmers,48initial mature wheat,water,beds,tables. Common initial language, initial charitable request budgets beyond test window. Maker starts used working hoe,"+makerRations+" bread,one marked coin; supplier three marked logs,three bread,no hoe; farmers16seeds,six carrots,two marked coins each,no hoe. Five finite starting coins; no mod currency endowment.80TPS/400base exhaustion per day. No post-setup supplies, positions, body/clock/growth/budget setters or scripted successful trade/recipe/meal. Finite input/income test, not sustained renewable/hostile survival or population capacity.");
        Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve(reopen?"input-market-restart-result.json":"input-market-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out));
    }
    // Read-only optional diagnostics keep this same probe source usable with the earlier raw-market artifact.
    private static Object optional(Object owner,String name) {
        try {var f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
        catch(NoSuchFieldException missing) {return null;}
        catch(ReflectiveOperationException failure) {throw new AssertionError(failure);}
    }
    private static long number(Session s,String name) {var value=optional(s,name);return value instanceof Number n?n.longValue():0;}
    private static Object knownSupplier(Session s) {var known=optional(s,"toolSupplier");return known==null?null:optional(known,"peer")!=null?optional(known,"peer"):optional(known,"supplier");}
    private static void check(boolean condition,String label) {if(!condition)throw new AssertionError(label);}
}
