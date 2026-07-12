package com.mamad.portfolio360.wizard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.mamad.portfolio360.R;
import com.mamad.portfolio360.network.DeriveApiClient;
import com.mamad.portfolio360.network.OptionContract;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * صفحه زنجیره آپشن (Option Chain) با داده زنده Derive.xyz:
 *   - بالای صفحه: انتخاب تاریخ سررسید
 *   - چپ: قراردادهای کال با گریک‌ها
 *   - وسط: قیمت اعمال (Strike)
 *   - راست: قراردادهای پوت با گریک‌ها
 */
public class OptionChainFragment extends Fragment {

    private static final String ARG_SELECT_FOR = "select_for";

    /** حالت عادی: فقط نمایش زنجیره */
    public static OptionChainFragment newInstance() {
        return new OptionChainFragment();
    }

    /** حالت انتخاب: کاربر یک قرارداد را برای استراتژی مشخص انتخاب می‌کند */
    public static OptionChainFragment newInstanceForStrategy(String strategyKey) {
        OptionChainFragment f = new OptionChainFragment();
        Bundle b = new Bundle();
        b.putString(ARG_SELECT_FOR, strategyKey);
        f.setArguments(b);
        return f;
    }

    private String selectForStrategy;   // null یعنی حالت عادی

    private TextView status;
    private LinearLayout expiryContainer;
    private LinearLayout chainContainer;

