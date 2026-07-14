package com.mamad.portfolio360.screener;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mamad.portfolio360.R;
import com.mamad.portfolio360.calc.CoveredCallScreener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * صفحه‌ی «بهینه‌یاب کاوردکال»: کاربر قیمت، نوسان، بازه‌ی زمانی و بازده هدف را
 * وارد می‌کند؛ اپ بهترین قرارداد کال را برای فروش کاوردکال پیدا می‌کند و منحنی
 * افت ارزش زمانی چند سررسید را روی یک نمودار مقایسه می‌کند.
 */
public class CoveredCallScreenerFragment extends Fragment {

    private static final int[] LINE_COLORS = {
            Color.parseColor("#38BDF8"), Color.parseColor("#F2B33D"),
            Color.parseColor("#4ADE80"), Color.parseColor("#A78BFA"),
            Color.parseColor("#F87171")
    };
    private static final int COLOR_GRID = Color.parseColor("#1E293B");
    private static final int COLOR_AXIS_TEXT = Color.parseColor("#94A3B8");

    private TextInputEditText spotInput, ivInput, riskFreeInput, maxDaysInput, targetInput;
    private LineChart chart;
    private View resultCard, chartCard;
    private TextView verdictView, bestBody;
    private LinearLayout legendContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cc_screener, container, false);

        spotInput = view.findViewById(R.id.screener_spot);
        ivInput = view.findViewById(R.id.screener_iv);
        riskFreeInput = view.findViewById(R.id.screener_risk_free);
        maxDaysInput = view.findViewById(R.id.screener_max_days);
        targetInput = view.findViewById(R.id.screener_target);
        chart = view.findViewById(R.id.screener_chart);
        resultCard = view.findViewById(R.id.screener_result_card);
        chartCard = view.findViewById(R.id.screener_chart_card);
        verdictView = view.findViewById(R.id.screener_verdict);
        bestBody = view.findViewById(R.id.screener_best_body);
        legendContainer = view.findViewById(R.id.screener_legend);

        MaterialButton runButton = view.findViewById(R.id.btn_screener_run);
        runButton.setOnClickListener(v -> run());

        return view;
    }

    private void run() {
        double spot = parse(spotInput, Double.NaN);
        double iv = parse(ivInput, Double.NaN);
        double riskFree = parse(riskFreeInput, Double.NaN);
        double maxDaysD = parse(maxDaysInput, Double.NaN);
        double target = parse(targetInput, Double.NaN);

        if (Double.isNaN(spot) || spot <= 0 || Double.isNaN(iv) || iv <= 0
                || Double.isNaN(riskFree) || Double.isNaN(maxDaysD) || maxDaysD < 7
                || Double.isNaN(target)) {
            Toast.makeText(getContext(), R.string.input_error, Toast.LENGTH_SHORT).show();
            return;
        }

        int maxDays = (int) Math.round(maxDaysD);
        CoveredCallScreener.Result result = CoveredCallScreener.screen(spot, iv, riskFree, maxDays, target);

        if (!result.hasAny || result.best == null) {
            Toast.makeText(getContext(), R.string.screener_no_result, Toast.LENGTH_LONG).show();
            return;
        }

        bindResult(result, target);
        renderChart(result);
    }

    private void bindResult(CoveredCallScreener.Result result, double target) {
        resultCard.setVisibility(View.VISIBLE);

        CoveredCallScreener.Candidate b = result.best;
        verdictView.setText(result.targetMet
                ? getString(R.string.screener_verdict_met, fmtPct(target))
                : getString(R.string.screener_verdict_not_met, fmtPct(target), fmtPct(b.staticAnnualPct)));

        bestBody.setText(getString(R.string.screener_best_body,
                b.days,
                fmt(b.strike),
                fmtPct(b.upsideBufferPct),
                fmt(b.premium),
                fmtPct(b.staticAnnualPct),
                fmtPct(b.ifCalledAnnualPct),
                fmtPct(b.assignmentProbPct)));
    }

    private void renderChart(CoveredCallScreener.Result result) {
        chartCard.setVisibility(View.VISIBLE);
        legendContainer.removeAllViews();

        List<CoveredCallScreener.Candidate> candidates = result.topForChart;
        List<com.github.mikephil.charting.interfaces.datasets.ILineDataSet> sets = new ArrayList<>();

        for (int idx = 0; idx < candidates.size(); idx++) {
            CoveredCallScreener.Candidate c = candidates.get(idx);
            int color = LINE_COLORS[idx % LINE_COLORS.length];

            List<Entry> points = new ArrayList<>();
            // محور افقی = روزهای سپری‌شده از امروز (۰ = امروز، days = سررسید)
            int steps = 40;
            for (int s = 0; s <= steps; s++) {
                double elapsed = (double) c.days * s / steps;
                double remaining = c.days - elapsed;
                double value = CoveredCallScreener.valueAtDaysRemaining(
                        result.spot, c.strike, remaining, result.r, result.sigma);
                points.add(new Entry((float) elapsed, (float) value));
            }

            LineDataSet set = new LineDataSet(points, "");
            set.setColor(color);
            set.setLineWidth(2.2f);
            set.setDrawCircles(false);
            set.setDrawValues(false);
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            set.setCubicIntensity(0.12f);
            set.setDrawFilled(false);
            set.setHighlightEnabled(false);
            sets.add(set);

            addLegendRow(color, c);
        }

        chart.setData(new LineData(sets));

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setExtraOffsets(4, 8, 8, 8);
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
                return String.format(Locale.US, "%.0f", value);
            }
        });

        YAxis left = chart.getAxisLeft();
        left.setTextColor(COLOR_AXIS_TEXT);
        left.setGridColor(COLOR_GRID);
        left.setAxisLineColor(COLOR_GRID);
        left.setTextSize(9f);
        left.setAxisMinimum(0f);
        left.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return String.format(Locale.US, "%,.1f", value);
            }
        });
        chart.getAxisRight().setEnabled(false);

        chart.animateX(500);
        chart.invalidate();
    }

    private void addLegendRow(int color, CoveredCallScreener.Candidate c) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.bottomMargin = dp(4);
        row.setLayoutParams(rp);

        TextView dot = new TextView(requireContext());
        dot.setText("●");
        dot.setTextColor(color);
        dot.setTextSize(13f);
        row.addView(dot);

        TextView label = new TextView(requireContext());
        label.setText(getString(R.string.screener_legend_row, c.days, fmt(c.strike),
                fmtPct(c.staticAnnualPct)));
        label.setTextColor(0xFFE2E8F0);
        label.setTextSize(11f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(6);
        label.setLayoutParams(lp);
        row.addView(label);

        legendContainer.addView(row);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private double parse(TextInputEditText field, double fallback) {
        String text = field.getText() != null ? field.getText().toString().trim() : "";
        if (text.isEmpty()) return fallback;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String fmt(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "—";
        return String.format(Locale.US, "%,.2f", v);
    }

    private String fmtPct(double v) {
        return String.format(Locale.US, "%.1f٪", v);
    }
}
