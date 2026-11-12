package local.villagers;

import java.util.*;
import local.villagers.capability.CapabilityPermissions.Capability;
import local.villagers.economy.FoodOffer;
import local.villagers.language.*;
import local.villagers.learning.LearningMode;
import local.villagers.relations.FoodRequest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.*;

/** Heard camp food requests and actual hand deliveries; closed cells remain closed. */
final class NativeCampMeals {
    // Existing camp fields can require a long native walk and several recipes.
    // Remember the heard request through that work; recipients still accept or
    // refuse the real item at contact, so stale requests cannot force a gift.
    private static final long REQUEST_LIFE=6000;
    // A native camp can contain several occupied cells. Retain the heard
    // requests for that bounded local population, not only its first four.
    static final int REQUEST_CAPACITY=16;
    static final class State {
        final LinkedHashMap<UUID,Long> heard=new LinkedHashMap<>();
        final Set<UUID> urgent=new HashSet<>();
        final Set<UUID> injured=new HashSet<>();
        final Map<UUID,Float> voices=new HashMap<>();
        final Map<UUID,Long> retryAt=new HashMap<>();
        UUID pursuing,handoffRecipient;long pursuitUntil;
        BlockPos handoffStand;
        final LinkedHashMap<String,Long> missedStands=new LinkedHashMap<>();
        void missStand(UUID recipient,BlockPos stand,long now){
            missedStands.entrySet().removeIf(e->e.getValue()<=now);
            String key=recipient+":"+stand.asLong();missedStands.remove(key);
            while(missedStands.size()>=64)missedStands.remove(missedStands.keySet().iterator().next());
            missedStands.put(key,now+400);
        }
        boolean retryStand(UUID recipient,BlockPos stand,long now){return now>=missedStands.getOrDefault(recipient+":"+stand.asLong(),0L);}
        net.minecraft.world.level.pathfinder.Path runningRoute;
        boolean runningForSupplies;
        boolean nativeSprint;double sprintDistance;
        Map<String,Object> supplyRunReceipt=Map.of();
        net.minecraft.world.level.pathfinder.Path deliveryRoute;
        UUID deliveryRecipient;
        final local.villagers.navigation.RouteProgress deliveryProgress=new local.villagers.navigation.RouteProgress(120);
        Vec3 runSample;long runSampleAt;
        long pathAt,giveAt;int requests,delivered,rejectedPaths;
        String activity="no camp meal request";
        Map<String,Object> receipt=Map.of();
        Map<String,Object> approach=Map.of();
        final CampFoodHatch.State hatch=new CampFoodHatch.State();
        final NativeCampCalls.State calls=new NativeCampCalls.State();
        BlockPos patrolTarget;
        long patrolUntil,patrolNext,patrolPathAt;
        long patrolSenseAt;
        final LinkedHashSet<BlockPos> patrolDoors=new LinkedHashSet<>();
        final LinkedHashMap<BlockPos,BlockPos> patrolCages=new LinkedHashMap<>();
        final LinkedHashMap<BlockPos,Long> patrolVisits=new LinkedHashMap<>();
        final LinkedHashMap<BlockPos,Long> groundVisits=new LinkedHashMap<>();
        long patrolLegs;
        Map<String,Object> patrolRoute=Map.of();
        boolean patrolling(long now){return now<patrolUntil;}
        void hear(UUID id,long expires,boolean emergency){
            hear(id,expires,emergency,false);
        }
        void hear(UUID id,long expires,boolean emergency,boolean hurt){
            var sound=calls.heard.get(id);boolean alarm=emergency&&sound!=null&&sound.volume()>NativeCampCalls.VOLUME;
            calls.heard.remove(id); // Understood nearby words supersede the distant sound cue.
            if(heard.size()>=REQUEST_CAPACITY&&!heard.containsKey(id)){
                var replace=heard.keySet().stream().filter(other->!urgent.contains(other)&&!injured.contains(other)).findFirst().orElse(null);
                if((alarm||hurt)&&replace==null)replace=heard.keySet().stream().filter(other->!injured.contains(other)).reduce((a,b)->b).orElse(null);
                if(hurt&&replace==null&&sound!=null){
                    var weakest=heard.keySet().stream().min(Comparator.comparingDouble(other->voices.getOrDefault(other,NativeCampCalls.INJURED_VOLUME))).orElse(null);
                    if(weakest!=null&&sound.volume()>voices.getOrDefault(weakest,NativeCampCalls.INJURED_VOLUME))replace=weakest;
                }
                if(!emergency&&!hurt||replace==null)return;heard.remove(replace);urgent.remove(replace);injured.remove(replace);voices.remove(replace);
            }
            if(!heard.containsKey(id))requests++;
            heard.put(id,expires);if(emergency)urgent.add(id);else urgent.remove(id);
            if(hurt){if(injured.add(id))retryAt.remove(id);}else injured.remove(id);
            voices.put(id,sound!=null?sound.volume():hurt?voices.getOrDefault(id,NativeCampCalls.INJURED_VOLUME):NativeCampCalls.VOLUME);
            if(alarm)heard.putFirst(id,expires);
        }
        UUID next(long now){
            heard.entrySet().removeIf(e->now>=e.getValue());urgent.retainAll(heard.keySet());injured.retainAll(heard.keySet());
            retryAt.keySet().retainAll(heard.keySet());voices.keySet().retainAll(heard.keySet());
            var eligible=heard.keySet().stream().filter(id->now>=retryAt.getOrDefault(id,0L)).toList();
            var best=eligible.stream().filter(injured::contains).max(Comparator.comparingDouble(id->voices.getOrDefault(id,NativeCampCalls.INJURED_VOLUME))).orElseGet(()->
                eligible.stream().filter(urgent::contains).findFirst().orElse(eligible.isEmpty()?null:eligible.getFirst()));
            // Equal calls do not restart an already agreed physical trip.
            // A stronger public request can still preempt it; expiry and the
            // existing blocked-trip deadline remain unchanged.
            if(best!=null&&pursuing!=null&&eligible.contains(pursuing)&&now<pursuitUntil){
                int currentRank=injured.contains(pursuing)?2:urgent.contains(pursuing)?1:0;
                int bestRank=injured.contains(best)?2:urgent.contains(best)?1:0;
                if(currentRank>bestRank||currentRank==bestRank&&voices.getOrDefault(pursuing,0f)>=voices.getOrDefault(best,0f))return pursuing;
            }
            return best;
        }
        boolean pursue(UUID id,long now){
            if(!id.equals(pursuing)){pursuing=id;pursuitUntil=now+1200;}
            if(now<pursuitUntil)return true;
            defer(id,now);return false;
        }
        void defer(UUID id,long now){
            // A repeated spoken call must not keep one failed route at the
            // head of the queue forever while other heard callers go hungry.
            retryAt.put(id,now+600);pursuing=null;rejectedPaths++;
        }
        void served(UUID id){
            // The real handoff addresses the last urgent signal. Keep the
            // outstanding ration request, but give other heard callers a turn.
            var expiry=heard.remove(id);if(expiry!=null)heard.put(id,expiry);
            urgent.remove(id);injured.remove(id);pursuing=null;
        }
        Map<String,Object> status(){var report=new LinkedHashMap<String,Object>(Map.of("activity",activity,"requests",requests,"delivered",delivered,
            "waiting",heard.size(),"rejectedPaths",rejectedPaths,"lastReceipt",receipt,"lastApproach",approach,"hatch",hatch.status(),"calls",calls.status(),
            "patrol",Map.of("target",patrolTarget==null?"":patrolTarget.toShortString(),"until",patrolUntil,"visited",patrolVisits.size(),"cages",patrolCages.size(),"lastRoute",patrolRoute)));
            report.put("nativeSprinting",nativeSprint);report.put("sprintDistance",sprintDistance);report.put("supplyRun",supplyRunReceipt);report.put("injuredRequests",injured.stream().map(UUID::toString).sorted().toList());report.put("pursuing",pursuing==null?"":pursuing.toString());return report;}
    }
    private static boolean enabled(ServerLevel level){return Villagers.config().nativeCampMeals()
        &&NativeRegionsScenario.nativeRegions(level)&&Villagers.LEARNING_MODE!=LearningMode.FROZEN
        &&AdventureActions.PERMISSIONS.allows(Capability.DELIVER_CAMP_MEAL);}
    static boolean sameCamp(ServerLevel level,Pillager donor,Villager receiver,Session s){
        return NativeIllagers.member(donor)&&NativeFoodStorage.homeStructure(level,receiver,s,receiver.blockPosition())
            &&NativeFoodStorage.homeStructure(level,receiver,s,donor.blockPosition())
            &&NativeIllagers.insideCamp(level,donor,receiver.blockPosition());
    }
    static boolean audible(double distanceSquared,double hearingRadius){
        double radius=Math.min(FoodRequest.SEARCH_RADIUS,hearingRadius);
        return Double.isFinite(distanceSquared)&&distanceSquared>=0&&Double.isFinite(radius)&&radius>0&&distanceSquared<=radius*radius;
    }
    private static boolean captiveHome(ServerLevel level,Villager v,Session s){
        var home=s.mind.home();
        if(home==null||!home.dimension().equals(level.dimension().identifier().toString()))return false;
        var at=new BlockPos(home.x(),home.y(),home.z());if(!level.isLoaded(at))return false;
        var camp=level.structureManager().getStructureWithPieceAt(at,h->h.getRegisteredName().equals("nova_structures:illager_barracks"));
        return camp.isValid()&&camp.getBoundingBox().isInside(v.blockPosition());
    }
    /** Receiver speaks from its own needs; this search never reads potential donors' stock. */
    static void request(ServerLevel level,Villager v,Session s,long now){
        if(!enabled(level)||!captiveHome(level,v,s)||now<s.nextCampMealRequest||v.isBaby()||v.isTrading()
            ||v.isInWater()||v.isOnFire()||!FoodOffer.wantsMeal(s.mind.foodLevel(),v.getHealth(),v.getMaxHealth(),false,
                FoodInventory.anySlot(v.getInventory(),AdventureActions.canHarvest(v))>=0))return;
        long previousCall=s.nextCampCall;
        NativeCampCalls.emit(level,v,s,now);
        // A hungry caller can prepare its own safe inside-gate rendezvous
        // after actually calling; no listener's private state is consulted.
        if(s.nextCampCall>previousCall&&s.mind.foodLevel()<=4)s.campMealWaitUntil=now+REQUEST_LIFE;
        var listeners=level.getEntitiesOfClass(Pillager.class,v.getBoundingBox().inflate(FoodRequest.SEARCH_RADIUS),
            p->p.isAlive()&&CreatureAgents.managed(p)&&sameCamp(level,p,v,s)&&p.getTarget()!=v
                // A spoken call can be heard through a cell, just like player
                // speech in the existing hearing UI. Hand contact still needs sight.
                &&audible(v.distanceToSqr(p),Villagers.config().hearingRadius()))
            .stream().sorted(Comparator.comparingDouble(v::distanceToSqr)).limit(4).toList();
        if(listeners.isEmpty())return;
        // A hungry sleeper must be able to call a guard who actually comes
        // within hearing. The gate wait below keeps the real meal trip awake.
        if(v.isSleeping())v.stopSleeping();
        boolean emergency=s.mind.foodLevel()<=4||v.getHealth()<v.getMaxHealth()*.5;
        var words=Speech.campFoodSignal(level,v,s,emergency);if(words==null)return;
        s.nextCampMealRequest=now+200;s.campMealRequests++;
        // The donor may have several heard requests queued. Keep the actual
        // rendezvous available for the same bounded lifetime as that request.
        s.campMealWaitUntil=now+REQUEST_LIFE;
        for(var p:listeners){
            var actor=CreatureAgents.loadedState(p);if(actor==null)continue;
            // Adults establish their own biome's founding dialect, just as villagers do.
            // Existing or developing vocabulary is never replaced by a requester's words.
            if(actor.mind.originBiome()==null)actor.mind.establishOrigin(level.getBiome(p.blockPosition()).unwrapKey().orElseThrow().identifier().toString());
            if(actor.mind.lexicon().isEmpty()&&!actor.mind.lexicon().developing())
                actor.mind.lexicon(BiomeDialect.foundingLanguage(actor.mind.originBiome()));
            actor.mind.lexicon(actor.mind.lexicon().establishGrammar(actor.mind.originBiome()));
            boolean hurtHeard=FoodRequest.understands(words,actor.mind.lexicon(),Concept.WANT,Concept.FOOD,Concept.HUNGRY,Concept.HURT)
                ||FoodRequest.understands(words,actor.mind.lexicon(),Concept.WANT,Concept.FOOD,Concept.HURT);
            boolean urgentHeard=hurtHeard||FoodRequest.understands(words,actor.mind.lexicon(),Concept.WANT,Concept.FOOD,Concept.HUNGRY);
            if(!urgentHeard&&!FoodRequest.understands(words,actor.mind.lexicon(),Concept.WANT,Concept.FOOD))continue;
            var work=actor.campMeals;
            if(!work.heard.containsKey(v.getUUID())){
                actor.mind.remember("Heard a camp food request from "+v.getUUID());
            }
            if(hurtHeard&&!work.injured.contains(v.getUUID()))actor.mind.remember("Understood a spoken hurt-and-food request from "+v.getUUID());
            work.hear(v.getUUID(),now+REQUEST_LIFE,urgentHeard||hurtHeard,hurtHeard);
        }
    }
    /** A hungry captive can wait at the reachable inside of its own cell door. */
    static String waitAtGate(ServerLevel level,Villager v,Session s,long now){
        if(!enabled(level)||!captiveHome(level,v,s)||now>=s.campMealWaitUntil||!wantsRations(v.getInventory(),AdventureActions.canHarvest(v))){
            s.campMealStand=s.campMealDoor=null;return null;
        }
        if(s.campMealStand!=null){
            double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,s.campMealStand);
            var center=new Vec3(s.campMealStand.getX()+.5,floor,s.campMealStand.getZ()+.5);
            if(v.onGround()&&v.position().distanceToSqr(center)<1.25*1.25&&Math.abs(v.getY()-floor)<.1){
                var body=v.getBoundingBox().move(center.subtract(v.position()));
                // Other captives can share a crowded cell. Native movement
                // still handles their collisions and pushing; their presence
                // must not invalidate an otherwise clear, supported approach.
                if(level.noBlockCollision(v,v.getBoundingBox().minmax(body))&&level.noBorderCollision(v,body)
                    &&!level.noBlockCollision(v,body.move(0,-.03,0))){
                    v.getNavigation().stop();
                    if(v.position().distanceToSqr(center)>.04){
                        v.getMoveControl().setWantedPosition(center.x,center.y,center.z,.4);
                        return "finishing the safe inside-gate rendezvous";
                    }
                    return "waiting inside the cell gate for requested food";
                }
            }
        }
        var current=v.getNavigation().getPath();
        if(current!=null&&!current.isDone()&&s.campMealStand!=null&&current.getTarget().equals(s.campMealStand))return "walking to the inside of the cell gate";
        // A request alone is not a reachable rendezvous. Until there is a
        // route or an actual stand above, let storage/other meal work proceed
        // during cooldown and planning admission instead of monopolizing the
        // hungry receiver's turn for the whole request lifetime.
        if(now<s.nextCampMealPath||!v.onGround())return null;
        if(v.getHealth()>5&&!WorkNavigation.admit(level,now,v))return null;
        s.nextCampMealPath=now+40;var origin=v.blockPosition();var doors=new ArrayList<BlockPos>();
        for(var p:BlockPos.betweenClosed(origin.offset(-8,-2,-8),origin.offset(8,1,8))){
            var block=level.getBlockState(p);
            if(block.is(Blocks.IRON_DOOR)&&block.getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER
                &&NativeFoodStorage.homeStructure(level,v,s,p))doors.add(p.immutable());
        }
        doors.sort(Comparator.comparingDouble(p->p.distSqr(origin)));
        for(var p:doors){
            // A body can stand behind the thin plane while its native start
            // cell is the closed iron door itself. Native navigation may have
            // no usable departure from that cell. Step onto the physically
            // clear, same-floor side with ordinary movement control first.
            if(p.equals(origin)&&!level.getBlockState(p).getValue(DoorBlock.OPEN)){
                var facing=level.getBlockState(p).getValue(DoorBlock.FACING);
                for(var stand:List.of(p.relative(facing),p.relative(facing.getOpposite()))){
                    double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,stand);
                    var center=new Vec3(stand.getX()+.5,floor,stand.getZ()+.5);
                    var body=v.getBoundingBox().move(center.subtract(v.position()));
                    if(Math.abs(floor-v.getY())>.1||!NativeFoodStorage.homeStructure(level,v,s,stand)
                        ||!level.getFluidState(stand).isEmpty()||!level.getFluidState(stand.below()).isEmpty()
                        ||!level.noBlockCollision(v,v.getBoundingBox().minmax(body).deflate(1e-6))
                        ||!level.noBorderCollision(v,body)||level.noBlockCollision(v,body.move(0,-.03,0)))continue;
                    s.campMealStand=stand;s.campMealDoor=p;v.getNavigation().stop();
                    v.getMoveControl().setWantedPosition(center.x,center.y,center.z,.4);
                    return "stepping clear of the closed door for the food rendezvous";
                }
            }
            var path=gateApproach(level,v,s,p);
            if(path!=null&&v.getNavigation().moveTo(path,.5)){
                s.campMealStand=path.getEndNode().asBlockPos();s.campMealDoor=p;return "walking to the inside of the cell gate";
            }
        }
        var gap=feedingGapApproach(level,v,s,now);
        if(gap!=null&&v.getNavigation().moveTo(gap,.5)){
            s.campMealStand=gap.getEndNode().asBlockPos();return "meeting the food request at the existing cage opening";
        }
        return null;
    }
    /** Move only across the captive's existing, level wooden floor to an
     * already open lower gap. No cage block is removed and no exit is planned. */
    private static net.minecraft.world.level.pathfinder.Path feedingGapApproach(ServerLevel level,Villager v,Session s,long now){
        var listeners=level.getEntitiesOfClass(Pillager.class,v.getBoundingBox().inflate(FoodRequest.SEARCH_RADIUS),
            p->p.isAlive()&&CreatureAgents.managed(p)&&p.getTarget()!=v&&sameCamp(level,p,v,s)
                &&audible(v.distanceToSqr(p),Villagers.config().hearingRadius()));
        if(listeners.isEmpty())return null;
        var origin=v.blockPosition();var gaps=new HashMap<BlockPos,BlockPos>();
        for(var gap:BlockPos.betweenClosed(origin.offset(-3,0,-3),origin.offset(3,0,3))){
            if(!level.isLoaded(gap.above(2))||!NativeFoodStorage.homeStructure(level,v,s,gap)||!level.getBlockState(gap).isAir()
                ||!level.getFluidState(gap).isEmpty()||!level.getBlockState(gap.above()).is(net.minecraft.tags.BlockTags.WOODEN_FENCES)
                ||!level.getBlockState(gap.above(2)).is(level.getBlockState(gap.above()).getBlock()))continue;
            for(var d:Direction.Plane.HORIZONTAL){
                var stand=gap.relative(d);
                if(!level.getBlockState(stand.below()).is(net.minecraft.tags.BlockTags.PLANKS)
                    ||!level.getBlockState(stand).isAir()||!level.getBlockState(stand.above()).isAir())continue;
                var body=v.getBoundingBox().move(Vec3.atBottomCenterOf(stand).subtract(v.position()));
                if(!level.noBlockCollision(v,body)||level.noBlockCollision(v,body.move(0,-.03,0)))continue;
                gaps.put(stand,gap.immutable());
            }
        }
        var stands=new ArrayList<>(gaps.keySet());
        stands.sort(Comparator.comparingDouble(p->listeners.stream().mapToDouble(d->d.position().distanceToSqr(Vec3.atCenterOf(p))).min().orElse(Double.MAX_VALUE)));
        for(var stand:stands.stream().limit(3).toList()){
            var path=NativePathLimits.search(v,1,()->v.getNavigation().createPath(stand,0,16));
            if(path==null||!path.canReach()||path.getNodeCount()>6)continue;
            boolean safe=true;
            for(int i=0;i<path.getNodeCount();i++){
                var at=path.getNodePos(i);
                if(at.getY()!=origin.getY()||!NativeFoodStorage.homeStructure(level,v,s,at)
                    ||!level.getBlockState(at.below()).is(net.minecraft.tags.BlockTags.PLANKS)||!level.getFluidState(at).isEmpty()){
                    safe=false;break;
                }
            }
            if(!safe)continue;
            s.campMealDoor=gaps.get(stand);
            s.campMealMeetReceipt=Map.of("gameTime",now,"from",v.position().toString(),"stand",stand.toShortString(),
                "existingGap",gaps.get(stand).toShortString(),"nodes",path.getNodeCount(),"blocksRemoved",0);
            return path;
        }
        return null;
    }
    /** Meet squarely at the doorway. A radius-one door path may stop diagonally
     * behind its stone jamb, where a guard cannot reach through the open gate. */
    static net.minecraft.world.level.pathfinder.Path gateApproach(ServerLevel level,Villager v,Session s,BlockPos door){
        var block=level.getBlockState(door);if(!block.is(Blocks.IRON_DOOR))return null;
        var facing=block.getValue(DoorBlock.FACING);
        var stands=new ArrayList<>(List.of(door.relative(facing),door.relative(facing.getOpposite())));
        stands.sort(Comparator.comparingDouble(p->p.distSqr(v.blockPosition())));
        for(var stand:stands){
            var path=NativePathLimits.search(v,v.getHealth()<=5?1:2,()->v.getNavigation().createPath(stand,0,32));
            if(path==null||!path.canReach())continue;
            int y=v.getBlockY();boolean safe=true;
            for(int n=0;n<path.getNodeCount();n++){
                var at=path.getNodePos(n);
                if(!level.isLoaded(at)||!NativeFoodStorage.homeStructure(level,v,s,at)||!level.getFluidState(at).isEmpty()
                    ||at.getY()<y-(v.getHealth()<=5?1:2)){safe=false;break;}y=at.getY();
            }
            if(safe)return path;
        }
        return null;
    }
    /** Reserve two bread-equivalent meals from actual supported food, without counting grain as a meal. */
    static int nutrition(Container bag){
        return nutrition(bag,false);
    }
    static int nutrition(Container bag,boolean farmer){
        int total=0;for(int i=0;i<bag.getContainerSize();i++){
            var item=bag.getItem(i);if(FoodInventory.supported(item))total+=FoodInventory.availableCount(item,farmer)*item.get(DataComponents.FOOD).nutrition();
        }return total;
    }
    static boolean wantsRations(Container bag,boolean farmer){return nutrition(bag,farmer)<10;}
    static int spareSlot(Container bag){
        return spareSlot(bag,ItemStack.EMPTY);
    }
    static int spareSlot(Container bag,ItemStack heldMeal){
        int total=nutrition(bag)+(FoodInventory.supported(heldMeal)?heldMeal.getCount()*heldMeal.get(DataComponents.FOOD).nutrition():0);
        return FoodInventory.sharingSlot(bag,false,one->total-one.get(DataComponents.FOOD).nutrition()>=10);
    }
    private static boolean reserveAvailable(Pillager donor){
        var actor=CreatureAgents.loadedState(donor);
        // Own food consumption already runs before deliveries. A healthy
        // donor who can defer that meal may answer an urgent call with an
        // actual ration, even while still building a ten-nutrition reserve.
        return actor!=null&&FoodOffer.mayDeferOwnMeal(actor.mind.foodLevel(),donor.getHealth(),donor.getMaxHealth())
            &&(usableDeliveryRation(donor.getInventory(),donor.getOffhandItem())||knownSingleRoot(donor));
    }
    static boolean usableDeliveryRation(Container bag,ItemStack offhand){
        if(FoodInventory.supported(offhand)&&(!plantingRoot(offhand)||offhand.getCount()>=2))return true;
        return usableDeliverySlot(bag)>=0;
    }
    private static int usableDeliverySlot(Container bag){
        return usableDeliverySlot(bag,item->!plantingRoot(item)||item.getCount()>=2);
    }
    private static int usableDeliverySlot(Container bag,java.util.function.Predicate<ItemStack> eligible){
        int best=-1,nutrition=0;
        for(int i=0;i<bag.getContainerSize();i++){
            var item=bag.getItem(i);var food=item.get(DataComponents.FOOD);
            if(FoodInventory.supported(item)&&food!=null&&food.nutrition()>nutrition&&eligible.test(item)){
                best=i;nutrition=food.nutrition();
            }
        }
        return best;
    }
    private static String rootPairTag(UUID recipient,ItemStack item){
        return "villagers:given_root_pair:"+recipient+":"+BuiltInRegistries.ITEM.getKey(item.getItem());
    }
    /** This is the donor's own actual handoff history, never a remote stock read. */
    private static boolean knownSingleRoot(Pillager donor){
        var actor=CreatureAgents.loadedState(donor);if(actor==null)return false;
        long now=donor.level().getGameTime();var w=actor.campMeals;
        var callers=new HashSet<UUID>();
        w.heard.forEach((id,expires)->{if(now<expires)callers.add(id);});
        w.calls.heard.forEach((id,call)->{if(now<call.expires())callers.add(id);});
        var items=new ArrayList<ItemStack>();for(int i=0;i<donor.getInventory().getContainerSize();i++)items.add(donor.getInventory().getItem(i));
        items.add(donor.getOffhandItem());
        return items.stream().anyMatch(item->plantingRoot(item)&&item.getCount()==1
            &&callers.stream().anyMatch(id->donor.entityTags().contains(rootPairTag(id,item))));
    }
    private static boolean plantingRoot(ItemStack item){return item.is(net.minecraft.world.item.Items.CARROT)||item.is(net.minecraft.world.item.Items.POTATO);}
    static net.minecraft.world.SimpleContainer bundlePlan(Container recipient,ItemStack source,int count){
        if(count<1||count>source.getCount())return null;
        var after=new net.minecraft.world.SimpleContainer(recipient.getContainerSize());
        for(int i=0;i<recipient.getContainerSize();i++)after.setItem(i,recipient.getItem(i).copy());
        return after.addItem(source.copyWithCount(count)).isEmpty()?after:null;
    }
    /** Minecraft's sprint action, charged each server tick from actual movement. */
    static void chargeRunning(Pillager donor,CreatureAgents.State actor,long now){
        var w=actor.campMeals;
        if(w.nativeSprint&&w.runSample!=null&&now>w.runSampleAt&&now-w.runSampleAt<=20){
            double distance=donor.position().subtract(w.runSample).horizontalDistance();
            actor.mind.exert((float)(distance*.1));w.sprintDistance+=distance;
        }
        if(w.runningRoute!=null&&w.runningRoute==donor.getNavigation().getPath()){
            boolean supplies=w.runningForSupplies&&actor.mind.foodLevel()>=14
                &&(criticalVoice(w,now)||NativeCampCalls.louderInjury(w.calls,now,13));
            if(!supplies&&!mayRunDelivery(actor.mind.foodLevel(),donor.getInventory(),donor.getOffhandItem(),criticalVoice(w,now))){
                donor.getNavigation().moveTo(w.runningRoute,.8);w.runningRoute=null;
            }
        }else w.runningRoute=null;
        w.runSample=donor.position();w.runSampleAt=now;
        if(donor.getNavigation().isDone())w.runningRoute=null;
        boolean levelRun=w.runningRoute!=null&&levelRunSegment(donor,w.runningRoute);
        if(w.runningRoute!=null)donor.getNavigation().setSpeedModifier(levelRun?VillageSurvival.RUN:.8);
        boolean sprint=levelRun&&!donor.isInWater()&&!donor.isOnFire()
            &&FoodOffer.mayDeferOwnMeal(actor.mind.foodLevel(),donor.getHealth(),donor.getMaxHealth());
        if(donor.isSprinting()!=sprint)donor.setSprinting(sprint);
        w.nativeSprint=sprint;
    }
    /** Fast native travel is reserved for level ground; land stair jumps at walking pace. */
    private static boolean levelRunSegment(Pillager donor,net.minecraft.world.level.pathfinder.Path path){
        if(!donor.onGround())return false;
        int next=path.getNextNodeIndex();
        for(int i=next;i<Math.min(path.getNodeCount(),next+2);i++){
            var at=path.getNodePos(i);
            if(Math.abs(at.getY()-donor.getY())>.25)return false;
            var feet=new Vec3(at.getX()+.5,donor.getY(),at.getZ()+.5);
            var body=donor.getBoundingBox().move(feet.subtract(donor.position()));
            if(!NativeBodyRoutes.clearBodyWalk((ServerLevel)donor.level(),donor,body))return false;
        }
        return true;
    }
    static boolean followRoute(Pillager donor,State w,net.minecraft.world.level.pathfinder.Path path,long now,boolean urgent){
        if(path==null)return false;var actor=CreatureAgents.loadedState(donor);
        boolean running=urgent&&actor!=null&&mayRunDelivery(actor.mind.foodLevel(),donor.getInventory(),donor.getOffhandItem(),criticalVoice(w,now));
        if(!donor.getNavigation().moveTo(path,running?VillageSurvival.RUN:.8))return false;
        w.runningForSupplies=false;
        w.runningRoute=running?donor.getNavigation().getPath():null;w.runSample=donor.position();w.runSampleAt=now;
        return true;
    }
    static void hurryForCriticalSupplies(Pillager donor,CreatureAgents.State actor,long now){
        var w=actor.campMeals;var path=donor.getNavigation().getPath();
        if(path==null||!BoatTravel.dryPath((ServerLevel)donor.level(),path)||actor.mind.foodLevel()<14
            ||!FoodOffer.mayDeferOwnMeal(actor.mind.foodLevel(),donor.getHealth(),donor.getMaxHealth())
            ||!(criticalVoice(w,now)||NativeCampCalls.louderInjury(w.calls,now,13)))return;
        if(!donor.getNavigation().moveTo(path,VillageSurvival.RUN))return;
        w.runningRoute=donor.getNavigation().getPath();w.runningForSupplies=true;w.runSample=donor.position();w.runSampleAt=now;
        w.supplyRunReceipt=Map.of("gameTime",now,"from",donor.position().toString(),"nodes",path.getNodeCount(),"food",actor.mind.foodLevel(),"speed",VillageSurvival.RUN);
    }
    static boolean maySprintWithRations(int food,Container bag,ItemStack offhand){
        // Preserve the actual emergency meal during a long delivery. Walking
        // uses normal movement; it avoids the explicitly charged sprint cost.
        return food>local.villagers.mind.Metabolism.SPRINT_FOOD_LEVEL&&spareSlot(bag,offhand)>=0;
    }
    static boolean criticalVoice(State w,long now){
        var call=w.calls.pursuing==null?null:w.calls.heard.get(w.calls.pursuing);
        return call!=null&&now<call.expires()&&call.volume()>=14
            ||w.pursuing!=null&&w.voices.getOrDefault(w.pursuing,0f)>=14
            ||w.heard.entrySet().stream().anyMatch(e->now<e.getValue()&&w.urgent.contains(e.getKey())
                &&now>=w.retryAt.getOrDefault(e.getKey(),0L)&&w.voices.getOrDefault(e.getKey(),0f)>=14);
    }
    static boolean deferCriticalMeal(Pillager donor,CreatureAgents.State actor,long now){
        return actor.mind.foodLevel()>=14&&FoodOffer.mayDeferOwnMeal(actor.mind.foodLevel(),donor.getHealth(),donor.getMaxHealth())
            &&(criticalVoice(actor.campMeals,now)||NativeCampCalls.louderInjury(actor.campMeals.calls,now,13));
    }
    static boolean mayRunDelivery(int food,Container bag,ItemStack offhand,boolean critical){
        // An immediately critical voice can justify running with a small
        // ration. Charge every actual step as usual and stop running before
        // the donor needs its next meal; own eating still takes priority.
        return maySprintWithRations(food,bag,offhand)||critical&&food>=14
            &&(FoodInventory.anySlot(bag,false)>=0||FoodInventory.supported(offhand));
    }
    /** The reached recipient asks for enough actual stock for recovery or
     * two ordinary meals. This is never called during remote call selection. */
    static int contactBundleCount(Container receiver,ItemStack source,boolean farmer,int food,float health,float maxHealth){
        var properties=source.get(net.minecraft.core.component.DataComponents.FOOD);
        if(properties==null||properties.nutrition()<1)return 0;
        int reserve=plantingRoot(source)&&farmer&&receiver.countItem(source.getItem())==0?1:0;
        int target=Math.max(10,local.villagers.mind.MealPolicy.needsRecoveryFood(food,health,maxHealth)?18-food:0);
        int needed=Math.max(0,target-nutrition(receiver,farmer));
        int edible=(needed+properties.nutrition()-1)/properties.nutrition();
        return Math.max(reserve+1,Math.min(source.getCount(),edible+reserve));
    }
    /** Only the recipient answers its remaining need and capacity, at actual hand contact. */
    private static boolean give(ServerLevel level,Pillager donor,Villager receiver,State w,long now){
        var s=Villagers.existingSession(receiver.getUUID());
        var contact=CampFoodHatch.contact(level,donor,receiver,donor.getEyePosition());
        if(s==null||contact==null||!FoodRequest.inReach(donor.getEyePosition().distanceToSqr(contact),Villagers.config().hearingRadius(),true))return false;
        int slot=spareSlot(donor.getInventory(),donor.getOffhandItem());
        boolean reserve=slot<0&&w.urgent.contains(receiver.getUUID())&&reserveAvailable(donor);
        // Recipient stock is answered only now, at actual hand contact.
        if(reserve)slot=usableDeliverySlot(donor.getInventory(),item->!plantingRoot(item)||item.getCount()>=2
            ||receiver.getInventory().countItem(item.getItem())>0);
        boolean held=slot<0&&reserve&&FoodInventory.supported(donor.getOffhandItem());
        if(slot<0&&!held)return false;
        if(!wantsRations(receiver.getInventory(),AdventureActions.canHarvest(receiver))){
            w.heard.remove(receiver.getUUID());return false;
        }
        var source=held?donor.getOffhandItem():donor.getInventory().getItem(slot);
        var item=source.getItem();int before=donor.getInventory().countItem(item),recipientBefore=receiver.getInventory().countItem(item);
        boolean farmer=AdventureActions.canHarvest(receiver);
        int minimum=plantingRoot(source)&&farmer&&recipientBefore==0?2:1;
        int count=contactBundleCount(receiver.getInventory(),source,farmer,s.mind.foodLevel(),receiver.getHealth(),receiver.getMaxHealth());
        net.minecraft.world.SimpleContainer after=null;
        int donorNutrition=nutrition(donor.getInventory())+(FoodInventory.supported(donor.getOffhandItem())?donor.getOffhandItem().getCount()*donor.getOffhandItem().get(DataComponents.FOOD).nutrition():0);
        for(;count>=minimum;count--){
            var plan=bundlePlan(receiver.getInventory(),source,count);if(plan==null)continue;
            boolean usesReserve=donorNutrition-count*source.get(DataComponents.FOOD).nutrition()<10;
            if(usesReserve&&(!w.urgent.contains(receiver.getUUID())||!reserveAvailable(donor)))continue;
            after=plan;reserve|=usesReserve;break;
        }
        if(after==null)return false;
        var offered=source.copyWithCount(1);
        int ownedBefore=before+(donor.getOffhandItem().is(item)?donor.getOffhandItem().getCount():0);
        for(int i=0;i<after.getContainerSize();i++)receiver.getInventory().setItem(i,after.getItem(i));
        if(held)source.shrink(count);else donor.getInventory().removeItem(slot,count);
        if(count>=2&&recipientBefore==0&&plantingRoot(offered)
            &&donor.entityTags().stream().filter(t->t.startsWith("villagers:given_root_pair:")).count()<16)
            donor.addTag(rootPairTag(receiver.getUUID(),offered));
        w.delivered++;w.giveAt=now+40;w.served(receiver.getUUID());
        s.campMealWaitUntil=now+600;
        var receipt=new LinkedHashMap<String,Object>();receipt.put("gameTime",now);receipt.put("from",donor.getUUID().toString());receipt.put("to",receiver.getUUID().toString());
        receipt.put("item",BuiltInRegistries.ITEM.getKey(item).toString());receipt.put("giverBefore",before);receipt.put("giverAfter",donor.getInventory().countItem(item));
        receipt.put("count",count);
        receipt.put("receiverBefore",recipientBefore);receipt.put("receiverAfter",receiver.getInventory().countItem(item));receipt.put("distance",donor.distanceTo(receiver));
        receipt.put("contact",contact.toString());
        receipt.put("handReach",donor.getEyePosition().distanceTo(contact));
        receipt.put("usedEmergencyReserve",reserve);receipt.put("donorSource",held?"offhand":"bag");
        receipt.put("ownedItemBefore",ownedBefore);receipt.put("ownedItemAfter",donor.getInventory().countItem(item)
            +(donor.getOffhandItem().is(item)?donor.getOffhandItem().getCount():0));
        w.receipt=Map.copyOf(receipt);s.lastFoodReceived=w.receipt;s.foodSharedReceived++;
        s.mind.remember("Received "+count+" actual camp food items from "+donor.getUUID());Villagers.persist(s);
        CreatureAgents.mind(donor).remember("Handed "+count+" owned food items to a requesting captive "+receiver.getUUID());
        return true;
    }
    /** Operate an observed native lever only while both handoff and lever are in real reach. */
    private static boolean giveThroughGate(ServerLevel level,Pillager donor,Villager v,State w,long now){
        if(now<w.giveAt||donor.getEyePosition().distanceToSqr(v.getBoundingBox().getCenter())>16||!AdventureActions.PERMISSIONS.allows(Capability.OPEN_DOOR)
            ||!AdventureActions.PERMISSIONS.allows(Capability.CLOSE_DOOR))return false;
        for(var p:BlockPos.betweenClosed(donor.blockPosition().offset(-3,-1,-3),donor.blockPosition().offset(3,3,3))){
            var before=level.getBlockState(p);
            if(!(before.getBlock() instanceof LeverBlock lever)||before.getValue(LeverBlock.POWERED)
                ||!NativeCampWork.local(level,donor,p)||!NativeIllagers.insideCamp(level,donor,p)
                ||BlockContact.interactionPoint(level,donor,donor.getEyePosition(),p,3)==null)continue;
            var gates=new ArrayList<BlockPos>();
            for(var at:BlockPos.betweenClosed(p.offset(-2,-2,-2),p.offset(2,1,2)))if(level.getBlockState(at).is(Blocks.IRON_DOOR)
                &&level.getBlockState(at).getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER&&!level.getBlockState(at).getValue(DoorBlock.OPEN))gates.add(at.immutable());
            if(gates.isEmpty())continue;
            boolean given=false;
            try{
                // This is Minecraft's lever interaction, including real redstone
                // propagation. No fake player or direct door-state override.
                lever.pull(before,level,p,null);
                given=give(level,donor,v,w,now);
            }finally{
                var state=level.getBlockState(p);
                if(state.is(before.getBlock())&&state.getValue(LeverBlock.POWERED))lever.pull(state,level,p,null);
            }
            if(given){
                var receipt=new LinkedHashMap<>(w.receipt);receipt.put("lever",p.toShortString());
                receipt.put("leverRestored",level.getBlockState(p).equals(before));
                receipt.put("gatesClosed",gates.stream().allMatch(at->level.getBlockState(at).is(Blocks.IRON_DOOR)&&!level.getBlockState(at).getValue(DoorBlock.OPEN)));
                w.receipt=Map.copyOf(receipt);var recipient=Villagers.existingSession(v.getUUID());if(recipient!=null)recipient.lastFoodReceived=w.receipt;
                return true;
            }
        }
        return false;
    }
    static List<BlockPos> givingStands(ServerLevel level,Pillager donor,Villager v){
        var stands=new ArrayList<BlockPos>();
            for(var p:BlockPos.betweenClosed(v.blockPosition().offset(-3,-2,-3),v.blockPosition().offset(3,1,3))){
                if(!NativeCampWork.local(level,donor,p)||!level.isLoaded(p.above())||!level.getBlockState(p).isAir()
                    ||!level.getBlockState(p.above()).isAir()||!level.isLoaded(p.below())
                    ||!level.getFluidState(p).isEmpty()||!level.getFluidState(p.below()).isEmpty())continue;
                // Camp stairs and slabs are ordinary native walking surfaces.
                // Use the pathfinder's actual floor height and a supported,
                // collision-free body instead of demanding a full solid face.
                double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,p);
                var feet=new Vec3(p.getX()+.5,floor,p.getZ()+.5);
                var body=donor.getBoundingBox().move(feet.subtract(donor.position()));
                var eye=feet.add(0,donor.getEyeHeight(),0);
                if(CampFoodHatch.contactPoints(v.getBoundingBox()).stream().noneMatch(point->eye.distanceToSqr(point)<=9)
                    ||!level.noCollision(donor,body)||level.noCollision(donor,body.move(0,-.03,0)))continue;
                if(CampFoodHatch.contact(level,donor,v,eye)!=null||gateHandoffStand(level,donor,v,eye)
                    ||CampFoodHatch.proposed(level,donor,v,eye)!=null)stands.add(p.immutable());
            }
        return stands;
    }
    /** Public camp geometry near the heard caller, not their private movement
     * plan. Approach an actual gate lever before trying the wall by their voice. */
    static List<BlockPos> meetingGateStands(ServerLevel level,Pillager donor,BlockPos voice){
        var stands=new LinkedHashSet<BlockPos>();var doors=new ArrayList<BlockPos>();
        if(!AdventureActions.PERMISSIONS.allows(Capability.OPEN_DOOR)||!AdventureActions.PERMISSIONS.allows(Capability.CLOSE_DOOR))return new ArrayList<>();
        for(var p:BlockPos.betweenClosed(voice.offset(-8,-2,-8),voice.offset(8,2,8)))
            if(level.isLoaded(p)&&NativeCampWork.local(level,donor,p)&&NativeIllagers.insideCamp(level,donor,p)
                &&level.getBlockState(p).is(Blocks.IRON_DOOR)&&level.getBlockState(p).getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER)doors.add(p.immutable());
        doors.sort(Comparator.comparingDouble(p->p.distSqr(voice)));
        for(var door:doors.stream().limit(2).toList())for(var lever:BlockPos.betweenClosed(door.offset(-2,-1,-2),door.offset(2,2,2))){
            if(!level.isLoaded(lever)||!NativeCampWork.local(level,donor,lever)||!(level.getBlockState(lever).getBlock() instanceof LeverBlock))continue;
            for(var p:BlockPos.betweenClosed(lever.offset(-2,-2,-2),lever.offset(2,1,2))){
                if(!level.isLoaded(p.above())||!NativeCampWork.local(level,donor,p)||!NativeIllagers.insideCamp(level,donor,p)
                    ||!level.getFluidState(p).isEmpty()||!level.getFluidState(p.below()).isEmpty())continue;
                double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,p);
                var feet=new Vec3(p.getX()+.5,floor,p.getZ()+.5);var body=donor.getBoundingBox().move(feet.subtract(donor.position()));
                if(feet.distanceToSqr(Vec3.atCenterOf(voice))>NativeCampCalls.listeningRadius()*NativeCampCalls.listeningRadius()
                    ||!level.noCollision(donor,body)||level.noBlockCollision(donor,body.move(0,-.03,0))
                    ||BlockContact.interactionPoint(level,donor,feet.add(0,donor.getEyeHeight(),0),lever,3)==null)continue;
                stands.add(p.immutable());
            }
        }
        return new ArrayList<>(stands);
    }
    /** Plan a real lever-side rendezvous even while its iron gate blocks sight.
     * Only planning ignores that gate; the actual handoff still pulls the native
     * lever and requires the real, newly unobstructed ray and normal reach. */
    private static boolean gateHandoffStand(ServerLevel level,Pillager donor,Villager receiver,Vec3 eye){
        for(var end:CampFoodHatch.contactPoints(receiver.getBoundingBox())){
        if(eye.distanceToSqr(end)>9)continue;
        var obstruction=level.clip(new net.minecraft.world.level.ClipContext(eye,end,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,donor));
        if(obstruction.getType()!=HitResult.Type.BLOCK)continue;
        var hit=obstruction.getBlockPos();var state=level.getBlockState(hit);
        if(!state.is(Blocks.IRON_DOOR)||state.getValue(DoorBlock.OPEN))continue;
        final var gate=state.getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER?hit.below():hit;
        if(!NativeCampWork.local(level,donor,gate)||!NativeIllagers.insideCamp(level,donor,gate))continue;
        var after=level.clip(new net.minecraft.world.level.ClipContext(eye,end,
            net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,donor){
            @Override public net.minecraft.world.phys.shapes.VoxelShape getBlockShape(net.minecraft.world.level.block.state.BlockState block,
                net.minecraft.world.level.BlockGetter world,BlockPos at){
                return at.equals(gate)||at.equals(gate.above())
                    ?super.getBlockShape(block.setValue(DoorBlock.OPEN,true),world,at):super.getBlockShape(block,world,at);
            }
        });
        if(after.getType()!=HitResult.Type.MISS)continue;
        for(var leverPos:BlockPos.betweenClosed(gate.offset(-2,-1,-2),gate.offset(2,2,2))){
            if(!level.isLoaded(leverPos)||!NativeCampWork.local(level,donor,leverPos))continue;
            var lever=level.getBlockState(leverPos);
            if(lever.getBlock() instanceof LeverBlock&&!lever.getValue(LeverBlock.POWERED)
                &&BlockContact.interactionPoint(level,donor,eye,leverPos,3)!=null)return true;
        }
        }
        return false;
    }
    /** Test ordinary slow walking beside a pushable camp beast. Never disable
     * collisions or redirect that body; the native push physics decides. */
    private static net.minecraft.world.level.pathfinder.Path crowdContactPath(ServerLevel level,Pillager donor,List<BlockPos> candidates){
        var neighbors=level.getEntities(donor,donor.getBoundingBox().inflate(8),e->e instanceof net.minecraft.world.entity.LivingEntity&&e.isAlive());
        if(neighbors.stream().noneMatch(e->e instanceof net.minecraft.world.entity.monster.Ravager&&e.isPushable()))return null;
        var sorted=new ArrayList<>(candidates);sorted.sort(Comparator.comparingDouble(p->p.distSqr(donor.blockPosition())));
        for(var stand:sorted.stream().limit(3).toList()){
            var route=NativePathLimits.search(donor,2,()->donor.getNavigation().createPath(stand,0,128));
            if(!NativeIllagers.safePath(level,donor,route))continue;
            boolean beast=false,blocked=false;
            for(int i=0;i<route.getNodeCount();i++){
                var at=route.getNodePos(i);double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,at);
                var body=donor.getBoundingBox().move(new Vec3(at.getX()+.5,floor,at.getZ()+.5).subtract(donor.position()));
                for(var e:neighbors)if(e.getBoundingBox().inflate(.05).intersects(body)){
                    if(!(e instanceof net.minecraft.world.entity.monster.Ravager)||!e.isPushable()
                        ||e instanceof net.minecraft.world.entity.Mob mob&&mob.getTarget()==donor){blocked=true;break;}
                    beast=true;
                }
            }
            if(beast&&!blocked)return route;
        }
        return null;
    }
    static String tick(ServerLevel level,Pillager donor,CreatureAgents.State actor,long now){
        if(!enabled(level)||!NativeIllagers.member(donor)||IllagerFamilies.juvenile(donor)||donor.isInWater()||donor.isOnFire()
            ||!FoodOffer.mayDeferOwnMeal(actor.mind.foodLevel(),donor.getHealth(),donor.getMaxHealth())
            ||spareSlot(donor.getInventory(),donor.getOffhandItem())<0&&!reserveAvailable(donor))return null;
        var w=actor.campMeals;var id=w.next(now);
        // Finish an understood urgent request before investigating another
        // uninterpreted voice; repeated alarms must not pull a worker away
        // from the actual nearby handoff it has already agreed to make.
        if(id==null||!w.urgent.contains(id)||NativeCampCalls.louderInjury(w.calls,now,
            w.injured.contains(id)?w.voices.getOrDefault(id,NativeCampCalls.INJURED_VOLUME):8)){
            var investigation=NativeCampCalls.investigate(level,donor,w,now);if(investigation!=null)return w.activity=investigation;
            if(id==null)return null;
        }
        if(!w.urgent.contains(id)&&spareSlot(donor.getInventory(),donor.getOffhandItem())<0)return null;
        var entity=level.getEntity(id);
        if(!(entity instanceof Villager v)||!v.isAlive()||!Villagers.isManaged(v)||v.isSleeping()||v.isTrading()||donor.getTarget()==v){w.heard.remove(id);return null;}
        var s=Villagers.existingSession(id);if(s==null||!sameCamp(level,donor,v,s)){w.heard.remove(id);return null;}
        if(!w.pursue(id,now)){donor.getNavigation().stop();return w.activity="rechecking other heard requests after a blocked meal trip";}
        if(CampFoodHatch.contact(level,donor,v,donor.getEyePosition())!=null
            &&!wantsRations(v.getInventory(),AdventureActions.canHarvest(v))){w.heard.remove(id);return null;}
        if(giveThroughGate(level,donor,v,w,now))return w.activity="handed a real meal through the native cell gate";
        // A reached handoff or reachable hatch is already actionable. Do it
        // before optional stand centering: a neighboring body can prevent
        // reaching that exact center while the native hand ray is clear.
        var hatch=CampFoodHatch.work(level,donor,v,w.hatch,now);if(hatch!=null)return w.activity=hatch;
        var contact=CampFoodHatch.contact(level,donor,v,donor.getEyePosition());
        boolean near=contact!=null&&FoodRequest.inReach(donor.getEyePosition().distanceToSqr(contact),Villagers.config().hearingRadius(),true);
        if(near){
            donor.getNavigation().stop();donor.getLookControl().setLookAt(v,30,30);
            if(now<w.giveAt)return w.activity="waiting between real camp meal handoffs";
            boolean given=give(level,donor,v,w,now);if(!given)w.heard.remove(id);
            return given?w.activity="handed over a real camp meal":null;
        }
        // Native paths finish within a node tolerance. At a narrow lever-side
        // handoff that can leave the worker against the jamb instead of at the
        // actual stand it selected. Finish only a short supported clear walk.
        if(id.equals(w.handoffRecipient)&&w.handoffStand!=null&&donor.onGround()){
            double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,w.handoffStand);
            var feet=new Vec3(w.handoffStand.getX()+.5,floor,w.handoffStand.getZ()+.5);
            double distance=donor.position().distanceToSqr(feet);
            if(distance>.04&&distance<1.25*1.25&&Math.abs(donor.getY()-floor)<.1){
                var body=donor.getBoundingBox().move(feet.subtract(donor.position()));
                if(level.noBlockCollision(donor,donor.getBoundingBox().minmax(body))&&level.noBorderCollision(donor,body)
                    &&NativeBodyRoutes.clearBodyWalk(level,donor,body)&&!level.noBlockCollision(donor,body.move(0,-.03,0))){
                    donor.getNavigation().stop();donor.getMoveControl().setWantedPosition(feet.x,feet.y,feet.z,.4);
                    return w.activity="finishing the clear native meal handoff stand";
                }
            }
        }
        // A reached lever-side meeting point may still put stone between
        // the actual hands. Remember that failed contact briefly and try a
        // different native stand, rather than choosing the same one-node path.
        if(id.equals(w.handoffRecipient)&&w.handoffStand!=null&&donor.onGround()){
            double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,w.handoffStand);
            var feet=new Vec3(w.handoffStand.getX()+.5,floor,w.handoffStand.getZ()+.5);
            if(donor.position().distanceToSqr(feet)<=.04){
                w.missStand(id,w.handoffStand,now);w.handoffStand=null;w.deliveryRoute=null;
                donor.getNavigation().stop();w.pathAt=now;
            }
        }
        var current=donor.getNavigation().getPath();
        if(current!=null&&!current.isDone()){
            boolean stalled=w.deliveryProgress.stalled(id.toString(),now,donor.getX(),donor.getY(),donor.getZ());
            if(!stalled&&id.equals(w.deliveryRecipient)&&current==w.deliveryRoute&&BoatTravel.dryPath(level,current))
                return w.activity="following the verified native meal handoff route";
            if(!donor.onGround()&&!stalled)return w.activity="finishing a native step on the meal route";
            if(stalled){donor.getNavigation().stop();w.deliveryRoute=null;w.pathAt=now;}
        }
        if(now>=w.pathAt){
            w.pathAt=now+40;
            // Understood requests need a physical handoff, not merely a
            // reachable lever somewhere near the caller. Try contact stands
            // (including an open-gate ray) before the wider meeting area.
            var stands=givingStands(level,donor,v);
            stands.removeIf(p->!w.retryStand(id,p,now));
            var path=CampBeds.pathToStands(level,donor,stands);boolean gate=false,crowd=false;
            if(path==null&&criticalVoice(w,now)){
                path=crowdContactPath(level,donor,stands);crowd=path!=null;
                if(crowd)actor.mind.remember("Following a normal walking route beside an actual camp ravager");
            }
            if(path==null){
                stands=meetingGateStands(level,donor,v.blockPosition());
                stands.removeIf(p->!w.retryStand(id,p,now));
                path=CampBeds.pathToStands(level,donor,stands);gate=path!=null;
            }
            w.approach=Map.of("gameTime",now,"actor",donor.position().toString(),"recipient",v.position().toString(),"recipientId",id.toString(),"stands",stands.size(),
                "nodes",path==null?0:path.getNodeCount(),"end",path==null?"":path.getEndNode().asBlockPos().toShortString(),"rendezvous",gate?"native gate":"recipient","nativeCrowdWalk",crowd);
            if(path!=null){w.handoffStand=path.getEndNode().asBlockPos();w.handoffRecipient=id;}
            if(path==null||!followRoute(donor,w,path,now,w.urgent.contains(id))){
                // A heard request supplies a destination even when the gate is
                // closed. A useful native partial path may approach its exterior.
                if(NativeIllagers.returnHome(level,donor,v.blockPosition())
                    &&followRoute(donor,w,donor.getNavigation().getPath(),now,w.urgent.contains(id))){
                    w.deliveryRoute=donor.getNavigation().getPath();w.deliveryRecipient=id;w.deliveryProgress.reset();
                    return w.activity="approaching the heard request at the cell entrance";
                }
                w.defer(id,now);return null;
            }
            w.deliveryRoute=donor.getNavigation().getPath();w.deliveryRecipient=id;w.deliveryProgress.reset();
        }
        return w.activity="bringing requested food to a camp captive";
    }
    /** Recognize a small enclosed pen, not an isolated stretch of palisade. */
    static boolean cageLandmark(BlockPos base,java.util.function.Predicate<BlockPos> wall,java.util.function.Predicate<BlockPos> room){
        for(var inward:Direction.Plane.HORIZONTAL){
            var sideways=inward.getClockWise();
            for(int depth=3;depth<=5;depth++){
                if(!wall.test(base.relative(inward,depth)))continue;
                for(int left=1;left<=4;left++)for(int right=1;right<=4;right++){
                    if(left+right<3||left+right>5)continue;boolean enclosed=true;
                    for(int j=1-left;j<right&&enclosed;j++)
                        if(!wall.test(base.relative(sideways,j))||!wall.test(base.relative(inward,depth).relative(sideways,j)))enclosed=false;
                    for(int i=1;i<depth&&enclosed;i++){
                        var row=base.relative(inward,i);
                        if(!wall.test(row.relative(sideways,-left))||!wall.test(row.relative(sideways,right))){enclosed=false;break;}
                        for(int j=1-left;j<right;j++)if(!room.test(row.relative(sideways,j))){enclosed=false;break;}
                    }
                    if(enclosed)return true;
                }
            }
        }
        return false;
    }
    private static boolean cageColumn(ServerLevel level,BlockPos p){
        if(!level.isLoaded(p.above(2)))return false;var block=level.getBlockState(p);
        return block.is(net.minecraft.tags.BlockTags.WOODEN_FENCES)&&level.getBlockState(p.above()).is(block.getBlock())
            &&level.getBlockState(p.above(2)).is(block.getBlock());
    }
    static boolean patrolArrived(Vec3 position,BlockPos target,BlockPos endpoint,boolean pathDone){
        if(position.distanceToSqr(Vec3.atBottomCenterOf(target))<=4)return true;
        // patrolPath deliberately ends at a supported stand up to three blocks
        // from the public landmark. Reaching that actual native endpoint is
        // arrival, even when the landmark itself is occupied or behind a door.
        return pathDone&&endpoint!=null&&endpoint.distSqr(target)<=9
            &&position.distanceToSqr(Vec3.atBottomCenterOf(endpoint))<=1;
    }
    /** Visit public native HOME locations to hear requests; never inspect residents' needs remotely. */
    static String patrol(ServerLevel level,Pillager donor,CreatureAgents.State actor,long now){
        var w=actor.campMeals;
        if(!enabled(level)||!NativeIllagers.member(donor)||IllagerFamilies.juvenile(donor)
            ||donor.isInWater()||donor.isOnFire()||actor.mind.fatigue()>.6
            ||!FoodOffer.mayDeferOwnMeal(actor.mind.foodLevel(),donor.getHealth(),donor.getMaxHealth())
            ||spareSlot(donor.getInventory(),donor.getOffhandItem())<0||w.next(now)!=null)return null;
        if(w.patrolUntil!=0&&now>=w.patrolUntil){
            w.patrolUntil=0;w.patrolTarget=null;w.patrolNext=now+400;donor.getNavigation().stop();return null;
        }
        if(now<w.patrolNext)return null;
        if(w.patrolUntil==0)w.patrolUntil=now+1200;
        var homes=new ArrayList<>(level.getPoiManager().findAll(t->t.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME),
            p->NativeCampWork.local(level,donor,p)&&NativeIllagers.insideCamp(level,donor,p),donor.blockPosition(),64,
            net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY).toList());
        if(now>=w.patrolSenseAt){
            w.patrolSenseAt=now+200;var origin=donor.blockPosition();
            for(var p:BlockPos.betweenClosed(origin.offset(-24,-3,-24),origin.offset(24,2,24))){
                if(!level.isLoaded(p))continue;var block=level.getBlockState(p);
                if(block.is(Blocks.IRON_DOOR)&&block.getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER
                    &&NativeCampWork.local(level,donor,p)&&NativeIllagers.insideCamp(level,donor,p)
                    &&BlockContact.interactionPoint(level,donor,donor.getEyePosition(),p,32)!=null)w.patrolDoors.add(p.immutable());
            }
            // Visible cage walls are public housing landmarks too. Remember one
            // per ground cell; hearing and actual food contact keep their bounds.
            for(var p:BlockPos.betweenClosed(origin.offset(-48,-3,-48),origin.offset(48,3,48))){
                if(!level.isLoaded(p.below(2)))continue;var top=level.getBlockState(p);
                if(!top.is(net.minecraft.tags.BlockTags.WOODEN_FENCES)||!level.getBlockState(p.below()).is(top.getBlock())
                    ||!level.getBlockState(p.below(2)).is(top.getBlock()))continue;
                var base=p.below(2);
                if(NativeCampWork.local(level,donor,base)&&NativeIllagers.insideCamp(level,donor,base)
                    &&!w.patrolCages.containsKey(patrolCell(base))
                    &&BlockContact.interactionPoint(level,donor,donor.getEyePosition(),p,48)!=null
                    &&cageLandmark(base,q->cageColumn(level,q),q->level.isLoaded(q.above())&&level.getBlockState(q).isAir()
                        &&level.getBlockState(q.above()).isAir()&&level.getBlockState(q.below()).isFaceSturdy(level,q.below(),Direction.UP)))
                    w.patrolCages.putIfAbsent(patrolCell(base),base.immutable());
            }
            w.patrolCages.values().removeIf(p->!level.isLoaded(p.above(2))
                ||!level.getBlockState(p.above()).is(net.minecraft.tags.BlockTags.WOODEN_FENCES));
            while(w.patrolCages.size()>24)w.patrolCages.remove(w.patrolCages.firstEntry().getKey());
            w.patrolDoors.removeIf(p->!level.isLoaded(p)||!level.getBlockState(p).is(Blocks.IRON_DOOR));
            while(w.patrolDoors.size()>32)w.patrolDoors.remove(w.patrolDoors.getFirst());
        }
        homes.addAll(w.patrolDoors);homes.addAll(w.patrolCages.values());
        // Native barracks put many HOME markers on roofs. A guard that only
        // visits beds and same-height frontiers can circle above unheard cells
        // forever. Its own witnessed supply landmarks provide real routes back
        // through the lower courtyards, without inspecting anyone else's needs.
        var supplies=new ArrayList<>(NativeIllagers.knownHay(level,donor));supplies.addAll(NativeIllagers.knownTables(level,donor));
        homes.addAll(supplies);
        int patrolCeiling=supplies.stream().mapToInt(BlockPos::getY).min().orElse(Integer.MAX_VALUE-3)+3;
        homes.removeIf(p->p.getY()>patrolCeiling);
        if(w.patrolTarget!=null&&w.patrolTarget.getY()>patrolCeiling)w.patrolTarget=null;
        w.groundVisits.put(patrolCell(donor.blockPosition()),now);
        var patrolPath=donor.getNavigation().getPath();
        if(w.patrolTarget!=null&&patrolArrived(donor.position(),w.patrolTarget,
            patrolPath==null||patrolPath.getEndNode()==null?null:patrolPath.getEndNode().asBlockPos(),patrolPath!=null&&patrolPath.isDone())){
            w.patrolVisits.put(w.patrolTarget,now);w.groundVisits.put(patrolCell(w.patrolTarget),now);w.patrolTarget=null;
        }
        for(var p:homes)if(donor.distanceToSqr(Vec3.atCenterOf(p))<=9){
            w.patrolVisits.put(p,now);if(p.equals(w.patrolTarget))w.patrolTarget=null;
        }
        while(w.patrolVisits.size()>128)w.patrolVisits.remove(w.patrolVisits.firstEntry().getKey());
        if(w.patrolTarget==null){
            if(w.patrolLegs++%2==0)w.patrolTarget=patrolFrontier(level,donor,w,patrolCeiling);
            if(w.patrolTarget==null)w.patrolTarget=homes.stream().filter(p->now-w.patrolVisits.getOrDefault(p,Long.MIN_VALUE/2)>=1200)
                .min(Comparator.<BlockPos>comparingLong(p->w.patrolVisits.getOrDefault(p,Long.MIN_VALUE/2))
                    .thenComparingInt(p->w.patrolCages.containsValue(p)?0:w.patrolDoors.contains(p)?1:2)
                    .thenComparingDouble(p->p.distSqr(donor.blockPosition()))).orElse(null);
            w.patrolPathAt=0;
        }
        if(w.patrolTarget==null){w.patrolUntil=0;w.patrolNext=now+400;return null;}
        if(now>=w.patrolPathAt){
            w.patrolPathAt=now+80;
            var complete=patrolPath(level,donor,w.patrolTarget);
            boolean moved=complete!=null&&donor.getNavigation().moveTo(complete,.8);
            if(!moved)moved=NativeIllagers.returnHome(level,donor,w.patrolTarget);
            var path=donor.getNavigation().getPath();
            w.patrolRoute=Map.of("gameTime",now,"target",w.patrolTarget.toShortString(),"actor",donor.position().toString(),
                "moved",moved,"nodes",path==null?0:path.getNodeCount(),"end",path==null||path.getEndNode()==null?"":path.getEndNode().asBlockPos().toShortString(),"patrolCeiling",patrolCeiling);
            if(!moved){w.patrolVisits.put(w.patrolTarget,now);w.groundVisits.put(patrolCell(w.patrolTarget),now);w.patrolTarget=null;return null;}
        }
        return "visiting camp housing to hear local food requests";
    }
    /** Hear from a supported stand near public housing, including outside a
     * closed cell. A partial route ending on a roof is only the fallback. */
    static net.minecraft.world.level.pathfinder.Path patrolPath(ServerLevel level,Pillager donor,BlockPos target){
        var stands=new ArrayList<BlockPos>();
        for(var p:BlockPos.betweenClosed(target.offset(-3,-1,-3),target.offset(3,1,3))){
            if(!level.isLoaded(p.above())||!NativeCampWork.local(level,donor,p)||!NativeIllagers.insideCamp(level,donor,p)
                ||!level.getFluidState(p).isEmpty()||!level.getFluidState(p.below()).isEmpty())continue;
            double floor=net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level,p);
            var feet=new Vec3(p.getX()+.5,floor,p.getZ()+.5);
            var body=donor.getBoundingBox().move(feet.subtract(donor.position()));
            if(feet.distanceToSqr(Vec3.atCenterOf(target))<=9&&level.noCollision(donor,body)&&!level.noCollision(donor,body.move(0,-.03,0)))stands.add(p.immutable());
        }
        return CampBeds.pathToStands(level,donor,stands);
    }
    private static BlockPos patrolCell(BlockPos p){return new BlockPos(Math.floorDiv(p.getX(),12),Math.floorDiv(p.getY(),4),Math.floorDiv(p.getZ(),12));}
    /** Observe a new corridor or courtyard as well as known beds; cell occupants may sleep elsewhere. */
    private static BlockPos patrolFrontier(ServerLevel level,Pillager donor,State w,int ceiling){
        var origin=donor.blockPosition();var options=new ArrayList<BlockPos>();
        while(w.groundVisits.size()>128)w.groundVisits.remove(w.groundVisits.firstEntry().getKey());
        for(int dx=-24;dx<=24;dx+=6)for(int dz=-24;dz<=24;dz+=6)for(int dy=-2;dy<=2;dy++){
            if(dx*dx+dz*dz<36||dx*dx+dz*dz>28*28)continue;var p=origin.offset(dx,dy,dz);
            if(p.getY()>ceiling||!NativeCampWork.local(level,donor,p)||!NativeIllagers.insideCamp(level,donor,p)||!level.isLoaded(p.above())
                ||!level.getBlockState(p).getCollisionShape(level,p).isEmpty()
                ||!level.getBlockState(p.above()).getCollisionShape(level,p.above()).isEmpty()
                ||!level.getFluidState(p).isEmpty()||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP)
                ||!NativeCampWork.visible(level,donor,p))continue;
            options.add(p);
        }
        var home=WarCombat.home(donor);int side=donor.getUUID().getMostSignificantBits()<0?1:-1;
        return options.stream().min(Comparator.<BlockPos>comparingLong(p->w.groundVisits.getOrDefault(patrolCell(p),Long.MIN_VALUE/2))
            .thenComparingInt(p->home!=null&&(p.getX()-home.x())*side>=0?0:1)
            .thenComparingDouble(p->-p.distSqr(origin))).orElse(null);
    }
}
