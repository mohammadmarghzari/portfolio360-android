package com.mamad.portfolio360.wizard;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.mamad.portfolio360.R;
import com.mamad.portfolio360.calc.PayoffEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * صفحه نتیجه استراتژی: نمودار سود/زیان در سررسید،
 * همراه با شاخص‌های کلیدی و مقایسه با حالت «بدون استراتژی».
 */
public class StrategyResultFragment extends Fragment {

    private static final String ARG_STRATEGY = "strategy";
    private static final String ARG_INSTRUMENT = "instrument";
    private static final String ARG_STRIKE = "strike";
    private static final String ARG_PREMIUM = "premium";
    private static final String ARG_SPOT = "spot";

    // رنگ‌های نمودار (تم تیره حرفه‌ای)
    private static final int COLOR_STRATEGY = Color.parseColor("#38BDF8");
    private static final int COLOR_BASELINE = Color.parseColor("#64748B");
    private static final int COLOR_PROFIT = Color.parseColor("#22C55E");
    private static final int COLOR_LOSS = Color.parseColor("#EF4444");
    private static final int COLOR_GRID = Color.parseColor("#1E293B");
    private static final int COLOR_AXIS_TEXT = Color.parseColor("#94A3B8");

    public static StrategyResultFragment newInstance(String strategyKey, String instrumentName,
                                                      double strike, double premium, double spot) {
        StrategyResultFragment f = new StrategyResultFragment();
        Bundle b = new Bundle();
        b.putString(ARG_STRATEGY, strategyKey);
        b.putString(ARG_INSTRUMENT, instrumentName);
        b.putDouble(ARG_STRIKE, strike);
        b.putDouble(ARG_PREMIUM, premium);
        b.putDouble(ARG_SPOT, spot);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_strategy_result, container, false);

        Bundle args = requireArguments();
        String strategyKey = args.getString(ARG_STRATEGY, "");
        String instrument = args.getString(ARG_INSTRUMENT, "");
        double strike = args.getDouble(ARG_STRIKE);
        double premium = args.getDouble(ARG_PREMIUM);
        double spot = args.getDouble(ARG_SPOT);

        // قیمت خرید دارایی را برابر قیمت لحظه‌ای فرض می‌کنیم
        double costBasis = spot;

        List<PayoffEngine.Leg> legs;
        String title;

        switch (strategyKey) {
            case "protective_put":
                legs = PayoffEngine.protectivePut(costBasis, strike, premium, 1);
                title = getString(R.string.strategy_pp_title);
                break;
            case "covered_call":
            default:
                legs = PayoffEngine.coveredCall(costBasis, strike, premium, 1);
                title = getString(R.string.strategy_cc_title);
                break;
        }

        PayoffEngine.Result result = PayoffEngine.compute(legs, spot, 220);
        List<PayoffEngine.Leg> baseline = PayoffEngine.spotOnly(costBasis, 1);

        ((TextView) view.findViewById(R.id.result_strategy_name)).setText(title);
        ((TextView) view.findViewById(R.id.result_contract_name)).setText(instrument);

        setupChart(view.findViewById(R.id.payoff_chart), result, baseline, spot);
        bindKpis(view, result, premium, strategyKey);
        bindExplanation(view, result, strategyKey, strike, premium, spot);

