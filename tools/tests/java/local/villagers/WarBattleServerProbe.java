package local.villagers;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.server.level.ServerLevel;

/** Observe the actual campaign. No teleports, direct damage, forced targets or resource setters. */
public final class WarBattleServerProbe implements ModInitializer {
    private boolean finished,restart,initialized;
    private int ticks;
    private final Map<String,UUID> representatives=new LinkedHashMap<>();
    private JsonObject previous;
    private boolean traded;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static List<Mob> actors(ServerLevel level) {
        return java.util.stream.StreamSupport.stream(level.getAllEntities().spliterator(),false)
            .filter(e->e instanceof Mob && e.entityTags().contains(WarScenario.ACTOR)).map(e->(Mob)e).toList();
    }
    private static double count(JsonObject state,String group,String key) {
        var values=state.getAsJsonObject(group);return values.has(key)?values.get(key).getAsDouble():0;
    }
    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(finished)return;
            var level=server.overworld();
            Path result=server.getWorldPath(LevelResource.ROOT).resolve("war-battle-result.json");
            try {
                ticks++;
                if(!initialized) {
                    initialized=true;server.tickRateManager().setTickRate(80);
                    restart=Files.exists(result.resolveSibling("war-battle-before-restart.json"));
                    if(restart)previous=JsonParser.parseString(Files.readString(result.resolveSibling("war-battle-before-restart.json"))).getAsJsonObject();
                }
                if(!WarCampaign.ready(level))return;
                if(ticks==150 && !restart) {
                    for(Mob mob:actors(level)) {
                        String species=mob instanceof Pillager?"pillager":mob instanceof Vindicator?"vindicator":
                            mob instanceof Enderman?"enderman":mob instanceof WanderingTrader?"trader":null;
                        if(species!=null && !representatives.containsKey(species)) {
                            check(CreatureAgents.managed(mob),"managed "+species);
                            check(((Number)CreatureAgents.status(mob).get("learningUpdates")).longValue()>0,"real learned attempts "+species);
                            representatives.put(species,mob.getUUID());
                        }
                    }
                    check(representatives.size()==4,"all additional species present and learning");
                    // TEST FIXTURE: a temporary player supplies exactly one native offer's payment.
                    WanderingTrader trader=(WanderingTrader)level.getEntity(representatives.get("trader"));
                    var player=new net.minecraft.server.level.ServerPlayer(server,level,
                        new com.mojang.authlib.GameProfile(new UUID(0x5741525452414445L,1),"WarTradeProbe"),
                        net.minecraft.server.level.ClientInformation.createDefault());
                    player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,
                        new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,
                        net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(),false)) {
                        @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
                        @Override public void send(net.minecraft.network.protocol.Packet<?> packet,io.netty.channel.ChannelFutureListener listener) {}
                    };
                    player.snapTo(trader.getX()+1,trader.getY(),trader.getZ(),0,0);
                    trader.mobInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND);
                    check(trader.getTradingPlayer()==player,"managed trader opens native trading interaction");
                    check(player.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu,"native merchant menu");
                    var menu=(net.minecraft.world.inventory.MerchantMenu)player.containerMenu;
                    check(!menu.getOffers().isEmpty(),"native generated trade offers");
                    var offer=menu.getOffers().getFirst();int uses=offer.getUses();
                    menu.setSelectionHint(0);menu.getSlot(0).set(offer.getCostA().copy());menu.getSlot(1).set(offer.getCostB().copy());
                    menu.updateSellItem();check(!menu.getSlot(2).getItem().isEmpty(),"native offer accepts real fixture payment");
                    var output=menu.quickMoveStack(player,2);
                    check(!output.isEmpty() && offer.getUses()==uses+1,"actual paid merchant transaction");
                    player.closeContainer();check(!trader.isTrading(),"native trade closes");traded=true;
                }
                JsonObject campaign=WarCampaign.snapshot(level);
                if(ticks%400==0) {
                    JsonObject progress=campaign.deepCopy();JsonArray positions=new JsonArray();
                    for(Mob mob:actors(level)) {
                        JsonObject actor=new JsonObject();actor.addProperty("id",mob.getUUID().toString());
                        actor.addProperty("species",mob.typeHolder().getRegisteredName());actor.addProperty("x",mob.getX());actor.addProperty("y",mob.getY());actor.addProperty("z",mob.getZ());
                        actor.addProperty("health",mob.getHealth());actor.addProperty("target",mob.getTarget()==null?"":mob.getTarget().getUUID().toString());
                        actor.addProperty("pathDone",mob.getNavigation().isDone());positions.add(actor);
                    }
                    progress.add("positions",positions);Files.writeString(result.resolveSibling("war-battle-progress.json"),new GsonBuilder().setPrettyPrinting().create().toJson(progress));
                }
                if(restart && ticks>=40) {
                    check(level.getGameTime()>previous.get("gameTime").getAsLong(),"saved clock continues");
                    for(String faction:new String[]{"VILLAGERS","ILLAGERS"}) {
                        check(count(campaign,"hits",faction)>=count(previous,"hits",faction),"saved damage history "+faction);
                        check(count(campaign,"marches",faction)>=count(previous,"marches",faction),"saved march history "+faction);
                    }
                    var casualties=previous.getAsJsonObject("casualties");
                    for(String id:casualties.keySet()) {
                        check(campaign.getAsJsonObject("casualties").has(id),"casualty history retained");
                        check(actors(level).stream().noneMatch(m->m.getUUID().toString().equals(id)),"dead founder was not restored");
                    }
                    var savedRepresentatives=previous.getAsJsonObject("representatives");
                    for(String species:savedRepresentatives.keySet()) {
                        var before=savedRepresentatives.getAsJsonObject(species);UUID id=UUID.fromString(before.get("id").getAsString());
                        var body=level.getEntity(id);
                        if(body instanceof Mob mob && mob.isAlive()) {
                            var current=CreatureAgents.status(mob);
                            check(current.get("name").equals(before.get("name").getAsString()),"same creature identity");
                            check(((Number)current.get("learningUpdates")).longValue()>=before.get("learningUpdates").getAsLong(),"saved creature learning");
                        } else check(campaign.getAsJsonObject("casualties").has(id.toString()),"missing mind representative must have actual recorded death");
                    }
                    campaign.addProperty("status","passed");campaign.addProperty("check","restart");
                    Files.writeString(result,new GsonBuilder().setPrettyPrinting().create().toJson(campaign));finished=true;server.halt(false);return;
                }
                boolean fought=true;
                for(String faction:new String[]{"VILLAGERS","ILLAGERS"})fought &=count(campaign,"marches",faction)>0 && count(campaign,"hits",faction)>0;
                boolean enemyKill=false;
                for(var entry:campaign.getAsJsonObject("casualties").entrySet()) {
                    var death=entry.getValue().getAsJsonObject();
                    if(death.has("killerFaction") && !death.get("faction").getAsString().equals("NEUTRAL")
                        && !death.get("killerFaction").getAsString().equals("NEUTRAL")
                        && !death.get("faction").getAsString().equals(death.get("killerFaction").getAsString()))enemyKill=true;
                }
                if(!restart && fought && enemyKill && traded && ticks>=400 && representatives.size()==4) {
                    JsonObject minds=new JsonObject();
                    for(var entry:representatives.entrySet()) {
                        var entity=level.getEntity(entry.getValue());
                        if(entity instanceof Mob mob)minds.add(entry.getKey(),new Gson().toJsonTree(CreatureAgents.status(mob)));
                    }
                    // Keep surviving representatives for restart; casualties are separately retained in the ledger.
                    campaign.add("representatives",minds);campaign.addProperty("nativePaidTrade",traded);
                    campaign.addProperty("status","passed");campaign.addProperty("check","battle");
                    Files.writeString(result,new GsonBuilder().setPrettyPrinting().create().toJson(campaign));finished=true;server.halt(false);return;
                }
                if(ticks>=8000)throw new AssertionError("Native campaign observation exhausted before both factions marched and hit: "+campaign);
            } catch(Throwable failure) {
                failure.printStackTrace();finished=true;
                try {Files.writeString(result,new GsonBuilder().create().toJson(Map.of("status","failed","failure",failure.toString())));}catch(Exception ignored){}
                server.halt(false);
            }
        });
    }
}
