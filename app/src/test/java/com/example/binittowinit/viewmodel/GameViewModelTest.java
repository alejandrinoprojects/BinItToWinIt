package com.example.binittowinit.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.binittowinit.audio.MockSoundManager;
import com.example.binittowinit.database.InMemoryScoreRepository;
import com.example.binittowinit.model.WasteCategory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class GameViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private InMemoryScoreRepository repository;
    private MockSoundManager soundManager;
    private GameViewModel viewModel;

    @Before
    public void setUp() {
        repository = new InMemoryScoreRepository();
        soundManager = new MockSoundManager();
        viewModel = new GameViewModel(repository, soundManager);
    }

    @Test
    public void testInitGameCreatesEngineAndState() {
        viewModel.initGame("TestHero");
        assertNotNull(viewModel.getEngine());
        assertEquals("TestHero", viewModel.getEngine().getPlayerName());
    }

    @Test
    public void testScoreChangedEmitsLiveData() {
        viewModel.initGame("TestHero");

        viewModel.onScoreChanged(250, 5, 2);

        GameViewModel.ScoreState state = viewModel.getScoreState().getValue();
        assertNotNull(state);
        assertEquals(250, state.score);
        assertEquals(5, state.streak);
        assertEquals(2, state.multiplier);
    }

    @Test
    public void testLivesAndLevelEmissions() {
        viewModel.initGame("TestHero");

        viewModel.onLivesChanged(2);
        assertEquals(Integer.valueOf(2), viewModel.getLives().getValue());

        viewModel.onLevelChanged(3);
        assertEquals(Integer.valueOf(3), viewModel.getLevel().getValue());
    }

    @Test
    public void testWindChangedEmission() {
        viewModel.initGame("TestHero");

        viewModel.onWindStatusChanged(true, false);
        GameViewModel.WindState wind = viewModel.getWindState().getValue();
        assertNotNull(wind);
        assertTrue(wind.isActive);
        assertEquals(false, wind.isBlowingRight);
    }

    @Test
    public void testGameOverSavesToRepositoryAndEmitsEvent() {
        viewModel.initGame("TestHero");

        viewModel.saveMatchScore("TestHero", 1200, 15, 2);
        viewModel.onGameOver(1200, 15, 2);

        // Check repository
        assertEquals(1, repository.getScoreCount());
        assertEquals(1200, repository.getHighScore());

        // Check LiveData event
        GameViewModel.GameOverEvent event = viewModel.getGameOverEvent().getValue();
        assertNotNull(event);
        assertEquals(1200, event.score);
        assertEquals(15, event.itemsSorted);
        assertEquals(2, event.crittersRescued);
    }

    @Test
    public void testEducationalTipTriggered() {
        viewModel.initGame("TestHero");

        viewModel.onEducationalFeedback("Banana Peel", WasteCategory.BIODEGRADABLE, "Compost banana peels!", 101);

        GameViewModel.EducationalTip tip = viewModel.getEducationalTip().getValue();
        assertNotNull(tip);
        assertEquals("Banana Peel", tip.itemName);
        assertEquals(WasteCategory.BIODEGRADABLE, tip.category);
        assertEquals("Compost banana peels!", tip.tipMessage);
        assertEquals(101, tip.drawableResId);
    }
}