        return view;
    }

    private void setupChart(LineChart chart, PayoffEngine.Result result,
                             List<PayoffEngine.Leg> baselineLegs, double spot) {

        List<Entry> strategyEntries = new ArrayList<>();
        List<Entry> baselineEntries = new ArrayList<>();

        for (int i = 0; i < result.prices.length; i++) {
            float x = (float) result.prices[i];
            strategyEntries.add(new Entry(x, (float) result.payoffs[i]));
            baselineEntries.add(new Entry(x, (float) PayoffEngine.totalPayoff(baselineLegs, result.prices[i])));
        }

        // منحنی استراتژی — با پرکردن گرادیانی
        LineDataSet strategySet = new LineDataSet(strategyEntries, "");
        strategySet.setColor(COLOR_STRATEGY);
        strategySet.setLineWidth(2.6f);
        strategySet.setDrawCircles(false);
        strategySet.setDrawValues(false);
        strategySet.setMode(LineDataSet.Mode.LINEAR);
        strategySet.setDrawFilled(true);
        strategySet.setFillAlpha(70);

        strategySet.setFillFormatter((dataSet, provider) -> 0f);
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

        // منحنی مرجع (بدون استراتژی) — خط‌چین خاکستری
        LineDataSet baselineSet = new LineDataSet(baselineEntries, "");
        baselineSet.setColor(COLOR_BASELINE);
        baselineSet.setLineWidth(1.4f);
        baselineSet.setDrawCircles(false);
        baselineSet.setDrawValues(false);
        baselineSet.enableDashedLine(12f, 8f, 0f);
        baselineSet.setDrawFilled(false);

        chart.setData(new LineData(baselineSet, strategySet));

        // ظاهر
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setExtraOffsets(4, 8, 4, 8);
        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(false);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(COLOR_AXIS_TEXT);
        x.setGridColor(COLOR_GRID);
        x.setAxisLineColor(COLOR_GRID);
        x.setTextSize(9f);
        x.setLabelCount(5, false);
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

        // خط عمودی قیمت لحظه‌ای
        LimitLine spotLine = new LimitLine((float) spot, getString(R.string.chart_spot_label));
        spotLine.setLineColor(Color.parseColor("#FBBF24"));
        spotLine.setLineWidth(1.4f);
        spotLine.enableDashedLine(8f, 6f, 0f);
        spotLine.setTextColor(Color.parseColor("#FBBF24"));
        spotLine.setTextSize(9f);
        spotLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        x.addLimitLine(spotLine);

        // خطوط عمودی نقاط سر به سر
        for (Double be : result.breakevens) {
            LimitLine beLine = new LimitLine(be.floatValue(), getString(R.string.chart_breakeven_label));
            beLine.setLineColor(Color.parseColor("#A78BFA"));
            beLine.setLineWidth(1.1f);
            beLine.enableDashedLine(6f, 5f, 0f);
            beLine.setTextColor(Color.parseColor("#A78BFA"));
            beLine.setTextSize(9f);
            beLine.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_BOTTOM);
            x.addLimitLine(beLine);
        }

        chart.animateX(700);
        chart.invalidate();
    }

    private void bindKpis(View view, PayoffEngine.Result r, double premium, String strategyKey) {
        TextView maxProfit = view.findViewById(R.id.kpi_max_profit);
        TextView maxLoss = view.findViewById(R.id.kpi_max_loss);
        TextView breakeven = view.findViewById(R.id.kpi_breakeven);
        TextView prem = view.findViewById(R.id.kpi_premium);

        maxProfit.setText(r.profitUnlimited
                ? getString(R.string.kpi_unlimited)
                : fmt(r.maxProfit));

        maxLoss.setText(r.lossUnlimited
                ? getString(R.string.kpi_unlimited_loss)
                : fmt(r.maxLoss));

        if (r.breakevens.isEmpty()) {
            breakeven.setText("—");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < r.breakevens.size(); i++) {
                if (i > 0) sb.append(" / ");
                sb.append(fmt(r.breakevens.get(i)));
            }
            breakeven.setText(sb.toString());
        }

        boolean received = "covered_call".equals(strategyKey);
        prem.setText((received ? "+" : "−") + fmt(Math.abs(premium)));
    }

    private void bindExplanation(View view, PayoffEngine.Result r, String strategyKey,
                                  double strike, double premium, double spot) {
        TextView tv = view.findViewById(R.id.result_explanation);

        String text;
        if ("protective_put".equals(strategyKey)) {
            text = getString(R.string.explain_protective_put,
                    fmt(strike), fmt(premium), fmt(Math.abs(r.maxLoss)),
                    r.breakevens.isEmpty() ? "—" : fmt(r.breakevens.get(0)));
        } else {
            text = getString(R.string.explain_covered_call,
                    fmt(strike), fmt(premium), fmt(r.maxProfit),
                    r.breakevens.isEmpty() ? "—" : fmt(r.breakevens.get(0)));
        }
        tv.setText(text);
    }

    private String fmt(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "—";
        return String.format(Locale.US, "%,.2f", v);
    }
}
