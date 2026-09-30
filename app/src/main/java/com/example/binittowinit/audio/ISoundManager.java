package com.example.binittowinit.audio;

/**
 * Interface contract defining audio playback, mute controls, and lifecycle audio focus management.
 * <p>
 * Decouples the audio subsystem to enable constructor dependency injection and test isolation
 * with mock audio managers.
 * </p>
 */
public interface ISoundManager {
    /**
     * Checks if sound effects are enabled.
     *
     * @return {@code true} if audio is active; {@code false} if muted.
     */
    boolean isSoundEnabled();

    /**
     * Sets sound effects enabled or disabled.
     *
     * @param enabled {@code true} to enable audio playback; {@code false} to mute.
     */
    void setSoundEnabled(boolean enabled);

    /**
     * Plays the success chime when an item is correctly sorted.
     */
    void playSuccessChime();

    /**
     * Plays the cheerful chirp when a critter is shooed to safety.
     */
    void playCritterShoo();

    /**
     * Plays the abrasive error buzz on a mistake or missed item.
     */
    void playErrorBuzz();

    /**
     * Plays the aerodynamic flick whoosh sound.
     */
    void playFlickWhoosh();

    /**
     * Plays the melancholic game over cadence.
     */
    void playGameOver();

    /**
     * Requests transient audio focus allowing ducking from Android's AudioManager.
     */
    void requestAudioFocus();

    /**
     * Abandons audio focus upon activity pause or exit.
     */
    void abandonAudioFocus();

    /**
     * Returns the current volume ducking multiplier (0.0f - 1.0f).
     *
     * @return Ducking volume multiplier.
     */
    float getDuckMultiplier();
}
