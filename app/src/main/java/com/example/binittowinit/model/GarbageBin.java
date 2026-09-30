package com.example.binittowinit.model;

import android.graphics.RectF;

/**
 * Represents one of the designated garbage receptacle bins positioned along the bottom of the screen.
 * <p>
 * Each bin handles a specific {@link WasteCategory}, maintains its screen bounding box coordinates,
 * provides hit-testing for dragged waste items, and plays a tactile scale-bump "catch" animation
 * whenever an item is successfully deposited into it.
 * </p>
 */
public class GarbageBin {
    /** The waste category accepted by this bin. */
    private final WasteCategory category;

    /** Human-readable label displayed on the front of the bin (e.g., "Biodegradable"). */
    private final String label;

    /** Primary theme color used to render the bin body and borders. */
    private final int color;

    /** Vector drawable resource ID representing the bin illustration. */
    private final int drawableResId;

    // --- Spatial Geometry (Pure Java float bounds for portable hit-testing) ---
    private float left;
    private float top;
    private float right;
    private float bottom;
    private RectF bounds;

    /** Whether an active dragged item is currently hovering over this bin. */
    private boolean isHighlighted;

    /** Dynamic scaling factor for the visual catch/bounce animation (defaults to 1.0f). */
    private float catchAnimationScale = 1.0f;

    /**
     * Constructs a GarbageBin specification.
     *
     * @param category      Target waste category accepted by this bin.
     * @param label         Visual text label for the bin.
     * @param color         Color hex code for rendering.
     * @param drawableResId Vector drawable icon.
     */
    public GarbageBin(WasteCategory category, String label, int color, int drawableResId) {
        this.category = category;
        this.label = label;
        this.color = color;
        this.drawableResId = drawableResId;
    }

    /**
     * Updates the screen boundary coordinates of the bin when layout size changes.
     *
     * @param left   Left X coordinate in pixels.
     * @param top    Top Y coordinate in pixels.
     * @param right  Right X coordinate in pixels.
     * @param bottom Bottom Y coordinate in pixels.
     */
    public void updateBounds(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        if (bounds != null) {
            bounds.set(left, top, right, bottom);
        }
    }

    /**
     * Determines whether the specified coordinate lies within the bin's target area.
     *
     * @param x Test point X.
     * @param y Test point Y.
     * @return {@code true} if inside the bin; {@code false} otherwise.
     */
    public boolean contains(float x, float y) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    /**
     * Returns the Android {@link RectF} bounding rectangle (instantiated lazily for Canvas drawing).
     *
     * @return Bounding rectangle.
     */
    public RectF getBounds() {
        if (bounds == null) {
            bounds = new RectF(left, top, right, bottom);
        }
        return bounds;
    }

    /**
     * Triggers a bounce animation indicating an item was caught/deposited.
     */
    public void triggerCatchAnimation() {
        catchAnimationScale = 1.25f;
    }

    /**
     * Decays the catch animation scale back to 1.0f over time.
     *
     * @param deltaTime Elapsed frame time in seconds.
     */
    public void updateAnimation(float deltaTime) {
        if (catchAnimationScale > 1.0f) {
            catchAnimationScale -= 2.0f * deltaTime;
            if (catchAnimationScale < 1.0f) {
                catchAnimationScale = 1.0f;
            }
        }
    }

    /**
     * Returns the target waste category accepted by this bin.
     *
     * @return Target WasteCategory.
     */
    public WasteCategory getCategory() { return category; }

    /**
     * Returns the human-readable text label of the bin.
     *
     * @return Display label string.
     */
    public String getLabel() { return label; }

    /**
     * Returns the ARGB theme color hex integer of this bin.
     *
     * @return Color hex integer.
     */
    public int getColor() { return color; }

    /**
     * Returns the vector drawable resource ID representing this bin's artwork.
     *
     * @return Drawable resource ID.
     */
    public int getDrawableResId() { return drawableResId; }

    /**
     * Returns whether an item is currently hovering above this bin.
     *
     * @return {@code true} if highlighted; {@code false} otherwise.
     */
    public boolean isHighlighted() { return isHighlighted; }

    /**
     * Sets whether this bin should display its drop-zone highlight spotlight.
     *
     * @param highlighted {@code true} to highlight; {@code false} to clear.
     */
    public void setHighlighted(boolean highlighted) { isHighlighted = highlighted; }

    /**
     * Returns the dynamic visual bounce animation scale factor.
     *
     * @return Scale multiplier (1.0f - 1.25f).
     */
    public float getCatchAnimationScale() { return catchAnimationScale; }
}
