package com.example.binittowinit;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.binittowinit.database.ScoreEntry;
import com.example.binittowinit.database.ScoreRepository;
import com.example.binittowinit.di.AppContainer;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * Activity for displaying the persistent Room high-score records and rankings.
 * <p>
 * Features:
 * <ul>
 *   <li>Queries top 50 score records from {@link ScoreRepository}.</li>
 *   <li>Renders entries via {@link LeaderboardAdapter} with gold/silver/bronze medals for the top 3 players.</li>
 *   <li>Displays a clean empty-state view when no records have been recorded yet.</li>
 *   <li>Provides a confirmation dialog to wipe/reset the leaderboard database.</li>
 * </ul>
 * </p>
 */
public class LeaderboardActivity extends AppCompatActivity {
    private ScoreRepository scoreRepository;
    private LeaderboardAdapter adapter;
    private LinearLayout layoutEmptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        // Apply system window insets so top app bar sits safely below status bar / camera cutout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_leaderboard), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        scoreRepository = AppContainer.getInstance(this).getScoreRepository();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_clear_scores).setOnClickListener(v -> confirmClearScores());

        RecyclerView recyclerView = findViewById(R.id.recycler_leaderboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter();
        recyclerView.setAdapter(adapter);

        layoutEmptyState = findViewById(R.id.layout_empty_state);

        loadScores();
    }

    /**
     * Loads up to 50 top records asynchronously and updates the RecyclerView.
     */
    private void loadScores() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ScoreEntry> scores = scoreRepository.getTopScores(50);
            runOnUiThread(() -> {
                adapter.setScores(scores);
                if (scores.isEmpty()) {
                    layoutEmptyState.setVisibility(View.VISIBLE);
                } else {
                    layoutEmptyState.setVisibility(View.GONE);
                }
            });
        });
    }

    /**
     * Shows a confirmation modal before wiping all score history from the database.
     */
    private void confirmClearScores() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Leaderboard")
                .setMessage("Are you sure you want to reset all high score records?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        scoreRepository.clearLeaderboard();
                        loadScores();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
