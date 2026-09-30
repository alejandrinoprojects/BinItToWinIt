package com.example.binittowinit.audio;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Isolated, zero-dependency mock sound manager for unit and integration testing.
 * <p>
 * Eliminates Android system audio track dependencies and tracks audio event invocations
 * to enable fast, headless verification of game scoring and penalties.
 * </p>
 */
public class MockSoundManager implements ISoundManager {
    private boolean soundEnabled = true;
    private float duckMultiplier = 1.0f;
    private boolean audioFocusActive = false;

    public final AtomicInteger successChimeCount = new AtomicInteger();
    public final AtomicInteger critterShooCount = new AtomicInteger();
    public final AtomicInteger errorBuzzCount = new AtomicInteger();
    public final AtomicInteger flickWhooshCount = new AtomicInteger();
    public final AtomicInteger gameOverCount = new AtomicInteger();

    @Override
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    @Override
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    @Override
    public void playSuccessChime() {
        if (soundEnabled) successChimeCount.incrementAndGet();
    }

    @Override
    public void playCritterShoo() {
        if (soundEnabled) critterShooCount.incrementAndGet();
    }

    @Override
    public void playErrorBuzz() {
        if (soundEnabled) errorBuzzCount.incrementAndGet();
    }

    @Override
    public void playFlickWhoosh() {
        if (soundEnabled) flickWhooshCount.incrementAndGet();
    }

    @Override
    public void playGameOver() {
        if (soundEnabled) gameOverCount.incrementAndGet();
    }

    @Override
    public void requestAudioFocus() {
        audioFocusActive = true;
        duckMultiplier = 1.0f;
    }

    @Override
    public void abandonAudioFocus() {
        audioFocusActive = false;
        duckMultiplier = 1.0f;
    }

    @Override
    public float getDuckMultiplier() {
        return duckMultiplier;
    }

    /**
     * Sets the volume ducking multiplier for testing audio focus changes.
     *
     * @param duckMultiplier Volume multiplier (0.0f - 1.0f).
     */
    public void setDuckMultiplier(float duckMultiplier) {
        this.duckMultiplier = duckMultiplier;
    }

    /**
     * Checks if audio focus is currently active.
     *
     * @return {@code true} if focus is acquired; {@code false} otherwise.
     */
    public boolean isAudioFocusActive() {
        return audioFocusActive;
    }

    /**
     * Resets all invocation counters back to zero.
     */
    public void resetCounts() {
        successChimeCount.set(0);
        critterShooCount.set(0);
        errorBuzzCount.set(0);
        flickWhooshCount.set(0);
        gameOverCount.set(0);
    }
}
