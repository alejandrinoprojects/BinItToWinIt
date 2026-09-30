package com.example.binittowinit.database;

import java.util.List;

/**
 * Repository interface abstracting data persistence operations for match scores.
 * Enables clean dependency injection, mockability, and test isolation.
 */
public interface ScoreRepository {
    /**
     * Inserts a match score record.
     *
     * @param playerName      Player name or handle.
     * @param score           Final score reached.
     * @param itemsSorted     Total waste correctly sorted.
     * @param crittersRescued Total critters safely flicked away.
     * @return Row ID of the newly inserted record.
     */
    long insertScore(String playerName, int score, int itemsSorted, int crittersRescued);

    /**
     * Retrieves the top scores limited to {@code limit} rows in descending order.
     *
     * @param limit Maximum number of leaderboard records to return.
     * @return List of top score entries.
     */
    List<ScoreEntry> getTopScores(int limit);

    /**
     * Retrieves all recorded scores ordered from highest to lowest.
     *
     * @return Complete list of recorded score entries.
     */
    List<ScoreEntry> getAllScores();

    /**
     * Returns the highest score achieved, or 0 if no games have been recorded.
     *
     * @return Highest score value.
     */
    int getHighScore();

    /**
     * Returns the total count of saved games.
     *
     * @return Total count of recorded games.
     */
    int getScoreCount();

    /**
     * Clears all leaderboard entries from persistent storage.
     */
    void clearLeaderboard();
}
