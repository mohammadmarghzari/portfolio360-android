package com.mamad.portfolio360.portfolio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mamad.portfolio360.R;
import com.mamad.portfolio360.calc.CvarEngine;
import com.mamad.portfolio360.calc.PortfolioReturns;
import com.mamad.portfolio360.calc.ReturnStats;
import com.mamad.portfolio360.network.HistoricalPoint;
import com.mamad.portfolio360.network.YahooFinanceClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * صفحه پیکربندی بهینه‌سازی پرتفوی: انتخاب دارایی‌ها (چند دسته)،
 * بازه زمانی داده تاریخی، و روش تحلیل مطلوب.
 * در این مرحله فقط رابط کاربری آماده است؛ اتصال داده تاریخی (Yahoo Finance)
 * در جلسه بعد اضافه می‌شود.
 */
public class PortfolioSetupFragment extends Fragment {

    private final Map<String, List<AssetCatalog.Asset>> categories = AssetCatalog.all();
    private final Set<String> selectedSymbols = new LinkedHashSet<>();

    private String activeCategory = AssetCatalog.CAT_STOCKS;
    private String selectedTimeframe = "1y";
    private String selectedMethod;
    private String selectedStyle = PortfolioOptimizerEngine.STYLE_BALANCED;

    private LinearLayout categoryContainer;
    private LinearLayout assetListContainer;
    private LinearLayout timeframeContainer;
    private LinearLayout styleContainer;
    private TextView selectedSummary;

    private View mcInputsCard;
    private TextInputEditText inputExpectedReturn;
    private TextInputEditText inputRiskFreeRate;
    private TextInputEditText inputInitialCapital;

    private static final int[] ASSET_PALETTE = {
            Color.parseColor("#38BDF8"), Color.parseColor("#22C55E"), Color.parseColor("#FBBF24"),
            Color.parseColor("#A78BFA"), Color.parseColor("#EF4444"), Color.parseColor("#F472B6"),
            Color.parseColor("#2DD4BF"), Color.parseColor("#FB923C"), Color.parseColor("#818CF8"),
            Color.parseColor("#A3E635"),
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_portfolio_setup, container, false);

        categoryContainer = view.findViewById(R.id.category_container);
        assetListContainer = view.findViewById(R.id.asset_list_container);
        timeframeContainer = view.findViewById(R.id.timeframe_container);
        styleContainer = view.findViewById(R.id.style_container);
        selectedSummary = view.findViewById(R.id.selected_summary);

        mcInputsCard = view.findViewById(R.id.mc_inputs_card);
        inputExpectedReturn = view.findViewById(R.id.input_expected_return);
        inputRiskFreeRate = view.findViewById(R.id.input_risk_free_rate);
        inputInitialCapital = view.findViewById(R.id.input_initial_capital);

        buildCategoryChips();
        buildTimeframeButtons();
        buildStyleChips();
        buildMethodCards(view.findViewById(R.id.method_container));
        renderAssetList();

        MaterialButton runButton = view.findViewById(R.id.btn_run_analysis);
        runButton.setOnClickListener(v -> onRunClicked());

        MaterialButton yahooTestButton = view.findViewById(R.id.btn_test_yahoo);
        TextView yahooResult = view.findViewById(R.id.yahoo_debug_result);
        yahooTestButton.setOnClickListener(v -> testYahoo(yahooResult));

