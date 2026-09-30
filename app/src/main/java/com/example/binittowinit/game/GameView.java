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
    private Paint cloudPaint;
    private Paint cloudShadowPaint;
    private Paint distantHillPaint;
    private Paint midHillPaint;
    private Paint parkLawnPaint;
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
     * Precomputes park scenery gradients and rolling hill paths for smooth 60fps rendering.
     */
    private void setupParkScenery(int w, int h) {
        float binTopY = h * 0.70f;
        if (engine != null && !engine.getBins().isEmpty()) {
            binTopY = engine.getBins().get(0).getBounds().top;
        }
        float promenadeY = binTopY - 28f;

        // Vibrant sunny sky gradient
        skyPaint.setShader(new LinearGradient(
                0, 0, 0, promenadeY,
                new int[]{0xFF38B6FF, 0xFF81D4FA, 0xFFE0F7FA},
                new float[]{0.0f, 0.50f, 1.0f},
                Shader.TileMode.CLAMP));

        // 1. Distant rolling hill path
        distantHillPath.reset();
        distantHillPath.moveTo(0, promenadeY * 0.62f);
        distantHillPath.quadTo(w * 0.32f, promenadeY * 0.44f, w * 0.62f, promenadeY * 0.58f);
        distantHillPath.quadTo(w * 0.82f, promenadeY * 0.50f, w, promenadeY * 0.62f);
        distantHillPath.lineTo(w, promenadeY);
        distantHillPath.lineTo(0, promenadeY);
        distantHillPath.close();

        // 2. Midground rolling hill path
        midHillPath.reset();
        midHillPath.moveTo(0, promenadeY * 0.74f);
        midHillPath.quadTo(w * 0.40f, promenadeY * 0.58f, w * 0.75f, promenadeY * 0.72f);
        midHillPath.quadTo(w * 0.90f, promenadeY * 0.66f, w, promenadeY * 0.72f);
        midHillPath.lineTo(w, promenadeY);
        midHillPath.lineTo(0, promenadeY);
        midHillPath.close();

        // 3. Foreground lush park lawn path
        lawnPath.reset();
        lawnPath.moveTo(0, promenadeY * 0.86f);
        lawnPath.quadTo(w * 0.50f, promenadeY * 0.80f, w, promenadeY * 0.85f);
        lawnPath.lineTo(w, promenadeY);
        lawnPath.lineTo(0, promenadeY);
        lawnPath.close();
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
     * Renders a cheerful cartoon park landscape: sunny gradient sky, warm sun, drifting clouds,
     * distant rolling hills, lush green park lawn, rustic wooden fence, and flanking trees.
     */
    private void drawParkLandscape(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float binTopY = h * 0.70f;
        if (engine != null && !engine.getBins().isEmpty()) {
            binTopY = engine.getBins().get(0).getBounds().top;
        }
        float promenadeY = binTopY - 28f;

        // 1. Sunny Sky Gradient
        canvas.drawRect(0, 0, w, promenadeY, skyPaint);

        // 2. Warm Cartoon Sun with glowing rings (top-right)
        float sunX = w * 0.85f;
        float sunY = h * 0.12f;
        sunGlowPaint.setColor(0x22FFF59D);
        canvas.drawCircle(sunX, sunY, 70f, sunGlowPaint);
        sunGlowPaint.setColor(0x44FFEE58);
        canvas.drawCircle(sunX, sunY, 48f, sunGlowPaint);
        sunCorePaint.setColor(0xFFFFF9C4);
        canvas.drawCircle(sunX, sunY, 30f, sunCorePaint);

        // 3. Fluffy Cartoon Clouds gently drifting
        float cloud1X = ((w * 0.15f + cloudDrift) % (w + 220f)) - 110f;
        drawCartoonCloud(canvas, cloud1X, h * 0.16f, 0.95f);

        float cloud2X = ((w * 0.58f + cloudDrift * 0.65f) % (w + 240f)) - 120f;
        drawCartoonCloud(canvas, cloud2X, h * 0.10f, 0.72f);

        float cloud3X = ((w * 0.82f + cloudDrift * 1.15f) % (w + 200f)) - 100f;
        drawCartoonCloud(canvas, cloud3X, h * 0.22f, 0.60f);

        // 4. Distant Rolling Hills
        canvas.drawPath(distantHillPath, distantHillPaint);

        // 5. Midground Rolling Hill
        canvas.drawPath(midHillPath, midHillPaint);

        // 6. Foreground Lush Green Park Meadow
        canvas.drawPath(lawnPath, parkLawnPaint);

        // 7. Rustic Wooden Split-Rail Park Fence along lawn border
        float fenceY = promenadeY - 26f;
        fenceRailPaint.setStrokeWidth(6f);
        canvas.drawLine(0, fenceY, w, fenceY, fenceRailPaint);
        canvas.drawLine(0, fenceY + 14f, w, fenceY + 14f, fenceRailPaint);

        // Fence posts across the screen
        int postCount = 7;
        float postSpacing = w / (float) (postCount + 1);
        for (int i = 1; i <= postCount; i++) {
            float px = i * postSpacing;
            // Wooden post
            canvas.drawRect(px - 7f, fenceY - 12f, px + 7f, fenceY + 30f, fencePostPaint);
            // Beveled post cap
            Path cap = new Path();
            cap.moveTo(px - 8f, fenceY - 12f);
            cap.lineTo(px, fenceY - 20f);
            cap.lineTo(px + 8f, fenceY - 12f);
            cap.close();
            canvas.drawPath(cap, fencePostPaint);
        }

        // 8. Flowering Park Bushes along the fence line
        int bushCount = 10;
        float bushSpacing = w / (float) bushCount;
        int[] flowerColors = {0xFFFFEB3B, 0xFFFFFFFF, 0xFFF48FB1, 0xFFFFEB3B, 0xFFBA68C8};
        for (int i = 0; i < bushCount; i++) {
            float bx = i * bushSpacing + bushSpacing * 0.5f;
            float by = promenadeY - 6f;
            // Leafy bush mound
            treeFoliageDarkPaint.setColor(0xFF2E7D32);
            canvas.drawCircle(bx, by, 18f, treeFoliageDarkPaint);
            treeFoliageLightPaint.setColor(0xFF43A047);
            canvas.drawCircle(bx + 4f, by - 4f, 13f, treeFoliageLightPaint);

            // Blossom dots
            flowerDotPaint.setColor(flowerColors[i % flowerColors.length]);
            canvas.drawCircle(bx - 5f, by - 6f, 3.5f, flowerDotPaint);
            canvas.drawCircle(bx + 7f, by - 3f, 3.5f, flowerDotPaint);
            flowerDotPaint.setColor(0xFFFF9800);
            canvas.drawCircle(bx - 5f, by - 6f, 1.5f, flowerDotPaint);
            canvas.drawCircle(bx + 7f, by - 3f, 1.5f, flowerDotPaint);
        }

        // 9. Framing Park Trees (Lush cartoon trees on screen borders)
        // 9. Framing Park Trees (Lush volumetric cartoon trees with organic curves and subtle wind sway)
        float currentWind = (engine != null) ? engine.getEnvironmentManager().getCurrentWindForce() : 0f;
        float swayPx = (float) Math.sin(treeSwayTimer) * (w * 0.005f) + (currentWind / 150f) * (w * 0.012f);

        // Ground shadow beneath Left Tree
        canvas.drawOval(-w * 0.04f, promenadeY - 8f, w * 0.16f, promenadeY + 12f, treeShadowPaint);

        // Left Trunk: Smooth curved woody trunk branching gracefully
        Path leftTrunk = new Path();
        leftTrunk.moveTo(-w * 0.04f, promenadeY);
        leftTrunk.quadTo(w * 0.02f, promenadeY * 0.74f, w * 0.04f + swayPx * 0.2f, promenadeY * 0.52f);
        // Main rising branch
        leftTrunk.lineTo(w * 0.07f + swayPx * 0.4f, promenadeY * 0.35f);
        leftTrunk.lineTo(w * 0.11f + swayPx * 0.4f, promenadeY * 0.37f);
        leftTrunk.quadTo(w * 0.07f, promenadeY * 0.48f, w * 0.10f + swayPx * 0.3f, promenadeY * 0.53f);
        // Side branch reaching inward
        leftTrunk.lineTo(w * 0.16f + swayPx * 0.3f, promenadeY * 0.46f);
        leftTrunk.lineTo(w * 0.17f + swayPx * 0.3f, promenadeY * 0.49f);
        leftTrunk.quadTo(w * 0.11f, promenadeY * 0.60f, w * 0.11f, promenadeY);
        leftTrunk.close();

        // Shaded bark back-edge
        canvas.drawPath(leftTrunk, treeBarkDarkPaint);
        // Warm main bark
        canvas.save();
        canvas.clipPath(leftTrunk);
        canvas.drawRect(w * 0.02f, 0, w * 0.30f, promenadeY + 20f, treeTrunkPaint);
        canvas.restore();

        // Left Canopy: Layered volumetric puff clouds scaled to screen size
        float lR1 = w * 0.16f;
        float lR2 = w * 0.13f;
        float lR3 = w * 0.11f;
        float lR4 = w * 0.09f;

        float lcx1 = w * 0.03f + swayPx;
        float lcy1 = promenadeY * 0.44f;
        float lcx2 = w * 0.10f + swayPx;
        float lcy2 = promenadeY * 0.48f;
        float lcx3 = w * 0.06f + swayPx;
        float lcy3 = promenadeY * 0.34f;
        float lcx4 = w * 0.13f + swayPx;
        float lcy4 = promenadeY * 0.38f;
        float lcx5 = w * 0.05f + swayPx;
        float lcy5 = promenadeY * 0.26f;

        // 1. Deep ambient shadow puffs (underside)
        canvas.drawCircle(lcx1, lcy1, lR1, treeFoliageDarkPaint);
        canvas.drawCircle(lcx2, lcy2, lR2, treeFoliageDarkPaint);

        // 2. Mid forest green body
        canvas.drawCircle(lcx1 + 6f, lcy1 - 6f, lR1 * 0.90f, treeFoliageMidPaint);
        canvas.drawCircle(lcx2 + 4f, lcy2 - 6f, lR2 * 0.90f, treeFoliageMidPaint);
        canvas.drawCircle(lcx3, lcy3, lR1 * 0.95f, treeFoliageMidPaint);
        canvas.drawCircle(lcx4, lcy4, lR2 * 0.85f, treeFoliageMidPaint);

        // 3. Vibrant sun-facing canopy crown
        canvas.drawCircle(lcx3 + 8f, lcy3 - 8f, lR1 * 0.78f, treeFoliageLightPaint);
        canvas.drawCircle(lcx4 + 6f, lcy4 - 6f, lR2 * 0.72f, treeFoliageLightPaint);
        canvas.drawCircle(lcx5, lcy5, lR3, treeFoliageLightPaint);

        // 4. Bright sunlit crest highlights facing the sun (top-right)
        canvas.drawCircle(lcx3 + 18f, lcy3 - 18f, lR3 * 0.55f, treeFoliageHighlightPaint);
        canvas.drawCircle(lcx4 + 14f, lcy4 - 14f, lR4 * 0.50f, treeFoliageHighlightPaint);
        canvas.drawCircle(lcx5 + 12f, lcy5 - 12f, lR4 * 0.60f, treeFoliageHighlightPaint);

        // Ground shadow beneath Right Tree
        canvas.drawOval(w * 0.84f, promenadeY - 8f, w * 1.04f, promenadeY + 12f, treeShadowPaint);

        // Right Trunk: Smooth curved woody trunk branching into screen
        Path rightTrunk = new Path();
        rightTrunk.moveTo(w * 1.04f, promenadeY);
        rightTrunk.quadTo(w * 0.98f, promenadeY * 0.74f, w * 0.96f + swayPx * 0.2f, promenadeY * 0.52f);
        // Main rising branch
        rightTrunk.lineTo(w * 0.93f + swayPx * 0.4f, promenadeY * 0.35f);
        rightTrunk.lineTo(w * 0.89f + swayPx * 0.4f, promenadeY * 0.37f);
        rightTrunk.quadTo(w * 0.93f, promenadeY * 0.48f, w * 0.90f + swayPx * 0.3f, promenadeY * 0.53f);
        // Side branch reaching inward left
        rightTrunk.lineTo(w * 0.84f + swayPx * 0.3f, promenadeY * 0.46f);
        rightTrunk.lineTo(w * 0.83f + swayPx * 0.3f, promenadeY * 0.49f);
        rightTrunk.quadTo(w * 0.89f, promenadeY * 0.60f, w * 0.89f, promenadeY);
        rightTrunk.close();

        // Shaded bark back-edge
        canvas.drawPath(rightTrunk, treeBarkDarkPaint);
        // Warm main bark
        canvas.save();
        canvas.clipPath(rightTrunk);
        canvas.drawRect(w * 0.70f, 0, w * 0.98f, promenadeY + 20f, treeTrunkPaint);
        canvas.restore();

        // Right Canopy: Layered volumetric puff clouds
        float rcx1 = w * 0.97f + swayPx;
        float rcy1 = promenadeY * 0.44f;
        float rcx2 = w * 0.90f + swayPx;
        float rcy2 = promenadeY * 0.48f;
        float rcx3 = w * 0.94f + swayPx;
        float rcy3 = promenadeY * 0.34f;
        float rcx4 = w * 0.87f + swayPx;
        float rcy4 = promenadeY * 0.38f;
        float rcx5 = w * 0.95f + swayPx;
        float rcy5 = promenadeY * 0.26f;

        // 1. Deep ambient shadow puffs (underside)
        canvas.drawCircle(rcx1, rcy1, lR1, treeFoliageDarkPaint);
        canvas.drawCircle(rcx2, rcy2, lR2, treeFoliageDarkPaint);

        // 2. Mid forest green body
        canvas.drawCircle(rcx1 + 6f, rcy1 - 6f, lR1 * 0.90f, treeFoliageMidPaint);
        canvas.drawCircle(rcx2 + 4f, rcy2 - 6f, lR2 * 0.90f, treeFoliageMidPaint);
        canvas.drawCircle(rcx3, rcy3, lR1 * 0.95f, treeFoliageMidPaint);
        canvas.drawCircle(rcx4, rcy4, lR2 * 0.85f, treeFoliageMidPaint);

        // 3. Vibrant sun-facing canopy crown
        canvas.drawCircle(rcx3 + 8f, rcy3 - 8f, lR1 * 0.78f, treeFoliageLightPaint);
        canvas.drawCircle(rcx4 + 6f, rcy4 - 6f, lR2 * 0.72f, treeFoliageLightPaint);
        canvas.drawCircle(rcx5, rcy5, lR3, treeFoliageLightPaint);

        // 4. Bright sunlit crest highlights facing the sun (top-right)
        canvas.drawCircle(rcx3 + 18f, rcy3 - 18f, lR3 * 0.55f, treeFoliageHighlightPaint);
        canvas.drawCircle(rcx4 + 14f, rcy4 - 14f, lR4 * 0.50f, treeFoliageHighlightPaint);
        canvas.drawCircle(rcx5 + 12f, rcy5 - 12f, lR4 * 0.60f, treeFoliageHighlightPaint);

        // 10. Park Sidewalk Promenade Pavement from promenadeY down to bottom
        canvas.drawRect(0, promenadeY, w, h, sidewalkPaint);
        // Flagstone expansion joints
        for (int i = 1; i <= 3; i++) {
            float sx = w * (i / 4f);
            canvas.drawLine(sx, promenadeY, sx, h, sidewalkPavingPaint);
        }
        canvas.drawLine(0, promenadeY + 50f, w, promenadeY + 50f, sidewalkPavingPaint);
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
