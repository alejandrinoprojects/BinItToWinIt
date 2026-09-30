package com.example.binittowinit.model;

import com.example.binittowinit.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Master catalog and spawner pool of all waste items and wandering critters in the game.
 * <p>
 * Contains definitions for:
 * <ol>
 *   <li><b>Biodegradable Waste:</b> Organic items (peels, cores, fish bones, leaves, bread).</li>
 *   <li><b>Recyclable Waste:</b> Clean containers & packaging (plastic bottles, soda cans, glass, cardboard, paper).</li>
 *   <li><b>Non-Biodegradable Waste:</b> Residual landfill items (shopping bags, styrofoam, candy wrappers, mugs, cutlery).</li>
 *   <li><b>Wandering Critters:</b> Friendly animals to be flicked/shooed to safety (kittens, puppies, pigeons, squirrels, raccoons, butterflies).</li>
 * </ol>
 * Each item has tailored mass, sway amplitudes, rotational speeds, and wind sensitivities.
 * </p>
 */
public class WasteCatalog {
    private static final List<WasteItem> ALL_ITEMS = new ArrayList<>();
    private static final List<WasteItem> BIODEGRADABLE_ITEMS = new ArrayList<>();
    private static final List<WasteItem> RECYCLABLE_ITEMS = new ArrayList<>();
    private static final List<WasteItem> NON_BIO_ITEMS = new ArrayList<>();
    private static final List<WasteItem> CRITTER_ITEMS = new ArrayList<>();

