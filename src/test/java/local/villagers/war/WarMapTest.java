package local.villagers.war;

import java.util.EnumMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WarMapTest {
    @Test void tickingRegionCoversEveryTownHomeAndItsFullRoamingStep() {
        int reach = WarMap.TOWN_RADIUS + WarMap.HOME_ROAM_RADIUS + WarMap.ROAM_STEP;
        int min = WarMap.SIMULATION_MIN_CHUNK * 16, max = (WarMap.SIMULATION_MAX_CHUNK + 1) * 16;
        for (var town : WarMap.SETTLEMENTS) for (int offset : new int[]{-reach, reach}) {
            assertTrue(town.x() + offset >= min && town.x() + offset < max);
            assertTrue(town.z() + offset >= min && town.z() + offset < max);
        }
        assertTrue(144 < max, "the previously frozen east boundary must tick");
    }
    @Test void exactChunkAlignedQuartersAndBorder() {
        var counts = new EnumMap<WarMap.Region,Integer>(WarMap.Region.class);
        for(int x=-32;x<32;x++) for(int z=-32;z<32;z++) {
            var region=WarMap.region(x*16,z*16);
            counts.merge(region,1,Integer::sum);
            assertEquals(region,WarMap.region(x*16+15,z*16+15));
            assertTrue(WarMap.contains(x*16,z*16));
        }
        assertEquals(4,counts.size());
        counts.values().forEach(count->assertEquals(32*32,count));
        assertFalse(WarMap.contains(512,0)); assertFalse(WarMap.contains(0,-513));
    }
    @Test void eachBiomeHasExactlyOneSettlementAndBothFactionsHaveTwo() {
        assertEquals(4,WarMap.SETTLEMENTS.stream().map(WarMap.Settlement::region).distinct().count());
        for(var faction:new WarMap.Faction[]{WarMap.Faction.VILLAGERS,WarMap.Faction.ILLAGERS})
            assertEquals(2,WarMap.SETTLEMENTS.stream().filter(s->s.faction()==faction).count());
        for(var town:WarMap.SETTLEMENTS) {
            assertEquals(town.region(),WarMap.region(town.x(),town.z()));
            int height=WarMap.height(town.x(),town.z());
            for(int x=-29;x<=29;x++) for(int z=-29;z<=29;z++)
                assertEquals(height,WarMap.height(town.x()+x,town.z()+z));
        }
    }
    @Test void terrainHasMountainsAndWalkableGradients() {
        int maxMountain=0,maxOther=0;
        for(int x=-512;x<512;x++) for(int z=-512;z<512;z++) {
            int height=WarMap.height(x,z);
            if(WarMap.region(x,z)==WarMap.Region.SNOWY_MOUNTAINS) maxMountain=Math.max(maxMountain,height);
            else maxOther=Math.max(maxOther,height);
            assertTrue(Math.abs(height-WarMap.height(x+1,z))<=2,"east gradient");
            assertTrue(Math.abs(height-WarMap.height(x,z+1))<=2,"south gradient");
        }
        assertTrue(maxMountain>130); assertTrue(maxOther<90);
    }
}
