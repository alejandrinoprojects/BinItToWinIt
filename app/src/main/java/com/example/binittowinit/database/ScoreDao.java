package com.example.binittowinit.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Room Data Access Object (DAO) for querying and persisting player scores.
 * <p>
 * Provides compile-time checked SQL statements and eliminates boilerplate Cursor parsing
 * for the leaderboard SQLite table.
 * </p>
 */
@Dao
public interface ScoreDao {

    /**
     * Inserts a completed game score entry into the leaderboard table.
     *
     * @param scoreEntry The entry record to insert.
     * @return The auto-generated row ID of the inserted record.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ScoreEntry scoreEntry);

    /**
     * Retrieves the top scores sorted descending by score up to the specified limit.
     *
     * @param limit Maximum number of records to return.
     * @return List of top {@link ScoreEntry} records.
     */
    @Query("SELECT * FROM leaderboard ORDER BY score DESC LIMIT :limit")
    List<ScoreEntry> getTopScores(int limit);

    /**
     * Retrieves all recorded scores sorted descending by score.
     *
     * @return List of all {@link ScoreEntry} records.
     */
    @Query("SELECT * FROM leaderboard ORDER BY score DESC")
    List<ScoreEntry> getAllScores();

    /**
     * Returns the total count of match records in the leaderboard table.
     *
     * @return Total row count.
     */
    @Query("SELECT COUNT(*) FROM leaderboard")
    int getScoreCount();

    /**
     * Returns the highest score ever recorded in the database, or {@code null} if empty.
     *
     * @return Maximum score integer or null.
     */
    @Query("SELECT MAX(score) FROM leaderboard")
    Integer getHighScore();

    /**
     * Deletes all score entries from the leaderboard table.
     */
    @Query("DELETE FROM leaderboard")
    void clearLeaderboard();
}
