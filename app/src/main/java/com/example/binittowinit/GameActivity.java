package com.example.binittowinit;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.binittowinit.audio.ISoundManager;
import com.example.binittowinit.database.ScoreRepository;
import com.example.binittowinit.di.AppContainer;
import com.example.binittowinit.game.GameEngine;
import com.example.binittowinit.game.GameView;
import com.example.binittowinit.model.WasteCategory;
import com.example.binittowinit.viewmodel.GameViewModel;

/**
 * Primary gameplay screen hosting the decoupled SurfaceView canvas, heads-up display (HUD),
 * and reactive ViewModel state streams.
 * <p>
 * Completely immune to Android configuration changes (rotations, multi-window, splits)
 * through AndroidX {@link GameViewModel} and reactive {@link androidx.lifecycle.LiveData}.
 * </p>
 */
public class GameActivity extends AppCompatActivity {
    private String playerName = "Player";
    private GameViewModel viewModel;
    private GameView gameView;
    private ISoundManager soundManager;
    private ScoreRepository scoreRepository;

    // --- HUD Views ---
    private TextView tvPlayerName;
    private TextView tvScore;
    private TextView tvStreak;
    private TextView tvHudLives;
    private ImageView ivHudHeart;
    private ImageView ivEduIcon;
    private LinearLayout layoutWindIndicator;
    private TextView tvWindLabel;
    private TextView tvHudLevel;
    private View layoutEducationalBanner;
    private TextView tvEduTitle;
    private TextView tvEduMessage;
    private TextView tvLevelUpSplash;

    private AlertDialog currentDialog;
    private final android.os.Handler uiHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable hideTipRunnable;
    private Runnable hideLevelSplashRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        if (getIntent().hasExtra("EXTRA_PLAYER_NAME")) {
            playerName = getIntent().getStringExtra("EXTRA_PLAYER_NAME");
        }

        AppContainer container = AppContainer.getInstance(this);
        soundManager = container.getSoundManager();
        scoreRepository = container.getScoreRepository();

        viewModel = new ViewModelProvider(this).get(GameViewModel.class);
        viewModel.setDependencies(scoreRepository, soundManager);

