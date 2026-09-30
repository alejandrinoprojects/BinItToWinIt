package com.example.binittowinit.game;

import com.example.binittowinit.R;
import com.example.binittowinit.model.GarbageBin;
import com.example.binittowinit.model.WasteCatalog;
import com.example.binittowinit.model.WasteCategory;
import com.example.binittowinit.model.WasteItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Core game state manager and logic controller for "Bin It to Win It".
 * <p>
 * Responsibilities include:
 * <ul>
 *   <li>Tracking game lifecycle states ({@code READY}, {@code RUNNING}, {@code PAUSED}, {@code GAME_OVER}).</li>
 *   <li>Managing the spawning interval and progressive difficulty curve based on current score.</li>
 *   <li>Processing waste sorting into garbage bins and verifying category matches.</li>
 *   <li>Evaluating flick-away resolutions for wandering critters versus penalties for flicking waste.</li>
 *   <li>Handling player lives (starting with 3 hearts), streaks, and score multipliers (up to 3x).</li>
 *   <li>Generating floating visual popup indicators (e.g. "+100", "-1 ❤", "Shooed! +150").</li>
 *   <li>Notifying UI listeners of state transitions, score updates, and screen shake alerts.</li>
 * </ul>
 * </p>
 */
public class GameEngine {
    /**
     * Enumeration of top-level game execution states.
     */
    public enum State {
        READY, RUNNING, PAUSED, GAME_OVER
    }

    /**
     * Visual pop-up text entity that floats upwards and fades out on the Canvas.
     */
    public static class FloatingText {
        public String text;
        public float x, y;
        public int color;
        public float lifeTime;
        public float maxLifeTime;

        /**
         * Default constructor for entity pool instantiation.
         */
        public FloatingText() {}

        /**
         * Constructs a floating text banner entity.
         *
         * @param text     String message.
         * @param x        Initial X position.
         * @param y        Initial Y position.
         * @param color    ARGB color integer.
         * @param duration Total lifetime in seconds.
         */
        public FloatingText(String text, float x, float y, int color, float duration) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.lifeTime = duration;
            this.maxLifeTime = duration;
        }

