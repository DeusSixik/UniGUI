package dev.sixik.unigui.demo;

import dev.sixik.unigui.demo.ui.data.SolarNavigationProceduralMap;
import dev.sixik.unigui.demo.ui.data.SolarNavigationQuestMarker;
import dev.sixik.unigui.demo.ui.data.SolarNavigationShipState;
import dev.sixik.unigui.demo.ui.data.SolarNavigationStationInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolarNavigationProceduralMapTest {
    @Test
    void nearbyStations_sameSeedAndState_areDeterministicAndLimited() {
        SolarNavigationShipState ship = new SolarNavigationShipState(12.0f, -18.0f, 0.0f, 0.0f, 0.0f);
        List<SolarNavigationQuestMarker> markers = List.of(
                new SolarNavigationQuestMarker("quest", "Quest Station", 30.0f, -4.0f, 24.0f, 0xFFFFCC66, 77L));

        List<SolarNavigationStationInfo> first = SolarNavigationProceduralMap.nearbyStations(
                0x5EED_51A7L, ship, markers, 1400.0f, 8);
        List<SolarNavigationStationInfo> second = SolarNavigationProceduralMap.nearbyStations(
                0x5EED_51A7L, ship, markers, 1400.0f, 8);

        assertEquals(first, second);
        assertTrue(first.size() <= 8);
        assertTrue(first.stream().anyMatch(SolarNavigationStationInfo::quest));
    }

    @Test
    void sectorSeed_differentSectors_producesDifferentStableSeeds() {
        long first = SolarNavigationProceduralMap.sectorSeed(42L, 0, 0, 7L);
        long second = SolarNavigationProceduralMap.sectorSeed(42L, 1, 0, 7L);

        assertTrue(first != second);
        assertEquals(first, SolarNavigationProceduralMap.sectorSeed(42L, 0, 0, 7L));
    }
}
