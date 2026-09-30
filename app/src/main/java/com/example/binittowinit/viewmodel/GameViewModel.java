package com.example.binittowinit.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.binittowinit.audio.ISoundManager;
import com.example.binittowinit.database.ScoreRepository;
import com.example.binittowinit.game.GameEngine;
import com.example.binittowinit.model.WasteCategory;

/**
 * AndroidX {@link ViewModel} hosting the game controller and reactive state streams.
 * <p>
 * Decouples the game engine and physics state from the Android Activity lifecycle.
 * Completely eliminates state loss during configuration changes (screen rotations,
 * split screen, folding displays) and handles audio focus transitions cleanly.
 * </p>
 */
public class GameViewModel extends ViewModel implements GameEngine.GameEventListener {

    // --- State Value Objects ---

    /**
     * Immutable value object capturing the player's score, current streak, and combo multiplier.
     */
    public static class ScoreState {
        /** Total accumulated match score. */
        public final int score;
        /** Consecutive count of correct sorting actions. */
        public final int streak;
        /** Active combo multiplier (1x, 2x, or 3x). */
        public final int multiplier;

        /**
         * Constructs a new ScoreState instance.
         *
         * @param score      Total score points.
         * @param streak     Current consecutive streak.
         * @param multiplier Score multiplier factor.
         */
        public ScoreState(int score, int streak, int multiplier) {
            this.score = score;
            this.streak = streak;
            this.multiplier = multiplier;
        }
    }

    /**
     * Immutable value object representing environmental wind conditions.
     */
    public static class WindState {
        /** Whether wind is actively blowing across the playfield. */
        public final boolean isActive;
        /** Direction indicator: {@code true} for rightward wind, {@code false} for leftward. */
        public final boolean isBlowingRight;

        /**
         * Constructs a new WindState instance.
         *
         * @param isActive       Whether wind is blowing.
         * @param isBlowingRight Direction flag.
         */
        public WindState(boolean isActive, boolean isBlowingRight) {
            this.isActive = isActive;
            this.isBlowingRight = isBlowingRight;
        }
    }

    /**
     * Data holder representing an educational sorting tip dispatched on mistakes or misses.
     */
    public static class EducationalTip {
        /** Display name of the item. */
        public final String itemName;
        /** Correct waste classification category. */
        public final WasteCategory category;
        /** Educational explanation of proper disposal. */
        public final String tipMessage;
        /** Vector drawable icon resource ID for visual presentation. */
        public final int drawableResId;

        /**
         * Constructs a new EducationalTip instance.
         *
         * @param itemName      Item display name.
         * @param category      Correct waste category.
         * @param tipMessage    Informative tip message.
         * @param drawableResId Drawable icon resource ID.
         */
        public EducationalTip(String itemName, WasteCategory category, String tipMessage, int drawableResId) {
            this.itemName = itemName;
            this.category = category;
            this.tipMessage = tipMessage;
            this.drawableResId = drawableResId;
        }
    }

    /**
     * Event data dispatched upon match conclusion.
     */
    public static class GameOverEvent {
        /** Final match score achieved. */
        public final int score;
        /** Total waste items successfully sorted into bins. */
        public final int itemsSorted;
        /** Total wandering critters safely shooed off-screen. */
        public final int crittersRescued;

        /**
         * Constructs a new GameOverEvent payload.
         *
         * @param score           Final score points.
         * @param itemsSorted     Count of waste items sorted.
         * @param crittersRescued Count of critters rescued.
         */
        public GameOverEvent(int score, int itemsSorted, int crittersRescued) {
            this.score = score;
            this.itemsSorted = itemsSorted;
            this.crittersRescued = crittersRescued;
        }
    }

    // --- Reactive LiveData Streams ---

    private final MutableLiveData<ScoreState> scoreState = new MutableLiveData<>();
    private final MutableLiveData<Integer> lives = new MutableLiveData<>();
    private final MutableLiveData<Integer> level = new MutableLiveData<>();
    private final MutableLiveData<WindState> windState = new MutableLiveData<>();
    private final MutableLiveData<EducationalTip> educationalTip = new MutableLiveData<>();
    private final MutableLiveData<GameOverEvent> gameOverEvent = new MutableLiveData<>();
    private final MutableLiveData<Boolean> screenShakeEvent = new MutableLiveData<>();
    private final MutableLiveData<Boolean> correctSortEvent = new MutableLiveData<>();
    private final MutableLiveData<Boolean> critterRescuedEvent = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mistakeEvent = new MutableLiveData<>();

    private GameEngine engine;
    private ScoreRepository scoreRepository;
    private ISoundManager soundManager;

    /**
     * Default zero-argument constructor for AndroidX {@code ViewModelProvider}.
     */
    public GameViewModel() {
    }

    /**
     * Testing constructor allowing direct injection of repository and audio dependencies.
     *
     * @param scoreRepository Database repository for saving scores.
     * @param soundManager    Audio subsystem manager.
     */
    public GameViewModel(ScoreRepository scoreRepository, ISoundManager soundManager) {
        this.scoreRepository = scoreRepository;
        this.soundManager = soundManager;
    }

    /**
     * Injects dependencies if not already provided via constructor.
     *
     * @param scoreRepository Database repository.
     * @param soundManager    Sound manager.
     */
    public void setDependencies(ScoreRepository scoreRepository, ISoundManager soundManager) {
        if (this.scoreRepository == null) this.scoreRepository = scoreRepository;
        if (this.soundManager == null) this.soundManager = soundManager;
    }