    static {
        // --- 1. BIODEGRADABLE (GREEN BIN - 8 ITEMS) ---
        add(new WasteItem("bio_banana", "Banana Peel", WasteCategory.BIODEGRADABLE,
                R.drawable.ic_banana, new ItemBehavior(1.0f, 0.9f, 0.40f, 2.5f, 20f, 40f, false), 100,
                "Banana Peel is Biodegradable (Green Bin) — Organic matter decomposes into nutrient-rich soil!"));
        add(new WasteItem("bio_apple", "Apple Core", WasteCategory.BIODEGRADABLE,
                R.drawable.ic_apple, new ItemBehavior(1.2f, 1.05f, 0.15f, 1.2f, 10f, 75f, false), 100,
                "Apple Core is Biodegradable (Green Bin) — Fruit remnants can be composted within weeks."));
        add(new WasteItem("bio_fishbone", "Fish Bone", WasteCategory.BIODEGRADABLE,
                R.drawable.ic_fishbone, new ItemBehavior(0.7f, 0.85f, 0.60f, 3.5f, 25f, 35f, false), 100,
                "Fish Bone is Biodegradable (Green Bin) — Animal bones and food scraps belong with organic compost."));
        add(new WasteItem("bio_leaf", "Dry Fallen Leaf", WasteCategory.BIODEGRADABLE,
                R.drawable.ic_leaf, new ItemBehavior(0.25f, 0.65f, 0.95f, 4.0f, 45f, 20f, false), 120,
                "Dry Leaf is Biodegradable (Green Bin) — Yard trimmings make excellent natural fertilizer."));
        add(new WasteItem("bio_bread", "Leftover Bread", WasteCategory.BIODEGRADABLE,
                R.drawable.ic_bread, new ItemBehavior(0.9f, 0.95f, 0.30f, 1.5f, 15f, 30f, false), 100,
                "Leftover Bread is Biodegradable (Green Bin) — Baked goods break down naturally without landfill burden."));
        add(new WasteItem("bio_eggshell", "Cracked Eggshell", WasteCategory.BIODEGRADABLE,
                R.drawable.ic_eggshell, new ItemBehavior(0.4f, 0.75f, 0.55f, 2.0f, 18f, 30f, false), 100,
                "Eggshells are Biodegradable (Green Bin) — Rich in calcium, eggshells enrich organic compost!"));
        add(new WasteItem("bio_watermelon", "Watermelon Rind", WasteCategory.BIODEGRADABLE,
                R.drawable.ic_watermelon, new ItemBehavior(1.4f, 1.10f, 0.20f, 1.1f, 12f, 60f, false), 100,
                "Watermelon Rind is Biodegradable (Green Bin) — Fruit scraps decompose into fertile garden soil!"));
        add(new WasteItem("bio_carrot", "Carrot Top", WasteCategory.BIODEGRADABLE,
                R.drawable.ic_carrot, new ItemBehavior(0.55f, 0.80f, 0.65f, 2.8f, 22f, 45f, false), 100,
                "Carrot Tops are Biodegradable (Green Bin) — Vegetable trimmings belong in organic compost."));

        // --- 2. RECYCLABLE (BLUE BIN - 8 ITEMS) ---
        add(new WasteItem("rec_plastic_bottle", "Plastic Water Bottle", WasteCategory.RECYCLABLE,
                R.drawable.ic_bottle_plastic, new ItemBehavior(0.6f, 0.85f, 0.70f, 2.8f, 30f, 50f, false), 100,
                "Plastic Bottle is Recyclable (Blue Bin) — Empty liquids and rinse before recycling!"));
        add(new WasteItem("rec_soda_can", "Aluminum Soda Can", WasteCategory.RECYCLABLE,
                R.drawable.ic_can, new ItemBehavior(1.1f, 1.1f, 0.20f, 1.0f, 8f, 120f, false), 100,
                "Aluminum Can is Recyclable (Blue Bin) — Aluminum can be remelted and reused infinitely!"));
        add(new WasteItem("rec_glass_bottle", "Glass Beverage Bottle", WasteCategory.RECYCLABLE,
                R.drawable.ic_bottle_glass, new ItemBehavior(2.4f, 1.45f, 0.0f, 0.0f, 0f, 15f, false), 100,
                "Glass Bottle is Recyclable (Blue Bin) — Clean glass is 100% recyclable into new containers."));
        add(new WasteItem("rec_box", "Cardboard Box", WasteCategory.RECYCLABLE,
                R.drawable.ic_box, new ItemBehavior(0.8f, 0.9f, 0.45f, 1.8f, 20f, 40f, false), 100,
                "Cardboard Box is Recyclable (Blue Bin) — Flatten boxes to save space in the recycling stream."));
        add(new WasteItem("rec_paper", "Crumpled Paper", WasteCategory.RECYCLABLE,
                R.drawable.ic_paper, new ItemBehavior(0.3f, 0.7f, 0.90f, 3.6f, 35f, 45f, false), 110,
                "Clean Paper is Recyclable (Blue Bin) — Dry scrap paper is repulped into fresh paper goods."));
        add(new WasteItem("rec_milk_carton", "Paper Milk Carton", WasteCategory.RECYCLABLE,
                R.drawable.ic_milk_carton, new ItemBehavior(0.7f, 0.88f, 0.50f, 2.1f, 22f, 35f, false), 100,
                "Milk Cartons are Recyclable (Blue Bin) — Rinse and flatten paperboard cartons for recycling!"));
        add(new WasteItem("rec_tin_can", "Food Tin Can", WasteCategory.RECYCLABLE,
                R.drawable.ic_tin_can, new ItemBehavior(1.3f, 1.15f, 0.15f, 0.9f, 8f, 80f, false), 100,
                "Tin Cans are Recyclable (Blue Bin) — Steel food cans can be melted down and repurposed endlessly!"));
        add(new WasteItem("rec_newspaper", "Daily Newspaper", WasteCategory.RECYCLABLE,
                R.drawable.ic_newspaper, new ItemBehavior(0.35f, 0.72f, 0.85f, 3.2f, 32f, 25f, false), 110,
                "Newspapers are Recyclable (Blue Bin) — Newsprint is easily recycled into new paper products!"));

        // --- 3. NON-BIODEGRADABLE / RESIDUAL (ORANGE BIN - 8 ITEMS) ---
        add(new WasteItem("non_plastic_bag", "Plastic Shopping Bag", WasteCategory.NON_BIODEGRADABLE,
                R.drawable.ic_plastic_bag, new ItemBehavior(0.2f, 0.6f, 1.0f, 3.8f, 50f, 25f, false), 120,
                "Plastic Bag is Non-Biodegradable (Orange Bin) — Thin film jams sorting machines; treat as residual!"));
        add(new WasteItem("non_styrofoam", "Styrofoam Cup", WasteCategory.NON_BIODEGRADABLE,
                R.drawable.ic_styrofoam, new ItemBehavior(0.35f, 0.75f, 0.85f, 3.2f, 35f, 30f, false), 110,
                "Styrofoam Cup is Non-Biodegradable (Orange Bin) — Polystyrene does not biodegrade easily."));
        add(new WasteItem("non_wrapper", "Candy Wrapper", WasteCategory.NON_BIODEGRADABLE,
                R.drawable.ic_wrapper, new ItemBehavior(0.25f, 0.68f, 0.80f, 4.2f, 40f, 60f, false), 100,
                "Candy Wrapper is Non-Biodegradable (Orange Bin) — Multi-layer foil-plastic is non-recyclable."));
        add(new WasteItem("non_mug", "Broken Ceramic Mug", WasteCategory.NON_BIODEGRADABLE,
                R.drawable.ic_ceramic_mug, new ItemBehavior(2.5f, 1.5f, 0.0f, 0.0f, 0f, 10f, false), 100,
                "Broken Ceramic is Non-Biodegradable (Orange Bin) — Ceramics melt at higher temps than container glass."));
        add(new WasteItem("non_cutlery", "Plastic Straw & Fork", WasteCategory.NON_BIODEGRADABLE,
                R.drawable.ic_cutlery, new ItemBehavior(0.7f, 0.95f, 0.45f, 2.2f, 18f, 65f, false), 100,
                "Disposable Cutlery is Non-Biodegradable (Orange Bin) — Small plastic utensils are residual waste."));
        add(new WasteItem("non_diaper", "Disposable Diaper", WasteCategory.NON_BIODEGRADABLE,
                R.drawable.ic_diaper, new ItemBehavior(1.1f, 0.90f, 0.25f, 1.4f, 12f, 20f, false), 100,
                "Diapers are Non-Biodegradable (Orange Bin) — Sanitary items are strictly residual landfill waste."));
        add(new WasteItem("non_lightbulb", "Incandescent Bulb", WasteCategory.NON_BIODEGRADABLE,
                R.drawable.ic_lightbulb, new ItemBehavior(0.6f, 0.82f, 0.35f, 1.8f, 15f, 30f, false), 100,
                "Lightbulbs are Non-Biodegradable (Orange Bin) — Mixed metal/tungsten filament glass cannot be recycled."));
        add(new WasteItem("non_chip_bag", "Potato Chip Bag", WasteCategory.NON_BIODEGRADABLE,
                R.drawable.ic_chip_bag, new ItemBehavior(0.22f, 0.64f, 0.90f, 4.0f, 45f, 50f, false), 110,
                "Chip Bags are Non-Biodegradable (Orange Bin) — Metallized plastic foil layers are non-recyclable."));

        // --- 4. WANDERING CRITTERS (FLICK TO SHOO SAFELY! - 20% SPAWN PROBABILITY) ---
        add(new WasteItem("critter_kitten", "Curious Kitten", WasteCategory.CRITTER_FLICK_AWAY,
                R.drawable.ic_kitten, new ItemBehavior(0.8f, 0.75f, 0.35f, 2.0f, 15f, 15f, true), 150,
                "Critters are NOT trash! Flick curious kittens off-screen to safety!"));
        add(new WasteItem("critter_puppy", "Playful Puppy", WasteCategory.CRITTER_FLICK_AWAY,
                R.drawable.ic_puppy, new ItemBehavior(0.9f, 0.8f, 0.30f, 1.8f, 12f, 15f, true), 150,
                "Critters are NOT trash! Flick playful puppies safely away!"));
        add(new WasteItem("critter_pigeon", "Fluttering Pigeon", WasteCategory.CRITTER_FLICK_AWAY,
                R.drawable.ic_pigeon, new ItemBehavior(0.5f, 0.7f, 0.80f, 3.0f, 30f, 25f, true), 150,
                "Wildlife belongs in nature! Shoo fluttering birds away!"));
        add(new WasteItem("critter_squirrel", "Sneaky Squirrel", WasteCategory.CRITTER_FLICK_AWAY,
                R.drawable.ic_squirrel, new ItemBehavior(0.6f, 0.85f, 0.25f, 2.4f, 16f, 20f, true), 150,
                "Critters are NOT trash! Flick squirrels safely up into the trees!"));
        add(new WasteItem("critter_raccoon", "Curious Raccoon", WasteCategory.CRITTER_FLICK_AWAY,
                R.drawable.ic_raccoon, new ItemBehavior(1.3f, 0.95f, 0.10f, 1.2f, 10f, 15f, true), 150,
                "Keep raccoons away from trash bins! Flick them off-screen!"));
        add(new WasteItem("critter_butterfly", "Delicate Butterfly", WasteCategory.CRITTER_FLICK_AWAY,
                R.drawable.ic_butterfly, new ItemBehavior(0.2f, 0.6f, 0.95f, 4.5f, 45f, 10f, true), 150,
                "Critters belong in the garden! Flick butterflies safely into the air!"));
    }

