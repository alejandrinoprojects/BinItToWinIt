package com.example.binittowinit.model;

/**
 * Encapsulates the unique physical dynamics, aerodynamics, and environmental
 * reactions for individual items in "Bin It to Win It".
 * <p>
 * Rather than all items falling identically, each item is assigned an {@code ItemBehavior}
 * that specifies:
 * <ul>
 *   <li><b>Mass:</b> Determines resistance to flick impulses and gravitational pull.</li>
 *   <li><b>Fall Speed Factor:</b> Scales the descent velocity relative to the game's base speed.</li>
 *   <li><b>Wind Sensitivity:</b> Scales how strongly ambient wind gusts blow the item horizontally.</li>
 *   <li><b>Horizontal Sway:</b> Sine-wave oscillation simulating air turbulence (e.g. for light bags).</li>
 *   <li><b>Rotational Velocity:</b> Angular tumble rate while falling.</li>
 *   <li><b>Critter Identity:</b> Marks whether the entity is a living animal.</li>
 * </ul>
 * </p>
 */
public class ItemBehavior {
    /** Item mass affecting momentum, drag, and flick inertia. */
    private final float mass;

    /** Multiplier applied to baseline descent velocity (e.g., 0.6x for floaters, 1.5x for heavy objects). */
    private final float fallSpeedFactor;

    /** Fraction (0.0 to 1.0) describing susceptibility to wind gusts and breezes. */
    private final float windSensitivity;

    /** Angular frequency in radians per second for horizontal sine-wave swaying. */
    private final float swayFrequency;

    /** Maximum horizontal displacement in pixels from the item's anchor during sway. */
    private final float swayAmplitude;

    /** Continuous tumble speed in degrees per second. */
    private final float rotationSpeed;

    /** Indicates whether this item represents a living animal that should be shooed away. */
    private final boolean isCritter;

    /**
     * Constructs a customized physical behavior specification.
     *
     * @param mass            Relative mass of the item.
     * @param fallSpeedFactor Descent velocity multiplier.
     * @param windSensitivity Wind response coefficient between 0.0 (immune) and 1.0 (full effect).
     * @param swayFrequency   Sway oscillation speed in radians per second.
     * @param swayAmplitude   Sway distance peak in pixels.
     * @param rotationSpeed   Angular tumble speed in degrees per second.
     * @param isCritter       {@code true} if this entity is a wandering critter; {@code false} if garbage.
     */
    public ItemBehavior(float mass, float fallSpeedFactor, float windSensitivity,
                        float swayFrequency, float swayAmplitude, float rotationSpeed,
                        boolean isCritter) {
        this.mass = mass;
        this.fallSpeedFactor = fallSpeedFactor;
        this.windSensitivity = windSensitivity;
        this.swayFrequency = swayFrequency;
        this.swayAmplitude = swayAmplitude;
        this.rotationSpeed = rotationSpeed;
        this.isCritter = isCritter;
    }

    /**
     * Returns the relative mass of this item.
     *
     * @return Mass value.
     */
    public float getMass() {
        return mass;
    }

    /**
     * Returns the descent speed multiplier.
     *
     * @return Velocity scaling factor.
     */
    public float getFallSpeedFactor() {
        return fallSpeedFactor;
    }

    /**
     * Returns how susceptible this item is to wind currents (0.0 to 1.0).
     *
     * @return Wind sensitivity coefficient.
     */
    public float getWindSensitivity() {
        return windSensitivity;
    }

    /**
     * Returns the sine-wave oscillation frequency for horizontal sway.
     *
     * @return Sway frequency in rad/s.
     */
    public float getSwayFrequency() {
        return swayFrequency;
    }

    /**
     * Returns the maximum horizontal sway excursion in pixels.
     *
     * @return Sway amplitude.
     */
    public float getSwayAmplitude() {
        return swayAmplitude;
    }

    /**
     * Returns the angular tumble speed in degrees per second.
     *
     * @return Rotation speed.
     */
    public float getRotationSpeed() {
        return rotationSpeed;
    }

    /**
     * Returns whether this item represents a living animal.
     *
     * @return {@code true} if a critter; {@code false} otherwise.
     */
    public boolean isCritter() {
        return isCritter;
    }

    /**
     * Factory preset for ultra-light, aerodynamic items such as plastic bags, fallen leaves,
     * or candy wrappers that sail on breezes with prominent horizontal swaying.
     *
     * @param windSens Wind sensitivity coefficient (typically 0.8 - 1.0).
     * @param swayAmp  Sway distance peak in pixels.
     * @return Configured {@code ItemBehavior} instance.
     */
    public static ItemBehavior createFloaty(float windSens, float swayAmp) {
        return new ItemBehavior(0.3f, 0.65f, windSens, 3.2f, swayAmp, 30f, false);
    }

    /**
     * Factory preset for medium items such as banana peels, apple cores, soda cans, and boxes
     * that descend with moderate speed and balanced rotational tumbling.
     *
     * @param windSens Wind sensitivity coefficient (typically 0.15 - 0.45).
     * @return Configured {@code ItemBehavior} instance.
     */
    public static ItemBehavior createMediumTumbler(float windSens) {
        return new ItemBehavior(1.0f, 1.0f, windSens, 1.5f, 15f, 75f, false);
    }

    /**
     * Factory preset for dense, heavy items such as glass bottles and broken ceramic mugs
     * that plunge straight down unaffected by ambient wind.
     *
     * @return Configured {@code ItemBehavior} instance.
     */
    public static ItemBehavior createHeavyPlunge() {
        return new ItemBehavior(2.2f, 1.4f, 0.0f, 0.0f, 0f, 15f, false);
    }

    /**
     * Factory preset for living critters (kittens, puppies, birds, butterflies)
     * characterized by gentle movement, distinctive sway, and critter classification.
     *
     * @param windSens   Wind susceptibility.
     * @param fallFactor Speed factor.
     * @param swayAmp    Sway amplitude in pixels.
     * @return Configured {@code ItemBehavior} instance.
     */
    public static ItemBehavior createCritter(float windSens, float fallFactor, float swayAmp) {
        return new ItemBehavior(0.7f, fallFactor, windSens, 2.8f, swayAmp, 20f, true);
    }
}
