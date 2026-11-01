package com.tiwenraj.football;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private FootballRenderer renderer;
    private GLSurfaceView gameSurface;
    private TextView scoreLabel;
    private final List<Button> controlButtons = new ArrayList<>();
    private boolean highContrast = false;
    private boolean largeControls = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(8, 22, 32));
        getWindow().setNavigationBarColor(Color.rgb(8, 22, 32));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(12, 45, 33));

        renderer = new FootballRenderer(score -> runOnUiThread(() ->
                scoreLabel.setText("GOALS  " + score)));

        gameSurface = new GLSurfaceView(this);
        gameSurface.setEGLContextClientVersion(2);
        gameSurface.setRenderer(renderer);
        gameSurface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        gameSurface.setContentDescription(
                "Three-dimensional football training pitch. Use the movement buttons and Kick to score.");
        root.addView(gameSurface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(12), dp(6), dp(12), dp(6));
        topBar.setBackgroundColor(0xCC0B1C2A);

        scoreLabel = new TextView(this);
        scoreLabel.setText("GOALS  0");
        scoreLabel.setTextColor(Color.WHITE);
        scoreLabel.setTextSize(20);
        scoreLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        scoreLabel.setContentDescription("Goals scored: 0");
        topBar.addView(scoreLabel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(this);
        hint.setText("   TRAINING  •  Move, then kick");
        hint.setTextColor(Color.WHITE);
        hint.setTextSize(14);
        topBar.addView(hint, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button settings = new Button(this);
        settings.setText("Accessibility");
        settings.setAllCaps(false);
        settings.setContentDescription("Open accessibility settings");
        settings.setOnClickListener(v -> showAccessibilitySettings());
        topBar.addView(settings);

        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);
        root.addView(topBar, topParams);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(dp(8), dp(6), dp(8), dp(6));
        controls.setBackgroundColor(0xCC0B1C2A);

        addMovementButton(controls, "◀  LEFT", "Move player left", -1f, 0f);
        addMovementButton(controls, "▲  UP", "Move player forward toward the goal", 0f, 1f);
        addMovementButton(controls, "▼  DOWN", "Move player backward", 0f, -1f);
        addMovementButton(controls, "RIGHT  ▶", "Move player right", 1f, 0f);

        Button kick = new Button(this);
        kick.setText("KICK");
        kick.setAllCaps(false);
        kick.setTextSize(18);
        kick.setTypeface(null, android.graphics.Typeface.BOLD);
        kick.setContentDescription("Kick the football toward the goal");
        kick.setOnClickListener(v -> renderer.kick());
        controls.addView(kick, controlLayoutParams());
        controlButtons.add(kick);

        FrameLayout.LayoutParams controlsParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        root.addView(controls, controlsParams);

        setContentView(root);
    }

    private void addMovementButton(LinearLayout row, String text, String description,
                                   float x, float z) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setContentDescription(description);
        button.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                renderer.setMovement(x, z);
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                renderer.setMovement(0f, 0f);
            }
            return false;
        });
        // A tap or a screen-reader click also moves the player a short distance.
        button.setOnClickListener(v -> renderer.nudge(x, z));
        row.addView(button, controlLayoutParams());
        controlButtons.add(button);
    }

    private LinearLayout.LayoutParams controlLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, dp(58), 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        return params;
    }

    private void showAccessibilitySettings() {
        String[] choices = {"High-contrast pitch", "Larger on-screen controls"};
        boolean[] checked = {highContrast, largeControls};

        new AlertDialog.Builder(this)
                .setTitle("Accessibility settings")
                .setMultiChoiceItems(choices, checked, (dialog, which, isChecked) -> {
                    if (which == 0) {
                        highContrast = isChecked;
                        renderer.setHighContrast(isChecked);
                    } else {
                        largeControls = isChecked;
                        for (Button button : controlButtons) {
                            button.setTextSize(largeControls ? 22 : 16);
                            LinearLayout.LayoutParams params =
                                    (LinearLayout.LayoutParams) button.getLayoutParams();
                            params.height = dp(largeControls ? 72 : 58);
                            button.setLayoutParams(params);
                        }
                    }
                })
                .setPositiveButton("Done", null)
                .show();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onPause() {
        if (gameSurface != null) {
            gameSurface.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameSurface != null) {
            gameSurface.onResume();
        }
    }
}