    private static void add(WasteItem item) {
        ALL_ITEMS.add(item);
        switch (item.getCategory()) {
            case BIODEGRADABLE:
                BIODEGRADABLE_ITEMS.add(item);
                break;
            case RECYCLABLE:
                RECYCLABLE_ITEMS.add(item);
                break;
            case NON_BIODEGRADABLE:
                NON_BIO_ITEMS.add(item);
                break;
            case CRITTER_FLICK_AWAY:
                CRITTER_ITEMS.add(item);
                break;
        }
    }

    /**
     * Returns an unmodifiable list of all registered item templates.
     *
     * @return Complete item list.
     */
    public static List<WasteItem> getAllItems() {
        return Collections.unmodifiableList(ALL_ITEMS);
    }

    /**
     * Returns all items belonging to a specific category.
     *
     * @param category The desired category.
     * @return Unmodifiable list of matching items.
     */
    public static List<WasteItem> getItemsByCategory(WasteCategory category) {
        switch (category) {
            case BIODEGRADABLE: return Collections.unmodifiableList(BIODEGRADABLE_ITEMS);
            case RECYCLABLE: return Collections.unmodifiableList(RECYCLABLE_ITEMS);
            case NON_BIODEGRADABLE: return Collections.unmodifiableList(NON_BIO_ITEMS);
            case CRITTER_FLICK_AWAY: return Collections.unmodifiableList(CRITTER_ITEMS);
            default: return Collections.emptyList();
        }
    }

