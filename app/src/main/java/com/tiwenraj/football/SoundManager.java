package com.tiwenraj.football;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;

/** Original synthesized court ambience and short racket/point sound effects. */
public final class SoundManager {
    private static final int RATE = 22050;
    private volatile boolean musicEnabled = true;
    private volatile boolean running;
    private AudioTrack track;
    private Thread musicThread;
    private ToneGenerator tones;

    public SoundManager() {
        tones = new ToneGenerator(AudioManager.STREAM_MUSIC, 65);
    }

    public synchronized void startMusic() {
        if (running || !musicEnabled) return;
        running = true;
        int min = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = Math.max(min, 4096);
        track = new AudioTrack(AudioManager.STREAM_MUSIC, RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                bufferSize, AudioTrack.MODE_STREAM);
        track.setLoopPoints(0, -1, 0);
        track.play();
        musicThread = new Thread(() -> {
            short[] samples = new short[1024];
            long sampleIndex = 0;
            final double[] notes = {261.63, 329.63, 392.00, 329.63, 293.66, 349.23, 440.00, 349.23};
            while (running) {
                for (int i = 0; i < samples.length; i++, sampleIndex++) {
                    int note = (int) ((sampleIndex / (RATE / 2)) % notes.length);
                    double phase = 2.0 * Math.PI * notes[note] * sampleIndex / RATE;
                    double envelope = 0.5 + 0.5 * Math.sin(2.0 * Math.PI * sampleIndex / RATE / 4.0);
                    samples[i] = (short) (Math.sin(phase) * envelope * 1100);
                }
                AudioTrack current = track;
                if (current != null && running) current.write(samples, 0, samples.length);
            }
        }, "BadmintonMusic");
        musicThread.start();
    }

    public synchronized void stopMusic() {
        running = false;
        if (musicThread != null) {
            try { musicThread.join(300); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            musicThread = null;
        }
        if (track != null) {
            try { track.pause(); track.flush(); track.release(); } catch (IllegalStateException ignored) { }
            track = null;
        }
    }

    public void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
        if (enabled) startMusic(); else stopMusic();
    }

    public boolean isMusicEnabled() { return musicEnabled; }

    public void playHit() {
        if (tones != null) tones.startTone(ToneGenerator.TONE_PROP_BEEP, 110);
    }

    public void playPoint() {
        if (tones != null) tones.startTone(ToneGenerator.TONE_PROP_ACK, 260);
    }

    public void release() {
        stopMusic();
        if (tones != null) { tones.release(); tones = null; }
    }
}