        /**
         * Reinitializes a pooled floating text banner with new content.
         *
         * @param text     String message.
         * @param x        Initial X position.
         * @param y        Initial Y position.
         * @param color    ARGB color integer.
         * @param duration Total lifetime in seconds.
         */
        public void reinitialize(String text, float x, float y, int color, float duration) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.color = color;
            this.lifeTime = duration;
            this.maxLifeTime = duration;
        }

        /**
         * Resets entity properties when recycled back into the pool.
         */
        public void reset() {
            this.text = "";
            this.x = 0;
            this.y = 0;
            this.color = 0;
            this.lifeTime = 0;
            this.maxLifeTime = 0;
        }

        /**
         * Advances the floating text motion and updates lifetime countdown.
         *
         * @param deltaTime Elapsed frame time in seconds.
         * @return {@code true} if lifetime has expired; {@code false} otherwise.
         */
        public boolean update(float deltaTime) {
            y -= 45f * deltaTime; // Float gently upwards
            lifeTime -= deltaTime;
            return lifeTime <= 0;
        }

        /**
         * Computes the normalized opacity alpha based on remaining lifetime.
         *
         * @return Alpha multiplier between 0.0 and 1.0.
         */
        public float getAlpha() {
            return Math.max(0f, Math.min(1f, lifeTime / maxLifeTime));
        }
    }

    /**
     * Callback interface dispatched to the host Activity for real-time HUD and audio synchronization.
     */
    public interface GameEventListener {
        /**
         * Triggered whenever the player's score, streak, or multiplier changes.
         *
         * @param score      Updated total score.
         * @param streak     Current count of consecutive correct actions.
         * @param multiplier Active score multiplier (1x, 2x, or 3x).
         */
        void onScoreChanged(int score, int streak, int multiplier);

        /**
         * Triggered when the player's remaining lives change.
         *
         * @param lives Current heart count (0 to 3).
         */
        void onLivesChanged(int lives);

        /**
         * Triggered when environmental wind status transitions between calm and gusting.
         *
         * @param isActive       {@code true} if wind is actively blowing; {@code false} if calm.
         * @param isBlowingRight {@code true} if wind blows to the right; {@code false} if left.
         */
        void onWindStatusChanged(boolean isActive, boolean isBlowingRight);

        /**
         * Triggered when lives reach zero and the game ends.
         *
         * @param finalScore      Final accumulated score.
         * @param itemsSorted     Total waste items correctly deposited into bins.
         * @param crittersRescued Total wandering critters shooed to safety.
         */
        void onGameOver(int finalScore, int itemsSorted, int crittersRescued);

        /**
         * Triggered on mistakes to request tactile screen-shake and red vignette feedback.
         */
        void onScreenShake();

        /**
         * Triggered when the player reaches a higher level milestone.
         *
         * @param newLevel The newly unlocked level (1, 2, 3+).
         */
        void onLevelChanged(int newLevel);

        /**
         * Triggered on mis-sorts or missed objects to provide informative educational classification feedback.
         *
         * @param itemName      Display name of the misclassified item.
         * @param category      Actual category the item belongs to.
         * @param tipMessage    Helpful educational tip explaining proper sorting.
         * @param drawableResId Android drawable resource ID for item icon.
         */
        void onEducationalFeedback(String itemName, WasteCategory category, String tipMessage, int drawableResId);

        /**
         * Triggered when a correct item is deposited or auto-caught in a matching bin.
         *
         * @param isAutoCatch {@code true} if automatically caught without dragging.
         */
        void onCorrectSort(boolean isAutoCatch);

        /**
         * Triggered when an incorrect sort, missed item, or invalid disposal occurs.
         */
        void onMistake();

        /**
         * Triggered when a wandering critter is successfully shooed to safety.
         */
        void onCritterRescued();
    }

    private final Random random = new Random();
    private final String playerName;
    private final GameEventListener listener;
    private final EnvironmentManager environmentManager;

    public static final int DEFAULT_LIVES = 5;

    private State state = State.READY;
    private int score = 0;
    private int streak = 0;
    private int lives = DEFAULT_LIVES;
    private int itemsSorted = 0;
    private int crittersRescued = 0;

    private int currentLevel = 1;

    private float spawnTimer = 0f;
    private final List<WasteItem> activeItems = new ArrayList<>();
    private final List<GarbageBin> bins = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();

    // --- Entity Pools (Zero-Allocation Game Loop) ---
    private final com.example.binittowinit.pool.EntityPool<WasteItem> wasteItemPool =
            new com.example.binittowinit.pool.EntityPool<>(WasteItem::new, WasteItem::reset, 25);
    private final com.example.binittowinit.pool.EntityPool<FloatingText> floatingTextPool =
            new com.example.binittowinit.pool.EntityPool<>(FloatingText::new, FloatingText::reset, 30);
    private final com.example.binittowinit.audio.ISoundManager soundManager;

    private int screenWidth = 1080;
    private int screenHeight = 1920;

    /**
     * Constructs a new GameEngine instance.
     *
     * @param playerName Player identifier for records and HUD display.
     * @param listener   Event listener for HUD updates and game over triggers.
     */
    public GameEngine(String playerName, GameEventListener listener) {
        this(playerName, listener, null);
    }

    /**
     * Constructs a new GameEngine instance with injected audio manager.
     *
     * @param playerName   Player identifier.
     * @param listener     Event listener.
     * @param soundManager Injected audio manager.
     */
    public GameEngine(String playerName, GameEventListener listener, com.example.binittowinit.audio.ISoundManager soundManager) {
        this.playerName = playerName;
        this.listener = listener;
        this.soundManager = soundManager;
        this.environmentManager = new EnvironmentManager();

        initBins();
    }

    /**
     * Initializes the 3 municipal sorting garbage bins with their respective categories and icons.
     */
    private void initBins() {
        bins.clear();
        bins.add(new GarbageBin(WasteCategory.BIODEGRADABLE, "Biodegradable", 0xFF2E7D32, R.drawable.bin_biodegradable));
        bins.add(new GarbageBin(WasteCategory.RECYCLABLE, "Recyclable", 0xFF1565C0, R.drawable.bin_recyclable));
        bins.add(new GarbageBin(WasteCategory.NON_BIODEGRADABLE, "Non-Biodegradable", 0xFFE65100, R.drawable.bin_non_biodegradable));
    }

    /**
     * Sets display screen dimensions and aligns the 3 garbage bins along the bottom edge.
     *
     * @param width  Screen width in pixels.
     * @param height Screen height in pixels.
     */
    public void setScreenDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;

        // Widen and space out the 3 bins to optimize finger-touch tap/drag targets
        float binWidth = width * 0.30f;
        float binHeight = binWidth * 1.28f;
        float spacing = (width - (binWidth * 3)) / 4f;
        float bottomY = height - Math.max(height * 0.045f, 100f);
        float topY = bottomY - binHeight;

        for (int i = 0; i < bins.size(); i++) {
            float leftX = spacing + i * (binWidth + spacing);
            float rightX = leftX + binWidth;
            bins.get(i).updateBounds(leftX, topY, rightX, bottomY);
        }
    }

    /**
     * Starts or restarts a new game session, resetting scores, lives (10), streaks, and lists.
     */
    public void start() {
        state = State.RUNNING;
        score = 0;
        streak = 0;
        lives = DEFAULT_LIVES;
        itemsSorted = 0;
        crittersRescued = 0;
        currentLevel = 1;
        for (WasteItem item : activeItems) {
            wasteItemPool.recycle(item);
        }
        activeItems.clear();
        for (FloatingText ft : floatingTexts) {
            floatingTextPool.recycle(ft);
        }
        floatingTexts.clear();
        spawnTimer = 0.5f; // Fast initial spawn for immediate engagement

        notifyScore();
        notifyLives();
        environmentManager.resetWindCooldown(currentLevel);
        if (listener != null) {
            listener.onLevelChanged(currentLevel);
        }
    }

    /**
     * Pauses the game loop and freezes item physics.
     */
    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
        }
    }

    /**
     * Resumes a paused game session.
     */
    public void resume() {
        if (state == State.PAUSED) {
            state = State.RUNNING;
        }
    }

    /**
     * Primary frame update method executing game logic, item motion, spawning, and collision.
     *
     * @param deltaTime Elapsed frame time in seconds.
     */
    public void update(float deltaTime) {
        if (state != State.RUNNING) return;

        // 1. Update dynamic environmental weather (wind)
        boolean prevWind = environmentManager.isWindActive();
        environmentManager.update(deltaTime, screenWidth, screenHeight, currentLevel);
        if (prevWind != environmentManager.isWindActive()) {
            if (listener != null) {
                listener.onWindStatusChanged(environmentManager.isWindActive(), environmentManager.isBlowingRight());
            }
        }

        // 2. Update bin catch/bounce animations
        for (GarbageBin bin : bins) {
            bin.updateAnimation(deltaTime);
        }

        // 3. Spawner countdown and instantiation
        spawnTimer -= deltaTime;
        if (spawnTimer <= 0) {
            spawnItem();
            spawnTimer = calculateSpawnInterval();
        }

        // 4. Update motion of falling and flicked items
        float currentWind = environmentManager.getCurrentWindForce();
        Iterator<WasteItem> iterator = activeItems.iterator();
        while (iterator.hasNext()) {
            WasteItem item = iterator.next();
            item.update(deltaTime, currentWind, screenWidth, screenHeight);

            // Check flicked item resolution when exiting screen boundaries
            if (item.isFlicked()) {
                if (item.getY() < -item.getSize() ||
                    item.getX() < -item.getSize() ||
                    item.getX() > screenWidth + item.getSize()) {
                    resolveFlickedItem(item);
                    iterator.remove();
                    wasteItemPool.recycle(item);
                    continue;
                }
            }

            // AUTO-CATCH: Check if falling item enters any bin's opening
            if (!item.isBeingDragged() && !item.isFlicked()) {
                GarbageBin autoCatchBin = null;
                for (GarbageBin bin : bins) {
                    if (bin.contains(item.getX(), item.getY())) {
                        autoCatchBin = bin;
                        break;
                    }
                }

                if (autoCatchBin != null) {
                    handleAutoCatch(item, autoCatchBin);
                    iterator.remove();
                    wasteItemPool.recycle(item);
                    continue;
                }
            }

            // Check bottom boundary exit for missed items that fell outside or between bins
            if (!item.isBeingDragged() && !item.isFlicked() && item.getY() > screenHeight - 40f) {
                resolveMissedItem(item);
                iterator.remove();
                wasteItemPool.recycle(item);
            }
        }

        // 5. Update floating score text popups
        Iterator<FloatingText> textIterator = floatingTexts.iterator();
        while (textIterator.hasNext()) {
            FloatingText text = textIterator.next();
            if (text.update(deltaTime)) {
                textIterator.remove();
                floatingTextPool.recycle(text);
            }
        }
    }

    /**
     * Spawns a randomized waste item or wandering critter above the screen viewport.
     */
    private void spawnItem() {
        int maxConcurrent = currentLevel == 1 ? 2 : (currentLevel == 2 ? 3 : 4);
        if (activeItems.size() >= maxConcurrent) {
            return;
        }

        WasteItem template;
        if (currentLevel == 1) {
            template = WasteCatalog.getRandomWasteOnly(random);
        } else {
            template = WasteCatalog.getRandomTemplate(random);
        }

        float itemSize = Math.max(120f, screenWidth * 0.16f);
        float spawnX = itemSize + random.nextFloat() * (screenWidth - itemSize * 2f);
        float spawnY = -itemSize;
        float baseFallSpeed = calculateFallSpeed();

        WasteItem spawned = wasteItemPool.obtain();
        spawned.reinitialize(template, spawnX, spawnY, itemSize, baseFallSpeed);
        activeItems.add(spawned);
    }

    /**
     * Computes the spawn interval according to current level progression.
     *
     * @return Interval in seconds.
     */
    private float calculateSpawnInterval() {
        if (currentLevel == 1) {
            return 2.2f;
        } else if (currentLevel == 2) {
            return 1.6f;
        } else {
            return Math.max(0.85f, 1.2f - ((currentLevel - 3) * 0.1f));
        }
    }

    /**
     * Computes the baseline fall velocity according to current level progression.
     *
     * @return Baseline fall speed in px/sec.
     */
    private float calculateFallSpeed() {
        if (currentLevel == 1) {
            return 200f;
        } else if (currentLevel == 2) {
            return 270f;
        } else {
            return Math.min(480f, 340f + ((currentLevel - 3) * 35f));
        }
    }

    private void checkLevelProgression() {
        int newLevel = 1 + (score / 2500);
        if (newLevel != currentLevel) {
            currentLevel = newLevel;
            if (listener != null) {
                listener.onLevelChanged(currentLevel);
            }
        }
    }

    /**
     * Calculates the active score multiplier based on consecutive correct streaks.
     *
     * @return 3 for streak >= 8, 2 for streak >= 3, 1 otherwise.
     */
    public int getMultiplier() {
        if (streak >= 8) return 3;
        if (streak >= 3) return 2;
        return 1;
    }

    /**
     * Handles dropping a dragged item into a target garbage bin.
     *
     * @param item The item being deposited.
     * @param bin  The target bin receiving the item.
     */
    public void handleDropOnBin(WasteItem item, GarbageBin bin) {
        if (state != State.RUNNING || item == null || bin == null) return;
        item.setResolved(true);
        activeItems.remove(item);
        bin.triggerCatchAnimation();

        if (item.getCategory() == WasteCategory.CRITTER_FLICK_AWAY) {
            // Error: Never throw living animals into garbage bins!
            penalizeLife("Don't Bin Critters! -1 ❤", item.getX(), item.getY());
            dispatchEducationalTip(item);
            wasteItemPool.recycle(item);
            return;
        }

        if (item.getCategory() == bin.getCategory()) {
            // Correct bin sort!
            int mult = getMultiplier();
            int pts = item.getPointsAwarded() * mult;
            score += pts;
            streak++;
            itemsSorted++;
            addFloatingText("+" + pts + (mult > 1 ? " (" + mult + "x)" : ""), item.getX(), item.getY(), 0xFF00C853);
            checkLevelProgression();
            notifyScore();
            if (listener != null) {
                listener.onCorrectSort(false);
            }
        } else {
            // Incorrect bin sort!
            penalizeLife("Wrong Bin! -1 ❤", item.getX(), item.getY());
            dispatchEducationalTip(item);
        }
        wasteItemPool.recycle(item);
    }

    /**
     * Automatically processes a falling item that naturally landed directly into a garbage bin's opening.
     *
     * @param item The falling waste item or critter.
     * @param bin  The bin that caught the item.
     */
    public void handleAutoCatch(WasteItem item, GarbageBin bin) {
        if (state != State.RUNNING || item == null || bin == null) return;
        item.setResolved(true);
        bin.triggerCatchAnimation();

        if (item.getCategory() == WasteCategory.CRITTER_FLICK_AWAY) {
            // Error: Living animals must never fall into trash bins!
            penalizeLife("Critter Fell In! -1 ❤", item.getX(), item.getY());
            dispatchEducationalTip(item);
            return;
        }

        if (item.getCategory() == bin.getCategory()) {
            // Correct bin auto-catch!
            int mult = getMultiplier();
            int pts = item.getPointsAwarded() * mult;
            score += pts;
            streak++;
            itemsSorted++;
            addFloatingText("Auto Catch! +" + pts + (mult > 1 ? " (" + mult + "x)" : ""), item.getX(), item.getY(), 0xFF00C853);
            checkLevelProgression();
            notifyScore();
            if (listener != null) {
                listener.onCorrectSort(true);
            }
        } else {
            // Wrong bin auto-catch!
            penalizeLife("Wrong Bin Catch! -1 ❤", item.getX(), item.getY());
            dispatchEducationalTip(item);
        }
    }

    /**
     * Resolves an item that was flicked off-screen.
     *
     * @param item The flicked item.
     */
    private void resolveFlickedItem(WasteItem item) {
        item.setResolved(true);
        if (item.getCategory() == WasteCategory.CRITTER_FLICK_AWAY) {
            // Successfully shooed animal safely to freedom!
            int mult = getMultiplier();
            int pts = item.getPointsAwarded() * mult;
            score += pts;
            streak++;
            crittersRescued++;
            addFloatingText("Shooed! +" + pts, item.getX(), item.getY(), 0xFFFF6D00);
            notifyScore();
            if (listener != null) {
                listener.onCritterRescued();
            }
        } else {
            // Penalize flicking actual trash away (littering!)
            penalizeLife("Littering! -1 ❤", item.getX(), item.getY());
            dispatchEducationalTip(item);
        }
    }

    /**
     * Resolves an item that fell past the bottom of the screen without being sorted or shooed.
     *
     * @param item The missed item.
     */
    private void resolveMissedItem(WasteItem item) {
        item.setResolved(true);
        if (item.getCategory() == WasteCategory.CRITTER_FLICK_AWAY) {
            penalizeLife("Critter Missed! -1 ❤", item.getX(), screenHeight - 120f);
            dispatchEducationalTip(item);
        } else {
            penalizeLife("Missed Trash! -1 ❤", item.getX(), screenHeight - 120f);
            dispatchEducationalTip(item);
        }
    }

    /**
     * Applies a mistake penalty: resets streak, decrements one life, triggers screen shake,
     * and ends the game if lives reach zero.
     *
     * @param reason Text to display on floating banner.
     * @param atX    Horizontal coordinate for floating text.
     * @param atY    Vertical coordinate for floating text.
     */
    private void penalizeLife(String reason, float atX, float atY) {
        streak = 0;
        lives--;
        addFloatingText(reason, atX, atY, 0xFFD50000);
        notifyScore();
        notifyLives();

        if (listener != null) {
            listener.onMistake();
            listener.onScreenShake();
        }

        if (lives <= 0) {
            state = State.GAME_OVER;
            if (listener != null) {
                listener.onGameOver(score, itemsSorted, crittersRescued);
            }
        }
    }

    /**
     * Adds an animated floating text indicator to the active scene.
     *
     * @param text  Banner string.
     * @param x     Screen X.
     * @param y     Screen Y.
     * @param color ARGB color code.
     */
    public void addFloatingText(String text, float x, float y, int color) {
        FloatingText ft = floatingTextPool.obtain();
        ft.reinitialize(text, x, y, color, 1.2f);
        floatingTexts.add(ft);
    }

    private void notifyScore() {
        if (listener != null) {
            listener.onScoreChanged(score, streak, getMultiplier());
        }
    }

    private void notifyLives() {
        if (listener != null) {
            listener.onLivesChanged(lives);
        }
    }

    private void dispatchEducationalTip(WasteItem item) {
        if (listener != null && item != null) {
            listener.onEducationalFeedback(item.getName(), item.getCategory(), item.getEducationalTip(), item.getDrawableResId());
        }
    }

    // --- State & Metrics Accessors ---

    /**
     * Returns the current engine execution state.
     *
     * @return Current State (READY, RUNNING, PAUSED, GAME_OVER).
     */
    public State getState() { return state; }

    /**
     * Returns the player handle.
     *
     * @return Player name string.
     */
    public String getPlayerName() { return playerName; }

    /**
     * Returns the current accumulated score.
     *
     * @return Current score.
     */
    public int getScore() { return score; }

    /**
     * Returns the current count of consecutive successful actions.
     *
     * @return Streak count.
     */
    public int getStreak() { return streak; }

    /**
     * Returns the remaining player lives.
     *
     * @return Hearts remaining.
     */
    public int getLives() { return lives; }

    /**
     * Returns the current gameplay level.
     *
     * @return Active level (1+).
     */
    public int getCurrentLevel() { return currentLevel; }

    /**
     * Returns the count of waste items sorted into bins.
     *
     * @return Total items sorted.
     */
    public int getItemsSorted() { return itemsSorted; }

    /**
     * Returns the count of critters safely flicked to safety.
     *
     * @return Total critters rescued.
     */
    public int getCrittersRescued() { return crittersRescued; }

    /**
     * Returns the active list of waste items and critters in play.
     *
     * @return Active items list.
     */
    public List<WasteItem> getActiveItems() { return activeItems; }

    /**
     * Returns the list of three municipal sorting bins.
     *
     * @return Garbage bins list.
     */
    public List<GarbageBin> getBins() { return bins; }

    /**
     * Returns the active floating text banner popups.
     *
     * @return Floating text entities list.
     */
    public List<FloatingText> getFloatingTexts() { return floatingTexts; }

    /**
     * Returns the environmental weather and wind manager.
     *
     * @return EnvironmentManager instance.
     */
    public EnvironmentManager getEnvironmentManager() { return environmentManager; }

    /**
     * Returns the zero-allocation pool for waste items.
     *
     * @return WasteItem EntityPool.
     */
    public com.example.binittowinit.pool.EntityPool<WasteItem> getWasteItemPool() { return wasteItemPool; }

    /**
     * Returns the zero-allocation pool for floating text banners.
     *
     * @return FloatingText EntityPool.
     */
    public com.example.binittowinit.pool.EntityPool<FloatingText> getFloatingTextPool() { return floatingTextPool; }

    /**
     * Returns the injected audio manager.
     *
     * @return ISoundManager instance.
     */
    public com.example.binittowinit.audio.ISoundManager getSoundManager() { return soundManager; }
}