        initViews();
        setupEngine();
        observeViewModel();
    }

    /**
     * Binds HUD UI elements and configures predictive back press navigation.
     */
    private void initViews() {
        gameView = findViewById(R.id.game_view);
        tvPlayerName = findViewById(R.id.tv_hud_player);
        tvScore = findViewById(R.id.tv_hud_score);
        tvStreak = findViewById(R.id.tv_hud_streak);
        tvHudLives = findViewById(R.id.tv_hud_lives);
        ivHudHeart = findViewById(R.id.iv_hud_heart);
        tvHudLevel = findViewById(R.id.tv_hud_level);
        layoutWindIndicator = findViewById(R.id.layout_wind_indicator);
        tvWindLabel = findViewById(R.id.tv_wind_label);

        layoutEducationalBanner = findViewById(R.id.layout_educational_banner);
        ivEduIcon = findViewById(R.id.iv_edu_icon);
        tvEduTitle = findViewById(R.id.tv_edu_title);
        tvEduMessage = findViewById(R.id.tv_edu_message);
        tvLevelUpSplash = findViewById(R.id.tv_level_up_splash);

        tvPlayerName.setText(playerName);

        findViewById(R.id.btn_pause).setOnClickListener(v -> showPauseDialog());

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                GameEngine engine = viewModel.getEngine();
                if (engine != null && engine.getState() == GameEngine.State.RUNNING) {
                    showPauseDialog();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    /**
     * Connects GameEngine from GameViewModel to the SurfaceView.
     */
    private void setupEngine() {
        viewModel.initGame(playerName);
        gameView.setEngine(viewModel.getEngine());
    }

    /**
     * Subscribes to reactive ViewModel LiveData streams to update HUD without tight coupling.
     */
    private void observeViewModel() {
        viewModel.getScoreState().observe(this, scoreState -> {
            if (scoreState == null) return;
            tvScore.setText("Score: " + scoreState.score);
            if (scoreState.multiplier > 1) {
                tvStreak.setVisibility(View.VISIBLE);
                tvStreak.setText(scoreState.multiplier + "x 🔥");
            } else {
                tvStreak.setVisibility(View.GONE);
            }
        });

        viewModel.getLives().observe(this, lives -> {
            if (lives == null) return;
            if (tvHudLives != null) {
                tvHudLives.setText(String.valueOf(Math.max(0, lives)));
                if (lives <= 3) {
                    tvHudLives.setTextColor(getColor(R.color.feedback_error));
                } else {
                    tvHudLives.setTextColor(getColor(R.color.heart_red));
                }
            }
            if (ivHudHeart != null && lives <= 0) {
                ivHudHeart.setImageResource(R.drawable.ic_heart_empty);
            }
        });

        viewModel.getLevel().observe(this, newLevel -> {
            if (newLevel == null) return;
            if (tvHudLevel != null) {
                tvHudLevel.setText("LVL " + newLevel);
            }
            if (tvLevelUpSplash != null && newLevel > 1) {
                tvLevelUpSplash.setText("⭐ LEVEL " + newLevel + ": GET READY! ⭐");
                tvLevelUpSplash.setVisibility(View.VISIBLE);
                if (hideLevelSplashRunnable != null) {
                    uiHandler.removeCallbacks(hideLevelSplashRunnable);
                }
                hideLevelSplashRunnable = () -> tvLevelUpSplash.setVisibility(View.GONE);
                uiHandler.postDelayed(hideLevelSplashRunnable, 2000);
            }
        });

        viewModel.getWindState().observe(this, windState -> {
            if (windState == null) return;
            if (windState.isActive) {
                layoutWindIndicator.setVisibility(View.VISIBLE);
                tvWindLabel.setText(windState.isBlowingRight ? "Wind ➔" : "⬅ Wind");
            } else {
                layoutWindIndicator.setVisibility(View.INVISIBLE);
            }
        });

        viewModel.getEducationalTip().observe(this, tip -> {
            if (tip == null) return;
            displayEducationalBanner(tip);
        });

        viewModel.getScreenShakeEvent().observe(this, shake -> {
            if (Boolean.TRUE.equals(shake)) {
                gameView.triggerScreenShake();
            }
        });

        viewModel.getCorrectSortEvent().observe(this, isAutoCatch -> {
            if (soundManager != null) {
                soundManager.playSuccessChime();
            }
        });

        viewModel.getMistakeEvent().observe(this, mistake -> {
            if (soundManager != null) {
                soundManager.playErrorBuzz();
            }
        });

        viewModel.getCritterRescuedEvent().observe(this, rescued -> {
            if (soundManager != null) {
                soundManager.playCritterShoo();
            }
        });

        viewModel.getGameOverEvent().observe(this, event -> {
            if (event == null) return;
            if (soundManager != null) {
                soundManager.playGameOver();
            }
            java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
                int previousBest = 0;
                try {
                    if (scoreRepository != null) {
                        previousBest = scoreRepository.getHighScore();
                    }
                } catch (Exception ignored) {}
                viewModel.saveMatchScore(playerName, event.score, event.itemsSorted, event.crittersRescued);
                boolean isNewBest = event.score > previousBest && event.score > 0;
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        showGameOverDialog(event.score, event.itemsSorted, event.crittersRescued, isNewBest);
                    }
                });
            });
        });
    }

    /**
     * Displays a slide-down educational tip banner highlighting the misclassified item and correct bin.
     *
     * @param tip EducationalTip data holding item name, correct category, message, and icon.
     */
    private void displayEducationalBanner(GameViewModel.EducationalTip tip) {
        if (layoutEducationalBanner == null || tvEduTitle == null || tvEduMessage == null) return;

        if (ivEduIcon != null) {
            if (tip.drawableResId != 0) {
                ivEduIcon.setImageResource(tip.drawableResId);
            } else {
                ivEduIcon.setImageResource(R.drawable.ic_tip_bulb);
            }
        }

        tvEduTitle.setText(tip.itemName + " ➔ " + tip.category.getDisplayName());
        int accentColor = 0xFFFFD54F;
        if (tip.category == WasteCategory.BIODEGRADABLE) {
            accentColor = 0xFF81C784;
        } else if (tip.category == WasteCategory.RECYCLABLE) {
            accentColor = 0xFF64B5F6;
        } else if (tip.category == WasteCategory.NON_BIODEGRADABLE) {
            accentColor = 0xFFFFAB91;
        }
        tvEduTitle.setTextColor(accentColor);
        tvEduMessage.setText(tip.tipMessage);

        if (hideTipRunnable != null) {
            uiHandler.removeCallbacks(hideTipRunnable);
        }

        layoutEducationalBanner.setVisibility(View.VISIBLE);
        layoutEducationalBanner.setAlpha(0f);
        layoutEducationalBanner.setTranslationY(-20f);
        layoutEducationalBanner.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(220)
                .start();

        hideTipRunnable = () -> {
            if (layoutEducationalBanner != null) {
                layoutEducationalBanner.animate()
                        .alpha(0f)
                        .translationY(-15f)
                        .setDuration(260)
                        .withEndAction(() -> layoutEducationalBanner.setVisibility(View.GONE))
                        .start();
            }
        };
        uiHandler.postDelayed(hideTipRunnable, 3400);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentDialog == null || !currentDialog.isShowing()) {
            viewModel.resumeGame();
        }
        gameView.startLoop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewModel.pauseGame();
        gameView.stopLoop();
    }

    // --- Dialogs ---

    /**
     * Displays pause overlay modal allowing Resume, Restart, or Main Menu navigation.
     */
    private void showPauseDialog() {
        viewModel.pauseGame();
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pause, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialogView.findViewById(R.id.btn_dialog_resume).setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.resumeGame();
        });

        dialogView.findViewById(R.id.btn_dialog_restart).setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.restartGame();
        });

        dialogView.findViewById(R.id.btn_dialog_menu).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        currentDialog = dialog;
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.show();
    }

    /**
     * Displays final game-over results modal with score summary and navigation actions.
     */
    private void showGameOverDialog(int finalScore, int itemsSorted, int crittersRescued, boolean isNewBest) {
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_game_over, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        TextView tvFinalScore = dialogView.findViewById(R.id.tv_dialog_final_score);
        TextView tvHighscoreBadge = dialogView.findViewById(R.id.tv_dialog_highscore_badge);
        TextView tvSorted = dialogView.findViewById(R.id.tv_dialog_items_sorted);
        TextView tvCritters = dialogView.findViewById(R.id.tv_dialog_critters_rescued);

        tvFinalScore.setText("Score: " + finalScore);
        tvSorted.setText(String.valueOf(itemsSorted));
        tvCritters.setText(String.valueOf(crittersRescued));

        if (isNewBest) {
            tvHighscoreBadge.setVisibility(View.VISIBLE);
        } else {
            tvHighscoreBadge.setVisibility(View.GONE);
        }

        dialogView.findViewById(R.id.btn_dialog_play_again).setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.restartGame();
        });

        dialogView.findViewById(R.id.btn_dialog_leaderboard).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, LeaderboardActivity.class));
            finish();
        });

        dialogView.findViewById(R.id.btn_dialog_quit).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        currentDialog = dialog;
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        dialog.show();
    }
}
