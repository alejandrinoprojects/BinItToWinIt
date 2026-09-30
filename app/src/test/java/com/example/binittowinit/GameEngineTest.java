package com.example.binittowinit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.binittowinit.game.GameEngine;
import com.example.binittowinit.model.GarbageBin;
import com.example.binittowinit.model.ItemBehavior;
import com.example.binittowinit.model.WasteCategory;
import com.example.binittowinit.model.WasteItem;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GameEngineTest {
    private GameEngine engine;
    private AtomicInteger lastScore;
    private AtomicInteger lastLives;
    private AtomicBoolean gameOverTriggered;
    private AtomicBoolean lastAutoCatch;
    private AtomicInteger correctSortCallCount;
    private AtomicInteger lastLevelChanged;

    @Before
    public void setUp() {
        lastScore = new AtomicInteger(0);
        lastLives = new AtomicInteger(10);
        gameOverTriggered = new AtomicBoolean(false);
        lastAutoCatch = new AtomicBoolean(false);
        correctSortCallCount = new AtomicInteger(0);
        lastLevelChanged = new AtomicInteger(1);

        engine = new GameEngine("TestHero", new GameEngine.GameEventListener() {
            @Override
            public void onScoreChanged(int score, int streak, int multiplier) {
                lastScore.set(score);
            }

            @Override
            public void onLivesChanged(int lives) {
                lastLives.set(lives);
            }

            @Override
            public void onWindStatusChanged(boolean isActive, boolean isBlowingRight) {}

            @Override
            public void onGameOver(int finalScore, int itemsSorted, int crittersRescued) {
                gameOverTriggered.set(true);
            }

            @Override
            public void onScreenShake() {}

            @Override
            public void onLevelChanged(int newLevel) {
                lastLevelChanged.set(newLevel);
            }

            @Override
            public void onEducationalFeedback(String itemName, com.example.binittowinit.model.WasteCategory category, String tipMessage, int drawableResId) {}

            @Override
            public void onCorrectSort(boolean isAutoCatch) {
                correctSortCallCount.incrementAndGet();
                lastAutoCatch.set(isAutoCatch);
            }

            @Override
            public void onMistake() {}

            @Override
            public void onCritterRescued() {}
        });

        engine.setScreenDimensions(1080, 1920);
        engine.start();
    }

    @Test
    public void testInitialState() {
        assertEquals("TestHero", engine.getPlayerName());
        assertEquals(0, engine.getScore());
        assertEquals(10, engine.getLives());
        assertEquals(0, engine.getStreak());
        assertEquals(1, engine.getMultiplier());
        assertEquals(GameEngine.State.RUNNING, engine.getState());
        assertEquals(3, engine.getBins().size());
    }

    @Test
    public void testCorrectSortingIncreasesScoreAndStreak() {
        GarbageBin bioBin = engine.getBins().get(0); // Biodegradable bin
        WasteItem bioItem = new WasteItem("test_apple", "Apple", WasteCategory.BIODEGRADABLE, 0,
                ItemBehavior.createMediumTumbler(0.1f), 100);

        engine.handleDropOnBin(bioItem, bioBin);

        assertEquals(100, engine.getScore());
        assertEquals(1, engine.getItemsSorted());
        assertEquals(1, engine.getStreak());
        assertEquals(10, engine.getLives());
    }

    @Test
    public void testStreakMultiplierProgression() {
        GarbageBin recBin = engine.getBins().get(1); // Recyclable bin

        for (int i = 0; i < 3; i++) {
            WasteItem item = new WasteItem("rec_" + i, "Can", WasteCategory.RECYCLABLE, 0,
                    ItemBehavior.createMediumTumbler(0.1f), 100);
            engine.handleDropOnBin(item, recBin);
        }

        // At streak 3, multiplier should be 2x
        assertEquals(3, engine.getStreak());
        assertEquals(2, engine.getMultiplier());

        // 4th item sorted with 2x multiplier
        WasteItem fourth = new WasteItem("rec_4", "Can", WasteCategory.RECYCLABLE, 0,
                ItemBehavior.createMediumTumbler(0.1f), 100);
        engine.handleDropOnBin(fourth, recBin);

        // Previous was 300, 4th gave 100 * 2 = 200 -> total 500
        assertEquals(500, engine.getScore());
    }

    @Test
    public void testWrongBinSortingPenalizesLifeAndResetsStreak() {
        GarbageBin bioBin = engine.getBins().get(0); // Biodegradable
        WasteItem nonBioItem = new WasteItem("test_bag", "Plastic Bag", WasteCategory.NON_BIODEGRADABLE, 0,
                ItemBehavior.createFloaty(1.0f, 40f), 100);

        // First increase streak
        GarbageBin nonBioBin = engine.getBins().get(2);
        engine.handleDropOnBin(nonBioItem, nonBioBin);
        assertEquals(1, engine.getStreak());

        // Now drop in wrong bin
        WasteItem wrongItem = new WasteItem("test_can", "Can", WasteCategory.RECYCLABLE, 0,
                ItemBehavior.createMediumTumbler(0.1f), 100);
        engine.handleDropOnBin(wrongItem, bioBin);

        assertEquals(9, engine.getLives());
        assertEquals(0, engine.getStreak());
    }

    @Test
    public void testBinningCritterPenalizesLife() {
        GarbageBin bioBin = engine.getBins().get(0);
        WasteItem kitten = new WasteItem("critter_cat", "Kitten", WasteCategory.CRITTER_FLICK_AWAY, 0,
                ItemBehavior.createCritter(0.3f, 0.7f, 15f), 150);

        engine.handleDropOnBin(kitten, bioBin);

        assertEquals(9, engine.getLives());
        assertEquals(0, engine.getStreak());
    }

    @Test
    public void testAutoCatchCorrectBin() {
        GarbageBin bioBin = engine.getBins().get(0);
        WasteItem bioItem = new WasteItem("test_leaf", "Leaf", WasteCategory.BIODEGRADABLE, 0,
                ItemBehavior.createFloaty(1.2f, 35f), 100);

        engine.handleAutoCatch(bioItem, bioBin);

        assertEquals(100, engine.getScore());
        assertEquals(1, engine.getItemsSorted());
        assertEquals(1, engine.getStreak());
        assertEquals(10, engine.getLives());
    }

    @Test
    public void testAutoCatchWrongBinPenalizesLife() {
        GarbageBin bioBin = engine.getBins().get(0);
        WasteItem recItem = new WasteItem("test_bottle", "Bottle", WasteCategory.RECYCLABLE, 0,
                ItemBehavior.createHeavyPlunge(), 100);

        engine.handleAutoCatch(recItem, bioBin);

        assertEquals(0, engine.getScore());
        assertEquals(9, engine.getLives());
        assertEquals(0, engine.getStreak());
    }

    @Test
    public void testAutoCatchCritterPenalizesLife() {
        GarbageBin recBin = engine.getBins().get(1);
        WasteItem puppy = new WasteItem("critter_puppy", "Puppy", WasteCategory.CRITTER_FLICK_AWAY, 0,
                ItemBehavior.createCritter(0.3f, 0.8f, 15f), 150);

        engine.handleAutoCatch(puppy, recBin);

        assertEquals(9, engine.getLives());
        assertEquals(0, engine.getStreak());
    }

    @Test
    public void testGameOverWhenLivesReachZero() {
        GarbageBin bioBin = engine.getBins().get(0);
        WasteItem wrong = new WasteItem("wrong", "Can", WasteCategory.RECYCLABLE, 0,
                ItemBehavior.createMediumTumbler(0.1f), 100);

        // Starting with 10 lives, penalize 10 times to reach 0
        for (int i = 0; i < 10; i++) {
            assertFalse(gameOverTriggered.get());
            engine.handleDropOnBin(wrong, bioBin);
        }

        assertEquals(0, engine.getLives());
        assertEquals(GameEngine.State.GAME_OVER, engine.getState());
        assertTrue(gameOverTriggered.get());
    }

    @Test
    public void testAutoCatchDispatchesCorrectSortAudioEvent() {
        GarbageBin bioBin = engine.getBins().get(0);
        WasteItem bioItem = new WasteItem("test_leaf", "Leaf", WasteCategory.BIODEGRADABLE, 0,
                ItemBehavior.createFloaty(1.2f, 35f), 100);

        int countBefore = correctSortCallCount.get();
        engine.handleAutoCatch(bioItem, bioBin);

        assertEquals(countBefore + 1, correctSortCallCount.get());
        assertTrue("Auto catch must set isAutoCatch=true for audio dispatcher", lastAutoCatch.get());
    }

    @Test
    public void testDropOnBinDispatchesCorrectSortAudioEvent() {
        GarbageBin recBin = engine.getBins().get(1);
        WasteItem recItem = new WasteItem("test_bottle", "Bottle", WasteCategory.RECYCLABLE, 0,
                ItemBehavior.createHeavyPlunge(), 100);

        int countBefore = correctSortCallCount.get();
        engine.handleDropOnBin(recItem, recBin);

        assertEquals(countBefore + 1, correctSortCallCount.get());
        assertFalse("Manual drop must set isAutoCatch=false", lastAutoCatch.get());
    }

    @Test
    public void testLevelProgressionMilestoneEvery2500() {
        GarbageBin bioBin = engine.getBins().get(0);
        assertEquals(1, engine.getCurrentLevel());

        // Drop items until score reaches 2500
        while (engine.getScore() < 2500) {
            WasteItem bioItem = new WasteItem("item", "Leaf", WasteCategory.BIODEGRADABLE, 0,
                    ItemBehavior.createFloaty(1.0f, 10f), 100);
            engine.handleDropOnBin(bioItem, bioBin);
            if (engine.getScore() < 2500) {
                assertEquals(1, engine.getCurrentLevel());
            }
        }

        // At 2500+ score, level must advance to Level 2
        assertEquals(2, engine.getCurrentLevel());
        assertEquals(2, lastLevelChanged.get());

        // Continue until score reaches 5000
        while (engine.getScore() < 5000) {
            WasteItem bioItem = new WasteItem("item", "Leaf", WasteCategory.BIODEGRADABLE, 0,
                    ItemBehavior.createFloaty(1.0f, 10f), 100);
            engine.handleDropOnBin(bioItem, bioBin);
            if (engine.getScore() < 5000) {
                assertEquals(2, engine.getCurrentLevel());
            }
        }

        // At 5000+ score, level must advance to Level 3
        assertEquals(3, engine.getCurrentLevel());
        assertEquals(3, lastLevelChanged.get());
    }

    @Test
    public void testEducationalFeedbackDispatchesItemIcon() {
        AtomicInteger reportedIcon = new AtomicInteger(0);
        GameEngine testEngine = new GameEngine("Hero", new GameEngine.GameEventListener() {
            @Override public void onScoreChanged(int score, int streak, int multiplier) {}
            @Override public void onLivesChanged(int lives) {}
            @Override public void onWindStatusChanged(boolean isActive, boolean isBlowingRight) {}
            @Override public void onGameOver(int finalScore, int itemsSorted, int crittersRescued) {}
            @Override public void onScreenShake() {}
            @Override public void onLevelChanged(int newLevel) {}
            @Override public void onEducationalFeedback(String itemName, WasteCategory category, String tipMessage, int drawableResId) {
                reportedIcon.set(drawableResId);
            }
            @Override public void onCorrectSort(boolean isAutoCatch) {}
            @Override public void onMistake() {}
            @Override public void onCritterRescued() {}
        });
        testEngine.setScreenDimensions(1080, 1920);
        testEngine.start();

        // Deliberately drop recyclable item in non-biodegradable bin to trigger educational feedback
        WasteItem recItem = new WasteItem("item", "Plastic Bottle", WasteCategory.RECYCLABLE, 12345,
                ItemBehavior.createMediumTumbler(0.2f), 100);
        GarbageBin nonBioBin = testEngine.getBins().get(2);
        testEngine.handleDropOnBin(recItem, nonBioBin);

        assertEquals("onEducationalFeedback must pass the item's drawableResId", 12345, reportedIcon.get());
    }
}
