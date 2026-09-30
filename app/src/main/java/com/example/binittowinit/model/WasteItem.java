package com.example.binittowinit.model;

import android.graphics.RectF;

/**
 * Represents an active, interactive entity on the game canvas.
 * <p>
 * A {@code WasteItem} can either be a standard piece of municipal waste (destined for
 * Biodegradable, Recyclable, or Non-Biodegradable bins) or a Wandering Critter (which
 * must be flicked off-screen).
 * </p>
 * <p>
 * This class tracks 2D screen positions, dynamic velocities, sine-wave sway oscillations,
 * wind susceptibility, touch drag states, and ballistic flick trajectories.
 * </p>
 */
public class WasteItem {
    /** Unique identifier string for template lookup. */
    private String id;

    /** Human-readable display name (e.g. "Plastic Shopping Bag", "Curious Kitten"). */
    private String name;

    /** Disposal or rescue category of this item. */
    private WasteCategory category;

    /** Android drawable resource ID for rendering the item on Canvas. */
    private int drawableResId;

    /** Physics profile governing mass, sway amplitude, and wind responsiveness. */
    private ItemBehavior behavior;

    /** Base score awarded upon correct sorting or successful critter rescue. */
    private int pointsAwarded;

    // --- Spatial & Dynamic properties ---
    /** Current center X coordinate on screen in pixels. */
    private float x;

    /** Current center Y coordinate on screen in pixels. */
    private float y;

    /** Anchor center X coordinate around which horizontal sway oscillates. */
    private float baseX;

    /** Horizontal velocity in pixels per second (active during ballistic flick). */
    private float vx;

    /** Vertical descent velocity in pixels per second. */
    private float vy;

    /** Current angular rotation in degrees. */
    private float rotation;

    /** Visual bounding square dimension (width/height) in pixels. */
    private float size;

    /** Accumulated time alive in seconds, driving continuous sine-wave sway. */
    private float timeAlive;

    // --- Touch & Resolution Interaction States ---
    /** Flag indicating whether the player's finger is currently dragging this item. */
    private boolean isBeingDragged;

    /** Flag indicating whether the item has been launched into a ballistic flick flight. */
    private boolean isFlicked;

    /** Flag set to true once the item is settled into a bin, flicked off-screen, or missed. */
    private boolean isResolved;

    /** Educational sorting tip or disposal advice. */
    private String educationalTip;

    /**
     * Default zero-arg constructor for pooling.
     */
    public WasteItem() {
        this.size = 130f;
    }

