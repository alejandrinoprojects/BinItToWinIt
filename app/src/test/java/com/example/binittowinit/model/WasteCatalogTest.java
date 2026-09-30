package com.example.binittowinit.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;
import java.util.Random;

public class WasteCatalogTest {

    @Test
    public void testEqualVarietyAcrossTrashCategories() {
        List<WasteItem> bio = WasteCatalog.getItemsByCategory(WasteCategory.BIODEGRADABLE);
        List<WasteItem> rec = WasteCatalog.getItemsByCategory(WasteCategory.RECYCLABLE);
        List<WasteItem> nonBio = WasteCatalog.getItemsByCategory(WasteCategory.NON_BIODEGRADABLE);

        assertEquals("Biodegradable count must be 8", 8, bio.size());
        assertEquals("Recyclable count must be 8", 8, rec.size());
        assertEquals("Non-biodegradable count must be 8", 8, nonBio.size());

        assertEquals("All 3 trash categories must have an exactly equal number of items", bio.size(), rec.size());
        assertEquals("All 3 trash categories must have an exactly equal number of items", rec.size(), nonBio.size());
    }

    @Test
    public void testSpawnDistribution20PercentCrittersAnd80PercentEqualTrash() {
        Random random = new Random(42);
        int totalSpawns = 100_000;

        int critterCount = 0;
        int bioCount = 0;
        int recCount = 0;
        int nonBioCount = 0;

        for (int i = 0; i < totalSpawns; i++) {
            WasteItem item = WasteCatalog.getRandomTemplate(random);
            switch (item.getCategory()) {
                case CRITTER_FLICK_AWAY:
                    critterCount++;
                    break;
                case BIODEGRADABLE:
                    bioCount++;
                    break;
                case RECYCLABLE:
                    recCount++;
                    break;
                case NON_BIODEGRADABLE:
                    nonBioCount++;
                    break;
            }
        }

        double critterPct = (double) critterCount / totalSpawns * 100.0;
        double bioPct = (double) bioCount / totalSpawns * 100.0;
        double recPct = (double) recCount / totalSpawns * 100.0;
        double nonBioPct = (double) nonBioCount / totalSpawns * 100.0;

        // Critters must be ~20%
        assertTrue("Critters should be ~20% (found " + critterPct + "%)",
                critterPct >= 18.5 && critterPct <= 21.5);

        // Each trash category should be ~26.67% (80% / 3)
        assertTrue("Biodegradable should be ~26.67% (found " + bioPct + "%)",
                bioPct >= 25.0 && bioPct <= 28.5);
        assertTrue("Recyclable should be ~26.67% (found " + recPct + "%)",
                recPct >= 25.0 && recPct <= 28.5);
        assertTrue("Non-Biodegradable should be ~26.67% (found " + nonBioPct + "%)",
                nonBioPct >= 25.0 && nonBioPct <= 28.5);
    }

    @Test
    public void testLevel1RandomWasteOnlyExcludesCrittersAndSplitsEqually() {
        Random random = new Random(42);
        int totalSpawns = 30_000;

        int bioCount = 0;
        int recCount = 0;
        int nonBioCount = 0;

        for (int i = 0; i < totalSpawns; i++) {
            WasteItem item = WasteCatalog.getRandomWasteOnly(random);
            switch (item.getCategory()) {
                case BIODEGRADABLE:
                    bioCount++;
                    break;
                case RECYCLABLE:
                    recCount++;
                    break;
                case NON_BIODEGRADABLE:
                    nonBioCount++;
                    break;
                default:
                    org.junit.Assert.fail("No critters allowed in getRandomWasteOnly");
            }
        }

        double bioPct = (double) bioCount / totalSpawns * 100.0;
        double recPct = (double) recCount / totalSpawns * 100.0;
        double nonBioPct = (double) nonBioCount / totalSpawns * 100.0;

        assertTrue(bioPct >= 31.5 && bioPct <= 35.0);
        assertTrue(recPct >= 31.5 && recPct <= 35.0);
        assertTrue(nonBioPct >= 31.5 && nonBioPct <= 35.0);
    }
}
