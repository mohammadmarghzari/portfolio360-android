package com.mamad.portfolio360.onchain;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.mamad.portfolio360.R;
import com.mamad.portfolio360.network.OnChainClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * صفحه‌ی «داده‌های آنچین»: متریک‌های سنتیمنت بازار مشتقات (نسبت خرید/فروش تیکرها،
 * نسبت لانگ/شورت حساب‌ها، اوپن‌اینترست، نرخ فاندینگ) برای ETH یا BTC، هر کدام با
 * نمودار زنده و یک تحلیل مختصر از وضعیت فعلی.
 */
public class OnChainFragment extends Fragment {

    private static final int COLOR_GRID = Color.parseColor("#1E293B");
    private static final int COLOR_AXIS_TEXT = Color.parseColor("#94A3B8");
    private static final int COLOR_LINE = Color.parseColor("#38BDF8");
    private static final int COLOR_GREEN = Color.parseColor("#4ADE80");
    private static final int COLOR_RED = Color.parseColor("#F87171");
    private static final int COLOR_NEUTRAL = Color.parseColor("#94A3B8");

    private LinearLayout container;
    private String symbol = "ETHUSDT";
    private String period = "1d";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onchain, parent, false);

        container = view.findViewById(R.id.onchain_container);

        MaterialButtonToggleGroup assetGroup = view.findViewById(R.id.onchain_asset_group);
        assetGroup.check(R.id.asset_eth);
        assetGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            symbol = checkedId == R.id.asset_btc ? "BTCUSDT" : "ETHUSDT";
        });

        MaterialButtonToggleGroup periodGroup = view.findViewById(R.id.onchain_period_group);
        periodGroup.check(R.id.period_1d);
        periodGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            period = checkedId == R.id.period_1h ? "1h" : checkedId == R.id.period_4h ? "4h" : "1d";
        });

        MaterialButton loadButton = view.findViewById(R.id.btn_onchain_load);
        loadButton.setOnClickListener(v -> loadAll());

        return view;
    }

    private void loadAll() {
        container.removeAllViews();
        addMetricCard(OnChainClient.Metric.TAKER_RATIO, getString(R.string.onchain_metric_taker));
        addMetricCard(OnChainClient.Metric.LONG_SHORT, getString(R.string.onchain_metric_ls));
        addMetricCard(OnChainClient.Metric.OPEN_INTEREST, getString(R.string.onchain_metric_oi));
        addMetricCard(OnChainClient.Metric.FUNDING, getString(R.string.onchain_metric_funding));
    }

    private void addMetricCard(OnChainClient.Metric metric, String title) {
        View card = LayoutInflater.from(requireContext()).inflate(R.layout.item_onchain_metric, container, false);
        ((TextView) card.findViewById(R.id.metric_title)).setText(title);
        container.addView(card);

        ProgressBar progress = card.findViewById(R.id.metric_progress);
        LineChart chart = card.findViewById(R.id.metric_chart);
        TextView valueView = card.findViewById(R.id.metric_value);
        TextView badgeView = card.findViewById(R.id.metric_badge);
        TextView analysisView = card.findViewById(R.id.metric_analysis);

        OnChainClient.fetch(metric, symbol, period, new OnChainClient.Callback() {
            @Override
            public void onSuccess(List<OnChainClient.Point> points) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                if (points.isEmpty()) {
                    analysisView.setText(getString(R.string.onchain_no_data));
                    return;
                }
                chart.setVisibility(View.VISIBLE);
                boolean zeroLine = metric == OnChainClient.Metric.FUNDING;
                configureChart(chart, points, COLOR_LINE, zeroLine);
                bindInterpretation(metric, points, valueView, badgeView, analysisView);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                analysisView.setText(getString(R.string.onchain_error));
            }
        });
    }

    private void bindInterpretation(OnChainClient.Metric metric, List<OnChainClient.Point> points,
                                     TextView valueView, TextView badgeView, TextView analysisView) {
        double last = points.get(points.size() - 1).value;
        double first = points.get(0).value;

        String valueText;
        String badgeText;
        int badgeColor;
        String analysis;

        switch (metric) {
            case TAKER_RATIO:
                valueText = String.format(Locale.US, "%.2f", last);
                if (last > 1.05) {
                    badgeText = "صعودی"; badgeColor = COLOR_GREEN;
                    analysis = "نسبت خرید به فروش تیکرها بالای ۱ است — خریدارها تهاجمی‌تر عمل می‌کنند و فشار خرید غالب است (سیگنال صعودی).";
                } else if (last < 0.95) {
                    badgeText = "نزولی"; badgeColor = COLOR_RED;
                    analysis = "نسبت خرید به فروش زیر ۱ است — فروشنده‌ها تهاجمی‌تر عمل می‌کنند و فشار فروش غالب است (سیگنال نزولی).";
                } else {
                    badgeText = "خنثی"; badgeColor = COLOR_NEUTRAL;
                    analysis = "نسبت خرید به فروش نزدیک ۱ است — تعادل نسبی بین خریدار و فروشنده.";
                }
                break;

            case LONG_SHORT:
                valueText = String.format(Locale.US, "%.2f", last);
                if (last > 2.0) {
                    badgeText = "ازدحام لانگ"; badgeColor = COLOR_RED;
                    analysis = "اکثریت حساب‌ها لانگ‌اند و بازار به‌شدت یک‌طرفه شده — از نظر معکوس (Contrarian) ریسک اصلاح/لیکویید شدن لانگ‌ها بالاست.";
                } else if (last > 1.0) {
                    badgeText = "تمایل صعودی"; badgeColor = COLOR_GREEN;
                    analysis = "تعداد حساب‌های لانگ بیشتر از شورت است — تمایل کلی بازار صعودی است.";
                } else {
                    badgeText = "تمایل نزولی"; badgeColor = COLOR_RED;
                    analysis = "تعداد حساب‌های شورت بیشتر از لانگ است — تمایل کلی بازار نزولی است.";
                }
                break;

            case OPEN_INTEREST:
                valueText = formatBig(last);
                double change = first != 0 ? (last - first) / first * 100.0 : 0;
                if (change > 3) {
                    badgeText = "در حال افزایش"; badgeColor = COLOR_GREEN;
                    analysis = String.format(Locale.US,
                            "اوپن‌اینترست در این بازه %.1f٪ رشد کرده — پول و اهرم جدید وارد بازار شده و روند فعلی پشتوانه‌ی قوی‌تری دارد.", change);
                } else if (change < -3) {
                    badgeText = "در حال کاهش"; badgeColor = COLOR_RED;
                    analysis = String.format(Locale.US,
                            "اوپن‌اینترست در این بازه %.1f٪ کاهش یافته — پوزیشن‌ها بسته می‌شوند و از شدت روند کاسته می‌شود.", change);
                } else {
                    badgeText = "باثبات"; badgeColor = COLOR_NEUTRAL;
                    analysis = "اوپن‌اینترست تقریباً ثابت مانده — ورود یا خروج قابل‌توجهی از بازار مشتقات دیده نمی‌شود.";
                }
                break;

            case FUNDING:
            default:
                valueText = String.format(Locale.US, "%.4f٪", last);
                if (last > 0.03) {
                    badgeText = "لانگ‌های پرهزینه"; badgeColor = COLOR_RED;
                    analysis = "نرخ فاندینگ به‌شکل قابل‌توجهی مثبت است — لانگ‌ها هزینه‌ی زیادی به شورت‌ها می‌پردازند؛ نشانه‌ی ازدحام و اهرم بالای خریداران و ریسک لیکویید شدن.";
                } else if (last > 0) {
                    badgeText = "تمایل صعودی"; badgeColor = COLOR_GREEN;
                    analysis = "نرخ فاندینگ مثبت است — لانگ‌ها به شورت‌ها هزینه می‌پردازند؛ تمایل کلی بازار صعودی است.";
                } else {
                    badgeText = "تمایل نزولی"; badgeColor = COLOR_RED;
                    analysis = "نرخ فاندینگ منفی است — شورت‌ها به لانگ‌ها هزینه می‌پردازند؛ تمایل کلی بازار نزولی است.";
                }
                break;
        }

        valueView.setText(valueText);
        badgeView.setText(badgeText);
        badgeView.setTextColor(badgeColor);
        analysisView.setText(analysis);
    }

    private void configureChart(LineChart chart, List<OnChainClient.Point> points, int color, boolean zeroLine) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            entries.add(new Entry(i, (float) points.get(i).value));
        }

        LineDataSet set = new LineDataSet(entries, "");
        set.setColor(color);
        set.setLineWidth(2.2f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.12f);
        set.setDrawFilled(true);
        set.setFillColor(color);
        set.setFillAlpha(40);
        set.setHighlightEnabled(false);

        chart.setData(new LineData(set));
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setExtraOffsets(4, 6, 8, 6);
        chart.setTouchEnabled(false);

        XAxis x = chart.getXAxis();
        x.setEnabled(false);

        YAxis left = chart.getAxisLeft();
        left.setTextColor(COLOR_AXIS_TEXT);
        left.setGridColor(COLOR_GRID);
        left.setAxisLineColor(COLOR_GRID);
        left.setTextSize(9f);
        if (zeroLine) {
            left.setDrawZeroLine(true);
            left.setZeroLineColor(Color.parseColor("#475569"));
            left.setZeroLineWidth(1f);
        }
        chart.getAxisRight().setEnabled(false);

        chart.animateX(400);
        chart.invalidate();
    }

    private String formatBig(double v) {
        if (v >= 1e9) return String.format(Locale.US, "$%.2fB", v / 1e9);
        if (v >= 1e6) return String.format(Locale.US, "$%.2fM", v / 1e6);
        if (v >= 1e3) return String.format(Locale.US, "$%.2fK", v / 1e3);
        return String.format(Locale.US, "$%.0f", v);
    }
}
