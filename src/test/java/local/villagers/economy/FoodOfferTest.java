package local.villagers.economy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodOfferTest {
    @Test void recoveryOfferUsesRecipientsOwnNeedAndRetainsOwnedMealPriority(){
        assertTrue(FoodOffer.wantsMeal(15,1,20,false,false));
        assertFalse(FoodOffer.wantsMeal(15,20,20,false,false));
        assertFalse(FoodOffer.wantsMeal(15,1,20,false,true));
        assertFalse(FoodOffer.wantsMeal(18,1,20,false,false));
    }
    @Test void urgentOrInjuredDonorNeverDelaysItsOwnMealForDelivery() {
        for(int food=0;food<=12;food++)assertFalse(FoodOffer.mayDeferOwnMeal(food,20,20));
        assertFalse(FoodOffer.mayDeferOwnMeal(15,19,20));
        assertFalse(FoodOffer.mayDeferOwnMeal(15,Float.NaN,20));
        assertTrue(FoodOffer.mayDeferOwnMeal(13,20,20));
    }
    @Test void offeredFoodRequiresTheRecipientsOwnUnmetNeedNotTheSendersAssumption() {
        assertTrue(FoodOffer.wantsFood(.5,false,false));
        assertTrue(FoodOffer.wantsFood(.35,true,false));
        assertFalse(FoodOffer.wantsFood(.49,false,false));
        assertFalse(FoodOffer.wantsFood(1,false,true));
        assertFalse(FoodOffer.wantsFood(Double.NaN,false,false));
    }
}
