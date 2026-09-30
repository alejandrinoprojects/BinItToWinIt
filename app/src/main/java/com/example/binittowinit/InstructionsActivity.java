package com.example.binittowinit;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Educational Tutorial Activity for "Bin It to Win It".
 * <p>
 * Displays a guide explaining:
 * <ul>
 *   <li>The 3 designated municipal garbage bins: Biodegradable, Recyclable, and Non-Biodegradable.</li>
 *   <li>The Wandering Critters mechanic (*"Animals don't belong in the trash! Shoo them to safety!"*).</li>
 *   <li>Touchscreen controls: Dragging to sort into bins, and flicking/flinging to shoo animals.</li>
 *   <li>Item physics archetypes and environmental wind gust weather effects.</li>
 *   <li>The 3 Hearts lives system, penalties for wrong sorting, and streak score multipliers.</li>
 * </ul>
 * </p>
 */
public class InstructionsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructions);

        // Apply system window insets so the app bar and back button sit safely below status bar / camera cutout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_instructions), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        // Back button to return to the Main Menu
        findViewById(R.id.btn_back_instructions).setOnClickListener(v -> finish());
    }
}
