package com.mamad.portfolio360.wizard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.mamad.portfolio360.calc.PayoffEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * رسم نمودار سود/زیان (Payoff) روی یک LineChart — منطق مشترک بین صفحه نتیجه
 * استراتژی‌های چندپایه (StrategyResultFragment) و ماشین‌حساب‌های سریع
 * (کاوردکال، پروتکتیو پوت).
 */
public class PayoffChartRenderer {

    private static final int COLOR_STRATEGY = Color.parseColor("#38BDF8");
    private static final int COLOR_BASELINE = Color.parseColor("#64748B");
    private static final int COLOR_GRID = Color.parseColor("#1E293B");
    private static final int COLOR_AXIS_TEXT = Color.parseColor("#94A3B8");

    public static void render(LineChart chart, Context context, PayoffEngine.Result result,
                               List<PayoffEngine.Leg> baselineLegs, double markerCostBasis,
                               double spot, String spotLabel, String breakevenLabel) {

        List<Entry> strategyEntries = new ArrayList<>();
        List<Entry> baselineEntries = new ArrayList<>();

        for (int i = 0; i < result.prices.length; i++) {
            float x = (float) result.prices[i];
            strategyEntries.add(new Entry(x, (float) result.payoffs[i]));
            baselineEntries.add(new Entry(x, (float) PayoffEngine.totalPayoff(baselineLegs, result.prices[i])));
        }

        LineDataSet strategySet = new LineDataSet(strategyEntries, "");
        strategySet.setColor(COLOR_STRATEGY);
        strategySet.setLineWidth(2.6f);
        strategySet.setDrawCircles(false);
        strategySet.setDrawValues(false);
        strategySet.setMode(LineDataSet.Mode.LINEAR);
        strategySet.setDrawFilled(true);
        strategySet.setFillAlpha(70);
        strategySet.setHighlightEnabled(true);
        strategySet.setDrawHorizontalHighlightIndicator(false);
        strategySet.setHighlightLineWidth(1f);
        strategySet.setHighLightColor(Color.parseColor("#64748B"));

        strategySet.setFillDrawable(new android.graphics.drawable.Drawable() {
            @Override public void draw(@NonNull android.graphics.Canvas canvas) {
                Paint paint = new Paint();
                android.graphics.Rect b = getBounds();
                paint.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                        new int[]{
                                Color.argb(120, 34, 197, 94),
                                Color.argb(20, 34, 197, 94),
                                Color.argb(20, 239, 68, 68),
                                Color.argb(120, 239, 68, 68)},
                        new float[]{0f, 0.48f, 0.52f, 1f},
                        Shader.TileMode.CLAMP));
                canvas.drawRect(b, paint);
            }
            @Override public void setAlpha(int alpha) {}
            @Override public void setColorFilter(@Nullable android.graphics.ColorFilter cf) {}
            @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        });

        LineDataSet baselineSet = new LineDataSet(baselineEntries, "");
        baselineSet.setColor(COLOR_BASELINE);
        baselineSet.setLineWidth(1.4f);
        baselineSet.setDrawCircles(false);
        baselineSet.setDrawValues(false);
        baselineSet.enableDashedLine(12f, 8f, 0f);
        baselineSet.setDrawFilled(false);
        baselineSet.setHighlightEnabled(false);

        chart.setData(new LineData(baselineSet, strategySet));

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setExtraOffsets(4, 8, 4, 30);
        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setHighlightPerTapEnabled(true);
        chart.setHighlightPerDragEnabled(true);

        chart.setMarker(new PayoffMarkerView(context, markerCostBasis));

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(COLOR_AXIS_TEXT);
        x.setGridColor(COLOR_GRID);
        x.setAxisLineColor(COLOR_GRID);
        x.setTextSize(9f);
        x.setLabelCount(5, false);
        x.removeAllLimitLines();
        x.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return String.format(Locale.US, "%,.0f", value);
            }
        });

        YAxis left = chart.getAxisLeft();
        left.setTextColor(COLOR_AXIS_TEXT);
        left.setGridColor(COLOR_GRID);
        left.setAxisLineColor(COLOR_GRID);
        left.setTextSize(9f);
        left.setDrawZeroLine(true);
        left.setZeroLineColor(Color.parseColor("#475569"));
        left.setZeroLineWidth(1.2f);
        left.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return String.format(Locale.US, "%,.0f", value);
            }
        });

        chart.getAxisRight().setEnabled(false);

        LimitLine spotLine = new LimitLine((float) spot, spotLabel);
        spotLine.setLineColor(Color.parseColor("#FBBF24"));
        spotLine.setLineWidth(1.4f);
        spotLine.enableDashedLine(8f, 6f, 0f);
        spotLine.setTextColor(Color.parseColor("#FBBF24"));
        spotLine.setTextSize(9f);
        spotLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        x.addLimitLine(spotLine);

        for (Double be : result.breakevens) {
            LimitLine beLine = new LimitLine(be.floatValue(), breakevenLabel);
            beLine.setLineColor(Color.parseColor("#A78BFA"));
            beLine.setLineWidth(1.1f);
            beLine.enableDashedLine(6f, 5f, 0f);
            beLine.setTextColor(Color.parseColor("#A78BFA"));
            beLine.setTextSize(9f);
            beLine.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_BOTTOM);
            x.addLimitLine(beLine);
        }

        chart.animateX(500);
        chart.invalidate();
    }
}
