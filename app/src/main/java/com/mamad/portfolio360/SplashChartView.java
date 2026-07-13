package com.mamad.portfolio360;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * پس‌زمینه انیمیشنی صفحه اسپلش: یک نمودار خط/ناحیه‌ای صعودی شبیه نمودار بازار،
 * که به‌صورت تدریجی از چپ به راست با یک درخشش (glow) رسم می‌شود، به‌همراه یک
 * نقطه نبض‌دار در لبه پیشروی خط و یک شبکه کم‌رنگ در پس‌زمینه.
 */
public class SplashChartView extends View {

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path linePath = new Path();
    private final Path fillPath = new Path();
    private final PathMeasure pathMeasure = new PathMeasure();

    private float[] xs = new float[0];
    private float[] ys = new float[0];

    private float revealFraction = 0f; // ۰..۱ پیشرفت رسم خط
    private float pulseFraction = 0f;  // ۰..۱ برای نبض نقطه پیشرو (بعد از پایان رسم)

    private ValueAnimator revealAnimator;
    private ValueAnimator pulseAnimator;

    public SplashChartView(Context context) {
        super(context);
        init();
    }

    public SplashChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // BlurMaskFilter فقط روی لایه نرم‌افزاری کار می‌کند
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        gridPaint.setColor(Color.parseColor("#141E33"));
        gridPaint.setStrokeWidth(1.5f);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(Color.parseColor("#38BDF8"));

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(14f);
        glowPaint.setStrokeCap(Paint.Cap.ROUND);
        glowPaint.setStrokeJoin(Paint.Join.ROUND);
        glowPaint.setColor(Color.parseColor("#38BDF8"));
        glowPaint.setAlpha(90);
        glowPaint.setMaskFilter(new BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL));

        fillPaint.setStyle(Paint.Style.FILL);

        dotPaint.setColor(Color.parseColor("#E0F7FF"));
        dotGlowPaint.setColor(Color.parseColor("#38BDF8"));
        dotGlowPaint.setMaskFilter(new BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildTrendPoints(w, h);
        rebuildPaths();

        fillPaint.setShader(new LinearGradient(
                0, h * 0.28f, 0, h,
                new int[]{Color.parseColor("#4038BDF8"), Color.parseColor("#0038BDF8")},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP));
    }

    /** یک روند صعودی با نوسان ملایم (داده ثابت و از پیش تعیین‌شده، نه تصادفی در هر بازسازی). */
    private void buildTrendPoints(int w, int h) {
        float[] shape = {0.62f, 0.68f, 0.58f, 0.64f, 0.50f, 0.55f, 0.40f, 0.46f,
                0.34f, 0.38f, 0.24f, 0.30f, 0.16f, 0.20f, 0.10f};
        int n = shape.length;
        xs = new float[n];
        ys = new float[n];
        float topMargin = h * 0.14f;
        float bottomMargin = h * 0.10f;
        float usableH = h - topMargin - bottomMargin;

        for (int i = 0; i < n; i++) {
            xs[i] = (w * (i / (float) (n - 1)));
            ys[i] = topMargin + usableH * shape[i];
        }
    }

    private void rebuildPaths() {
        linePath.reset();
        if (xs.length == 0) return;

        linePath.moveTo(xs[0], ys[0]);
        for (int i = 1; i < xs.length; i++) {
            float midX = (xs[i - 1] + xs[i]) / 2f;
            linePath.cubicTo(midX, ys[i - 1], midX, ys[i], xs[i], ys[i]);
        }
    }

    /** انیمیشن رسم تدریجی خط را از ۰ آغاز می‌کند؛ در پایان، نبض نقطه پیشرو شروع می‌شود. */
    public void startReveal(long durationMs) {
        if (revealAnimator != null) revealAnimator.cancel();
        revealAnimator = ValueAnimator.ofFloat(0f, 1f);
        revealAnimator.setDuration(durationMs);
        revealAnimator.setInterpolator(new DecelerateInterpolator(1.3f));
        revealAnimator.addUpdateListener(a -> {
            revealFraction = (float) a.getAnimatedValue();
            invalidate();
        });
        revealAnimator.start();

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::startPulse, durationMs);
    }

    private void startPulse() {
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f, 0f);
        pulseAnimator.setDuration(1500);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.addUpdateListener(a -> {
            pulseFraction = (float) a.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (xs.length == 0) return;

        int w = getWidth();
        int h = getHeight();

        // شبکه کم‌رنگ پس‌زمینه
        int rows = 5;
        for (int i = 1; i < rows; i++) {
            float y = h * (i / (float) rows);
            canvas.drawLine(0, y, w, y, gridPaint);
        }

        pathMeasure.setPath(linePath, false);
        float totalLen = pathMeasure.getLength();
        float revealLen = totalLen * revealFraction;

        Path revealedLine = new Path();
        pathMeasure.getSegment(0, revealLen, revealedLine, true);

        // ناحیه پرشده زیر خط، تا لبه پیشروی فعلی
        fillPath.reset();
        float[] pos = new float[2];
        pathMeasure.getPosTan(revealLen, pos, null);
        fillPath.set(revealedLine);
        fillPath.lineTo(pos[0], h);
        fillPath.lineTo(xs[0], h);
        fillPath.close();
        canvas.drawPath(fillPath, fillPaint);

        canvas.drawPath(revealedLine, glowPaint);
        canvas.drawPath(revealedLine, linePaint);

        // نقطه نبض‌دار در لبه پیشروی خط
        float radius = 6f + pulseFraction * 10f;
        dotGlowPaint.setAlpha((int) (160 * (1f - pulseFraction * 0.7f)));
        canvas.drawCircle(pos[0], pos[1], radius + 10f, dotGlowPaint);
        canvas.drawCircle(pos[0], pos[1], 6f, dotPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (revealAnimator != null) revealAnimator.cancel();
        if (pulseAnimator != null) pulseAnimator.cancel();
    }
}
