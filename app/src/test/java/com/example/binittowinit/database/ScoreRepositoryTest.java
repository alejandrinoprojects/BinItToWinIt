package com.example.binittowinit.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ScoreRepositoryTest {

    private ScoreRepository repository;

    @Before
    public void setUp() {
        repository = new InMemoryScoreRepository();
    }

    @Test
    public void testEmptyRepository() {
        assertEquals(0, repository.getScoreCount());
        assertEquals(0, repository.getHighScore());
        assertTrue(repository.getAllScores().isEmpty());
        assertTrue(repository.getTopScores(5).isEmpty());
    }

    @Test
    public void testInsertAndRetrieveTopScores() {
        repository.insertScore("PlayerA", 500, 10, 1);
        repository.insertScore("PlayerB", 1500, 25, 3);
        repository.insertScore("PlayerC", 800, 15, 2);

        assertEquals(3, repository.getScoreCount());
        assertEquals(1500, repository.getHighScore());

        List<ScoreEntry> top2 = repository.getTopScores(2);
        assertEquals(2, top2.size());
        assertEquals("PlayerB", top2.get(0).getPlayerName());
        assertEquals(1500, top2.get(0).getScore());
        assertEquals("PlayerC", top2.get(1).getPlayerName());
        assertEquals(800, top2.get(1).getScore());
    }

    @Test
    public void testClearLeaderboard() {
        repository.insertScore("PlayerA", 500, 10, 1);
        repository.insertScore("PlayerB", 1500, 25, 3);
        assertEquals(2, repository.getScoreCount());

        repository.clearLeaderboard();
        assertEquals(0, repository.getScoreCount());
        assertEquals(0, repository.getHighScore());
    }
}