    /**
     * Constructs a template item definition.
     *
     * @param id             Unique template ID.
     * @param name           Display name of the waste item or critter.
     * @param category       Sorting category.
     * @param drawableResId  Vector drawable resource.
     * @param behavior       Physical behavior configuration.
     * @param pointsAwarded  Points earned for correct handling.
     * @param educationalTip Informative tip explaining disposal.
     */
    public WasteItem(String id, String name, WasteCategory category, int drawableResId,
                     ItemBehavior behavior, int pointsAwarded, String educationalTip) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.drawableResId = drawableResId;
        this.behavior = behavior;
        this.pointsAwarded = pointsAwarded;
        this.educationalTip = educationalTip;
        this.size = 130f;
    }

    public WasteItem(String id, String name, WasteCategory category, int drawableResId,
                     ItemBehavior behavior, int pointsAwarded) {
        this(id, name, category, drawableResId, behavior, pointsAwarded, "");
    }

    /**
     * Re-initializes this pooled entity from a template definition without allocating a new object.
     *
     * @param template      Template to copy characteristics from.
     * @param spawnX        Initial horizontal center coordinate.
     * @param spawnY        Initial vertical center coordinate.
     * @param itemSize      Render size in pixels.
     * @param baseFallSpeed Base game fall speed.
     */
    public void reinitialize(WasteItem template, float spawnX, float spawnY, float itemSize, float baseFallSpeed) {
        this.id = template.id;
        this.name = template.name;
        this.category = template.category;
        this.drawableResId = template.drawableResId;
        this.behavior = template.behavior;
        this.pointsAwarded = template.pointsAwarded;
        this.educationalTip = template.educationalTip;
        this.x = spawnX;
        this.y = spawnY;
        this.baseX = spawnX;
        this.size = itemSize;
        this.vy = baseFallSpeed * template.behavior.getFallSpeedFactor();
        this.vx = 0f;
        this.rotation = 0f;
        this.timeAlive = 0f;
        this.isBeingDragged = false;
        this.isFlicked = false;
        this.isResolved = false;
    }

    /**
     * Cleans and resets all dynamic state before returning to the entity pool.
     */
    public void reset() {
        this.x = 0;
        this.y = 0;
        this.baseX = 0;
        this.vx = 0;
        this.vy = 0;
        this.rotation = 0;
        this.timeAlive = 0;
        this.isBeingDragged = false;
        this.isFlicked = false;
        this.isResolved = false;
    }

    /**
     * Spawns an active runtime instance of this item at the specified initial coordinates.
     *
     * @param spawnX        Initial horizontal center coordinate.
     * @param spawnY        Initial vertical center coordinate (typically off the top of screen).
     * @param itemSize      Render size in pixels.
     * @param baseFallSpeed Current baseline game fall speed to scale with the item's behavior.
     * @return Fresh, active {@code WasteItem} instance ready to enter the game loop.
     */
    public WasteItem spawnAt(float spawnX, float spawnY, float itemSize, float baseFallSpeed) {
        WasteItem item = new WasteItem(this.id, this.name, this.category, this.drawableResId,
                this.behavior, this.pointsAwarded, this.educationalTip);
        item.x = spawnX;
        item.y = spawnY;
        item.baseX = spawnX;
        item.size = itemSize;
        item.vy = baseFallSpeed * this.behavior.getFallSpeedFactor();
        item.vx = 0f;
        item.rotation = 0f;
        item.timeAlive = 0f;
        item.isBeingDragged = false;
        item.isFlicked = false;
        item.isResolved = false;
        return item;
    }

    public String getEducationalTip() {
        if (educationalTip != null && !educationalTip.isEmpty()) {
            return educationalTip;
        }
        if (category == WasteCategory.CRITTER_FLICK_AWAY) {
            return "Animals don't belong in the trash! Flick/shoo them to safety!";
        }
        return name + " belongs in the " + category.getDisplayName() + " (" + category.getBinColorName() + ")!";
    }

    /**
     * Updates positions, sine-wave sway, wind deviations, and rotation for one frame.
     *
     * @param deltaTime        Elapsed time since last frame in seconds.
     * @param currentWindForce Current environmental horizontal wind velocity in px/sec.
     * @param screenWidth      Display width in pixels for boundary clamping.
     * @param screenHeight     Display height in pixels.
     */
    public void update(float deltaTime, float currentWindForce, int screenWidth, int screenHeight) {
        if (isResolved || isBeingDragged) {
            return;
        }

        timeAlive += deltaTime;

        // Ballistic flick trajectory
        if (isFlicked) {
            x += vx * deltaTime;
            y += vy * deltaTime;
            rotation += 360f * deltaTime; // rapid tumble while flying
            return;
        }

        // Natural descent physics:
        // 1. Horizontal wind drift applied to the anchor coordinate baseX
        float effectiveWind = currentWindForce * behavior.getWindSensitivity();
        baseX += effectiveWind * deltaTime;

        // Clamp baseX within screen bounds with slight edge margin
        float halfSize = size / 2f;
        if (baseX < halfSize) baseX = halfSize;
        if (baseX > screenWidth - halfSize) baseX = screenWidth - halfSize;

        // 2. Sinusoidal horizontal sway oscillation
        float swayOffset = 0f;
        if (behavior.getSwayAmplitude() > 0f) {
            swayOffset = (float) Math.sin(timeAlive * behavior.getSwayFrequency()) * behavior.getSwayAmplitude();
        }
        x = baseX + swayOffset;

        // 3. Vertical descent
        y += vy * deltaTime;

        // 4. Angular rotation
        rotation += behavior.getRotationSpeed() * deltaTime;
    }

    /**
     * Checks whether a screen touch point falls within the item's hit box (with generous UX padding).
     *
     * @param touchX Screen touch X.
     * @param touchY Screen touch Y.
     * @return {@code true} if touch hits the item; {@code false} otherwise.
     */
    public boolean contains(float touchX, float touchY) {
        float half = size / 2f * 1.3f; // 30% touch target padding for smooth mobile ergonomics
        return touchX >= (x - half) && touchX <= (x + half) &&
               touchY >= (y - half) && touchY <= (y + half);
    }

    /**
     * Calculates the bounding rectangle of the item.
     *
     * @return {@code RectF} bounds.
     */
    public RectF getBounds() {
        float half = size / 2f;
        return new RectF(x - half, y - half, x + half, y + half);
    }

    /**
     * Initiates a ballistic flick release with the provided initial impulse velocity.
     *
     * @param velocityX Horizontal release speed in px/sec.
     * @param velocityY Vertical release speed in px/sec.
     */
    public void launchFlick(float velocityX, float velocityY) {
        this.isBeingDragged = false;
        this.isFlicked = true;
        this.vx = velocityX;
        this.vy = velocityY;
    }

    // --- Standard Getters and Setters ---

    /**
     * Returns the unique identifier of the item template.
     *
     * @return Template identifier string.
     */
    public String getId() { return id; }

    /**
     * Returns the display name of the item.
     *
     * @return Item name string.
     */
    public String getName() { return name; }

    /**
     * Returns the waste or rescue category.
     *
     * @return Target WasteCategory.
     */
    public WasteCategory getCategory() { return category; }

    /**
     * Returns the vector drawable resource ID representing the visual asset.
     *
     * @return Android drawable resource ID.
     */
    public int getDrawableResId() { return drawableResId; }

    /**
     * Returns the physical behavior configuration (mass, sway, aerodynamics).
     *
     * @return ItemBehavior configuration.
     */
    public ItemBehavior getBehavior() { return behavior; }

    /**
     * Returns the base score awarded for sorting or rescuing this item.
     *
     * @return Score points.
     */
    public int getPointsAwarded() { return pointsAwarded; }

    /**
     * Returns the horizontal center position on screen.
     *
     * @return X coordinate in pixels.
     */
    public float getX() { return x; }

    /**
     * Sets the horizontal center position on screen.
     *
     * @param x X coordinate in pixels.
     */
    public void setX(float x) { this.x = x; }

    /**
     * Returns the vertical center position on screen.
     *
     * @return Y coordinate in pixels.
     */
    public float getY() { return y; }

    /**
     * Sets the vertical center position on screen.
     *
     * @param y Y coordinate in pixels.
     */
    public void setY(float y) { this.y = y; }

    /**
     * Returns the baseline anchor X around which sway oscillates.
     *
     * @return Anchor X coordinate.
     */
    public float getBaseX() { return baseX; }

    /**
     * Sets the baseline anchor X coordinate.
     *
     * @param baseX Anchor X in pixels.
     */
    public void setBaseX(float baseX) { this.baseX = baseX; }

    /**
     * Returns horizontal velocity in px/sec.
     *
     * @return Velocity X.
     */
    public float getVx() { return vx; }

    /**
     * Sets horizontal velocity in px/sec.
     *
     * @param vx Velocity X.
     */
    public void setVx(float vx) { this.vx = vx; }

    /**
     * Returns vertical descent velocity in px/sec.
     *
     * @return Velocity Y.
     */
    public float getVy() { return vy; }

    /**
     * Sets vertical descent velocity in px/sec.
     *
     * @param vy Velocity Y.
     */
    public void setVy(float vy) { this.vy = vy; }

    /**
     * Returns current angular tumble rotation in degrees.
     *
     * @return Rotation degrees.
     */
    public float getRotation() { return rotation; }

    /**
     * Sets angular tumble rotation in degrees.
     *
     * @param rotation Degrees.
     */
    public void setRotation(float rotation) { this.rotation = rotation; }

    /**
     * Returns bounding box square dimension (width/height) in pixels.
     *
     * @return Size in pixels.
     */
    public float getSize() { return size; }

    /**
     * Sets bounding box size in pixels.
     *
     * @param size Size in pixels.
     */
    public void setSize(float size) { this.size = size; }

    /**
     * Returns whether the player's finger is actively dragging this item.
     *
     * @return {@code true} if being dragged; {@code false} otherwise.
     */
    public boolean isBeingDragged() { return isBeingDragged; }

    /**
     * Sets the active touch-drag state.
     *
     * @param beingDragged Drag flag.
     */
    public void setBeingDragged(boolean beingDragged) { isBeingDragged = beingDragged; }

    /**
     * Returns whether the item is in ballistic flick flight.
     *
     * @return {@code true} if flicked; {@code false} otherwise.
     */
    public boolean isFlicked() { return isFlicked; }

    /**
     * Returns whether the item has completed its lifecycle (deposited, shooed, or missed).
     *
     * @return {@code true} if resolved; {@code false} if active.
     */
    public boolean isResolved() { return isResolved; }

    /**
     * Sets whether this item is resolved and eligible for pooling/removal.
     *
     * @param resolved Resolved flag.
     */
    public void setResolved(boolean resolved) { isResolved = resolved; }
}
