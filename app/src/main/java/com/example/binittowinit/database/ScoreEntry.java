package com.example.binittowinit.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Immutable data model representing a single completed match record stored in Room / SQLite.
 * <p>
 * Contains final match metrics including the player's name, total score, total waste
 * items sorted into bins, total wandering critters shooed to safety, and the timestamp.
 * </p>
 */
@Entity(tableName = "leaderboard")
public class ScoreEntry {
    /** Primary key row ID in the database. */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    /** Name of the player who completed the game session. */
    @NonNull
    @ColumnInfo(name = "player_name")
    private String playerName;

    /** Final score achieved. */
    @ColumnInfo(name = "score")
    private int score;

    /** Number of waste items correctly deposited into matching bins. */
    @ColumnInfo(name = "items_sorted", defaultValue = "0")
    private int itemsSorted;

    /** Number of wandering critters successfully flicked away to safety. */
    @ColumnInfo(name = "critters_rescued", defaultValue = "0")
    private int crittersRescued;

    /** Formatted date and time string when the game was completed. */
    @NonNull
    @ColumnInfo(name = "timestamp")
    private String timestamp;

    /**
     * Constructs a ScoreEntry record.
     *
     * @param id              Database row ID.
     * @param playerName      Player handle/name.
     * @param score           Final score.
     * @param itemsSorted     Total waste correctly sorted.
     * @param crittersRescued Total critters rescued.
     * @param timestamp       Date/time string.
     */
    public ScoreEntry(long id, @NonNull String playerName, int score, int itemsSorted, int crittersRescued, @NonNull String timestamp) {
        this.id = id;
        this.playerName = playerName;
        this.score = score;
        this.itemsSorted = itemsSorted;
        this.crittersRescued = crittersRescued;
        this.timestamp = timestamp;
    }

    /**
     * Convenience constructor omitting row ID for insertion before auto-generation.
     *
     * @param playerName      Player handle/name.
     * @param score           Final score.
     * @param itemsSorted     Total waste correctly sorted.
     * @param crittersRescued Total critters rescued.
     * @param timestamp       Date/time string.
     */
    @Ignore
    public ScoreEntry(@NonNull String playerName, int score, int itemsSorted, int crittersRescued, @NonNull String timestamp) {
        this(0, playerName, score, itemsSorted, crittersRescued, timestamp);
    }

    /**
     * Returns the primary database row ID.
     *
     * @return Row ID.
     */
    public long getId() { return id; }

    /**
     * Sets the primary database row ID.
     *
     * @param id Row ID.
     */
    public void setId(long id) { this.id = id; }

    /**
     * Returns the player name.
     *
     * @return Player name.
     */
    @NonNull
    public String getPlayerName() { return playerName; }

    /**
     * Sets the player name.
     *
     * @param playerName Player name.
     */
    public void setPlayerName(@NonNull String playerName) { this.playerName = playerName; }

    /**
     * Returns the final match score.
     *
     * @return Final score.
     */
    public int getScore() { return score; }

    /**
     * Sets the final match score.
     *
     * @param score Final score.
     */
    public void setScore(int score) { this.score = score; }

    /**
     * Returns the total count of waste items successfully sorted.
     *
     * @return Total items sorted.
     */
    public int getItemsSorted() { return itemsSorted; }

    /**
     * Sets the count of items sorted.
     *
     * @param itemsSorted Total items sorted.
     */
    public void setItemsSorted(int itemsSorted) { this.itemsSorted = itemsSorted; }

    /**
     * Returns the total count of wandering critters safely flicked away.
     *
     * @return Critters rescued count.
     */
    public int getCrittersRescued() { return crittersRescued; }

    /**
     * Sets the total count of critters rescued.
     *
     * @param crittersRescued Critters rescued count.
     */
    public void setCrittersRescued(int crittersRescued) { this.crittersRescued = crittersRescued; }

    /**
     * Returns the match completion date/time formatted string.
     *
     * @return Formatted timestamp.
     */
    @NonNull
    public String getTimestamp() { return timestamp; }

    /**
     * Sets the match completion date/time formatted string.
     *
     * @param timestamp Formatted timestamp.
     */
    public void setTimestamp(@NonNull String timestamp) { this.timestamp = timestamp; }
}
