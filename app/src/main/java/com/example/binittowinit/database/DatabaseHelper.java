package com.example.binittowinit.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * SQLite Database manager for persistent local storage of player profiles,
 * high scores, and match statistics in "Bin It to Win It".
 * <p>
 * Implements a thread-safe singleton pattern accessing {@code binit_game.db}.
 * Persists match records upon game over and supplies sorted records for the
 * in-game Leaderboard screen.
 * </p>
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "binit_game.db";
    private static final int DATABASE_VERSION = 1;

    // Table and Column definitions
    public static final String TABLE_LEADERBOARD = "leaderboard";
    public static final String COL_ID = "id";
    public static final String COL_PLAYER_NAME = "player_name";
    public static final String COL_SCORE = "score";
    public static final String COL_ITEMS_SORTED = "items_sorted";
    public static final String COL_CRITTERS_RESCUED = "critters_rescued";
    public static final String COL_TIMESTAMP = "timestamp";

    /** SQL statement to create the leaderboard table. */
    private static final String CREATE_LEADERBOARD_TABLE =
            "CREATE TABLE " + TABLE_LEADERBOARD + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_PLAYER_NAME + " TEXT NOT NULL, " +
                    COL_SCORE + " INTEGER NOT NULL, " +
                    COL_ITEMS_SORTED + " INTEGER NOT NULL DEFAULT 0, " +
                    COL_CRITTERS_RESCUED + " INTEGER NOT NULL DEFAULT 0, " +
                    COL_TIMESTAMP + " TEXT NOT NULL);";

    private static DatabaseHelper instance;
    private final java.util.concurrent.ExecutorService dbExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

    /**
     * Retrieves or initializes the shared singleton instance of DatabaseHelper.
     *
     * @param context Application or activity context.
     * @return Thread-safe DatabaseHelper instance.
     */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Returns the background single-thread executor for database operations.
     *
     * @return Background ExecutorService instance.
     */
    public java.util.concurrent.ExecutorService getExecutor() {
        return dbExecutor;
    }

    /**
     * Private constructor for singleton usage.
     *
     * @param context Application context.
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_LEADERBOARD_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LEADERBOARD);
        onCreate(db);
    }

    /**
     * Inserts a new match record into the leaderboard table.
     *
     * @param playerName      Player handle/name. Defaults to "Player" if blank.
     * @param score           Final score achieved.
     * @param itemsSorted     Total waste items deposited into correct bins.
     * @param crittersRescued Total wandering critters flicked to safety.
     * @return The row ID of the newly inserted record, or -1 if an error occurred.
     */
    public long insertScore(String playerName, int score, int itemsSorted, int crittersRescued) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PLAYER_NAME, (playerName != null && !playerName.trim().isEmpty()) ? playerName.trim() : "Player");
        cv.put(COL_SCORE, score);
        cv.put(COL_ITEMS_SORTED, itemsSorted);
        cv.put(COL_CRITTERS_RESCUED, crittersRescued);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        cv.put(COL_TIMESTAMP, sdf.format(new Date()));

        return db.insert(TABLE_LEADERBOARD, null, cv);
    }

    /**
     * Retrieves the top scores from the database ordered descending by score.
     *
     * @param limit Maximum number of leaderboard records to fetch.
     * @return List of {@link ScoreEntry} objects.
     */
    public List<ScoreEntry> getTopScores(int limit) {
        List<ScoreEntry> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_LEADERBOARD + " ORDER BY " + COL_SCORE + " DESC LIMIT " + limit;
        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndex(COL_ID);
                int nameCol = cursor.getColumnIndex(COL_PLAYER_NAME);
                int scoreCol = cursor.getColumnIndex(COL_SCORE);
                int itemsCol = cursor.getColumnIndex(COL_ITEMS_SORTED);
                int critterCol = cursor.getColumnIndex(COL_CRITTERS_RESCUED);
                int timeCol = cursor.getColumnIndex(COL_TIMESTAMP);

                do {
                    long id = cursor.getLong(idCol);
                    String name = cursor.getString(nameCol);
                    int score = cursor.getInt(scoreCol);
                    int items = cursor.getInt(itemsCol);
                    int critters = cursor.getInt(critterCol);
                    String timestamp = cursor.getString(timeCol);

                    list.add(new ScoreEntry(id, name, score, items, critters, timestamp));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Retrieves the all-time highest score achieved by a specific player.
     *
     * @param playerName The player's name.
     * @return The highest score recorded for that player, or 0 if none.
     */
    public int getPlayerHighestScore(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) return 0;
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT MAX(" + COL_SCORE + ") FROM " + TABLE_LEADERBOARD + " WHERE " + COL_PLAYER_NAME + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{playerName.trim()});
        int max = 0;
        if (cursor != null) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                max = cursor.getInt(0);
            }
            cursor.close();
        }
        return max;
    }

    /**
     * Retrieves the highest score across all players in the database.
     *
     * @return Global highest score recorded, or 0 if empty.
     */
    public int getGlobalHighestScore() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT MAX(" + COL_SCORE + ") FROM " + TABLE_LEADERBOARD;
        Cursor cursor = db.rawQuery(query, null);
        int max = 0;
        if (cursor != null) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                max = cursor.getInt(0);
            }
            cursor.close();
        }
        return max;
    }

    /**
     * Clears all leaderboard entries from the database.
     */
    public void clearLeaderboard() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LEADERBOARD, null, null);
    }
}
