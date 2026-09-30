package com.example.binittowinit.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;

/**
 * High-performance, zero-static audio subsystem powered by Android's native {@link SoundPool}.
 * <p>
 * Eliminates audio buffer underruns, popping, and distorted static artifacts by pre-rendering
 * studio-quality 44.1kHz PCM WAV assets into cache with smooth anti-click envelopes and
 * streaming them via hardware-accelerated SoundPool voices.
 * </p>
 */
public class SoundManager implements ISoundManager {
    private static final String TAG = "SoundManager";
    private static final String PREFS_NAME = "binit_sound_prefs";
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final int SAMPLE_RATE = 44100;

    private static SoundManager instance;
    private final Context appContext;
    private final SharedPreferences prefs;
    private boolean soundEnabled;

    // --- SoundPool Engine ---
    private SoundPool soundPool;
    private int soundSuccess = 0;
    private int soundCritter = 0;
    private int soundError = 0;
    private int soundWhoosh = 0;
    private int soundGameOver = 0;
    private volatile boolean isLoaded = false;

    // --- Audio Focus & Ducking ---
    private final AudioManager audioManager;
    private final AudioManager.OnAudioFocusChangeListener focusChangeListener;
    private AudioFocusRequest focusRequest;
    private volatile float duckMultiplier = 1.0f;
    private boolean hasAudioFocus = false;