        return view;
    }

    // ---------- تست دیباگ Yahoo Finance ----------

    private void testYahoo(TextView resultView) {
        resultView.setText(R.string.debug_fetching);

        YahooFinanceClient.fetchHistory("AAPL", "1y", new YahooFinanceClient.HistoryCallback() {
            @Override
            public void onSuccess(String symbol, List<HistoricalPoint> points) {
                if (!isAdded()) return;

                ReturnStats.Result stats = ReturnStats.compute(points);

                String text = String.format(Locale.US,
                        "%s — %d نقطه داده دریافت شد\n\n" +
                        "آخرین قیمت: %.2f$\n" +
                        "بازده کل بازه: %.2f%%\n" +
                        "بازده سالانه‌شده: %.2f%%\n" +
                        "نوسان سالانه‌شده: %.2f%%\n" +
                        "حداکثر افت (Max Drawdown): %.2f%%",
                        symbol, stats.dataPoints, stats.latestClose,
                        stats.totalReturnPct, stats.annualizedReturnPct,
                        stats.annualizedVolPct, stats.maxDrawdownPct);

                resultView.setText(text);
            }

            @Override
            public void onError(String symbol, String message) {
                if (!isAdded()) return;
                resultView.setText("خطا (" + symbol + "): " + message);
            }
        });
    }

    // ---------- دسته‌های دارایی ----------

    private void buildCategoryChips() {
        String[] keys = {
                AssetCatalog.CAT_STOCKS, AssetCatalog.CAT_FUNDS,
                AssetCatalog.CAT_CRYPTO, AssetCatalog.CAT_COMMODITIES
        };
        int[] labels = {
                R.string.category_stocks, R.string.category_funds,
                R.string.category_crypto, R.string.category_commodities
        };

        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            MaterialButton chip = new MaterialButton(requireContext(), null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            chip.setText(getString(labels[i]));
            chip.setTextSize(12);
            chip.setAllCaps(false);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(8);
            chip.setLayoutParams(lp);
            chip.setTag(key);

            chip.setOnClickListener(v -> {
                activeCategory = key;
                highlightCategoryChips();
                renderAssetList();
            });

            categoryContainer.addView(chip);
        }
        highlightCategoryChips();
    }

    private void highlightCategoryChips() {
        for (int i = 0; i < categoryContainer.getChildCount(); i++) {
            View child = categoryContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                boolean active = activeCategory.equals(child.getTag());
                applyChipStyle((MaterialButton) child, active, R.color.home_card_portfolio);
            }
        }
    }

    // ---------- لیست دارایی‌های دسته فعال ----------

    private void renderAssetList() {
        assetListContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        List<AssetCatalog.Asset> assets = categories.get(activeCategory);
        if (assets == null) return;

        for (AssetCatalog.Asset asset : assets) {
            View row = inflater.inflate(R.layout.item_asset_row, assetListContainer, false);

            TextView symbolView = row.findViewById(R.id.asset_symbol);
            TextView nameView = row.findViewById(R.id.asset_name);
            CheckBox checkBox = row.findViewById(R.id.asset_checkbox);

            symbolView.setText(asset.symbol);
            nameView.setText(asset.name);
            checkBox.setChecked(selectedSymbols.contains(asset.symbol));

            row.setOnClickListener(v -> {
                boolean nowChecked = !checkBox.isChecked();
                checkBox.setChecked(nowChecked);
                if (nowChecked) selectedSymbols.add(asset.symbol);
                else selectedSymbols.remove(asset.symbol);
                updateSummary();
            });

            assetListContainer.addView(row);
        }
    }

    private void updateSummary() {
        if (selectedSymbols.isEmpty()) {
            selectedSummary.setText(R.string.portfolio_none_selected);
        } else {
            selectedSummary.setText(getString(R.string.portfolio_selected_count,
                    selectedSymbols.size(), String.join("، ", selectedSymbols)));
        }
    }

    // ---------- بازه زمانی ----------

    private void buildTimeframeButtons() {
        String[] keys = {"6m", "1y", "2y", "3y"};
        int[] labels = {
                R.string.timeframe_6m, R.string.timeframe_1y,
                R.string.timeframe_2y, R.string.timeframe_3y
        };

        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            MaterialButton btn = new MaterialButton(requireContext(), null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(getString(labels[i]));
            btn.setTextSize(12);
            btn.setAllCaps(false);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMarginEnd(i < keys.length - 1 ? 6 : 0);
            btn.setLayoutParams(lp);
            btn.setTag(key);

            btn.setOnClickListener(v -> {
                selectedTimeframe = key;
                highlightTimeframeButtons();
            });

            timeframeContainer.addView(btn);
        }
        highlightTimeframeButtons();
    }

    private void highlightTimeframeButtons() {
        for (int i = 0; i < timeframeContainer.getChildCount(); i++) {
            View child = timeframeContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                boolean active = selectedTimeframe.equals(child.getTag());
                applyChipStyle((MaterialButton) child, active, R.color.home_card_portfolio);
            }
        }
    }

    // ---------- سبک سرمایه‌گذاری (فقط برای مونت‌کارلو) ----------

    private void buildStyleChips() {
        String[] keys = {
                PortfolioOptimizerEngine.STYLE_CONSERVATIVE,
                PortfolioOptimizerEngine.STYLE_BALANCED,
                PortfolioOptimizerEngine.STYLE_AGGRESSIVE
        };
        int[] labels = {R.string.style_conservative, R.string.style_balanced, R.string.style_aggressive};

        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            MaterialButton chip = new MaterialButton(requireContext(), null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            chip.setText(getString(labels[i]));
            chip.setTextSize(12);
            chip.setAllCaps(false);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lp.setMarginEnd(i < keys.length - 1 ? 6 : 0);
            chip.setLayoutParams(lp);
            chip.setTag(key);

            chip.setOnClickListener(v -> {
                selectedStyle = key;
                highlightStyleChips();
            });

            styleContainer.addView(chip);
        }
        highlightStyleChips();
    }

    private void highlightStyleChips() {
        for (int i = 0; i < styleContainer.getChildCount(); i++) {
            View child = styleContainer.getChildAt(i);
            if (child instanceof MaterialButton) {
                boolean active = selectedStyle.equals(child.getTag());
                applyChipStyle((MaterialButton) child, active, R.color.home_card_portfolio);
            }
        }
    }

    /** دکمه انتخاب‌شده را کاملاً پر و رنگی نشان می‌دهد؛ بقیه خط‌دار و خنثی می‌مانند. */
    private void applyChipStyle(MaterialButton button, boolean active, int activeColorRes) {
        int activeColor = androidx.core.content.ContextCompat.getColor(requireContext(), activeColorRes);
        if (active) {
            button.setBackgroundTintList(ColorStateList.valueOf(activeColor));
            button.setTextColor(Color.WHITE);
            button.setStrokeWidth(0);
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            button.setTextColor(activeColor);
            button.setStrokeColor(ColorStateList.valueOf(activeColor));
            button.setStrokeWidth(2);
        }
    }

    // ---------- روش‌های تحلیل ----------

    private LinearLayout methodContainer;

    private void buildMethodCards(LinearLayout container) {
        methodContainer = container;
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (OptimizationMethod method : OptimizationMethod.all()) {
            View cardView = inflater.inflate(R.layout.item_strategy_card, container, false);
            CardView card = (CardView) cardView;

            TextView title = card.findViewById(R.id.text_title);
            TextView description = card.findViewById(R.id.text_description);
            TextView badge = card.findViewById(R.id.text_badge);

            title.setText(method.title);
            description.setText(method.description);
            badge.setVisibility(method.implemented ? View.GONE : View.VISIBLE);
            card.setTag(method.key);

            card.setOnClickListener(v -> {
                selectedMethod = method.key;
                highlightMethodCards();
                mcInputsCard.setVisibility("monte_carlo".equals(selectedMethod) ? View.VISIBLE : View.GONE);
            });

            container.addView(card);
        }
    }

    private void highlightMethodCards() {
        int selectedTint = Color.argb(40, 91, 62, 158); // بنفش کم‌رنگ برای کارت انتخاب‌شده
        for (int i = 0; i < methodContainer.getChildCount(); i++) {
            View child = methodContainer.getChildAt(i);
            if (!(child instanceof CardView)) continue;
            CardView card = (CardView) child;
            boolean selected = selectedMethod != null && selectedMethod.equals(card.getTag());
            card.setCardBackgroundColor(selected ? selectedTint
                    : androidx.core.content.ContextCompat.getColor(requireContext(), R.color.blueprint_surface));
        }
    }

    // ---------- اجرا ----------

    private void onRunClicked() {
        if (selectedSymbols.isEmpty()) {
            Toast.makeText(getContext(), R.string.portfolio_select_at_least_one,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedMethod == null) {
            Toast.makeText(getContext(), R.string.portfolio_select_method_first,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        switch (selectedMethod) {
            case "cvar":
                runCvarAnalysis();
                break;
            case "monte_carlo":
                runMonteCarloAnalysis();
                break;
            case "stress_test":
                runStressTestAnalysis();
                break;
            case "taleb_barbell":
                runBarbellAnalysis();
                break;
            default:
                Toast.makeText(getContext(), R.string.portfolio_coming_soon, Toast.LENGTH_LONG).show();
        }
    }

    private void runCvarAnalysis() {
        View root = getView();
        if (root == null) return;
        root.findViewById(R.id.mc_result_card).setVisibility(View.GONE);

        CardView resultCard = root.findViewById(R.id.cvar_result_card);
        TextView resultTitle = root.findViewById(R.id.cvar_result_title);
        TextView resultBody = root.findViewById(R.id.cvar_result_body);

        resultCard.setVisibility(View.VISIBLE);
        resultTitle.setText(R.string.cvar_loading_title);
        resultBody.setText(getString(R.string.cvar_loading_body, selectedSymbols.size()));

        String yahooRange = mapTimeframeToYahooRange(selectedTimeframe);
        Map<String, List<HistoricalPoint>> histories = new HashMap<>();
        List<String> failedSymbols = new java.util.ArrayList<>();

        YahooFinanceClient.fetchMultipleHistories(new java.util.ArrayList<>(selectedSymbols), yahooRange,
                new YahooFinanceClient.MultiHistoryCallback() {
                    @Override
                    public void onEachSuccess(String symbol, List<HistoricalPoint> points) {
                        histories.put(symbol, points);
                    }

                    @Override
                    public void onEachError(String symbol, String message) {
                        failedSymbols.add(symbol);
                    }

                    @Override
                    public void onComplete() {
                        if (!isAdded()) return;

                        if (histories.size() < 1) {
                            resultTitle.setText(R.string.cvar_error_title);
                            resultBody.setText(getString(R.string.cvar_error_no_data));
                            return;
                        }

                        CvarEngine.Result result = CvarEngine.computeEqualWeighted(histories);

                        if (result.alignedDays < 30) {
                            resultTitle.setText(R.string.cvar_error_title);
                            resultBody.setText(getString(R.string.cvar_error_insufficient_days, result.alignedDays));
                            return;
                        }

                        String failedNote = failedSymbols.isEmpty() ? ""
                                : "\n\n" + getString(R.string.cvar_failed_symbols, String.join("، ", failedSymbols));

                        resultTitle.setText(getString(R.string.cvar_result_title, histories.size()));
                        resultBody.setText(String.format(Locale.US,
                                "%s\n\n%s\n%s\n%s\n%s\n%s%s",
                                getString(R.string.cvar_aligned_days, result.alignedDays),
                                getString(R.string.cvar_annualized_return, fmtPct(result.annualizedReturnPct)),
                                getString(R.string.cvar_annualized_vol, fmtPct(result.annualizedVolPct)),
                                getString(R.string.cvar_var95, fmtPct(result.var95Pct)),
                                getString(R.string.cvar_cvar95, fmtPct(result.cvar95Pct)),
                                getString(R.string.cvar_weights_note),
                                failedNote));
                    }
                });
    }

    // ---------- مونت‌کارلو: پیشنهاد وزن پرتفوی ----------

    private void runMonteCarloAnalysis() {
        View root = getView();
        if (root == null) return;

        double initialCapital = parseDouble(inputInitialCapital, Double.NaN);
        if (Double.isNaN(initialCapital) || initialCapital <= 0) {
            Toast.makeText(getContext(), R.string.mc_invalid_capital, Toast.LENGTH_SHORT).show();
            return;
        }
        double riskFreeRate = parseDouble(inputRiskFreeRate, Double.NaN);
        if (Double.isNaN(riskFreeRate)) {
            Toast.makeText(getContext(), R.string.mc_invalid_risk_free, Toast.LENGTH_SHORT).show();
            return;
        }
        double expectedReturn = parseDouble(inputExpectedReturn, 0);

        root.findViewById(R.id.cvar_result_card).setVisibility(View.GONE);

        CardView mcCard = root.findViewById(R.id.mc_result_card);
        TextView mcTitle = root.findViewById(R.id.mc_result_title);
        TextView mcMetrics = root.findViewById(R.id.mc_metrics_body);

        mcCard.setVisibility(View.VISIBLE);
        mcTitle.setText(R.string.mc_loading_title);
        mcMetrics.setText(getString(R.string.mc_loading_body, selectedSymbols.size()));

        String yahooRange = mapTimeframeToYahooRange(selectedTimeframe);
        Map<String, List<HistoricalPoint>> histories = new HashMap<>();
        String style = selectedStyle;

        YahooFinanceClient.fetchMultipleHistories(new ArrayList<>(selectedSymbols), yahooRange,
                new YahooFinanceClient.MultiHistoryCallback() {
                    @Override
                    public void onEachSuccess(String symbol, List<HistoricalPoint> points) {
                        histories.put(symbol, points);
                    }

                    @Override
                    public void onEachError(String symbol, String message) { /* نادیده گرفته می‌شود */ }

                    @Override
                    public void onComplete() {
                        if (!isAdded()) return;

                        PortfolioReturns.PerSymbolAligned aligned = PortfolioReturns.alignPerSymbol(histories);

                        if (aligned.symbols.length == 0 || aligned.alignedDays < 30) {
                            mcTitle.setText(R.string.cvar_error_title);
                            mcMetrics.setText(getString(R.string.cvar_error_insufficient_days, aligned.alignedDays));
                            return;
                        }

                        PortfolioOptimizerEngine.Result opt = PortfolioOptimizerEngine.optimize(
                                aligned, riskFreeRate, expectedReturn, style, 4000, 7L);

                        mcTitle.setText(R.string.mc_result_title2);
                        renderMonteCarloResult(root, opt, initialCapital);
                    }
                });
    }

    private void renderMonteCarloResult(View root, PortfolioOptimizerEngine.Result opt, double initialCapital) {
        TextView metricsBody = root.findViewById(R.id.mc_metrics_body);
        LinearLayout weightList = root.findViewById(R.id.mc_weight_list_container);
        PieChart pieChart = root.findViewById(R.id.mc_pie_chart);
        ScatterChart scatterChart = root.findViewById(R.id.mc_scatter_chart);

        weightList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        List<PieEntry> pieEntries = new ArrayList<>();
        List<Integer> sliceColors = new ArrayList<>();

        for (int i = 0; i < opt.symbols.length; i++) {
            int color = ASSET_PALETTE[i % ASSET_PALETTE.length];
            double weightPct = opt.weights[i] * 100.0;
            double dollar = opt.weights[i] * initialCapital;

            pieEntries.add(new PieEntry((float) weightPct, opt.symbols[i]));
            sliceColors.add(color);

            View row = inflater.inflate(R.layout.item_weight_row, weightList, false);
            row.findViewById(R.id.weight_row_dot).setBackgroundTintList(ColorStateList.valueOf(color));
            ((TextView) row.findViewById(R.id.weight_row_symbol)).setText(opt.symbols[i]);
            ((TextView) row.findViewById(R.id.weight_row_percent))
                    .setText(String.format(Locale.US, "%.1f%%", weightPct));
            ((TextView) row.findViewById(R.id.weight_row_dollar))
                    .setText(String.format(Locale.US, "$%,.2f", dollar));
            weightList.addView(row);
        }

        setupPieChart(pieChart, pieEntries, sliceColors);
        setupScatterChart(scatterChart, opt);

        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.mc_metric_capital, fmtDollar(initialCapital))).append("\n");
        sb.append(getString(R.string.mc_metric_expected_return, fmtPct(opt.annualizedReturnPct))).append("\n");
        sb.append(getString(R.string.mc_metric_risk, fmtPct(opt.annualizedVolPct))).append("\n");
        sb.append(getString(R.string.mc_metric_sharpe, fmtRatio(opt.sharpe))).append("\n");
        sb.append(getString(R.string.mc_metric_max_drawdown, fmtPct(opt.maxDrawdownPct))).append("\n");

        if (opt.recovered) {
            sb.append(getString(R.string.mc_metric_recovery_yes, opt.recoveryDays)).append("\n");
        } else if (opt.recoveryDays >= 0) {
            sb.append(getString(R.string.mc_metric_recovery_no, opt.recoveryDays)).append("\n");
        }

        sb.append(getString(R.string.mc_metric_median, fmtPct(opt.medianForwardReturnPct))).append("\n");
        sb.append(getString(R.string.mc_metric_range, fmtPct(opt.p5ForwardReturnPct), fmtPct(opt.p95ForwardReturnPct))).append("\n");
        sb.append(getString(R.string.mc_metric_best_sharpe, fmtRatio(opt.bestPossibleSharpe))).append("\n");
        sb.append(getString(R.string.mc_metric_best_median, fmtPct(opt.bestPossibleMedianReturnPct)));

        if (!opt.targetAchieved) {
            sb.append("\n\n").append(getString(R.string.mc_target_missed));
        }

        metricsBody.setText(sb.toString());
    }

    private void setupPieChart(PieChart chart, List<PieEntry> entries, List<Integer> colors) {
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(11f);
        dataSet.setSliceSpace(2f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return String.format(Locale.US, "%.1f%%", value);
            }
        });

        chart.setData(new PieData(dataSet));
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setTransparentCircleAlpha(0);
        chart.setHoleRadius(42f);
        chart.setTransparentCircleRadius(46f);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.animateY(500);
        chart.invalidate();
    }

    private void setupScatterChart(ScatterChart chart, PortfolioOptimizerEngine.Result opt) {
        List<Entry> cloudEntries = new ArrayList<>();
        for (int i = 0; i < opt.cloudVolPct.length; i++) {
            cloudEntries.add(new Entry((float) opt.cloudVolPct[i], (float) opt.cloudReturnPct[i]));
        }
        ScatterDataSet cloudSet = new ScatterDataSet(cloudEntries, "");
        cloudSet.setColor(Color.parseColor("#64748B"));
        cloudSet.setScatterShape(com.github.mikephil.charting.charts.ScatterChart.ScatterShape.CIRCLE);
        cloudSet.setScatterShapeSize(6f);
        cloudSet.setDrawValues(false);

        List<Entry> pickEntry = new ArrayList<>();
        pickEntry.add(new Entry((float) opt.annualizedVolPct, (float) opt.annualizedReturnPct));
        ScatterDataSet pickSet = new ScatterDataSet(pickEntry, "");
        pickSet.setColor(Color.parseColor("#38BDF8"));
        pickSet.setScatterShape(com.github.mikephil.charting.charts.ScatterChart.ScatterShape.CIRCLE);
        pickSet.setScatterShapeSize(18f);
        pickSet.setDrawValues(false);

        chart.setData(new ScatterData(cloudSet, pickSet));
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setTouchEnabled(false);

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(Color.parseColor("#94A3B8"));
        x.setGridColor(Color.parseColor("#1E293B"));
        x.setAxisLineColor(Color.parseColor("#1E293B"));
        x.setTextSize(9f);
        x.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return String.format(Locale.US, "%.0f%%", value);
            }
        });

        YAxis left = chart.getAxisLeft();
        left.setTextColor(Color.parseColor("#94A3B8"));
        left.setGridColor(Color.parseColor("#1E293B"));
        left.setAxisLineColor(Color.parseColor("#1E293B"));
        left.setTextSize(9f);
        left.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return String.format(Locale.US, "%.0f%%", value);
            }
        });
        chart.getAxisRight().setEnabled(false);

        chart.invalidate();
    }

    // ---------- آزمون استرس ----------

    private void runStressTestAnalysis() {
        View root = getView();
        if (root == null) return;
        root.findViewById(R.id.mc_result_card).setVisibility(View.GONE);

        CardView resultCard = root.findViewById(R.id.cvar_result_card);
        TextView resultTitle = root.findViewById(R.id.cvar_result_title);
        TextView resultBody = root.findViewById(R.id.cvar_result_body);

        resultCard.setVisibility(View.VISIBLE);
        resultTitle.setText(R.string.stress_result_title);

        StringBuilder sb = new StringBuilder();
        for (com.mamad.portfolio360.calc.StressTestEngine.Scenario sc :
                com.mamad.portfolio360.calc.StressTestEngine.scenarios()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(getString(R.string.stress_scenario_line,
                    sc.titleFa, sc.periodFa, fmtPct(sc.shockPct)));
        }
        sb.append("\n\n").append(getString(R.string.stress_disclaimer));

        resultBody.setText(sb.toString());
    }

    // ---------- بارِبل طالب ----------

    private void runBarbellAnalysis() {
        View root = getView();
        if (root == null) return;
        root.findViewById(R.id.mc_result_card).setVisibility(View.GONE);

        CardView resultCard = root.findViewById(R.id.cvar_result_card);
        TextView resultTitle = root.findViewById(R.id.cvar_result_title);
        TextView resultBody = root.findViewById(R.id.cvar_result_body);

        resultCard.setVisibility(View.VISIBLE);
        resultTitle.setText(R.string.barbell_result_title);

        BarbellEngine.Result bar = BarbellEngine.compute(selectedSymbols);

        StringBuilder sb = new StringBuilder();

        if (bar.hasSafeBucket) {
            sb.append(getString(R.string.barbell_safe_bucket_label)).append("\n");
            for (String s : bar.safeAssets) {
                sb.append(getString(R.string.barbell_weight_line, s, fmtPct(bar.weightPct.get(s)))).append("\n");
            }
        } else {
            sb.append(getString(R.string.barbell_no_safe_note)).append("\n");
        }

        sb.append("\n");

        if (bar.hasRiskyBucket) {
            sb.append(getString(R.string.barbell_risky_bucket_label)).append("\n");
            for (String s : bar.riskyAssets) {
                sb.append(getString(R.string.barbell_weight_line, s, fmtPct(bar.weightPct.get(s)))).append("\n");
            }
        } else {
            sb.append(getString(R.string.barbell_no_risky_note)).append("\n");
        }

        sb.append("\n").append(getString(R.string.barbell_note));

        resultBody.setText(sb.toString());
    }

    private String mapTimeframeToYahooRange(String key) {
        if ("6m".equals(key)) return "6mo";
        return key; // "1y", "2y", "3y" مستقیماً معتبرند
    }

    private String fmtPct(double v) {
        return String.format(Locale.US, "%.2f%%", v);
    }

    private String fmtRatio(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private String fmtDollar(double v) {
        return String.format(Locale.US, "$%,.2f", v);
    }

    /** متن یک TextView را به عدد اعشاری تبدیل می‌کند؛ اگر خالی بود fallback و اگر نامعتبر بود NaN برمی‌گرداند. */
    private double parseDouble(TextView tv, double fallbackIfEmpty) {
        if (tv == null || tv.getText() == null) return fallbackIfEmpty;
        String s = tv.getText().toString().trim();
        if (s.isEmpty()) return fallbackIfEmpty;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
