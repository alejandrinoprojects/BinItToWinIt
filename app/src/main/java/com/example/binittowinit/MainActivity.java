package com.example.binittowinit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.binittowinit.audio.ISoundManager;
import com.example.binittowinit.database.ScoreEntry;
import com.example.binittowinit.database.ScoreRepository;
import com.example.binittowinit.di.AppContainer;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * Main Menu Activity for "Bin It to Win It".
 * <p>
 * Serves as the primary entry point of the application:
 * <ul>
 *   <li><b>Player Name Registration:</b> Input field allowing players to set their identity,
 *       which is saved to {@link SharedPreferences} and passed into the game session.</li>
 *   <li><b>Game Launch:</b> Validates player name and starts {@link GameActivity}.</li>
 *   <li><b>Navigation:</b> Launches the high-score {@link LeaderboardActivity} and tutorial {@link InstructionsActivity}.</li>
 *   <li><b>Sound Controls:</b> Toggle button allowing global sound mute/unmute.</li>
 * </ul>
 * </p>
 */
public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "binit_user_prefs";
    private static final String KEY_LAST_PLAYER = "last_player_name";

    private EditText etPlayerName;
    private TextView tvBestScore;
    private ImageButton btnSoundToggle;
    private ISoundManager soundManager;
    private ScoreRepository scoreRepository;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Apply system window insets so top elements sit safely below status bar / camera cutout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        AppContainer container = AppContainer.getInstance(this);
        soundManager = container.getSoundManager();
        scoreRepository = container.getScoreRepository();
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        etPlayerName = findViewById(R.id.et_player_name);
        tvBestScore = findViewById(R.id.tv_best_score);
        btnSoundToggle = findViewById(R.id.btn_sound_toggle);

        // Populate input with previously saved player name if available
        String savedName = prefs.getString(KEY_LAST_PLAYER, "");
        if (!savedName.isEmpty()) {
            etPlayerName.setText(savedName);
        }

        updateSoundButtonIcon();

        // Audio toggle button listener
        btnSoundToggle.setOnClickListener(v -> {
            boolean current = soundManager.isSoundEnabled();
            soundManager.setSoundEnabled(!current);
            updateSoundButtonIcon();
            Toast.makeText(this, !current ? R.string.sound_on : R.string.sound_off, Toast.LENGTH_SHORT).show();
        });

        // Navigation button listeners
        findViewById(R.id.btn_play).setOnClickListener(v -> startGame());
        findViewById(R.id.btn_leaderboard).setOnClickListener(v -> {
            startActivity(new Intent(this, LeaderboardActivity.class));
        });
        findViewById(R.id.btn_instructions).setOnClickListener(v -> {
            startActivity(new Intent(this, InstructionsActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBestScore();
    }

    /**
     * Queries the database asynchronously to display the top high score banner.
     */
    private void refreshBestScore() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ScoreEntry> topScores = scoreRepository.getTopScores(1);
            runOnUiThread(() -> {
                if (topScores != null && !topScores.isEmpty()) {
                    ScoreEntry top = topScores.get(0);
                    tvBestScore.setText("🏆 Best: " + top.getScore() + " pts (" + top.getPlayerName() + ")");
                    tvBestScore.setVisibility(View.VISIBLE);
                } else {
                    tvBestScore.setVisibility(View.GONE);
                }
            });
        });
    }

    /**
     * Validates that the player has entered a name before launching the GameActivity.
     */
    private void startGame() {
        String name = etPlayerName.getText().toString().trim();
        if (name.isEmpty()) {
            etPlayerName.setError(getString(R.string.name_required_prompt));
            etPlayerName.requestFocus();
            return;
        }

        // Persist player name for future sessions
        prefs.edit().putString(KEY_LAST_PLAYER, name).apply();

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("EXTRA_PLAYER_NAME", name);
        startActivity(intent);
    }

    /**
     * Updates the sound toggle icon based on current audio preference.
     */
    private void updateSoundButtonIcon() {
        if (soundManager.isSoundEnabled()) {
            btnSoundToggle.setImageResource(R.drawable.ic_sound_on);
        } else {
            btnSoundToggle.setImageResource(R.drawable.ic_sound_off);
        }
    }
}