package com.example.binittowinit.game;

import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages dynamic environmental weather events during gameplay, primarily wind gusts.
 * <p>
 * Periodically triggers gusts of wind with randomized direction (left or right),
 * duration, and velocity. During an active wind event, the manager:
 * <ul>
 *   <li>Supplies horizontal wind force to the game loop to blow light objects sideways.</li>
 *   <li>Renders translucent cyan wind streak particles across the canvas.</li>
 *   <li>Notifies the HUD to display visual wind indicator arrows.</li>
 * </ul>
 * </p>
 */
public class EnvironmentManager {
    /**
     * Data holder representing a single visual breeze streak drawn on the Canvas.
     */
    public static class WindStreak {
        /** Leading edge X position in pixels. */
        float x;

        /** Fixed vertical line coordinate. */
        float y;

        /** Horizontal traversal speed in pixels per second. */
        float speed;

        /** Visual tail length in pixels. */
        float length;

        /** Opacity alpha (0-255). */
        int alpha;

        /**
         * Default constructor for entity pooling allocation.
         */
        public WindStreak() {}

        /**
         * Constructs a wind streak particle.
         *
         * @param x      Initial X position.
         * @param y      Initial Y position.
         * @param speed  Signed velocity in px/sec.
         * @param length Tail length.
         * @param alpha  Alpha opacity.
         */
        public WindStreak(float x, float y, float speed, float length, int alpha) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.length = length;
            this.alpha = alpha;
        }

        /**
         * Reinitializes a pooled wind streak particle with fresh trajectory properties.
         *
         * @param x      Initial X position.
         * @param y      Initial Y position.
         * @param speed  Signed velocity in px/sec.
         * @param length Tail length.
         * @param alpha  Alpha opacity.
         */
        public void reinitialize(float x, float y, float speed, float length, int alpha) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.length = length;
            this.alpha = alpha;
        }

        /**
         * Resets all particle properties back to zero when recycled into the pool.
         */
        public void reset() {
            this.x = 0;
            this.y = 0;
            this.speed = 0;
            this.length = 0;
            this.alpha = 0;
        }
    }

    private final Random random = new Random();
    private final Paint windPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final com.example.binittowinit.pool.EntityPool<WindStreak> streakPool =
            new com.example.binittowinit.pool.EntityPool<>(WindStreak::new, WindStreak::reset, 20);

    /** Countdown timer in seconds until the next random wind gust triggers. */
    private float nextWindTimer;

    /** Remaining duration in seconds of the currently active wind gust. */
    private float windDurationTimer;

    /** Flag indicating whether a wind event is currently blowing. */
    private boolean isWindActive;

    /** Active horizontal wind force in pixels per second (+ for rightward, - for leftward). */
    private float currentWindForce;

    /** Active collection of animated breeze lines. */
    private final List<WindStreak> streaks = new ArrayList<>();

    /**
     * Initializes the environment manager and configures wind particle paint styles.
     */
    public EnvironmentManager() {
        windPaint.setColor(0x8800BCD4);
        windPaint.setStrokeWidth(3f);
        windPaint.setStrokeCap(Paint.Cap.ROUND);
        resetWindCooldown();
    }

    /**
     * Resets wind state to calm and schedules the next gust 8 to 15 seconds in the future.
     */
    private void resetWindCooldown() {
        isWindActive = false;
        currentWindForce = 0f;
        for (WindStreak streak : streaks) {
            streakPool.recycle(streak);
        }
        streaks.clear();
        nextWindTimer = 8f + random.nextFloat() * 7f;
    }

    /**
     * Triggers a new wind gust with randomized strength, direction, and visual particles.
     */
    private void triggerWindGust() {
        isWindActive = true;
        windDurationTimer = 4f + random.nextFloat() * 3f; // Gust lasts 4 to 7 seconds

        // Choose random direction: left (-1) or right (+1)
        float direction = random.nextBoolean() ? 1f : -1f;
        float speed = 100f + random.nextFloat() * 80f; // 100 to 180 px/sec
        currentWindForce = direction * speed;

        // Populate wind visual breeze lines from entity pool
        for (WindStreak streak : streaks) {
            streakPool.recycle(streak);
        }
        streaks.clear();
        for (int i = 0; i < 14; i++) {
            float sx = random.nextFloat() * 1000f;
            float sy = random.nextFloat() * 1400f;
            float slen = 50f + random.nextFloat() * 80f;
            float sspeed = (Math.abs(currentWindForce) * 2.5f) * (0.8f + random.nextFloat() * 0.4f);
            WindStreak streak = streakPool.obtain();
            streak.reinitialize(sx, sy, sspeed * direction, slen, 90 + random.nextInt(100));
            streaks.add(streak);
        }
    }

    /**
     * Updates wind countdown timers and advances particle positions.
     *
     * @param deltaTime    Elapsed frame time in seconds.
     * @param screenWidth  Screen width in pixels.
     * @param screenHeight Screen height in pixels.
     */
    public void update(float deltaTime, int screenWidth, int screenHeight) {
        if (!isWindActive) {
            nextWindTimer -= deltaTime;
            if (nextWindTimer <= 0) {
                triggerWindGust();
            }
        } else {
            windDurationTimer -= deltaTime;
            if (windDurationTimer <= 0) {
                resetWindCooldown();
            } else {
                // Advance visual streak positions
                for (WindStreak streak : streaks) {
                    streak.x += streak.speed * deltaTime;
                    if (streak.speed > 0 && streak.x > screenWidth + streak.length) {
                        streak.x = -streak.length;
                        streak.y = random.nextFloat() * (screenHeight * 0.75f);
                    } else if (streak.speed < 0 && streak.x < -streak.length) {
                        streak.x = screenWidth + streak.length;
                        streak.y = random.nextFloat() * (screenHeight * 0.75f);
                    }
                }
            }
        }
    }

    /**
     * Renders translucent breeze lines across the Canvas when wind is active.
     *
     * @param canvas Hardware-accelerated canvas.
     */
    public void render(Canvas canvas) {
        if (!isWindActive) return;

        for (WindStreak streak : streaks) {
            windPaint.setAlpha(streak.alpha);
            float tailX = streak.x - (streak.speed > 0 ? streak.length : -streak.length);
            canvas.drawLine(tailX, streak.y, streak.x, streak.y, windPaint);
        }
    }

    /**
     * Returns the active signed horizontal wind velocity in pixels per second.
     *
     * @return Positive for rightward wind, negative for leftward wind, 0.0 when calm.
     */
    public float getCurrentWindForce() {
        return currentWindForce;
    }

    /**
     * Returns whether a wind gust event is currently active.
     *
     * @return {@code true} if windy; {@code false} if calm.
     */
    public boolean isWindActive() {
        return isWindActive;
    }

    /**
     * Returns whether the active wind is blowing towards the right screen edge.
     *
     * @return {@code true} if blowing right; {@code false} if blowing left.
     */
    public boolean isBlowingRight() {
        return currentWindForce > 0;
    }
}