    /**
     * Obtains the shared singleton instance of SoundManager.
     *
     * @param context Application context.
     * @return Thread-safe SoundManager instance.
     */
    public static synchronized SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Constructs a SoundManager and initializes audio attributes and background WAV synthesis.
     *
     * @param context Application context.
     */
    public SoundManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true);
        this.audioManager = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);

        this.focusChangeListener = focusChange -> {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    duckMultiplier = 1.0f;
                    if (soundPool != null) soundPool.autoResume();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    duckMultiplier = 0.20f; // Duck to 20%
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                case AudioManager.AUDIOFOCUS_LOSS:
                    duckMultiplier = 0.0f;
                    if (soundPool != null) soundPool.autoPause();
                    break;
            }
        };

        initSoundPool();
    }

    private void initSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attributes)
                .build();

        // Asynchronously synthesize & load clean WAV files in background
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File dir = new File(appContext.getCacheDir(), "audio_v2");
                if (!dir.exists()) dir.mkdirs();

                File fSuccess = new File(dir, "snd_success.wav");
                File fCritter = new File(dir, "snd_critter.wav");
                File fError = new File(dir, "snd_error.wav");
                File fWhoosh = new File(dir, "snd_whoosh.wav");
                File fGameOver = new File(dir, "snd_gameover.wav");

                if (!fSuccess.exists()) writeWav(fSuccess, generateSuccessChime());
                if (!fCritter.exists()) writeWav(fCritter, generateCritterShoo());
                if (!fError.exists()) writeWav(fError, generateErrorBuzz());
                if (!fWhoosh.exists()) writeWav(fWhoosh, generateFlickWhoosh());
                if (!fGameOver.exists()) writeWav(fGameOver, generateGameOver());

                soundSuccess = soundPool.load(fSuccess.getAbsolutePath(), 1);
                soundCritter = soundPool.load(fCritter.getAbsolutePath(), 1);
                soundError = soundPool.load(fError.getAbsolutePath(), 1);
                soundWhoosh = soundPool.load(fWhoosh.getAbsolutePath(), 1);
                soundGameOver = soundPool.load(fGameOver.getAbsolutePath(), 1);

                isLoaded = true;
            } catch (Exception e) {
                Log.e(TAG, "Audio synthesis failed", e);
            }
        });
    }

    @Override
    @SuppressWarnings("deprecation")
    public synchronized void requestAudioFocus() {
        if (audioManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (focusRequest == null) {
                    AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build();
                    focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                            .setAudioAttributes(playbackAttributes)
                            .setAcceptsDelayedFocusGain(false)
                            .setOnAudioFocusChangeListener(focusChangeListener)
                            .build();
                }
                int res = audioManager.requestAudioFocus(focusRequest);
                hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
            } else {
                int res = audioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
            }
        } catch (Exception ignored) {}
        duckMultiplier = 1.0f;
        if (soundPool != null) soundPool.autoResume();
    }

    @Override
    @SuppressWarnings("deprecation")
    public synchronized void abandonAudioFocus() {
        if (audioManager == null || !hasAudioFocus) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (focusRequest != null) {
                    audioManager.abandonAudioFocusRequest(focusRequest);
                }
            } else {
                audioManager.abandonAudioFocus(focusChangeListener);
            }
        } catch (Exception ignored) {}
        hasAudioFocus = false;
        duckMultiplier = 1.0f;
        if (soundPool != null) soundPool.autoPause();
    }

    @Override
    public float getDuckMultiplier() {
        return duckMultiplier;
    }

    @Override
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    @Override
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
        if (!enabled && soundPool != null) {
            soundPool.autoPause();
        }
    }

    private void playSound(int soundId, float baseVolume) {
        if (!soundEnabled || !isLoaded || soundPool == null || soundId == 0) return;
        float vol = baseVolume * duckMultiplier;
        if (vol <= 0.01f) return;
        soundPool.play(soundId, vol, vol, 1, 0, 1.0f);
    }

    @Override
    public void playSuccessChime() {
        playSound(soundSuccess, 0.75f);
    }

    @Override
    public void playCritterShoo() {
        playSound(soundCritter, 0.80f);
    }

    @Override
    public void playErrorBuzz() {
        playSound(soundError, 0.70f);
    }

    @Override
    public void playFlickWhoosh() {
        playSound(soundWhoosh, 0.50f);
    }

    @Override
    public void playGameOver() {
        playSound(soundGameOver, 0.85f);
    }

    // --- Pure Wave Synthesizers (With Smooth Anti-Click Envelopes) ---

    /**
     * Synthesizes an arpeggiated 4-tone major chord bell chime (C5-E5-G5-C6) with natural decay.
     *
     * @return 16-bit PCM sample array.
     */
    private static short[] generateSuccessChime() {
        int durationMs = 320;
        int totalSamples = (int) (SAMPLE_RATE * (durationMs / 1000.0));
        short[] buffer = new short[totalSamples];

        double[] freqs = {523.25, 659.25, 783.99, 1046.50}; // C5 -> E5 -> G5 -> C6
        int noteDuration = totalSamples / freqs.length;

        for (int i = 0; i < totalSamples; i++) {
            int noteIndex = Math.min(i / noteDuration, freqs.length - 1);
            int localI = i - (noteIndex * noteDuration);
            double t = (double) i / SAMPLE_RATE;

            // Smooth attack (5ms) and decay envelope
            double attack = Math.min(1.0, (double) localI / (SAMPLE_RATE * 0.005));
            double decay = Math.exp(-2.5 * ((double) localI / noteDuration));
            double env = attack * decay;

            // Musical bell: fundamental + soft 2nd harmonic
            double sample = (Math.sin(2 * Math.PI * freqs[noteIndex] * t)
                    + 0.25 * Math.sin(4 * Math.PI * freqs[noteIndex] * t)) * env * 0.7;

            buffer[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (long) (sample * Short.MAX_VALUE)));
        }
        return buffer;
    }

    /**
     * Synthesizes an ascending frequency chirp (880Hz to 1320Hz) indicating a critter safely rescued.
     *
     * @return 16-bit PCM sample array.
     */
    private static short[] generateCritterShoo() {
        int durationMs = 180;
        int totalSamples = (int) (SAMPLE_RATE * (durationMs / 1000.0));
        short[] buffer = new short[totalSamples];

        for (int i = 0; i < totalSamples; i++) {
            double frac = (double) i / totalSamples;
            double t = (double) i / SAMPLE_RATE;

            // Chirp frequency smoothly glides up from 880Hz to 1320Hz
            double freq = 880.0 + (440.0 * Math.pow(frac, 1.2));
            double attack = Math.min(1.0, (double) i / (SAMPLE_RATE * 0.006));
            double decay = Math.cos(frac * Math.PI * 0.5);
            double env = attack * decay;

            double sample = Math.sin(2 * Math.PI * freq * t) * env * 0.75;
            buffer[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (long) (sample * Short.MAX_VALUE)));
        }
        return buffer;
    }

    /**
     * Synthesizes a warm low-frequency thump (160Hz down to 100Hz) with natural exponential decay.
     *
     * @return 16-bit PCM sample array.
     */
    private static short[] generateErrorBuzz() {
        int durationMs = 200;
        int totalSamples = (int) (SAMPLE_RATE * (durationMs / 1000.0));
        short[] buffer = new short[totalSamples];

        for (int i = 0; i < totalSamples; i++) {
            double frac = (double) i / totalSamples;
            double t = (double) i / SAMPLE_RATE;

            // Warm low thump (160Hz -> 100Hz) with natural decay, no harsh clipping
            double freq = 160.0 - (60.0 * frac);
            double attack = Math.min(1.0, (double) i / (SAMPLE_RATE * 0.004));
            double decay = Math.exp(-6.0 * frac);
            double env = attack * decay;

            double sample = Math.sin(2 * Math.PI * freq * t) * env * 0.85;
            buffer[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (long) (sample * Short.MAX_VALUE)));
        }
        return buffer;
    }

    /**
     * Synthesizes an aerodynamic pitch sweep (350Hz up to 750Hz) simulating a fast flick whoosh.
     *
     * @return 16-bit PCM sample array.
     */
    private static short[] generateFlickWhoosh() {
        int durationMs = 120;
        int totalSamples = (int) (SAMPLE_RATE * (durationMs / 1000.0));
        short[] buffer = new short[totalSamples];

        for (int i = 0; i < totalSamples; i++) {
            double frac = (double) i / totalSamples;
            double t = (double) i / SAMPLE_RATE;

            // Aerodynamic pitch sweep 350Hz -> 750Hz
            double freq = 350.0 + (400.0 * frac);
            double attack = Math.min(1.0, (double) i / (SAMPLE_RATE * 0.008));
            double decay = 1.0 - frac;
            double env = attack * decay;

            double sample = Math.sin(2 * Math.PI * freq * t) * env * 0.55;
            buffer[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (long) (sample * Short.MAX_VALUE)));
        }
        return buffer;
    }

    /**
     * Synthesizes a descending minor sequence cadence (G4-E4-D4-C4) denoting game over.
     *
     * @return 16-bit PCM sample array.
     */
    private static short[] generateGameOver() {
        int durationMs = 500;
        int totalSamples = (int) (SAMPLE_RATE * (durationMs / 1000.0));
        short[] buffer = new short[totalSamples];

        double[] freqs = {392.00, 329.63, 293.66, 261.63}; // G4 -> E4 -> D4 -> C4
        int noteDuration = totalSamples / freqs.length;

        for (int i = 0; i < totalSamples; i++) {
            int noteIndex = Math.min(i / noteDuration, freqs.length - 1);
            int localI = i - (noteIndex * noteDuration);
            double t = (double) i / SAMPLE_RATE;

            double attack = Math.min(1.0, (double) localI / (SAMPLE_RATE * 0.006));
            double decay = Math.exp(-3.0 * ((double) localI / noteDuration));
            double env = attack * decay;

            double sample = Math.sin(2 * Math.PI * freqs[noteIndex] * t) * env * 0.7;
            buffer[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (long) (sample * Short.MAX_VALUE)));
        }
        return buffer;
    }

    /**
     * Encodes 16-bit mono PCM sample buffers into standard RIFF WAVE (.wav) container files.
     *
     * @param file       Destination file on disk.
     * @param pcmSamples 16-bit PCM audio samples.
     * @throws IOException If file writing fails.
     */
    private static void writeWav(File file, short[] pcmSamples) throws IOException {
        int totalAudioLen = pcmSamples.length * 2;
        int totalDataLen = totalAudioLen + 36;
        int byteRate = SAMPLE_RATE * 2;

        try (FileOutputStream out = new FileOutputStream(file);
             DataOutputStream data = new DataOutputStream(new BufferedOutputStream(out))) {
            // RIFF header
            data.writeBytes("RIFF");
            data.writeInt(Integer.reverseBytes(totalDataLen));
            data.writeBytes("WAVE");

            // 'fmt ' subchunk
            data.writeBytes("fmt ");
            data.writeInt(Integer.reverseBytes(16)); // Subchunk1Size
            data.writeShort(Short.reverseBytes((short) 1)); // PCM format
            data.writeShort(Short.reverseBytes((short) 1)); // Mono
            data.writeInt(Integer.reverseBytes(SAMPLE_RATE));
            data.writeInt(Integer.reverseBytes(byteRate));
            data.writeShort(Short.reverseBytes((short) 2)); // BlockAlign
            data.writeShort(Short.reverseBytes((short) 16)); // BitsPerSample

            // 'data' subchunk
            data.writeBytes("data");
            data.writeInt(Integer.reverseBytes(totalAudioLen));

            for (short s : pcmSamples) {
                data.writeShort(Short.reverseBytes(s));
            }
        }
    }
}
