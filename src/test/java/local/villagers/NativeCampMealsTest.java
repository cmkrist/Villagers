package local.villagers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class NativeCampMealsTest {


    @Test void aReachedBlockedHandoffTriesAnotherStandWithoutForgettingTheCaller(){
        var work=new NativeCampMeals.State();var caller=java.util.UUID.randomUUID();var other=java.util.UUID.randomUUID();
        var blocked=new net.minecraft.core.BlockPos(-633,65,-610);var adjacent=blocked.north();
        work.hear(caller,6000,true,true);assertTrue(work.pursue(caller,0));
        work.missStand(caller,blocked,100);
        assertFalse(work.retryStand(caller,blocked,101));assertTrue(work.retryStand(caller,adjacent,101));
        assertTrue(work.retryStand(other,blocked,101));assertEquals(caller,work.next(101));
        assertTrue(work.retryStand(caller,blocked,500));
        for(int i=0;i<100;i++)work.missStand(caller,blocked.offset(i,0,0),200);
        assertEquals(64,work.missedStands.size());assertEquals(0,work.delivered);
    }

    @Test void aReachedCriticalPatientCanAskForTheCarriedBreadNeededToRecover(){
        var receiver=new SimpleContainer(8);receiver.setItem(0,ration(Items.POTATO,1,1));
        var carried=ration(Items.BREAD,3,5);
        assertEquals(3,NativeCampMeals.contactBundleCount(receiver,carried,true,7,1,20));
        assertEquals(3,carried.getCount());assertEquals(1,receiver.countItem(Items.POTATO));
        assertEquals(2,NativeCampMeals.contactBundleCount(receiver,carried,true,20,20,20));
    }
    @Test void aReachedRootBundleStillIncludesAnUnspentPlantingRoot(){
        var empty=new SimpleContainer(8);var pair=ration(Items.POTATO,2,1);
        assertEquals(2,NativeCampMeals.contactBundleCount(empty,pair,true,0,1,20));
        var impossible=ration(Items.POTATO,1,1);
        int asked=NativeCampMeals.contactBundleCount(empty,impossible,true,0,1,20);
        assertEquals(2,asked);assertNull(NativeCampMeals.bundlePlan(empty,impossible,asked));
        assertTrue(empty.isEmpty());assertEquals(1,impossible.getCount());
    }

    @Test void equalSpokenAlarmsCannotKeepReversingAnAgreedDelivery(){
        var work=new NativeCampMeals.State();var first=java.util.UUID.randomUUID();var equal=java.util.UUID.randomUUID();var stronger=java.util.UUID.randomUUID();
        work.calls.hear(first,new net.minecraft.core.BlockPos(40,64,0),0,12);work.hear(first,6000,true,true);
        assertTrue(work.pursue(first,0));
        work.calls.hear(equal,new net.minecraft.core.BlockPos(-40,64,0),10,12);work.hear(equal,6000,true,true);
        assertEquals(first,work.next(10));assertEquals(1200,work.pursuitUntil);
        work.calls.hear(stronger,new net.minecraft.core.BlockPos(4,64,0),20,14);work.hear(stronger,6000,true,true);
        assertEquals(stronger,work.next(20));
    }

    @Test void anAdditionalOccupiedCellDoesNotLoseItsCriticalSpokenRequest(){
        var work=new NativeCampMeals.State();var heard=new java.util.ArrayList<java.util.UUID>();
        for(int i=0;i<5;i++){
            var caller=java.util.UUID.randomUUID();heard.add(caller);
            work.calls.hear(caller,new net.minecraft.core.BlockPos(i,64,0),0,14);
            work.hear(caller,6000,true,true);
        }
        assertEquals(5,work.heard.size());assertTrue(work.heard.keySet().containsAll(heard));
        var selected=work.next(0);assertNotNull(selected);work.served(selected);
        assertNotEquals(selected,work.next(40));
    }

    @Test void aThinReserveRequestsCriticalHelpBeforeItIsCompletelyEmpty(){
        assertEquals(14,NativeCampCalls.volume(2,8,20));
        assertEquals(14,NativeCampCalls.volume(0,8,20));
        assertEquals(14,NativeCampCalls.volume(2,5,20));
        assertEquals(10,NativeCampCalls.volume(3,8,20));
        assertEquals(10,NativeCampCalls.volume(2,13,20));
        assertEquals(8,NativeCampCalls.volume(2,20,20));
        var calls=new NativeCampCalls.State();
        var recovering=java.util.UUID.randomUUID();var thin=java.util.UUID.randomUUID();
        calls.hear(recovering,new net.minecraft.core.BlockPos(1,64,0),0,NativeCampCalls.volume(3,8,20));
        calls.hear(thin,new net.minecraft.core.BlockPos(40,64,0),0,NativeCampCalls.volume(2,8,20));
        assertEquals(thin,calls.select(new net.minecraft.world.phys.Vec3(0,64,0),0));
    }
    @Test void understandingACriticalCallKeepsItsPriorityBeforeSelectingADelivery(){
        var work=new NativeCampMeals.State();var caller=java.util.UUID.randomUUID();
        work.calls.hear(caller,new net.minecraft.core.BlockPos(8,64,0),0,14);
        work.hear(caller,6000,true,true);
        assertFalse(work.calls.heard.containsKey(caller));assertNull(work.pursuing);
        assertTrue(NativeCampMeals.criticalVoice(work,100));
        work.served(caller);assertFalse(NativeCampMeals.criticalVoice(work,101));
        work.hear(caller,6000,true,true);assertFalse(NativeCampMeals.criticalVoice(work,6000));
    }
    @Test void aRootDeliveryPaysForBothAPlantingReserveAndAnEdibleRation(){
        var bag=new SimpleContainer(5);bag.setItem(0,ration(Items.POTATO,1,1));
        assertFalse(NativeCampMeals.usableDeliveryRation(bag,ItemStack.EMPTY));
        bag.getItem(0).grow(1);assertTrue(NativeCampMeals.usableDeliveryRation(bag,ItemStack.EMPTY));
        var receiver=new SimpleContainer(2);var after=NativeCampMeals.bundlePlan(receiver,bag.getItem(0),2);
        assertNotNull(after);assertTrue(receiver.isEmpty());assertEquals(2,bag.countItem(Items.POTATO));
        assertEquals(0,FoodInventory.anySlot(after,true));after.removeItem(0,1);
        assertEquals(1,after.countItem(Items.POTATO));assertEquals(-1,FoodInventory.anySlot(after,true));
    }
    @Test void aBundleThatOnlyPartlyFitsDoesNotChangeEitherInventory(){
        var receiver=new SimpleContainer(1);receiver.setItem(0,ration(Items.POTATO,63,1));
        var source=ration(Items.POTATO,2,1);
        assertNull(NativeCampMeals.bundlePlan(receiver,source,2));
        assertEquals(63,receiver.countItem(Items.POTATO));assertEquals(2,source.getCount());
    }
    @Test void aLongMealTripDoesNotSprintAwayItsLastActualRation(){
        var bag=new SimpleContainer(5);var held=ration(Items.BREAD,1,5);
        assertFalse(NativeCampMeals.maySprintWithRations(20,bag,held));
        bag.setItem(0,ration(Items.BREAD,1,5));assertFalse(NativeCampMeals.maySprintWithRations(20,bag,held));
        bag.setItem(0,ration(Items.BREAD,2,5));assertTrue(NativeCampMeals.maySprintWithRations(20,bag,held));
        assertFalse(NativeCampMeals.maySprintWithRations(6,bag,held));
        held.shrink(1);assertFalse(NativeCampMeals.maySprintWithRations(20,bag,held));
        assertEquals(2,bag.countItem(Items.BREAD));
    }
    @Test void oneRawRootDoesNotSilenceACriticallyInjuredCallersAlarm(){
        assertEquals(14,NativeCampCalls.volume(1,1,20));
        assertEquals(14,NativeCampCalls.volume(2,5,20));
        assertEquals(14,NativeCampCalls.volume(0,10,20));
        assertEquals(14,NativeCampCalls.volume(2,10,20));
        assertEquals(10,NativeCampCalls.volume(3,10,20));
        assertEquals(12,NativeCampCalls.volume(0,11,20));
        assertEquals(8,NativeCampCalls.volume(0,20,20));
        assertEquals(10,NativeCampCalls.volume(3,5,20));
        assertEquals(8,NativeCampCalls.volume(1,20,20));
    }
    @Test void anInjuredVoiceIsPublicAndDoesNotCreateUnderstoodWords(){
        assertEquals(6,NativeCampCalls.volume(6,20,20));assertEquals(8,NativeCampCalls.volume(0,20,20));
        assertEquals(12,NativeCampCalls.volume(0,19,20));assertEquals(10,NativeCampCalls.volume(5,13,20));
        var work=new NativeCampMeals.State();var id=java.util.UUID.randomUUID();
        work.calls.hear(id,new net.minecraft.core.BlockPos(0,64,0),0,10);
        assertTrue(NativeCampCalls.heardInjury(work.calls,0));assertNull(work.next(0));assertTrue(work.injured.isEmpty());
        work.calls.reject(id,10);assertFalse(NativeCampCalls.heardInjury(work.calls,11));assertTrue(NativeCampCalls.heardInjury(work.calls,410));
        assertFalse(NativeCampCalls.heardInjury(work.calls,1200));
        assertFalse(NativeCampCalls.louderInjury(work.calls,410,10));
        work.calls.hear(id,new net.minecraft.core.BlockPos(0,64,0),500,12);
        assertTrue(NativeCampCalls.louderInjury(work.calls,500,10));
    }
    @Test void aCriticalDeliveryMayRunWhileTheDonorCanDeferItsOwnMeal(){
        var bag=new SimpleContainer(5);bag.setItem(0,ration(Items.POTATO,1,1));
        assertFalse(NativeCampMeals.mayRunDelivery(17,bag,ItemStack.EMPTY,false));
        assertTrue(NativeCampMeals.mayRunDelivery(17,bag,ItemStack.EMPTY,true));
        assertTrue(NativeCampMeals.mayRunDelivery(16,bag,ItemStack.EMPTY,true));
        assertTrue(NativeCampMeals.mayRunDelivery(14,bag,ItemStack.EMPTY,true));
        assertFalse(NativeCampMeals.mayRunDelivery(13,bag,ItemStack.EMPTY,true));
        bag.clearContent();assertFalse(NativeCampMeals.mayRunDelivery(20,bag,ItemStack.EMPTY,true));
    }
    @Test void understoodHurtWordsPreemptHungerAndSurviveAFullOrdinaryQueue(){
        var work=new NativeCampMeals.State();var hurt=java.util.UUID.randomUUID();
        for(int i=0;i<NativeCampMeals.REQUEST_CAPACITY;i++)work.hear(java.util.UUID.randomUUID(),6000,true);
        work.hear(hurt,6000,true,true);assertEquals(NativeCampMeals.REQUEST_CAPACITY,work.heard.size());assertEquals(hurt,work.next(0));
        var newcomer=java.util.UUID.randomUUID();work.calls.hear(newcomer,new net.minecraft.core.BlockPos(0,64,0),0,8);
        work.hear(newcomer,6000,true);assertEquals(hurt,work.next(0));assertTrue(work.injured.contains(hurt));
        work.served(hurt);assertFalse(work.injured.contains(hurt));assertNotEquals(hurt,work.next(0));
    }
    @Test void newlyUnderstoodInjuryGetsOneRetryButRepeatedWordsCannotRenewAStalledTrip(){
        var work=new NativeCampMeals.State();var hurt=java.util.UUID.randomUUID();
        work.hear(hurt,6000,true);work.defer(hurt,100);assertNull(work.next(101));
        work.hear(hurt,6100,true,true);assertEquals(hurt,work.next(101));
        work.defer(hurt,200);work.hear(hurt,6200,true,true);assertNull(work.next(201));
        assertEquals(hurt,work.next(800));assertNull(work.next(6200));assertTrue(work.injured.isEmpty());
    }
    @Test void aLouderHeardStarvationCallCanEnterAQueueOfRecoveryRequests(){
        var work=new NativeCampMeals.State();for(int i=0;i<NativeCampMeals.REQUEST_CAPACITY;i++)work.hear(java.util.UUID.randomUUID(),6000,true,true);
        var caller=java.util.UUID.randomUUID();work.calls.hear(caller,new net.minecraft.core.BlockPos(0,64,0),0,12);
        work.hear(caller,6000,true,true);assertEquals(NativeCampMeals.REQUEST_CAPACITY,work.heard.size());assertEquals(caller,work.next(0));
        assertEquals(12,work.voices.get(caller));
    }
    @Test void anEqualAlarmCannotTurnAListenerBackHalfwayThroughARealDetour(){
        var calls=new NativeCampCalls.State();var west=java.util.UUID.randomUUID();var east=java.util.UUID.randomUUID();
        calls.hear(west,new net.minecraft.core.BlockPos(0,64,0),0,8);calls.hear(east,new net.minecraft.core.BlockPos(20,64,0),0,8);
        assertEquals(west,calls.select(new net.minecraft.world.phys.Vec3(1,64,0),0));
        calls.hear(west,new net.minecraft.core.BlockPos(0,64,0),1000,8);calls.hear(east,new net.minecraft.core.BlockPos(20,64,0),1000,8);
        assertEquals(east,calls.nearest(new net.minecraft.world.phys.Vec3(19,64,0),1000));
        assertEquals(west,calls.select(new net.minecraft.world.phys.Vec3(19,64,0),1000));
        assertEquals(east,calls.select(new net.minecraft.world.phys.Vec3(19,64,0),1200));
        assertEquals(1600,calls.retryAt.get(west));
        calls.hear(west,new net.minecraft.core.BlockPos(0,64,0),1300,8);
        assertEquals(east,calls.select(new net.minecraft.world.phys.Vec3(1,64,0),1300));
    }
    @Test void staggeredEqualInjuryCallsChooseAMuchNearerVoiceBeforeTheDetour(){
        var calls=new NativeCampCalls.State();var far=java.util.UUID.randomUUID();var near=java.util.UUID.randomUUID();
        var listener=new net.minecraft.world.phys.Vec3(0,64,0);
        calls.hear(far,new net.minecraft.core.BlockPos(64,64,0),0,12);
        assertEquals(far,calls.select(listener,0));
        calls.hear(near,new net.minecraft.core.BlockPos(18,64,0),10,12);
        assertEquals(near,calls.select(listener,10));
        calls.hear(far,new net.minecraft.core.BlockPos(64,64,0),100,12);
        assertEquals(near,calls.select(new net.minecraft.world.phys.Vec3(63,64,0),100));
    }
    @Test void anActuallyLouderNewVoiceCanInterruptAnOrdinaryInvestigation(){
        var calls=new NativeCampCalls.State();var ordinary=java.util.UUID.randomUUID();var alarm=java.util.UUID.randomUUID();
        calls.hear(ordinary,new net.minecraft.core.BlockPos(0,64,0),0,6);
        assertEquals(ordinary,calls.select(new net.minecraft.world.phys.Vec3(1,64,0),0));
        calls.hear(alarm,new net.minecraft.core.BlockPos(40,64,0),10,8);
        assertEquals(alarm,calls.select(new net.minecraft.world.phys.Vec3(1,64,0),10));
    }
    @Test void aRealMealLetsAnotherHungryCallerGoNextWithoutDiscardingTheRationRequest(){
        var work=new NativeCampMeals.State();var first=java.util.UUID.randomUUID();var second=java.util.UUID.randomUUID();
        work.hear(first,6000,true);work.hear(second,6000,true);assertEquals(first,work.next(0));
        work.served(first);assertEquals(second,work.next(40));assertTrue(work.heard.containsKey(first));
        work.served(second);assertEquals(first,work.next(80));assertEquals(2,work.heard.size());
        assertNull(work.next(6000),"A handoff must not silently extend a spoken request's lifetime");
    }
    @Test void anotherExplicitUrgentCallCanReprioritizeAPreviouslyServedRecipient(){
        var work=new NativeCampMeals.State();var first=java.util.UUID.randomUUID();var second=java.util.UUID.randomUUID();
        work.hear(first,6000,true);work.hear(second,6000,false);work.served(first);assertEquals(second,work.next(40));
        work.hear(first,6200,true);assertEquals(first,work.next(200));
    }
    @BeforeAll static void items(){
        BreadCraftingTest.initializeItemRegistries();
        for(var item:java.util.List.of(Items.APPLE,Items.CARROT,Items.POTATO,Items.BEETROOT))BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getKey(item)).orElseThrow().bindComponents(DataComponents.COMMON_ITEM_COMPONENTS);
    }
    private static ItemStack ration(Item item,int count,int nutrition){
        var stack=new ItemStack(item,count);stack.set(DataComponents.FOOD,new FoodProperties.Builder().nutrition(nutrition).saturationModifier(.6f).build());return stack;
    }
    @Test void aDonorKeepsTwoRealMealsAndNeverCountsWheatAsFood(){
        var bag=new SimpleContainer(ration(Items.BREAD,2,5),new ItemStack(Items.WHEAT,64));
        assertEquals(10,NativeCampMeals.nutrition(bag));assertEquals(-1,NativeCampMeals.spareSlot(bag));
        bag.setItem(0,ration(Items.BREAD,3,5));assertEquals(0,NativeCampMeals.spareSlot(bag));
        assertEquals(3,bag.countItem(Items.BREAD));assertEquals(64,bag.countItem(Items.WHEAT));
    }
    @Test void aMixedReserveUsesActualNutritionAndOnlyOffersARealItem(){
        var bag=new SimpleContainer(ration(Items.BREAD,1,5),ration(Items.APPLE,3,4));
        assertEquals(17,NativeCampMeals.nutrition(bag));assertEquals(0,NativeCampMeals.spareSlot(bag));
        bag.setItem(1,ration(Items.APPLE,2,4));assertEquals(-1,NativeCampMeals.spareSlot(bag));
    }
    @Test void aHeldRealMealCountsTowardTheSameTenNutritionReserve(){
        var bag=new SimpleContainer(ration(Items.BREAD,2,5));
        assertEquals(0,NativeCampMeals.spareSlot(bag,ration(Items.BREAD,1,5)));
        bag.setItem(0,ration(Items.BREAD,1,5));assertEquals(-1,NativeCampMeals.spareSlot(bag,ration(Items.BREAD,1,5)));
        assertEquals(-1,NativeCampMeals.spareSlot(bag,new ItemStack(Items.WHEAT,64)));
    }
    @Test void spokenRequestsHaveABoundedHearingDistanceWithoutAuthorizingARemoteGift(){
        assertTrue(NativeCampMeals.audible(144,12));assertFalse(NativeCampMeals.audible(Math.nextUp(144d),12));
        assertFalse(NativeCampMeals.audible(257,48));assertFalse(NativeCampMeals.audible(Double.NaN,12));
        assertFalse(NativeCampMeals.audible(-1,12));assertFalse(NativeCampMeals.audible(1,0));
    }
    @Test void anExplicitlyUnderstoodUrgentRequestPrecedesOrdinaryRequestsAndExpires(){
        var work=new NativeCampMeals.State();var a=java.util.UUID.randomUUID();var b=java.util.UUID.randomUUID();
        work.hear(a,100,false);work.hear(b,50,true);assertEquals(b,work.next(49));assertEquals(a,work.next(50));
        assertNull(work.next(100));
    }
    @Test void anUrgentCallCanReplaceARegularCallWithoutGrowingTheBoundedQueue(){
        var work=new NativeCampMeals.State();for(int i=0;i<NativeCampMeals.REQUEST_CAPACITY;i++)work.hear(java.util.UUID.randomUUID(),100,false);
        var urgent=java.util.UUID.randomUUID();work.hear(urgent,100,true);
        assertEquals(NativeCampMeals.REQUEST_CAPACITY,work.heard.size());assertEquals(urgent,work.next(0));
    }
    @Test void repeatedCallsCannotMonopolizeDeliveryAfterADeadEnd(){
        var work=new NativeCampMeals.State();var blocked=java.util.UUID.randomUUID();var other=java.util.UUID.randomUUID();
        work.hear(blocked,6000,true);work.hear(other,6000,true);
        assertEquals(blocked,work.next(0));assertTrue(work.pursue(blocked,0));
        work.hear(blocked,7000,true);assertTrue(work.pursue(blocked,1199));
        assertFalse(work.pursue(blocked,1200));assertEquals(other,work.next(1200));
        work.hear(blocked,8000,true);assertEquals(other,work.next(1799));
        assertEquals(blocked,work.next(1800));assertTrue(work.pursue(blocked,1800));
        assertEquals(2,work.heard.size());assertEquals(1,work.rejectedPaths);
        assertNull(work.next(8000));assertTrue(work.retryAt.isEmpty());
    }
    @Test void aReceivedMealDoesNotEndAHeardRationRequestUntilTwoRealMealsCanBeKept(){
        var bag=new SimpleContainer(ration(Items.BREAD,1,5),new ItemStack(Items.WHEAT,9));
        assertTrue(NativeCampMeals.wantsRations(bag,true));
        bag.setItem(0,ration(Items.BREAD,2,5));assertFalse(NativeCampMeals.wantsRations(bag,true));
        assertEquals(2,bag.countItem(Items.BREAD));assertEquals(9,bag.countItem(Items.WHEAT));
    }
    @Test void aPlantingRootCannotBeMistakenForTheRecipientsReserveMeal(){
        var bag=new SimpleContainer(ration(Items.CARROT,1,3),ration(Items.BREAD,1,5));
        assertEquals(5,NativeCampMeals.nutrition(bag,true));assertTrue(NativeCampMeals.wantsRations(bag,true));
    }
    @Test void reachingTheActualThreeBlockPatrolStandFinishesTheVisit(){
        var target=new net.minecraft.core.BlockPos(-686,63,-643);var end=new net.minecraft.core.BlockPos(-684,63,-641);
        var body=new net.minecraft.world.phys.Vec3(-683.27,63,-640.47);
        assertTrue(body.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(target))>9);
        assertTrue(NativeCampMeals.patrolArrived(body,target,end,true));
        assertFalse(NativeCampMeals.patrolArrived(body,target,end,false));
    }
    @Test void anUnfinishedOrDistantPartialPatrolRouteCannotCountAsArrival(){
        var target=new net.minecraft.core.BlockPos(10,64,0);var partial=new net.minecraft.core.BlockPos(3,64,0);
        assertFalse(NativeCampMeals.patrolArrived(net.minecraft.world.phys.Vec3.atBottomCenterOf(partial),target,partial,true));
        assertFalse(NativeCampMeals.patrolArrived(new net.minecraft.world.phys.Vec3(20,64,0),target,new net.minecraft.core.BlockPos(8,64,0),true));
        assertTrue(NativeCampMeals.patrolArrived(new net.minecraft.world.phys.Vec3(9.5,64,.5),target,null,false));
    }

    @Test void aSmallPenIsAPublicCageLandmarkWithoutReadingItsOccupants(){
        var base=new net.minecraft.core.BlockPos(0,64,1);
        java.util.function.Predicate<net.minecraft.core.BlockPos> wall=p->p.getY()==64&&(
            (p.getX()==0||p.getX()==3)&&p.getZ()>0&&p.getZ()<3
            ||(p.getZ()==0||p.getZ()==3)&&p.getX()>0&&p.getX()<3);
        java.util.function.Predicate<net.minecraft.core.BlockPos> room=p->p.getY()==64&&p.getX()>0&&p.getX()<3&&p.getZ()>0&&p.getZ()<3;
        assertTrue(NativeCampMeals.cageLandmark(base,wall,room));
    }
    @Test void aLongPalisadeDoesNotDrawTheHousingPatrolAwayFromCaptives(){
        assertFalse(NativeCampMeals.cageLandmark(new net.minecraft.core.BlockPos(0,64,0),
            p->p.getY()==64&&p.getX()==0,p->p.getY()==64&&p.getX()>0));
    }

    @Test void aLoudCallIsLimitedByActualSoundRangeAndDoesNotCreateAFoodRequest(){
        assertEquals(72,NativeCampCalls.radius(12,96));assertEquals(64,NativeCampCalls.radius(48,64));
        assertEquals(0,NativeCampCalls.radius(Double.NaN,96));assertEquals(0,NativeCampCalls.radius(0,96));
        var meals=new NativeCampMeals.State();var caller=java.util.UUID.randomUUID();
        meals.calls.hear(caller,new net.minecraft.core.BlockPos(1,64,2),100);
        assertEquals(1,meals.calls.heard.size());assertNull(meals.next(100));assertEquals(0,meals.delivered);
        assertEquals(1300,meals.calls.heard.get(caller).expires());
    }
    @Test void anInjuredCallerCanSeekRecoveryCaloriesAfterStarvationHasEnded(){
        assertTrue(NativeCampCalls.needsCall(15,6,20));
        assertTrue(NativeCampCalls.needsCall(17,19,20));
        assertFalse(NativeCampCalls.needsCall(18,6,20));
        assertFalse(NativeCampCalls.needsCall(15,20,20));
        assertTrue(NativeCampCalls.needsCall(6,20,20));
        assertFalse(NativeCampCalls.needsCall(-1,6,20));
    }

    @Test void aCriticalStarvationVoiceInterruptsANewlyInjuredCall(){
        var calls=new NativeCampCalls.State();var injured=java.util.UUID.randomUUID();var critical=java.util.UUID.randomUUID();
        var listener=new net.minecraft.world.phys.Vec3(0,64,0);
        calls.hear(injured,new net.minecraft.core.BlockPos(8,64,0),0,NativeCampCalls.volume(0,17,20));
        assertEquals(injured,calls.select(listener,0));
        calls.hear(critical,new net.minecraft.core.BlockPos(18,64,0),10,NativeCampCalls.volume(0,4,20));
        assertEquals(critical,calls.select(listener,10));
        assertEquals(NativeCampCalls.INJURED_VOLUME,NativeCampCalls.volume(12,4,20));
    }
    @Test void laterOrdinaryCallsCannotEvictACriticalVoiceBeforeProvisioningFinishes(){
        var calls=new NativeCampCalls.State();var critical=java.util.UUID.randomUUID();
        calls.hear(critical,new net.minecraft.core.BlockPos(18,64,0),0,NativeCampCalls.volume(0,4,20));
        for(int i=0;i<12;i++)calls.hear(java.util.UUID.randomUUID(),new net.minecraft.core.BlockPos(i,64,0),i+1,6);
        assertEquals(8,calls.heard.size());assertTrue(calls.heard.containsKey(critical));
        assertEquals(critical,calls.select(new net.minecraft.world.phys.Vec3(0,64,0),20));
    }

    @Test void understandingNearbyWordsReplacesOnlyThatCallersDistantSoundCue(){
        var meals=new NativeCampMeals.State();var first=java.util.UUID.randomUUID();var other=java.util.UUID.randomUUID();
        meals.calls.hear(first,new net.minecraft.core.BlockPos(1,64,2),0);
        meals.calls.hear(other,new net.minecraft.core.BlockPos(20,64,2),0);
        meals.hear(first,6000,true);
        assertFalse(meals.calls.heard.containsKey(first));assertTrue(meals.calls.heard.containsKey(other));
        assertEquals(first,meals.next(0));assertEquals(0,meals.delivered);
    }


    @Test void aNearbyHeardCallDoesNotWaitBehindAnEarlierDistantSound(){
        var calls=new NativeCampCalls.State();var far=java.util.UUID.randomUUID();var near=java.util.UUID.randomUUID();
        calls.hear(far,new net.minecraft.core.BlockPos(50,64,0),0);
        calls.hear(near,new net.minecraft.core.BlockPos(8,64,0),0);
        assertEquals(near,calls.nearest(new net.minecraft.world.phys.Vec3(.5,64,.5),10));
        assertEquals(far,calls.nearest(new net.minecraft.world.phys.Vec3(48.5,64,.5),10));
        assertNull(calls.nearest(new net.minecraft.world.phys.Vec3(.5,64,.5),1200));
    }
    @Test void aLouderAlarmGainsAttentionButOnlyUnderstoodWordsQueueItsMeal(){
        var meals=new NativeCampMeals.State();var regular=java.util.UUID.randomUUID();var alarm=java.util.UUID.randomUUID();
        meals.hear(regular,6000,true);
        meals.calls.hear(regular,new net.minecraft.core.BlockPos(2,64,0),0);
        meals.calls.hear(alarm,new net.minecraft.core.BlockPos(50,64,0),0,8);
        assertEquals(alarm,meals.calls.nearest(new net.minecraft.world.phys.Vec3(.5,64,.5),10));
        assertEquals(regular,meals.next(10));assertEquals(0,meals.delivered);
        meals.hear(alarm,6200,true);
        assertEquals(alarm,meals.next(20));assertFalse(meals.calls.heard.containsKey(alarm));
        assertEquals(0,meals.delivered);
    }
}
