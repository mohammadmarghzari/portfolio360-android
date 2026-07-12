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

import com.google.android.material.button.MaterialButton;
import com.mamad.portfolio360.R;
import com.mamad.portfolio360.calc.CvarEngine;
import com.mamad.portfolio360.calc.ReturnStats;
import com.mamad.portfolio360.network.HistoricalPoint;
import com.mamad.portfolio360.network.YahooFinanceClient;

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

    private LinearLayout categoryContainer;
    private LinearLayout assetListContainer;
    private LinearLayout timeframeContainer;
    private TextView selectedSummary;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_portfolio_setup, container, false);

        categoryContainer = view.findViewById(R.id.category_container);
        assetListContainer = view.findViewById(R.id.asset_list_container);
        timeframeContainer = view.findViewById(R.id.timeframe_container);
        selectedSummary = view.findViewById(R.id.selected_summary);

        buildCategoryChips();
        buildTimeframeButtons();
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
        if ("cvar".equals(selectedMethod)) {
            runCvarAnalysis();
        } else {
            Toast.makeText(getContext(), R.string.portfolio_coming_soon, Toast.LENGTH_LONG).show();
        }
    }

    private void runCvarAnalysis() {
        View root = getView();
        if (root == null) return;

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

    private String mapTimeframeToYahooRange(String key) {
        if ("6m".equals(key)) return "6mo";
        return key; // "1y", "2y", "3y" مستقیماً معتبرند
    }

    private String fmtPct(double v) {
        return String.format(Locale.US, "%.2f%%", v);
    }
}
