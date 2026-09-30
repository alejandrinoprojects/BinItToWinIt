package com.example.binittowinit.di;

import android.content.Context;

import com.example.binittowinit.audio.ISoundManager;
import com.example.binittowinit.audio.SoundManager;
import com.example.binittowinit.database.AppDatabase;
import com.example.binittowinit.database.RoomScoreRepository;
import com.example.binittowinit.database.ScoreRepository;

/**
 * Dependency Injection container providing application-wide services.
 * Enables constructor injection across activities, viewmodels, and game components,
 * while allowing test harnesses to swap implementations with test doubles.
 */
public class AppContainer {
    private static volatile AppContainer instance;

    private final Context context;
    private final AppDatabase appDatabase;
    private final ScoreRepository scoreRepository;
    private final ISoundManager soundManager;

    /**
     * Constructs a default AppContainer initialized with production Room and SoundManager instances.
     *
     * @param context Android context.
     */
    public AppContainer(Context context) {
        this.context = context.getApplicationContext();
        this.appDatabase = AppDatabase.getInstance(this.context);
        this.scoreRepository = new RoomScoreRepository(appDatabase.scoreDao());
        this.soundManager = SoundManager.getInstance(this.context);
    }

    /**
     * Constructs an AppContainer with explicitly injected components for testing.
     *
     * @param context Android context.
     * @param db      Room AppDatabase instance.
     * @param repo    ScoreRepository implementation.
     * @param sound   ISoundManager implementation.
     */
    public AppContainer(Context context, AppDatabase db, ScoreRepository repo, ISoundManager sound) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.appDatabase = db;
        this.scoreRepository = repo;
        this.soundManager = sound;
    }

    /**
     * Returns the singleton AppContainer instance.
     *
     * @param context Android context.
     * @return Thread-safe singleton AppContainer.
     */
    public static AppContainer getInstance(Context context) {
        if (instance == null) {
            synchronized (AppContainer.class) {
                if (instance == null) {
                    instance = new AppContainer(context);
                }
            }
        }
        return instance;
    }

    /**
     * Injects a custom or mock AppContainer instance for automated testing.
     *
     * @param testContainer Custom test container.
     */
    public static void setInstance(AppContainer testContainer) {
        instance = testContainer;
    }

    /**
     * Returns the application context.
     *
     * @return Application context.
     */
    public Context getContext() {
        return context;
    }

    /**
     * Returns the Room AppDatabase instance.
     *
     * @return AppDatabase instance.
     */
    public AppDatabase getAppDatabase() {
        return appDatabase;
    }

    /**
     * Returns the active ScoreRepository instance.
     *
     * @return ScoreRepository instance.
     */
    public ScoreRepository getScoreRepository() {
        return scoreRepository;
    }

    /**
     * Returns the active ISoundManager instance.
     *
     * @return ISoundManager instance.
     */
    public ISoundManager getSoundManager() {
        return soundManager;
    }
}
