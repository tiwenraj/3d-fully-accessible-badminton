package com.tiwenraj.football;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
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
    private SoundManager sounds;
    private final List<Button> controlButtons = new ArrayList<>();
    private boolean highContrast = false;
    private boolean largeControls = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(8, 22, 32));
        getWindow().setNavigationBarColor(Color.rgb(8, 22, 32));

        sounds = new SoundManager();
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(12, 45, 33));

        renderer = new FootballRenderer(score -> runOnUiThread(() -> {
            scoreLabel.setText("POINTS  " + score);
            scoreLabel.setContentDescription("Points won: " + score);
            scoreLabel.announceForAccessibility("Point won! Your score is " + score);
            sounds.playPoint();
        }));

        gameSurface = new GLSurfaceView(this);
        gameSurface.setEGLContextClientVersion(2);
        gameSurface.setRenderer(renderer);
        gameSurface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        gameSurface.setContentDescription(
                "Three-dimensional badminton court. Move with the direction buttons and tap HIT to strike the shuttlecock.");
        root.addView(gameSurface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(12), dp(4), dp(12), dp(4));
        topBar.setBackgroundColor(0xCC0B1C2A);

        scoreLabel = new TextView(this);
        scoreLabel.setText("POINTS  0");
        scoreLabel.setTextColor(Color.WHITE);
        scoreLabel.setTextSize(20);
        scoreLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        scoreLabel.setContentDescription("Points won: 0");
        topBar.addView(scoreLabel);

        TextView hint = new TextView(this);
        hint.setText("   BADMINTON  •  Move and hit the shuttle");
        hint.setTextColor(Color.WHITE);
        hint.setTextSize(14);
        topBar.addView(hint, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button music = new Button(this);
        music.setAllCaps(false);
        music.setText("Music: ON");
        music.setContentDescription("Toggle background music");
        music.setOnClickListener(v -> {
            boolean enabled = !sounds.isMusicEnabled();
            sounds.setMusicEnabled(enabled);
            music.setText(enabled ? "Music: ON" : "Music: OFF");
            music.setContentDescription("Background music " + (enabled ? "on" : "off") + ". Toggle music");
        });
        topBar.addView(music);

        Button settings = new Button(this);
        settings.setText("Accessibility");
        settings.setAllCaps(false);
        settings.setContentDescription("Open accessibility settings");
        settings.setOnClickListener(v -> showAccessibilitySettings());
        topBar.addView(settings);

        root.addView(topBar, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(dp(8), dp(4), dp(8), dp(4));
        controls.setBackgroundColor(0xCC0B1C2A);

        addMovementButton(controls, "◀ LEFT", "Move player left", -1f, 0f);
        addMovementButton(controls, "▲ FORWARD", "Move player toward the net", 0f, 1f);
        addMovementButton(controls, "▼ BACK", "Move player backward", 0f, -1f);
        addMovementButton(controls, "RIGHT ▶", "Move player right", 1f, 0f);

        Button hit = new Button(this);
        hit.setText("HIT");
        hit.setAllCaps(false);
        hit.setTextSize(20);
        hit.setTypeface(null, android.graphics.Typeface.BOLD);
        hit.setContentDescription("Hit the badminton shuttlecock");
        hit.setOnClickListener(v -> {
            renderer.kick();
            sounds.playHit();
        });
        controls.addView(hit, controlLayoutParams());
        controlButtons.add(hit);

        root.addView(controls, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
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
            if (event.getAction() == MotionEvent.ACTION_DOWN) renderer.setMovement(x, z);
            else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) renderer.setMovement(0f, 0f);
            return false;
        });
        button.setOnClickListener(v -> renderer.nudge(x, z));
        row.addView(button, controlLayoutParams());
        controlButtons.add(button);
    }

    private LinearLayout.LayoutParams controlLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(58), 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private void showAccessibilitySettings() {
        String[] choices = {"High-contrast court", "Larger on-screen controls"};
        boolean[] checked = {highContrast, largeControls};
        new AlertDialog.Builder(this).setTitle("Accessibility settings")
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
                }).setPositiveButton("Done", null).show();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onPause() {
        if (gameSurface != null) gameSurface.onPause();
        if (sounds != null) sounds.stopMusic();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (gameSurface != null) gameSurface.onResume();
        if (sounds != null) sounds.startMusic();
    }

    @Override protected void onDestroy() {
        if (sounds != null) sounds.release();
        super.onDestroy();
    }
}
