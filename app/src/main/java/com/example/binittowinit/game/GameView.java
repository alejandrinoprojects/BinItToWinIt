package com.example.binittowinit.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.VelocityTracker;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.binittowinit.R;
import com.example.binittowinit.audio.ISoundManager;
import com.example.binittowinit.audio.SoundManager;
import com.example.binittowinit.model.GarbageBin;
import com.example.binittowinit.model.WasteCategory;
import com.example.binittowinit.model.WasteItem;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Custom hardware-accelerated SurfaceView hosting the 2D game presentation
 * on a dedicated background Render Thread.
 * <p>
 * Decouples physics and rendering from the Android Main UI Thread, delivering locked
 * 60fps rendering free of UI layout passes, garbage collection pauses, or input jank.
 * </p>
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private final Random random = new Random();
    private GameEngine engine;
    private ISoundManager soundManager;

    private final Object renderLock = new Object();
    private RenderThread renderThread;
    private boolean isSurfaceReady = false;

    // --- Paint & Graphics Pipeline ---
    private Paint textPaint;
    private Paint binLabelPaint;
    private Paint curbPaint;
    private Paint curbHighlightPaint;
    private Paint streetPaint;
    private Paint seamPaint;
    private Paint dropShadowPaint;
    private Paint discFillPaint;
    private Paint ringPaint;
    private Paint critterPulsePaint;
    private Paint critterAuraPaint;
    private Paint itemTagBgPaint;
    private Paint itemTagBorderPaint;
    private Paint itemTagTextPaint;
    private Paint pillBgPaint;
    private Paint pillStrokePaint;
    private Paint dropPromptPaint;
    private final Map<Integer, Drawable> drawableCache = new HashMap<>();

    // --- Park Scenery Graphics Pipeline ---
    private Paint skyPaint;
    private Paint sunGlowPaint;
    private Paint sunCorePaint;
    private Paint sunbeamPaint;
    private Paint skylineFarPaint;
    private Paint skylineMidPaint;
    private Paint skylineWindowPaint;
    private Paint cloudPaint;
    private Paint cloudShadowPaint;
    private Paint distantHillPaint;
    private Paint midHillPaint;
    private Paint parkLawnPaint;
    private Paint stonePathPaint;
    private Paint stoneSlabPaint;
    private Paint pondRimPaint;
    private Paint pondWaterPaint;
    private Paint pondRipplePaint;
    private Paint fountainStonePaint;
    private Paint fountainSprayPaint;
    private Paint benchWoodPaint;
    private Paint benchWoodLightPaint;
    private Paint ironWorkPaint;
    private Paint lampGlowPaint;
    private Paint lampGlassPaint;
    private Paint fencePostPaint;
    private Paint fenceRailPaint;
    private Paint treeTrunkPaint;
    private Paint treeBarkDarkPaint;
    private Paint treeFoliageDarkPaint;
    private Paint treeFoliageMidPaint;
    private Paint treeFoliageLightPaint;
    private Paint treeFoliageHighlightPaint;
    private Paint treeShadowPaint;
    private float treeSwayTimer = 0f;
    private Paint flowerDotPaint;
    private Paint sidewalkPaint;
    private Paint sidewalkPavingPaint;
    private Paint dotPaint;
    private Paint specularPaint;
    private Paint laneGlowPaint;

    private final Path distantHillPath = new Path();
    private final Path midHillPath = new Path();
    private final Path lawnPath = new Path();
    private final Path windingStonePath = new Path();
    private final Path sunbeamPath = new Path();
    private final Path scratchPath = new Path();
    private float cloudDrift = 0f;

    // --- Touch Interaction & Gesture Tracking ---
    private WasteItem draggedItem = null;
    private float touchOffsetX = 0f;
    private float touchOffsetY = 0f;
    private VelocityTracker velocityTracker = null;

    // --- Frame Loop Timing ---
    private long lastFrameTimeNanos = 0;
    private boolean isLoopRunning = false;

    // --- Visual Feedback Timers & Camera Trauma ---
    private float cameraTrauma = 0f;
    private float traumaTime = 0f;
    private float redFlashTimer = 0f;

    /**
     * Constructs a GameView programmatically.
     *
     * @param context Android context.
     */
    public GameView(Context context) {
        super(context);
        init();
    }

    /**
     * Constructs a GameView from XML layout inflation.
     *
     * @param context Android context.
     * @param attrs   XML attribute set.
     */
    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Initializes paints, typefaces, shadows, and audio manager.
     */
    private void init() {
        getHolder().addCallback(this);
        soundManager = SoundManager.getInstance(getContext());

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setShadowLayer(4f, 2f, 2f, 0x88000000);

        binLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        binLabelPaint.setColor(Color.WHITE);
        binLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        binLabelPaint.setTextAlign(Paint.Align.CENTER);

        curbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        curbPaint.setColor(0xFF78909C);

        curbHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        curbHighlightPaint.setColor(0xFFCFD8DC);
        curbHighlightPaint.setStrokeWidth(3f);

        streetPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        streetPaint.setColor(0xFF263238);

        seamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        seamPaint.setColor(0x22FFFFFF);
        seamPaint.setStrokeWidth(2f);

        dropShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        discFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);

        critterPulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        critterPulsePaint.setStyle(Paint.Style.STROKE);
        critterPulsePaint.setColor(0x66FF4081);

        critterAuraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        critterAuraPaint.setColor(0x33FFD54F);

        itemTagBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        itemTagBgPaint.setColor(0xDD1B242C);

        itemTagBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        itemTagBorderPaint.setStyle(Paint.Style.STROKE);
        itemTagBorderPaint.setStrokeWidth(2f);

        itemTagTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        itemTagTextPaint.setColor(Color.WHITE);
        itemTagTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        itemTagTextPaint.setTextAlign(Paint.Align.CENTER);

        pillBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pillBgPaint.setColor(0xEE1E272E);

        pillStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pillStrokePaint.setStyle(Paint.Style.STROKE);
        pillStrokePaint.setStrokeWidth(3f);

        dropPromptPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dropPromptPaint.setColor(Color.WHITE);
        dropPromptPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        dropPromptPaint.setTextAlign(Paint.Align.CENTER);
        dropPromptPaint.setTextSize(26f);
        dropPromptPaint.setShadowLayer(6f, 0f, 2f, 0x88000000);

        // Park Scenery Paints
        skyPaint = new Paint();

        sunGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        sunbeamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunbeamPaint.setColor(0x16FFF9C4);

        skylineFarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        skylineFarPaint.setColor(0xFF90CAF9);

        skylineMidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        skylineMidPaint.setColor(0xFF78B7E2);

        skylineWindowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        skylineWindowPaint.setColor(0x66E1F5FE);

        cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cloudPaint.setColor(Color.WHITE);

        cloudShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cloudShadowPaint.setColor(0xFFD6E4EC);

        distantHillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        distantHillPaint.setColor(0xFFA5D6A7);

        midHillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        midHillPaint.setColor(0xFF81C784);

        parkLawnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        parkLawnPaint.setColor(0xFF4CAF50);

        stonePathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stonePathPaint.setColor(0xFFD7CCC8);

        stoneSlabPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        stoneSlabPaint.setColor(0xFFEFEBE9);

        pondRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pondRimPaint.setColor(0xFFCFD8DC);

        pondWaterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pondWaterPaint.setColor(0xFF29B6F6);

        pondRipplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pondRipplePaint.setStyle(Paint.Style.STROKE);
        pondRipplePaint.setStrokeWidth(2.5f);
        pondRipplePaint.setColor(0x88E1F5FE);

        fountainStonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fountainStonePaint.setColor(0xFFECEFF1);

        fountainSprayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fountainSprayPaint.setStyle(Paint.Style.STROKE);
        fountainSprayPaint.setStrokeWidth(3f);
        fountainSprayPaint.setStrokeCap(Paint.Cap.ROUND);
        fountainSprayPaint.setColor(0xCCFFFFFF);

        benchWoodPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        benchWoodPaint.setColor(0xFF8D6E63);

        benchWoodLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        benchWoodLightPaint.setColor(0xFFA1887F);

        ironWorkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ironWorkPaint.setColor(0xFF263238);
        ironWorkPaint.setStrokeWidth(3.5f);

        lampGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lampGlowPaint.setColor(0x44FFF59D);

        lampGlassPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lampGlassPaint.setColor(0xFFFFF9C4);

        fencePostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fencePostPaint.setColor(0xFF6D4C41);

        fenceRailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fenceRailPaint.setColor(0xFF8D6E63);
        fenceRailPaint.setStrokeWidth(5f);

        treeTrunkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        treeTrunkPaint.setColor(0xFF5D4037);

        treeBarkDarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        treeBarkDarkPaint.setColor(0xFF3E2723);

        treeFoliageDarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        treeFoliageDarkPaint.setColor(0xFF1B5E20);

        treeFoliageMidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        treeFoliageMidPaint.setColor(0xFF2E7D32);

        treeFoliageLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        treeFoliageLightPaint.setColor(0xFF43A047);

        treeFoliageHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        treeFoliageHighlightPaint.setColor(0xFF81C784);

        treeShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        treeShadowPaint.setColor(0x33000000);

        flowerDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        sidewalkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sidewalkPaint.setColor(0xFFECEFF1);

        sidewalkPavingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sidewalkPavingPaint.setColor(0xFFCFD8DC);
        sidewalkPavingPaint.setStrokeWidth(2f);

        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        specularPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        laneGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    /**
     * Attaches the GameEngine and configures screen dimensions if already measured.
     *
     * @param engine The GameEngine controller.
     */
    public void setEngine(GameEngine engine) {
        this.engine = engine;
        if (getWidth() > 0 && getHeight() > 0) {
            engine.setScreenDimensions(getWidth(), getHeight());
        }
    }

    /**
     * Starts or resumes the background render thread.
     */
    public void startLoop() {
        isLoopRunning = true;
        if (isSurfaceReady) {
            startRenderThread();
        }
    }

    /**
     * Stops the background render thread when activity is paused.
     */
    public void stopLoop() {
        isLoopRunning = false;
        stopRenderThread();
    }

    private synchronized void startRenderThread() {
        if (!isLoopRunning || !isSurfaceReady) return;
        if (renderThread == null || !renderThread.isAlive()) {
            renderThread = new RenderThread(getHolder());
            renderThread.setRunning(true);
            renderThread.start();
        }
    }

    private synchronized void stopRenderThread() {
        if (renderThread != null) {
            renderThread.setRunning(false);
            try {
                renderThread.join(350);
            } catch (InterruptedException ignored) {}
            renderThread = null;
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        isSurfaceReady = true;
        startRenderThread();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        synchronized (renderLock) {
            if (engine != null && width > 0 && height > 0) {
                engine.setScreenDimensions(width, height);
            }
            if (width > 0 && height > 0) {
                setupParkScenery(width, height);
            }
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        isSurfaceReady = false;
        stopRenderThread();
    }

    /**
     * Triggers a punchy screen-shake visual effect via non-linear camera trauma and red flash.
     */
    public void triggerScreenShake() {
        synchronized (renderLock) {
            cameraTrauma = Math.min(1.0f, cameraTrauma + 0.85f);
            redFlashTimer = 0.35f;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        synchronized (renderLock) {
            if (engine != null && w > 0 && h > 0) {
                engine.setScreenDimensions(w, h);
            }
            if (w > 0 && h > 0) {
                setupParkScenery(w, h);
            }
        }
    }

    /**
     * Precomputes park scenery gradients, winding paths, and rolling hill paths for smooth 60fps rendering.
     */
    private void setupParkScenery(int w, int h) {
        float u = w / 400f;
        float binTopY = h * 0.70f;
        if (engine != null && !engine.getBins().isEmpty()) {
            binTopY = engine.getBins().get(0).getBounds().top;
        }
        float promenadeY = binTopY - 10f * u;

        // Vibrant sunny sky gradient with warm horizon glow
        skyPaint.setShader(new LinearGradient(
                0, 0, 0, promenadeY * 0.72f,
                new int[]{0xFF1E88E5, 0xFF4FC3F7, 0xFFB3E5FC, 0xFFFFFDE7},
                new float[]{0.0f, 0.42f, 0.82f, 1.0f},
                Shader.TileMode.CLAMP));

        // Shaded gradient for distant hills
        distantHillPaint.setShader(new LinearGradient(
                0, promenadeY * 0.44f, 0, promenadeY * 0.80f,
                0xFFA5D6A7, 0xFF66BB6A, Shader.TileMode.CLAMP));

        // Shaded gradient for midground hills
        midHillPaint.setShader(new LinearGradient(
                0, promenadeY * 0.58f, 0, promenadeY * 0.92f,
                0xFF81C784, 0xFF388E3C, Shader.TileMode.CLAMP));

        // Shaded gradient for foreground park meadow
        parkLawnPaint.setShader(new LinearGradient(
                0, promenadeY * 0.78f, 0, promenadeY,
                0xFF66BB6A, 0xFF2E7D32, Shader.TileMode.CLAMP));

        // Sparkling turquoise pond gradient
        pondWaterPaint.setShader(new LinearGradient(
                w * 0.50f, promenadeY * 0.78f, w * 0.82f, promenadeY * 0.90f,
                0xFF4FC3F7, 0xFF0288D1, Shader.TileMode.CLAMP));

        // Textured promenade sidewalk gradient
        sidewalkPaint.setShader(new LinearGradient(
                0, promenadeY, 0, h,
                0xFFF5F7F8, 0xFFCFD8DC, Shader.TileMode.CLAMP));

        // 1. Distant rolling hill path
        distantHillPath.reset();
        distantHillPath.moveTo(0, promenadeY * 0.62f);
        distantHillPath.quadTo(w * 0.30f, promenadeY * 0.46f, w * 0.60f, promenadeY * 0.58f);
        distantHillPath.quadTo(w * 0.82f, promenadeY * 0.50f, w, promenadeY * 0.60f);
        distantHillPath.lineTo(w, promenadeY);
        distantHillPath.lineTo(0, promenadeY);
        distantHillPath.close();

        // 2. Midground rolling hill path
        midHillPath.reset();
        midHillPath.moveTo(0, promenadeY * 0.74f);
        midHillPath.quadTo(w * 0.38f, promenadeY * 0.60f, w * 0.72f, promenadeY * 0.72f);
        midHillPath.quadTo(w * 0.88f, promenadeY * 0.66f, w, promenadeY * 0.73f);
        midHillPath.lineTo(w, promenadeY);
        midHillPath.lineTo(0, promenadeY);
        midHillPath.close();

        // 3. Foreground lush park lawn path
        lawnPath.reset();
        lawnPath.moveTo(0, promenadeY * 0.85f);
        lawnPath.quadTo(w * 0.48f, promenadeY * 0.78f, w, promenadeY * 0.84f);
        lawnPath.lineTo(w, promenadeY);
        lawnPath.lineTo(0, promenadeY);
        lawnPath.close();

        // 4. Winding perspective stone pathway from midground hill down to promenade
        windingStonePath.reset();
        windingStonePath.moveTo(w * 0.36f, promenadeY * 0.66f);
        windingStonePath.quadTo(w * 0.24f, promenadeY * 0.78f, w * 0.40f, promenadeY);
        windingStonePath.lineTo(w * 0.54f, promenadeY);
        windingStonePath.quadTo(w * 0.33f, promenadeY * 0.78f, w * 0.41f, promenadeY * 0.66f);
        windingStonePath.close();
    }

    /**
     * Dedicated background render thread executing physics, animations, and hardware canvas draw.
     */
    private class RenderThread extends Thread {
        private volatile boolean running = false;
        private final SurfaceHolder surfaceHolder;
        private long lastTimeNanos = 0;

        public RenderThread(SurfaceHolder surfaceHolder) {
            super("GameRenderThread");
            this.surfaceHolder = surfaceHolder;
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        @Override
        public void run() {
            while (running) {
                long now = System.nanoTime();
                if (lastTimeNanos == 0) {
                    lastTimeNanos = now;
                }
                float deltaTime = (now - lastTimeNanos) / 1_000_000_000.0f;
                deltaTime = Math.min(deltaTime, 0.05f); // Cap delta to prevent physics warping
                lastTimeNanos = now;

                synchronized (renderLock) {
                    if (engine != null) {
                        engine.update(deltaTime);
                    }

                    // Animate gentle drifting of park clouds and foliage sway
                    cloudDrift += deltaTime * 14f;
                    treeSwayTimer += deltaTime * 2.2f;
                    int viewWidth = getWidth();
                    if (cloudDrift > (viewWidth + 300f)) {
                        cloudDrift = 0f;
                    }

                    if (cameraTrauma > 0) {
                        traumaTime += deltaTime * 28f;
                        cameraTrauma = Math.max(0f, cameraTrauma - deltaTime * 1.8f);
                    }
                    if (redFlashTimer > 0) {
                        redFlashTimer -= deltaTime;
                    }
                }

                Canvas canvas = null;
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        canvas = surfaceHolder.lockHardwareCanvas();
                    } else {
                        canvas = surfaceHolder.lockCanvas();
                    }
                    if (canvas != null) {
                        synchronized (renderLock) {
                            drawGameScene(canvas);
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    if (canvas != null) {
                        try {
                            surfaceHolder.unlockCanvasAndPost(canvas);
                        } catch (Exception ignored) {}
                    }
                }

                // Smooth 60fps frame pacing (~16ms)
                long elapsedNanos = System.nanoTime() - now;
                long sleepMs = 16 - (elapsedNanos / 1_000_000);
                if (sleepMs > 1) {
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException ignored) {}
                }
            }
        }
    }

    /**
     * Primary presentation render method called exclusively from the background RenderThread.
     */
    private void drawGameScene(Canvas canvas) {
        if (engine == null) return;

        canvas.save();

        // Apply punchy non-linear camera trauma screen-shake (offset = trauma^2 * shakeMagnitude)
        if (cameraTrauma > 0) {
            float shakeIntensity = cameraTrauma * cameraTrauma;
            float maxOffset = 32f * shakeIntensity;
            float ox = (float) Math.sin(traumaTime) * maxOffset;
            float oy = (float) Math.cos(traumaTime * 1.3f) * maxOffset;
            canvas.translate(ox, oy);
        }

        // 1. Draw cartoon park landscape background (sky, sun, clouds, hills, trees, fence, lawn)
        drawParkLandscape(canvas);

        // 2. Draw environmental effects (wind gust breeze lines)
        engine.getEnvironmentManager().render(canvas);

        // 3. Draw municipal garbage bins & pavement
        drawBins(canvas);

        // 4. Draw falling items (non-dragged first, active dragged item on top)
        for (WasteItem item : engine.getActiveItems()) {
            if (item != draggedItem) {
                drawItem(canvas, item, 1.0f);
            }
        }

        if (draggedItem != null) {
            drawItem(canvas, draggedItem, 1.25f);
        }

        // 5. Draw floating score popups
        drawFloatingTexts(canvas);

        // 6. Draw red damage flash vignette on life loss
        if (redFlashTimer > 0) {
            int alpha = (int) (120 * (redFlashTimer / 0.35f));
            canvas.drawColor((alpha << 24) | 0xFF0000);
        }

        canvas.restore();
    }

    /**
     * Renders a rich, multi-layered vector park landscape using XML vector drawables
     * (ic_scenery_oak_tree, ic_scenery_fountain_pond, ic_scenery_park_bench, ic_scenery_streetlamp)
     * and hardware-accelerated Canvas paths for locked 60fps performance.
     */
    private void drawParkLandscape(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Density-independent proportional unit based on screen width
        float u = w / 400f;

        float binTopY = h * 0.70f;
        if (engine != null && !engine.getBins().isEmpty()) {
            binTopY = engine.getBins().get(0).getBounds().top;
        }
        float promenadeY = binTopY - 10f * u;

        // 1. Vibrant Daytime Sky Gradient
        canvas.drawRect(0, 0, w, promenadeY, skyPaint);

        // 2. Radiant Sun & Diagonal Volumetric Sunbeams
        float sunX = w * 0.84f;
        float sunY = promenadeY * 0.16f;
        drawSunbeams(canvas, sunX, sunY, w, promenadeY);
        sunGlowPaint.setColor(0x28FFF59D);
        canvas.drawCircle(sunX, sunY, 58f * u, sunGlowPaint);
        sunGlowPaint.setColor(0x48FFEE58);
        canvas.drawCircle(sunX, sunY, 40f * u, sunGlowPaint);
        sunCorePaint.setColor(0xFFFFF9C4);
        canvas.drawCircle(sunX, sunY, 24f * u, sunCorePaint);

        // 3. Distant Eco-City Skyline Silhouette & Rotating Wind Turbines
        drawEcoSkyline(canvas, w, promenadeY, u);

        // 4. Drifting Fluffy Cumulus Clouds
        float wrapW = w + 260f * u;
        float c1x = ((w * 0.14f + cloudDrift * u) % wrapW) - 130f * u;
        float c2x = ((w * 0.56f + cloudDrift * 0.7f * u) % wrapW) - 130f * u;
        float c3x = ((w * 0.86f + cloudDrift * 0.45f * u) % wrapW) - 130f * u;
        drawCartoonCloud(canvas, c1x, promenadeY * 0.14f, 0.90f * u);
        drawCartoonCloud(canvas, c2x, promenadeY * 0.22f, 0.72f * u);
        drawCartoonCloud(canvas, c3x, promenadeY * 0.11f, 0.60f * u);

        // 5. Distant Rolling Hills (Shaded Gradient)
        canvas.drawPath(distantHillPath, distantHillPaint);

        // 6. Distant Forest Treeline (Tucked cleanly behind the midground hill ridge)
        drawDistantTreeline(canvas, w, promenadeY, u);

        // 7. Midground Rolling Hills & Foreground Park Meadow (Shaded Gradients)
        canvas.drawPath(midHillPath, midHillPaint);
        canvas.drawPath(lawnPath, parkLawnPaint);

        // 8. Winding Cobblestone Pathway with Sandstone Border & Flagstones
        drawWindingStonePath(canvas, w, promenadeY, u);

        // 9. Sparkling Park Pond & Tiered Stone Fountain (Vector XML + Live Animated Ripples & Spray)
        drawPondAndFountain(canvas, w * 0.71f, promenadeY * 0.85f, w * 0.22f, promenadeY * 0.085f, u);

        // 10. Wooden Park Benches (Vector XML) along the stone path and pond overlook
        drawParkBench(canvas, w * 0.23f, promenadeY * 0.88f, 0.88f * u);
        drawParkBench(canvas, w * 0.52f, promenadeY * 0.77f, 0.64f * u);

        // 11. Grass Tufts & Wildflower Clusters on the Meadow
        drawLawnDetails(canvas, w, promenadeY, u);

        // 12. Rustic Split-Rail Wooden Park Fence along the Promenade Edge
        float fenceTopY = promenadeY - 22f * u;
        fenceRailPaint.setStrokeWidth(3.2f * u);
        canvas.drawLine(0, fenceTopY + 5f * u, w, fenceTopY + 5f * u, fenceRailPaint);
        canvas.drawLine(0, fenceTopY + 14f * u, w, fenceTopY + 14f * u, fenceRailPaint);
        float postSpacing = 36f * u;
        float postHalfW = 2.8f * u;
        for (float px = 16f * u; px < w; px += postSpacing) {
            canvas.drawRoundRect(px - postHalfW, fenceTopY - 2f * u, px + postHalfW, promenadeY, 2f * u, 2f * u, fencePostPaint);
        }

        // 13. Flowering Park Shrubs along the Fence Line
        drawParkBush(canvas, w * 0.14f, promenadeY, 26f * u, 0xFFFF7043, u);
        drawParkBush(canvas, w * 0.34f, promenadeY, 22f * u, 0xFFFFEE58, u);
        drawParkBush(canvas, w * 0.62f, promenadeY, 24f * u, 0xFFEC407A, u);
        drawParkBush(canvas, w * 0.86f, promenadeY, 27f * u, 0xFFAB47BC, u);

        // 14. Vintage Wrought-Iron Park Streetlamps (Vector XML + Warm Pulsing Lantern Aura)
        drawVintageStreetlamp(canvas, w * 0.19f, promenadeY, 0.92f * u);
        drawVintageStreetlamp(canvas, w * 0.82f, promenadeY, 0.92f * u);

        // 15. Detailed Framing Oak Trees (Vector XML) firmly rooted on the left and right borders
        drawFramingParkTree(canvas, w, promenadeY, true);
        drawFramingParkTree(canvas, w, promenadeY, false);

        // 16. Textured Stonework Promenade Pavement from promenadeY down to bottom
        canvas.drawRect(0, promenadeY, w, h, sidewalkPaint);
        float course1Y = promenadeY + 14f * u;
        float course2Y = promenadeY + 28f * u;
        sidewalkPavingPaint.setStrokeWidth(1.2f * u);
        canvas.drawLine(0, course1Y, w, course1Y, sidewalkPavingPaint);
        canvas.drawLine(0, course2Y, w, course2Y, sidewalkPavingPaint);
        for (int i = 1; i <= 5; i++) {
            float sx = w * (i / 6f);
            canvas.drawLine(sx, promenadeY, sx, course1Y, sidewalkPavingPaint);
            float sxShifted = sx - (w / 12f);
            canvas.drawLine(sxShifted, course1Y, sxShifted, course2Y, sidewalkPavingPaint);
            canvas.drawLine(sx, course2Y, sx, h, sidewalkPavingPaint);
        }
        stoneSlabPaint.setColor(0x55FFFFFF);
        canvas.drawRoundRect(w * 0.05f, promenadeY + 2.5f * u, w * 0.14f, course1Y - 2.5f * u, 3f * u, 3f * u, stoneSlabPaint);
        canvas.drawRoundRect(w * 0.36f, promenadeY + 2.5f * u, w * 0.47f, course1Y - 2.5f * u, 3f * u, 3f * u, stoneSlabPaint);
        canvas.drawRoundRect(w * 0.70f, promenadeY + 2.5f * u, w * 0.80f, course1Y - 2.5f * u, 3f * u, 3f * u, stoneSlabPaint);
        stoneSlabPaint.setColor(0xFFEFEBE9);
    }

    /**
     * Draws a richly detailed framing oak tree firmly rooted on the left or right border using ic_scenery_oak_tree.xml.
     */
    private void drawFramingParkTree(Canvas canvas, int w, float promenadeY, boolean isLeft) {
        float shLeft = isLeft ? -w * 0.06f : w * 0.80f;
        float shRight = isLeft ? w * 0.20f : w * 1.06f;
        canvas.drawOval(shLeft, promenadeY - 5f, shRight, promenadeY + 9f, treeShadowPaint);

        Drawable oakDrawable = getCachedDrawable(R.drawable.ic_scenery_oak_tree);
        if (oakDrawable != null) {
            int treeW = (int) (w * 0.42f);
            int treeH = (int) (promenadeY * 0.56f);
            int top = (int) (promenadeY - treeH + 6f);
            int left = (int) (-w * 0.15f);
            canvas.save();
            if (!isLeft) {
                canvas.translate(w, 0);
                canvas.scale(-1f, 1f);
            }
            oakDrawable.setBounds(left, top, left + treeW, (int) (promenadeY + 6f));
            oakDrawable.draw(canvas);
            canvas.restore();
        }
    }

    /**
     * Draws warm diagonal volumetric light rays radiating from the sun across the park.
     */
    private void drawSunbeams(Canvas canvas, float sunX, float sunY, int w, float promenadeY) {
        float[][] rays = {
            {-0.55f, -0.35f},
            {-0.28f, -0.10f},
            {-0.02f, 0.14f}
        };
        for (float[] r : rays) {
            sunbeamPath.reset();
            sunbeamPath.moveTo(sunX, sunY);
            sunbeamPath.lineTo(sunX + w * r[0], promenadeY);
            sunbeamPath.lineTo(sunX + w * r[1], promenadeY);
            sunbeamPath.close();
            canvas.drawPath(sunbeamPath, sunbeamPaint);
        }
    }

    /**
     * Draws a soft atmospheric eco-city skyline silhouette and rotating wind turbines on the far horizon.
     */
    private void drawEcoSkyline(Canvas canvas, int w, float promenadeY, float u) {
        float baseY = promenadeY * 0.60f;

        float[][] farBuildings = {
            {0.12f, 0.19f, 0.43f},
            {0.21f, 0.28f, 0.39f},
            {0.29f, 0.35f, 0.45f},
            {0.52f, 0.59f, 0.41f},
            {0.61f, 0.68f, 0.36f},
            {0.70f, 0.77f, 0.43f}
        };
        for (float[] b : farBuildings) {
            canvas.drawRect(w * b[0], promenadeY * b[2], w * b[1], baseY, skylineFarPaint);
        }

        float[][] midBuildings = {
            {0.16f, 0.23f, 0.41f},
            {0.25f, 0.31f, 0.44f},
            {0.56f, 0.63f, 0.39f},
            {0.66f, 0.73f, 0.42f}
        };
        for (float[] b : midBuildings) {
            float left = w * b[0];
            float right = w * b[1];
            float top = promenadeY * b[2];
            canvas.drawRect(left, top, right, baseY, skylineMidPaint);
            canvas.drawOval(left, top - 4f * u, right, top + 4f * u, skylineMidPaint);
            float winW = (right - left) * 0.22f;
            for (float wy = top + 5f * u; wy < baseY - 15f * u; wy += 7f * u) {
                canvas.drawRect(left + winW, wy, left + winW * 2f, wy + 3.5f * u, skylineWindowPaint);
                canvas.drawRect(right - winW * 2f, wy, right - winW, wy + 3.5f * u, skylineWindowPaint);
            }
        }

        // Eco Wind Turbines rooted down into distantHillPath (baseY = promenadeY * 0.60f) so they never float
        drawWindTurbine(canvas, w * 0.39f, promenadeY * 0.48f, baseY, 15f * u, treeSwayTimer * 45f, u);
        drawWindTurbine(canvas, w * 0.46f, promenadeY * 0.50f, baseY, 13f * u, treeSwayTimer * 45f + 60f, u);
    }

    /**
     * Draws a clean white wind turbine with 3 rotating blades rooted into the horizon hill.
     */
    private void drawWindTurbine(Canvas canvas, float x, float hubY, float baseY, float bladeLen, float angleDeg, float u) {
        pondRimPaint.setColor(0xFFE1F5FE);
        pondRimPaint.setStrokeWidth(2.4f * u);
        canvas.drawLine(x, hubY, x, baseY, pondRimPaint);
        canvas.save();
        canvas.translate(x, hubY);
        canvas.rotate(angleDeg);
        for (int i = 0; i < 3; i++) {
            canvas.drawLine(0, 0, 0, -bladeLen, pondRimPaint);
            canvas.rotate(120f);
        }
        canvas.drawCircle(0, 0, 2.5f * u, cloudPaint);
        canvas.restore();
    }

    /**
     * Draws a continuous scalloped forest canopy ridge and distant oak trees rooted behind the midground hill crest.
     */
    private void drawDistantTreeline(Canvas canvas, int w, float promenadeY, float u) {
        // Continuous scalloped background forest ridge (rooted well below midHillPath so no gaps appear)
        scratchPath.reset();
        float ridgeBaseY = promenadeY * 0.76f;
        scratchPath.moveTo(0, ridgeBaseY);
        int lobes = 14;
        float stepW = (float) w / lobes;
        for (int i = 0; i < lobes; i++) {
            float x1 = i * stepW;
            float x2 = (i + 1) * stepW;
            float cx = (x1 + x2) * 0.5f;
            float archVariation = (i % 3 == 0) ? 18f * u : ((i % 2 == 0) ? 12f * u : 8f * u);
            float ridgeCurveY = promenadeY * (0.65f - 0.04f * (float) Math.sin(i * 0.55f)) - archVariation;
            scratchPath.quadTo(cx, ridgeCurveY, x2, ridgeCurveY + 9f * u);
        }
        scratchPath.lineTo(w, ridgeBaseY);
        scratchPath.close();
        treeFoliageMidPaint.setColor(0xFF4CAF50);
        canvas.drawPath(scratchPath, treeFoliageMidPaint);
        treeFoliageMidPaint.setColor(0xFF2E7D32);

        // Render scaled distant oak trees tucked behind midHillPath
        Drawable oakDrawable = getCachedDrawable(R.drawable.ic_scenery_oak_tree);
        if (oakDrawable != null) {
            float[][] distantTrees = {
                {0.20f, 0.71f, 44f, 60f},
                {0.31f, 0.69f, 52f, 70f},
                {0.51f, 0.70f, 48f, 65f},
                {0.66f, 0.73f, 44f, 60f},
                {0.78f, 0.72f, 50f, 68f}
            };
            for (float[] dt : distantTrees) {
                int cx = (int) (w * dt[0]);
                int bottomY = (int) (promenadeY * dt[1]);
                int tw = (int) (dt[2] * u);
                int th = (int) (dt[3] * u);
                oakDrawable.setBounds(cx - tw / 2, bottomY - th, cx + tw / 2, bottomY);
                oakDrawable.draw(canvas);
            }
        }
    }

    /**
     * Draws the winding stone pathway with sandstone border and individual flagstones across the park meadow.
     */
    private void drawWindingStonePath(Canvas canvas, int w, float promenadeY, float u) {
        canvas.drawPath(windingStonePath, stonePathPaint);

        float[][] stones = {
            {0.38f, 0.69f, 9f, 3.2f},
            {0.36f, 0.73f, 11f, 3.8f},
            {0.35f, 0.78f, 13.5f, 4.5f},
            {0.36f, 0.83f, 16.5f, 5.5f},
            {0.39f, 0.89f, 20f, 6.5f},
            {0.44f, 0.95f, 24f, 7.5f}
        };
        for (float[] s : stones) {
            float sx = w * s[0];
            float sy = promenadeY * s[1];
            float rx = s[2] * u;
            float ry = s[3] * u;
            stoneSlabPaint.setColor(0xFFBCAAA4);
            canvas.drawOval(sx - rx, sy - ry + 1.5f * u, sx + rx, sy + ry + 1.5f * u, stoneSlabPaint);
            stoneSlabPaint.setColor(0xFFEFEBE9);
            canvas.drawOval(sx - rx, sy - ry, sx + rx, sy + ry, stoneSlabPaint);
        }
    }

    /**
     * Draws the sparkling park pond and tiered stone fountain using ic_scenery_fountain_pond.xml
     * augmented with live 60fps animated water ripples and top fountain spray arcs.
     */
    private void drawPondAndFountain(Canvas canvas, float cx, float cy, float rx, float ry, float u) {
        Drawable pondDrawable = getCachedDrawable(R.drawable.ic_scenery_fountain_pond);
        if (pondDrawable != null) {
            int pw = (int) (rx * 2.15f);
            int ph = (int) (pw * (150f / 260f));
            int left = (int) (cx - pw * 0.5f);
            int top = (int) (cy - ph * 0.64f);
            pondDrawable.setBounds(left, top, left + pw, top + ph);
            pondDrawable.draw(canvas);

            // Live animated concentric water ripple centered around the lower fountain basin (x=130/260, y=88/150)
            float ripplePhase = (treeSwayTimer * 0.55f) % 1.0f;
            int rAlpha = (int) ((1.0f - ripplePhase) * 150);
            pondRipplePaint.setStrokeWidth(1.4f * u);
            pondRipplePaint.setColor((rAlpha << 24) | 0xE1F5FE);
            float ripRx = pw * (0.17f + ripplePhase * 0.22f);
            float ripRy = ph * (0.065f + ripplePhase * 0.11f);
            float basinCenterY = top + ph * (88f / 150f);
            canvas.drawOval(cx - ripRx, basinCenterY - ripRy, cx + ripRx, basinCenterY + ripRy, pondRipplePaint);

            // Live animated fountain water spray arcs aligned to the finial spout tip (x=130/260, y=16/150)
            // landing cleanly inside the upper bowl (y=30/150, dx=13/260) and middle bowl (y=52/150, dx=22/260)
            fountainSprayPaint.setStrokeWidth(1.6f * u);
            float sprayBounce = (float) Math.sin(treeSwayTimer * 4.5f) * (1.2f * u);
            float spoutY = top + ph * (16f / 150f);
            float upperBowlY = top + ph * (30f / 150f);
            float midBowlY = top + ph * (52f / 150f);
            for (int side = -1; side <= 1; side += 2) {
                // Inner arc into upper bowl
                scratchPath.reset();
                scratchPath.moveTo(cx, spoutY);
                scratchPath.quadTo(
                        cx + side * pw * (7f / 260f),
                        spoutY - ph * (6f / 150f) + sprayBounce,
                        cx + side * pw * (13f / 260f),
                        upperBowlY);
                canvas.drawPath(scratchPath, fountainSprayPaint);

                // Outer arc into middle bowl
                scratchPath.reset();
                scratchPath.moveTo(cx, spoutY);
                scratchPath.quadTo(
                        cx + side * pw * (15f / 260f),
                        spoutY - ph * (4f / 150f) - sprayBounce,
                        cx + side * pw * (22f / 260f),
                        midBowlY);
                canvas.drawPath(scratchPath, fountainSprayPaint);
            }
        }
    }

    /**
     * Draws a detailed 3D wooden & wrought-iron park bench using ic_scenery_park_bench.xml.
     */
    private void drawParkBench(Canvas canvas, float cx, float cy, float scale) {
        Drawable benchDrawable = getCachedDrawable(R.drawable.ic_scenery_park_bench);
        if (benchDrawable != null) {
            int bw = (int) (82f * scale);
            int bh = (int) (bw * (75f / 120f));
            int left = (int) (cx - bw / 2f);
            int top = (int) (cy - bh * 0.65f);
            benchDrawable.setBounds(left, top, left + bw, top + bh);
            benchDrawable.draw(canvas);
        }
    }

    /**
     * Draws a vintage Victorian park streetlamp using ic_scenery_streetlamp.xml with a pulsing warm aura.
     */
    private void drawVintageStreetlamp(Canvas canvas, float x, float baseY, float scale) {
        Drawable lampDrawable = getCachedDrawable(R.drawable.ic_scenery_streetlamp);
        if (lampDrawable != null) {
            int lw = (int) (46f * scale);
            int lh = (int) (lw * (200f / 70f));
            int left = (int) (x - lw / 2f);
            int top = (int) (baseY - lh + 4f * scale);

            // Pulsing warm golden lantern glow
            float glowPulse = 1.0f + 0.12f * (float) Math.sin(treeSwayTimer * 3.0f);
            lampGlowPaint.setColor(0x36FFF59D);
            canvas.drawCircle(x, top + lh * 0.25f, 18f * scale * glowPulse, lampGlowPaint);

            lampDrawable.setBounds(left, top, left + lw, (int) (baseY + 4f * scale));
            lampDrawable.draw(canvas);
        }
    }

    /**
     * Draws a multi-layered flowering park shrub with 5-petal blossoms along the promenade edge.
     */
    private void drawParkBush(Canvas canvas, float cx, float bottomY, float radius, int flowerColor, float u) {
        // 1. Deep shadow foliage base
        canvas.drawCircle(cx - radius * 0.65f, bottomY - radius * 0.36f, radius * 0.62f, treeFoliageDarkPaint);
        canvas.drawCircle(cx + radius * 0.65f, bottomY - radius * 0.36f, radius * 0.62f, treeFoliageDarkPaint);
        canvas.drawCircle(cx, bottomY - radius * 0.56f, radius * 0.76f, treeFoliageDarkPaint);

        // 2. Mid forest green body
        canvas.drawCircle(cx - radius * 0.55f, bottomY - radius * 0.42f, radius * 0.52f, treeFoliageMidPaint);
        canvas.drawCircle(cx + radius * 0.55f, bottomY - radius * 0.42f, radius * 0.52f, treeFoliageMidPaint);
        canvas.drawCircle(cx, bottomY - radius * 0.62f, radius * 0.65f, treeFoliageMidPaint);

        // 3. Sunlit top leaf crowns
        canvas.drawCircle(cx - radius * 0.38f, bottomY - radius * 0.52f, radius * 0.36f, treeFoliageLightPaint);
        canvas.drawCircle(cx + radius * 0.40f, bottomY - radius * 0.50f, radius * 0.36f, treeFoliageLightPaint);
        canvas.drawCircle(cx + radius * 0.05f, bottomY - radius * 0.72f, radius * 0.46f, treeFoliageLightPaint);

        // 4. Multi-petal flower blossoms with sunny yellow centers
        float[][] flowers = {
            {-0.42f, -0.45f},
            {0.06f, -0.68f},
            {0.44f, -0.42f}
        };
        float petalR = 2.2f * u;
        for (float[] f : flowers) {
            float fx = cx + radius * f[0];
            float fy = bottomY + radius * f[1];
            flowerDotPaint.setColor(flowerColor);
            canvas.drawCircle(fx - petalR, fy, petalR, flowerDotPaint);
            canvas.drawCircle(fx + petalR, fy, petalR, flowerDotPaint);
            canvas.drawCircle(fx, fy - petalR, petalR, flowerDotPaint);
            canvas.drawCircle(fx, fy + petalR, petalR, flowerDotPaint);
            flowerDotPaint.setColor(0xFFFFF59D);
            canvas.drawCircle(fx, fy, petalR * 0.65f, flowerDotPaint);
        }
    }

    /**
     * Draws small decorative grass tufts across the lawn for natural texture.
     */
    private void drawLawnDetails(Canvas canvas, int w, float promenadeY, float u) {
        treeFoliageDarkPaint.setColor(0xFF2E7D32);
        treeFoliageDarkPaint.setStrokeWidth(1.8f * u);
        float[][] tufts = {
            {0.14f, 0.88f}, {0.19f, 0.93f}, {0.31f, 0.91f},
            {0.58f, 0.93f}, {0.82f, 0.90f}, {0.88f, 0.94f}
        };
        for (float[] t : tufts) {
            float tx = w * t[0];
            float ty = promenadeY * t[1];
            canvas.drawLine(tx, ty, tx - 3.5f * u, ty - 5.5f * u, treeFoliageDarkPaint);
            canvas.drawLine(tx, ty, tx, ty - 7f * u, treeFoliageDarkPaint);
            canvas.drawLine(tx, ty, tx + 3.5f * u, ty - 5.5f * u, treeFoliageDarkPaint);
        }
        treeFoliageDarkPaint.setColor(0xFF1B5E20);
    }

    /**
     * Draws an individual fluffy cartoon cumulus cloud.
     */
    private void drawCartoonCloud(Canvas canvas, float cx, float cy, float scale) {
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(scale, scale);

        // Soft cloud shadow under-layer
        canvas.drawCircle(-25f, 5f, 22f, cloudShadowPaint);
        canvas.drawCircle(5f, 0f, 28f, cloudShadowPaint);
        canvas.drawCircle(32f, 4f, 20f, cloudShadowPaint);
        canvas.drawRect(-25f, 8f, 32f, 24f, cloudShadowPaint);

        // Crisp white cloud body
        canvas.drawCircle(-25f, 2f, 22f, cloudPaint);
        canvas.drawCircle(5f, -4f, 28f, cloudPaint);
        canvas.drawCircle(32f, 1f, 20f, cloudPaint);
        canvas.drawRect(-25f, 4f, 32f, 20f, cloudPaint);

        canvas.restore();
    }

    /**
     * Renders the 3 sorting bins with borders, icons, labels, and catch animations.
     */
    private void drawBins(Canvas canvas) {
        if (engine.getBins().isEmpty()) return;

        float binBottomY = engine.getBins().get(0).getBounds().bottom;
        int screenW = getWidth();
        int screenH = getHeight();

        // Curb stone strip below bins
        float curbY = binBottomY + 8f;
        canvas.drawRect(0, curbY, screenW, curbY + 16f, curbPaint);
        canvas.drawLine(0, curbY, screenW, curbY, curbHighlightPaint);

        // Asphalt Street surface below curb
        canvas.drawRect(0, curbY + 16f, screenW, screenH, streetPaint);

        // Asphalt texture / seam lines
        canvas.drawLine(screenW * 0.33f, curbY + 16f, screenW * 0.33f, screenH, seamPaint);
        canvas.drawLine(screenW * 0.66f, curbY + 16f, screenW * 0.66f, screenH, seamPaint);

        for (GarbageBin bin : engine.getBins()) {
            RectF bounds = bin.getBounds();
            float scale = bin.getCatchAnimationScale();

            // Contact drop-shadow grounding the wheelie bin on the pavement
            dropShadowPaint.setColor(0x55000000);
            canvas.drawOval(bounds.left + 16f, bounds.bottom - 10f, bounds.right - 16f, bounds.bottom + 12f, dropShadowPaint);

            // Check if any active item is heading towards or in this bin's lane
            boolean hasIncomingItem = false;
            for (WasteItem item : engine.getActiveItems()) {
                if (item.getCategory() == bin.getCategory() && Math.abs(item.getX() - bounds.centerX()) < bounds.width() * 0.75f) {
                    hasIncomingItem = true;
                    break;
                }
            }

            // Subtle landing-zone indicator glow column when an item is falling toward the correct lane or dragged
            if (bin.isHighlighted() || hasIncomingItem) {
                int glowColor = bin.getColor() & 0x00FFFFFF;
                int topAlpha = bin.isHighlighted() ? 0x66000000 : 0x22000000;
                laneGlowPaint.setShader(new LinearGradient(
                        0, bounds.top - 160f, 0, bounds.bottom,
                        0x00000000 | glowColor, topAlpha | glowColor, Shader.TileMode.CLAMP));
                canvas.drawRect(bounds.left - 4f, bounds.top - 160f, bounds.right + 4f, bounds.bottom, laneGlowPaint);
            }

            // Highlight drop-zone spotlight beam when dragging an item over this bin
            if (bin.isHighlighted()) {
                Path beamPath = new Path();
                beamPath.moveTo(bounds.left + 15f, bounds.top + 25f);
                beamPath.lineTo(bounds.right - 15f, bounds.top + 25f);
                beamPath.lineTo(bounds.right + 25f, bounds.top - 160f);
                beamPath.lineTo(bounds.left - 25f, bounds.top - 160f);
                beamPath.close();

                Paint beamPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                int glowColor = bin.getColor() & 0x00FFFFFF;
                beamPaint.setShader(new LinearGradient(
                        0, bounds.top - 160f, 0, bounds.top + 25f,
                        0x11000000 | glowColor, 0x66000000 | glowColor, Shader.TileMode.CLAMP));
                canvas.drawPath(beamPath, beamPaint);

                // Animated drop prompt chevron
                canvas.drawText("▼ DROP HERE ▼", bounds.centerX(), bounds.top - 45f, dropPromptPaint);
            }

            canvas.save();
            canvas.scale(scale, scale, bounds.centerX(), bounds.bottom);

            // Draw full 3D vector wheelie bin
            Drawable d = getCachedDrawable(bin.getDrawableResId());
            if (d != null) {
                d.setBounds((int) bounds.left, (int) bounds.top, (int) bounds.right, (int) bounds.bottom);
                d.draw(canvas);
            }

            // Category color and title
            String labelText;
            int badgeColor;
            if (bin.getCategory() == WasteCategory.BIODEGRADABLE) {
                labelText = "ORGANIC";
                badgeColor = 0xFF2E7D32;
            } else if (bin.getCategory() == WasteCategory.RECYCLABLE) {
                labelText = "RECYCLABLE";
                badgeColor = 0xFF1976D2;
            } else {
                labelText = "NON-BIO";
                badgeColor = 0xFFF57C00;
            }

            // Draw colored rounded chip directly on/above the bin opening
            binLabelPaint.setTextSize(Math.max(22f, bounds.width() * 0.11f));
            binLabelPaint.setColor(Color.WHITE);
            binLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            binLabelPaint.setTextAlign(Paint.Align.CENTER);

            float textWidth = binLabelPaint.measureText(labelText);
            float pillPadding = 18f;
            float pillW = textWidth + pillPadding * 2f;
            float pillH = 34f;
            // Positioned cleanly directly above the bin opening collar
            float pillY = bounds.top - 16f;

            RectF pillRect = new RectF(
                    bounds.centerX() - pillW / 2f,
                    pillY - pillH / 2f,
                    bounds.centerX() + pillW / 2f,
                    pillY + pillH / 2f
            );

            // Drop shadow for the chip
            dropShadowPaint.setColor(0x33000000);
            canvas.drawRoundRect(new RectF(pillRect.left, pillRect.top + 2f, pillRect.right, pillRect.bottom + 3f), pillH / 2f, pillH / 2f, dropShadowPaint);

            // Pill colored solid matching bin category
            pillBgPaint.setColor(badgeColor);
            canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillBgPaint);

            // Pill subtle white rim highlight
            pillStrokePaint.setColor(0x80FFFFFF);
            pillStrokePaint.setStrokeWidth(1.8f);
            canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, pillStrokePaint);

            // Crisp category label typography centered
            Paint.FontMetrics fm = binLabelPaint.getFontMetrics();
            float textY = pillRect.centerY() - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(labelText, pillRect.centerX(), textY, binLabelPaint);

            canvas.restore();
        }
    }

    /**
     * Renders an individual waste item or critter with glossy floating bubble, subtle gloss highlights, and translucent dark name tag.
     */
    private void drawItem(Canvas canvas, WasteItem item, float extraScale) {
        Drawable d = getCachedDrawable(item.getDrawableResId());
        if (d == null) return;

        // Determine category theme color
        int ringColor;
        int bubbleFill;
        if (item.getCategory() == WasteCategory.BIODEGRADABLE) {
            ringColor = 0xFF2E7D32; // Forest Green
            bubbleFill = 0xF2FFFFFF;
        } else if (item.getCategory() == WasteCategory.RECYCLABLE) {
            ringColor = 0xFF1976D2; // Vibrant Blue
            bubbleFill = 0xF2FFFFFF;
        } else if (item.getCategory() == WasteCategory.NON_BIODEGRADABLE) {
            ringColor = 0xFFF57C00; // Amber/Orange
            bubbleFill = 0xF2FFFFFF;
        } else {
            ringColor = 0xFFFF4081; // Rose Pink / Gold for Critters
            bubbleFill = 0xFFFFF8E1;
        }

        float itemRadius = (item.getSize() / 2f) * extraScale;

        canvas.save();
        canvas.translate(item.getX(), item.getY());

        // 1. Tactile elevation shadow underneath item while dragged or falling
        dropShadowPaint.setColor(item.isBeingDragged() ? 0x44000000 : 0x26000000);
        float shadowOffset = item.isBeingDragged() ? 12f : 5f;
        canvas.drawCircle(item.isBeingDragged() ? 4f : 1f, shadowOffset, itemRadius * 1.04f, dropShadowPaint);

        // 2. Extra pulsating halo for Wandering Critters
        if (item.getCategory() == WasteCategory.CRITTER_FLICK_AWAY) {
            canvas.drawCircle(0, 0, itemRadius * 1.25f, critterPulsePaint);
            canvas.drawCircle(0, 0, itemRadius * 1.25f, critterAuraPaint);
        }

        // 3. Clean floating glossy bubble base
        discFillPaint.setColor(bubbleFill);
        canvas.drawCircle(0, 0, itemRadius, discFillPaint);

        // Subtle soft category rim outline
        ringPaint.setStrokeWidth(item.isBeingDragged() ? 5.5f : 3.5f);
        ringPaint.setColor(ringColor);
        canvas.drawCircle(0, 0, itemRadius, ringPaint);

        // Glossy bubble highlight arc (top-left reflection)
        specularPaint.setColor(0x99FFFFFF);
        specularPaint.setStyle(Paint.Style.STROKE);
        specularPaint.setStrokeWidth(Math.max(2f, itemRadius * 0.08f));
        RectF specRect = new RectF(-itemRadius * 0.80f, -itemRadius * 0.80f, itemRadius * 0.80f, itemRadius * 0.80f);
        canvas.drawArc(specRect, 200f, 75f, false, specularPaint);

        // Tiny specular dot glint
        specularPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(-itemRadius * 0.45f, -itemRadius * 0.45f, Math.max(2.5f, itemRadius * 0.07f), specularPaint);

        // 4. Rotated Item Vector Asset inside the token bubble
        canvas.save();
        canvas.rotate(item.getRotation());
        float iconHalf = itemRadius * 0.70f;
        d.setBounds((int) -iconHalf, (int) -iconHalf, (int) iconHalf, (int) iconHalf);
        d.draw(canvas);
        canvas.restore();

        // 5. Upright Floating Name Pill (Always readable, non-rotating, translucent dark pill #80000000)
        String tagText = item.getName();
        if (item.getCategory() == WasteCategory.CRITTER_FLICK_AWAY) {
            tagText = "🐾 " + tagText + " (FLICK!)";
        }

        itemTagTextPaint.setTextSize(Math.max(22f, item.getSize() * 0.16f));
        itemTagTextPaint.setColor(Color.WHITE);
        itemTagTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        float tagWidth = itemTagTextPaint.measureText(tagText) + 24f;
        float tagHeight = itemTagTextPaint.getTextSize() * 1.55f;
        float tagOffsetY = itemRadius + tagHeight * 0.8f;
        if (item.getY() > getHeight() - 250f) {
            tagOffsetY = -itemRadius - tagHeight * 0.8f;
        }

        RectF tagRect = new RectF(-tagWidth / 2f, tagOffsetY - tagHeight / 2f, tagWidth / 2f, tagOffsetY + tagHeight / 2f);

        // Translucent dark pill background (#80000000)
        itemTagBgPaint.setColor(0x80000000);
        canvas.drawRoundRect(tagRect, tagHeight / 2f, tagHeight / 2f, itemTagBgPaint);

        // Soft accent border
        itemTagBorderPaint.setColor(ringColor & 0x66FFFFFF);
        itemTagBorderPaint.setStrokeWidth(1.5f);
        canvas.drawRoundRect(tagRect, tagHeight / 2f, tagHeight / 2f, itemTagBorderPaint);

        Paint.FontMetrics tfm = itemTagTextPaint.getFontMetrics();
        float textBaseline = tagRect.centerY() - (tfm.ascent + tfm.descent) / 2f;
        itemTagTextPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(tagText, 0, textBaseline, itemTagTextPaint);

        canvas.restore();
    }

    /**
     * Renders floating points and feedback texts with alpha fade.
     */
    private void drawFloatingTexts(Canvas canvas) {
        textPaint.setTextSize(44f);
        for (GameEngine.FloatingText ft : engine.getFloatingTexts()) {
            textPaint.setColor(ft.color);
            textPaint.setAlpha((int) (255 * ft.getAlpha()));
            canvas.drawText(ft.text, ft.x, ft.y, textPaint);
        }
    }

    /**
     * Handles touch interactions: dragging to bins and flicking off-screen.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (engine == null || engine.getState() != GameEngine.State.RUNNING) {
            return super.onTouchEvent(event);
        }

        synchronized (renderLock) {
            float touchX = event.getX();
            float touchY = event.getY();

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (velocityTracker == null) {
                        velocityTracker = VelocityTracker.obtain();
                    } else {
                        velocityTracker.clear();
                    }
                    velocityTracker.addMovement(event);

                    // Detect top-most touched item in reverse list order
                    for (int i = engine.getActiveItems().size() - 1; i >= 0; i--) {
                        WasteItem item = engine.getActiveItems().get(i);
                        if (!item.isResolved() && !item.isFlicked() && item.contains(touchX, touchY)) {
                            draggedItem = item;
                            draggedItem.setBeingDragged(true);
                            touchOffsetX = touchX - item.getX();
                            touchOffsetY = touchY - item.getY();
                            break;
                        }
                    }
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (velocityTracker != null) {
                        velocityTracker.addMovement(event);
                    }

                    if (draggedItem != null) {
                        draggedItem.setX(touchX - touchOffsetX);
                        draggedItem.setY(touchY - touchOffsetY);
                        draggedItem.setBaseX(draggedItem.getX());

                        // Highlight bin when item hovers over it
                        for (GarbageBin bin : engine.getBins()) {
                            bin.setHighlighted(bin.contains(draggedItem.getX(), draggedItem.getY()));
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (draggedItem != null && velocityTracker != null) {
                        velocityTracker.addMovement(event);
                        velocityTracker.computeCurrentVelocity(1000); // Compute velocity in pixels per second
                        float vx = velocityTracker.getXVelocity();
                        float vy = velocityTracker.getYVelocity();
                        float speed = (float) Math.hypot(vx, vy);

                        // Clear bin highlights
                        GarbageBin targetBin = null;
                        for (GarbageBin bin : engine.getBins()) {
                            if (bin.contains(draggedItem.getX(), draggedItem.getY())) {
                                targetBin = bin;
                            }
                            bin.setHighlighted(false);
                        }

                        if (targetBin != null) {
                            // Action 1: Dropped inside a designated bin
                            engine.handleDropOnBin(draggedItem, targetBin);
                        } else if (speed > 900f && (vy < -400f || Math.abs(vx) > 500f)) {
                            // Action 2: Rapid flick gesture recognized
                            draggedItem.launchFlick(vx, vy);
                            if (soundManager != null) {
                                soundManager.playFlickWhoosh();
                            }
                        } else {
                            // Action 3: Released in open air without sufficient flick speed; resumes falling
                            draggedItem.setBeingDragged(false);
                        }

                        draggedItem = null;
                    }

                    if (velocityTracker != null) {
                        velocityTracker.recycle();
                        velocityTracker = null;
                    }
                    return true;
            }

            return super.onTouchEvent(event);
        }
    }

    /**
     * Retrieves or caches mutated drawables by resource ID.
     *
     * @param resId Android drawable resource ID.
     * @return Cached Drawable instance, or null if not found.
     */
    private Drawable getCachedDrawable(int resId) {
        if (!drawableCache.containsKey(resId)) {
            Drawable d = ContextCompat.getDrawable(getContext(), resId);
            if (d != null) {
                drawableCache.put(resId, d.mutate());
            }
        }
        return drawableCache.get(resId);
    }
}