    /**
     * Selects a random item template for spawning, dynamically weighted with ~80% chance
     * for standard sorting garbage and ~20% chance for a wandering critter.
     *
     * @param random Shared Random generator instance.
     * @return Randomly selected {@code WasteItem} template ready to be instantiated.
     */
    public static WasteItem getRandomTemplate(Random random) {
        boolean spawnCritter = random.nextFloat() < 0.20f;
        if (spawnCritter && !CRITTER_ITEMS.isEmpty()) {
            return CRITTER_ITEMS.get(random.nextInt(CRITTER_ITEMS.size()));
        }

        // Pick one of the 3 waste categories evenly
        int catChoice = random.nextInt(3);
        List<WasteItem> pool;
        if (catChoice == 0) pool = BIODEGRADABLE_ITEMS;
        else if (catChoice == 1) pool = RECYCLABLE_ITEMS;
        else pool = NON_BIO_ITEMS;

        return pool.get(random.nextInt(pool.size()));
    }

    /**
     * Selects a random waste item excluding critters (used for Level 1 introductory phase).
     *
     * @param random Shared Random generator.
     * @return Randomly selected standard waste template.
     */
    public static WasteItem getRandomWasteOnly(Random random) {
        int catChoice = random.nextInt(3);
        List<WasteItem> pool;
        if (catChoice == 0) pool = BIODEGRADABLE_ITEMS;
        else if (catChoice == 1) pool = RECYCLABLE_ITEMS;
        else pool = NON_BIO_ITEMS;

        return pool.get(random.nextInt(pool.size()));
    }
}