    /**
     * Initializes or recovers the GameEngine instance.
     * If the ViewModel already retained an engine across configuration change,
     * it simply re-emits the current game state to update the newly attached Activity.
     *
     * @param playerName Player username.
     */
    public void initGame(String playerName) {
        if (engine == null) {
            engine = new GameEngine(playerName, this, soundManager);
            engine.start();
        } else {
            // Re-emit existing state for re-attached Activity
            scoreState.setValue(new ScoreState(engine.getScore(), engine.getStreak(), engine.getMultiplier()));
            lives.setValue(engine.getLives());
            level.setValue(engine.getCurrentLevel());
            windState.setValue(new WindState(
                    engine.getEnvironmentManager().isWindActive(),
                    engine.getEnvironmentManager().isBlowingRight()));
        }
    }

    /**
     * Returns the underlying {@link GameEngine} controller.
     *
     * @return Active GameEngine instance.
     */
    public GameEngine getEngine() {
        return engine;
    }

    /**
     * Pauses the game engine and abandons transient audio focus.
     */
    public void pauseGame() {
        if (engine != null) {
            engine.pause();
        }
        if (soundManager != null) {
            soundManager.abandonAudioFocus();
        }
    }

    /**
     * Resumes the game engine and requests audio focus from Android AudioManager.
     */
    public void resumeGame() {
        if (engine != null) {
            engine.resume();
        }
        if (soundManager != null) {
            soundManager.requestAudioFocus();
        }
    }

    /**
     * Restarts a fresh game session, resetting scores, lives, and active entities.
     */
    public void restartGame() {
        if (engine != null) {
            engine.start();
        }
    }

    /**
     * Persists final match statistics into the database repository.
     *
     * @param playerName      Name of the player.
     * @param score           Final score achieved.
     * @param itemsSorted     Number of waste items correctly deposited.
     * @param crittersRescued Number of wandering critters shooed to safety.
     */
    public void saveMatchScore(String playerName, int score, int itemsSorted, int crittersRescued) {
        if (scoreRepository != null) {
            scoreRepository.insertScore(playerName, score, itemsSorted, crittersRescued);
        }
    }

    // --- GameEventListener Implementation ---

    @Override
    public void onScoreChanged(int score, int streak, int multiplier) {
        scoreState.postValue(new ScoreState(score, streak, multiplier));
    }

    @Override
    public void onLivesChanged(int lives) {
        this.lives.postValue(lives);
    }

    @Override
    public void onLevelChanged(int newLevel) {
        this.level.postValue(newLevel);
    }

    @Override
    public void onWindStatusChanged(boolean isActive, boolean isBlowingRight) {
        windState.postValue(new WindState(isActive, isBlowingRight));
    }

    @Override
    public void onEducationalFeedback(String itemName, WasteCategory category, String tipMessage, int drawableResId) {
        educationalTip.postValue(new EducationalTip(itemName, category, tipMessage, drawableResId));
    }

    @Override
    public void onGameOver(int finalScore, int itemsSorted, int crittersRescued) {
        gameOverEvent.postValue(new GameOverEvent(finalScore, itemsSorted, crittersRescued));
    }

    @Override
    public void onScreenShake() {
        screenShakeEvent.postValue(true);
    }

    @Override
    public void onCorrectSort(boolean isAutoCatch) {
        correctSortEvent.postValue(isAutoCatch);
    }

    @Override
    public void onCritterRescued() {
        critterRescuedEvent.postValue(true);
    }

    @Override
    public void onMistake() {
        mistakeEvent.postValue(true);
    }

    // --- Observable LiveData Getters ---

    /**
     * Observable stream emitting score, streak, and multiplier changes.
     *
     * @return LiveData stream for ScoreState.
     */
    public LiveData<ScoreState> getScoreState() { return scoreState; }

    /**
     * Observable stream emitting player life count updates.
     *
     * @return LiveData stream for remaining lives.
     */
    public LiveData<Integer> getLives() { return lives; }

    /**
     * Observable stream emitting level milestone promotions.
     *
     * @return LiveData stream for current level integer.
     */
    public LiveData<Integer> getLevel() { return level; }

    /**
     * Observable stream emitting dynamic wind weather changes.
     *
     * @return LiveData stream for WindState.
     */
    public LiveData<WindState> getWindState() { return windState; }

    /**
     * Observable stream emitting educational tips on mistakes or misses.
     *
     * @return LiveData stream for EducationalTip.
     */
    public LiveData<EducationalTip> getEducationalTip() { return educationalTip; }

    /**
     * Observable one-shot stream notifying game over with final statistics.
     *
     * @return LiveData stream for GameOverEvent.
     */
    public LiveData<GameOverEvent> getGameOverEvent() { return gameOverEvent; }

    /**
     * Observable one-shot stream triggering tactile screen shake animations.
     *
     * @return LiveData stream for screen shake trigger.
     */
    public LiveData<Boolean> getScreenShakeEvent() { return screenShakeEvent; }

    /**
     * Observable stream notifying successful sort events (and whether auto-caught).
     *
     * @return LiveData stream for correct sort notifications.
     */
    public LiveData<Boolean> getCorrectSortEvent() { return correctSortEvent; }

    /**
     * Observable stream notifying successful critter rescue events.
     *
     * @return LiveData stream for critter rescue notifications.
     */
    public LiveData<Boolean> getCritterRescuedEvent() { return critterRescuedEvent; }

    /**
     * Observable stream notifying mistake events (missed item or wrong bin).
     *
     * @return LiveData stream for mistake notifications.
     */
    public LiveData<Boolean> getMistakeEvent() { return mistakeEvent; }
}
