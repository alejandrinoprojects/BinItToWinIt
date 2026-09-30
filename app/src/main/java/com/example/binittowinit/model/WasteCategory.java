package com.example.binittowinit.model;

/**
 * Defines the classification categories for all falling items within "Bin It to Win It".
 * <p>
 * There are three municipal waste sorting categories that map directly to the
 * physical garbage bins on screen (Biodegradable, Recyclable, Non-Biodegradable),
 * plus a special non-waste category for living animals (Wandering Critters) that
 * must never be placed in a bin and should instead be flicked off-screen to safety.
 * </p>
 */
public enum WasteCategory {
    /**
     * Organic and compostable waste (e.g., banana peel, apple core, leaves).
     * Mapped to the Green Bin.
     */
    BIODEGRADABLE("Biodegradable", 0xFF2E7D32, "Organic / Compostable waste", "Green Bin"),
    RECYCLABLE("Recyclable", 0xFF1565C0, "Plastics, Glass, Metals, Clean Paper", "Blue Bin"),
    NON_BIODEGRADABLE("Non-Biodegradable", 0xFFE65100, "Residual & Landfill waste", "Orange Bin"),
    CRITTER_FLICK_AWAY("Wandering Critter", 0xFFE91E63, "Living creature! Shoo/flick to safety!", "Flick Away");

    private final String displayName;
    private final int primaryColor;
    private final String description;
    private final String binColorName;

    /**
     * Constructs a WasteCategory enum instance.
     *
     * @param displayName  Human-readable name of the category for UI presentation.
     * @param primaryColor ARGB color hex integer representing the category's theme.
     * @param description  Educational explanation of items belonging to this category.
     * @param binColorName Color description of the target bin.
     */
    WasteCategory(String displayName, int primaryColor, String description, String binColorName) {
        this.displayName = displayName;
        this.primaryColor = primaryColor;
        this.description = description;
        this.binColorName = binColorName;
    }

    public String getBinColorName() {
        return binColorName;
    }

    /**
     * Returns the human-readable display name.
     *
     * @return Formatted category title string.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the primary ARGB color code associated with this category.
     *
     * @return Color hex integer.
     */
    public int getPrimaryColor() {
        return primaryColor;
    }

    /**
     * Returns the educational description of the category.
     *
     * @return Description text string.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Determines whether items in this category belong in one of the 3 sorting bins.
     *
     * @return {@code true} if sortable into a bin; {@code false} if flick-away only.
     */
    public boolean isSortableInBin() {
        return this != CRITTER_FLICK_AWAY;
    }
}