    private final List<OptionContract> allContracts = new ArrayList<>();
    private final List<Long> expiries = new ArrayList<>();
    private long selectedExpiry = -1;
    private double spotPrice = Double.NaN;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_option_chain, container, false);

        status = view.findViewById(R.id.chain_status);
        expiryContainer = view.findViewById(R.id.expiry_container);
        chainContainer = view.findViewById(R.id.chain_container);

        if (getArguments() != null) {
            selectForStrategy = getArguments().getString(ARG_SELECT_FOR, null);
        }

        TextView hint = view.findViewById(R.id.chain_hint);
        if (selectForStrategy != null) {
            hint.setVisibility(View.VISIBLE);
            hint.setText(isPutStrategy()
                    ? R.string.chain_hint_pick_put
                    : R.string.chain_hint_pick_call);
        } else {
            hint.setVisibility(View.GONE);
        }

        loadSpot();
        loadChain();

        return view;
    }

    private void loadSpot() {
        DeriveApiClient.fetchSpotPrice("ETH", new DeriveApiClient.PriceCallback() {
            @Override public void onSuccess(double price) {
                if (!isAdded()) return;
                spotPrice = price;
                updateStatus();
            }
            @Override public void onError(String message) { /* بی‌صدا */ }
        });
    }

    private void loadChain() {
        status.setText(R.string.chain_loading);

        DeriveApiClient.fetchOptionChain("ETH", new DeriveApiClient.ChainCallback() {
            @Override public void onSuccess(List<OptionContract> contracts) {
                if (!isAdded()) return;

                allContracts.clear();
                allContracts.addAll(contracts);

                // تاریخ‌های سررسید یکتا و مرتب
                TreeSet<Long> set = new TreeSet<>();
                for (OptionContract c : contracts) set.add(c.expirySeconds);
                expiries.clear();
                expiries.addAll(set);

                if (expiries.isEmpty()) {
                    status.setText(R.string.chain_empty);
                    return;
                }

                buildExpiryChips();
                selectExpiry(expiries.get(0));
            }

            @Override public void onError(String message) {
                if (!isAdded()) return;
                status.setText(getString(R.string.chain_error, message));
            }
        });
    }

    private void buildExpiryChips() {
        expiryContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Long expiry : expiries) {
            MaterialButton chip = new MaterialButton(requireContext(), null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            chip.setText(formatExpiry(expiry));
            chip.setTextSize(11);
            chip.setAllCaps(false);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(6);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> selectExpiry(expiry));
            chip.setTag(expiry);

            expiryContainer.addView(chip);
        }
    }

    private void selectExpiry(long expiry) {
        selectedExpiry = expiry;
        highlightSelectedChip();
        updateStatus();
        renderChain();
    }

    private void highlightSelectedChip() {
        for (int i = 0; i < expiryContainer.getChildCount(); i++) {
            View child = expiryContainer.getChildAt(i);
            if (!(child instanceof MaterialButton)) continue;
            MaterialButton chip = (MaterialButton) child;
            boolean selected = Long.valueOf(selectedExpiry).equals(chip.getTag());
            chip.setAlpha(selected ? 1f : 0.45f);
        }
    }

    /** ردیف‌ها را می‌سازد و سپس گریک‌های همان سررسید را واکشی می‌کند. */
    private void renderChain() {
        chainContainer.removeAllViews();

        List<OptionContract> forExpiry = new ArrayList<>();
        for (OptionContract c : allContracts) {
            if (c.expirySeconds == selectedExpiry) forExpiry.add(c);
        }

        // گروه‌بندی بر اساس استرایک
        TreeSet<Double> strikeSet = new TreeSet<>();
        for (OptionContract c : forExpiry) strikeSet.add(c.strike);

        List<Double> strikes = new ArrayList<>(strikeSet);
        Collections.sort(strikes);

        Map<Double, OptionContract> calls = new LinkedHashMap<>();
        Map<Double, OptionContract> puts = new LinkedHashMap<>();
        for (OptionContract c : forExpiry) {
            if (c.isCall) calls.put(c.strike, c);
            else puts.put(c.strike, c);
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (Double strike : strikes) {
            View row = inflater.inflate(R.layout.item_chain_row, chainContainer, false);

            TextView strikeView = row.findViewById(R.id.strike_value);
            strikeView.setText(fmtStrike(strike));

            // نزدیک‌ترین استرایک به قیمت لحظه‌ای را برجسته کن
            if (!Double.isNaN(spotPrice) && isNearSpot(strike, strikes)) {
                row.setBackgroundColor(Color.parseColor("#1A3E7CB1"));
            }

            OptionContract call = calls.get(strike);
            OptionContract put = puts.get(strike);

            bindSide(row, R.id.call_price, R.id.call_greeks, call);
            bindSide(row, R.id.put_price, R.id.put_greeks, put);

            row.setTag(new OptionContract[]{call, put});

            if (selectForStrategy != null) {
                OptionContract target = isPutStrategy() ? put : call;
                if (target != null) {
                    row.setOnClickListener(v -> onContractPicked(target));
                }
            }

            chainContainer.addView(row);
        }

        // حالا گریک‌های واقعی را برای همین سررسید بگیر
        status.setText(getString(R.string.chain_loading_greeks, forExpiry.size()));

        DeriveApiClient.fetchGreeksFor(forExpiry, debugSample -> {
            if (!isAdded()) return;
            refreshRows();
            updateStatus();
        });
    }

    /** بعد از رسیدن گریک‌ها، متن ردیف‌ها را به‌روزرسانی می‌کند. */
    private void refreshRows() {
        for (int i = 0; i < chainContainer.getChildCount(); i++) {
            View row = chainContainer.getChildAt(i);
            Object tag = row.getTag();
            if (!(tag instanceof OptionContract[])) continue;

            OptionContract[] pair = (OptionContract[]) tag;
            bindSide(row, R.id.call_price, R.id.call_greeks, pair[0]);
            bindSide(row, R.id.put_price, R.id.put_greeks, pair[1]);
        }
    }

    private void bindSide(View row, int priceId, int greeksId, @Nullable OptionContract c) {
        TextView priceView = row.findViewById(priceId);
        TextView greeksView = row.findViewById(greeksId);

        if (c == null) {
            priceView.setText("—");
            greeksView.setText("");
            return;
        }

        priceView.setText(Double.isNaN(c.markPrice) ? "…" : fmt2(c.markPrice));

        if (c.hasGreeks()) {
            greeksView.setText(getString(R.string.chain_greeks_line,
                    fmt3(c.delta), fmt4(c.gamma), fmt2(c.vega), fmt2(c.theta)));
        } else {
            greeksView.setText("…");
        }
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();
        if (!Double.isNaN(spotPrice)) {
            sb.append(getString(R.string.chain_spot, fmt2(spotPrice)));
        }
        if (selectedExpiry > 0) {
            if (sb.length() > 0) sb.append("  •  ");
            sb.append(getString(R.string.chain_expiry_label, formatExpiry(selectedExpiry)));
        }
        if (sb.length() > 0) status.setText(sb.toString());
    }

    private boolean isNearSpot(double strike, List<Double> strikes) {
        double best = Double.MAX_VALUE;
        double bestStrike = strike;
        for (Double s : strikes) {
            double d = Math.abs(s - spotPrice);
            if (d < best) { best = d; bestStrike = s; }
        }
        return bestStrike == strike;
    }

    private String formatExpiry(long epochSeconds) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.US);
        return sdf.format(new Date(epochSeconds * 1000L));
    }

    private boolean isPutStrategy() {
        return "protective_put".equals(selectForStrategy);
    }

    private void onContractPicked(OptionContract c) {
        if (Double.isNaN(c.markPrice)) {
            android.widget.Toast.makeText(getContext(),
                    R.string.chain_no_price, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (Double.isNaN(spotPrice)) {
            android.widget.Toast.makeText(getContext(),
                    R.string.chain_no_spot, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        StrategyResultFragment fragment = StrategyResultFragment.newInstance(
                selectForStrategy, c.instrumentName, c.strike, c.markPrice, spotPrice);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private String fmtStrike(double v) { return String.format(Locale.US, "%,.0f", v); }
    private String fmt2(double v) { return Double.isNaN(v) ? "—" : String.format(Locale.US, "%.2f", v); }
    private String fmt3(double v) { return Double.isNaN(v) ? "—" : String.format(Locale.US, "%.3f", v); }
    private String fmt4(double v) { return Double.isNaN(v) ? "—" : String.format(Locale.US, "%.4f", v); }
                                                     }
