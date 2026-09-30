package com.example.binittowinit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.binittowinit.model.ItemBehavior;
import com.example.binittowinit.model.WasteCatalog;
import com.example.binittowinit.model.WasteCategory;
import com.example.binittowinit.model.WasteItem;

import org.junit.Test;

import java.util.List;

public class WasteItemPhysicsTest {

    @Test
    public void testCatalogHasAllCategoriesAndCritters() {
        List<WasteItem> bioItems = WasteCatalog.getItemsByCategory(WasteCategory.BIODEGRADABLE);
        List<WasteItem> recItems = WasteCatalog.getItemsByCategory(WasteCategory.RECYCLABLE);
        List<WasteItem> nonBioItems = WasteCatalog.getItemsByCategory(WasteCategory.NON_BIODEGRADABLE);
        List<WasteItem> critters = WasteCatalog.getItemsByCategory(WasteCategory.CRITTER_FLICK_AWAY);

        assertFalse(bioItems.isEmpty());
        assertFalse(recItems.isEmpty());
        assertFalse(nonBioItems.isEmpty());
        assertFalse(critters.isEmpty());

        assertEquals(8, bioItems.size());
        assertEquals(8, recItems.size());
        assertEquals(8, nonBioItems.size());
        assertEquals(6, critters.size());
    }

    @Test
    public void testWindSensitivityDifferenceBetweenLightAndHeavyItems() {
        // Light Plastic Bag
        WasteItem plasticBag = new WasteItem("bag", "Bag", WasteCategory.NON_BIODEGRADABLE, 0,
                new ItemBehavior(0.2f, 0.6f, 1.0f, 3.8f, 50f, 25f, false), 100);
        plasticBag = plasticBag.spawnAt(500f, 100f, 120f, 200f);

        // Heavy Glass Bottle
        WasteItem glassBottle = new WasteItem("bottle", "Glass Bottle", WasteCategory.RECYCLABLE, 0,
                new ItemBehavior(2.4f, 1.45f, 0.0f, 0.0f, 0f, 15f, false), 100);
        glassBottle = glassBottle.spawnAt(500f, 100f, 120f, 200f);

        float windForce = 150f; // 150 px/s blowing to the right
        float deltaTime = 0.5f; // half second

        plasticBag.update(deltaTime, windForce, 1080, 1920);
        glassBottle.update(deltaTime, windForce, 1080, 1920);

        // Plastic bag's baseX moved to the right by windForce * 1.0 * deltaTime = 75px
        assertEquals(575f, plasticBag.getBaseX(), 0.01f);

        // Glass bottle's baseX remained exactly 500f because windSensitivity is 0.0
        assertEquals(500f, glassBottle.getBaseX(), 0.01f);
    }

    @Test
    public void testFlickBallisticMovement() {
        WasteItem item = new WasteItem("item", "Item", WasteCategory.CRITTER_FLICK_AWAY, 0,
                ItemBehavior.createCritter(0.5f, 0.7f, 10f), 150);
        item = item.spawnAt(400f, 600f, 120f, 200f);

        // Launch flick upwards and to the right
        item.launchFlick(800f, -1200f);
        assertTrue(item.isFlicked());
        assertFalse(item.isBeingDragged());

        float deltaTime = 0.1f;
        item.update(deltaTime, 0f, 1080, 1920);

        // x increased by 800 * 0.1 = 80 -> 480
        assertEquals(480f, item.getX(), 0.01f);
        // y decreased by 1200 * 0.1 = 120 -> 480
        assertEquals(480f, item.getY(), 0.01f);
    }
}
