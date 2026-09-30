package com.example.binittowinit.database;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Production implementation of {@link ScoreRepository} backed by AndroidX Room.
 * <p>
 * Handles timestamp formatting and sanitization of player names before delegating
 * database persistence and queries to {@link ScoreDao}.
 * </p>
 */
public class RoomScoreRepository implements ScoreRepository {
    private final ScoreDao scoreDao;

    /**
     * Constructs a RoomScoreRepository with the provided DAO instance.
     *
     * @param scoreDao Room Data Access Object for scores.
     */
    public RoomScoreRepository(ScoreDao scoreDao) {
        this.scoreDao = scoreDao;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long insertScore(String playerName, int score, int itemsSorted, int crittersRescued) {
        String cleanName = (playerName != null && !playerName.trim().isEmpty()) ? playerName.trim() : "Player";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        ScoreEntry entry = new ScoreEntry(cleanName, score, itemsSorted, crittersRescued, timestamp);
        return scoreDao.insert(entry);
    }

    @Override
    public List<ScoreEntry> getTopScores(int limit) {
        return scoreDao.getTopScores(limit);
    }

    @Override
    public List<ScoreEntry> getAllScores() {
        return scoreDao.getAllScores();
    }

    @Override
    public int getHighScore() {
        Integer high = scoreDao.getHighScore();
        return high != null ? high : 0;
    }

    @Override
    public int getScoreCount() {
        return scoreDao.getScoreCount();
    }

    @Override
    public void clearLeaderboard() {
        scoreDao.clearLeaderboard();
    }
}
