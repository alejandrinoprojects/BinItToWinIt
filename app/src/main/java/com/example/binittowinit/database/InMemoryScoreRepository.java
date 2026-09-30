package com.example.binittowinit.database;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory implementation of {@link ScoreRepository} for test isolation without Android SQLite dependencies.
 * <p>
 * Completely thread-safe, supporting lightning-fast unit tests and isolated mocking scenarios.
 * </p>
 */
public class InMemoryScoreRepository implements ScoreRepository {
    private final List<ScoreEntry> entries = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long insertScore(String playerName, int score, int itemsSorted, int crittersRescued) {
        String cleanName = (playerName != null && !playerName.trim().isEmpty()) ? playerName.trim() : "Player";
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        long id = idGenerator.getAndIncrement();
        ScoreEntry entry = new ScoreEntry(id, cleanName, score, itemsSorted, crittersRescued, timestamp);
        entries.add(entry);
        return id;
    }

    @Override
    public synchronized List<ScoreEntry> getTopScores(int limit) {
        List<ScoreEntry> copy = new ArrayList<>(entries);
        Collections.sort(copy, (a, b) -> Integer.compare(b.getScore(), a.getScore()));
        if (copy.size() > limit) {
            return new ArrayList<>(copy.subList(0, limit));
        }
        return copy;
    }

    @Override
    public synchronized List<ScoreEntry> getAllScores() {
        List<ScoreEntry> copy = new ArrayList<>(entries);
        Collections.sort(copy, (a, b) -> Integer.compare(b.getScore(), a.getScore()));
        return copy;
    }

    @Override
    public synchronized int getHighScore() {
        int max = 0;
        for (ScoreEntry entry : entries) {
            if (entry.getScore() > max) {
                max = entry.getScore();
            }
        }
        return max;
    }

    @Override
    public synchronized int getScoreCount() {
        return entries.size();
    }

    @Override
    public synchronized void clearLeaderboard() {
        entries.clear();
    }
}
