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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * صفحه زنجیره آپشن (Option Chain) با داده زنده Derive.xyz:
 *   - بالای صفحه: انتخاب تاریخ سررسید
 *   - چپ: قراردادهای کال با گریک‌ها
 *   - وسط: قیمت اعمال (Strike)
 *   - راست: قراردادهای پوت با گریک‌ها
 *
 * برای استراتژی‌های دو-پایه (مثل بول کال اسپرد)، این صفحه دوبار پشت سر هم
 * باز می‌شود: یک‌بار برای پایه اول، یک‌بار برای پایه دوم.
 */
public class OptionChainFragment extends Fragment {

    private static final String ARG_SELECT_FOR = "select_for";
    private static final String ARG_LEG1_INSTRUMENT = "leg1_instrument";
    private static final String ARG_LEG1_STRIKE = "leg1_strike";
    private static final String ARG_LEG1_PREMIUM = "leg1_premium";

    // انباشتگر عمومی برای استراتژی‌های سه/چهارپایه (آیرون کاندور، پروانه‌ای)
    private static final String ARG_ACC_INSTRUMENTS = "acc_instruments";
    private static final String ARG_ACC_STRIKES = "acc_strikes";
    private static final String ARG_ACC_PREMIUMS = "acc_premiums";

    /** استراتژی‌هایی که نیاز به انتخاب دو قرارداد دارند. */
    private static final Set<String> TWO_LEG_STRATEGIES = new HashSet<>(
            Arrays.asList("bull_call_spread", "long_strangle"));

    /** استراتژی‌هایی که با یک لمس روی یک ردیف، هر دو پایه (کال+پوت هم‌قیمت) انتخاب می‌شوند. */
    private static final Set<String> SAME_ROW_STRATEGIES = new HashSet<>(
            Arrays.asList("long_straddle"));

    /** حالت عادی: فقط نمایش زنجیره */
    public static OptionChainFragment newInstance() {
        return new OptionChainFragment();
    }

    /** حالت انتخاب: کاربر یک (یا اولین) قرارداد را برای استراتژی مشخص انتخاب می‌کند */
    public static OptionChainFragment newInstanceForStrategy(String strategyKey) {
        OptionChainFragment f = new OptionChainFragment();
        Bundle b = new Bundle();
        b.putString(ARG_SELECT_FOR, strategyKey);
        f.setArguments(b);
        return f;
    }

    /** حالت انتخاب پایه دوم، با اطلاعات پایه اول که قبلاً انتخاب شده. */
    public static OptionChainFragment newInstanceForSecondLeg(
            String strategyKey, String leg1Instrument, double leg1Strike, double leg1Premium) {
        OptionChainFragment f = new OptionChainFragment();
        Bundle b = new Bundle();
        b.putString(ARG_SELECT_FOR, strategyKey);
        b.putString(ARG_LEG1_INSTRUMENT, leg1Instrument);
        b.putDouble(ARG_LEG1_STRIKE, leg1Strike);
        b.putDouble(ARG_LEG1_PREMIUM, leg1Premium);
        f.setArguments(b);
        return f;
    }

    /** ادامه انتخاب برای استراتژی‌های چندپایه عمومی (آیرون کاندور، پروانه‌ای) */
    public static OptionChainFragment newInstanceForMultiLeg(
            String strategyKey, ArrayList<String> instruments,
            ArrayList<Double> strikes, ArrayList<Double> premiums) {
        OptionChainFragment f = new OptionChainFragment();
        Bundle b = new Bundle();
        b.putString(ARG_SELECT_FOR, strategyKey);
        b.putStringArrayList(ARG_ACC_INSTRUMENTS, instruments);
        b.putSerializable(ARG_ACC_STRIKES, strikes);
        b.putSerializable(ARG_ACC_PREMIUMS, premiums);
        f.setArguments(b);
        return f;
    }

    private String selectForStrategy;   // null یعنی حالت عادی
    private boolean isSecondLeg;
    private String leg1Instrument;
    private double leg1Strike;
    private double leg1Premium;

    private final List<String> accInstruments = new ArrayList<>();
    private final List<Double> accStrikes = new ArrayList<>();
    private final List<Double> accPremiums = new ArrayList<>();

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
            if (getArguments().containsKey(ARG_LEG1_INSTRUMENT)) {
                isSecondLeg = true;
                leg1Instrument = getArguments().getString(ARG_LEG1_INSTRUMENT, "");
                leg1Strike = getArguments().getDouble(ARG_LEG1_STRIKE);
                leg1Premium = getArguments().getDouble(ARG_LEG1_PREMIUM);
            }
            if (getArguments().containsKey(ARG_ACC_INSTRUMENTS)) {
                List<String> savedInstruments = getArguments().getStringArrayList(ARG_ACC_INSTRUMENTS);
                if (savedInstruments != null) accInstruments.addAll(savedInstruments);

                java.io.Serializable savedStrikes = getArguments().getSerializable(ARG_ACC_STRIKES);
                if (savedStrikes instanceof ArrayList) {
                    //noinspection unchecked
                    accStrikes.addAll((ArrayList<Double>) savedStrikes);
                }
                java.io.Serializable savedPremiums = getArguments().getSerializable(ARG_ACC_PREMIUMS);
                if (savedPremiums instanceof ArrayList) {
                    //noinspection unchecked
                    accPremiums.addAll((ArrayList<Double>) savedPremiums);
                }
            }
        }

        TextView hint = view.findViewById(R.id.chain_hint);
        if (selectForStrategy != null) {
            hint.setVisibility(View.VISIBLE);
            hint.setText(buildHintText());
        } else {
            hint.setVisibility(View.GONE);
        }

        loadSpot();
        loadChain();

        return view;
    }

    private String buildHintText() {
        if (MultiLegPlan.isMultiLeg(selectForStrategy)) {
            List<MultiLegPlan.Step> plan = MultiLegPlan.getPlan(selectForStrategy);
            int stepIndex = accInstruments.size();
            if (stepIndex < plan.size()) {
                return getString(plan.get(stepIndex).hintRes);
            }
        }
        if (SAME_ROW_STRATEGIES.contains(selectForStrategy)) {
            return getString(R.string.chain_hint_pick_straddle_row);
        }
        if ("long_strangle".equals(selectForStrategy)) {
            return isSecondLeg
                    ? getString(R.string.chain_hint_pick_second_put, fmtStrike(leg1Strike))
                    : getString(R.string.chain_hint_pick_first_call);
        }
        if (isTwoLegStrategy() && isSecondLeg) {
            return getString(R.string.chain_hint_pick_second_call, fmtStrike(leg1Strike));
        }
        if (isTwoLegStrategy()) {
            return getString(R.string.chain_hint_pick_first_call);
        }
        if (isPutStrategy()) {
            return getString(R.string.chain_hint_pick_put);
        }
        return getString("covered_call".equals(selectForStrategy)
                ? R.string.chain_hint_pick_call
                : R.string.chain_hint_pick_call_buy);
    }

    private boolean isTwoLegStrategy() {
        return TWO_LEG_STRATEGIES.contains(selectForStrategy);
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

    private void renderChain() {
        chainContainer.removeAllViews();

        List<OptionContract> forExpiry = new ArrayList<>();
        for (OptionContract c : allContracts) {
            if (c.expirySeconds == selectedExpiry) forExpiry.add(c);
        }

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

            if (!Double.isNaN(spotPrice) && isNearSpot(strike, strikes)) {
                row.setBackgroundColor(Color.parseColor("#1A3E7CB1"));
            }

            OptionContract call = calls.get(strike);
            OptionContract put = puts.get(strike);

            bindSide(row, R.id.call_price, R.id.call_greeks, call);
            bindSide(row, R.id.put_price, R.id.put_greeks, put);

            // در حالت پایه دوم، همان قرارداد پایه اول را قابل انتخاب مجدد نکن
            row.setTag(new OptionContract[]{call, put});

            if (MultiLegPlan.isMultiLeg(selectForStrategy)) {
                List<MultiLegPlan.Step> plan = MultiLegPlan.getPlan(selectForStrategy);
                int stepIndex = accInstruments.size();
                if (stepIndex < plan.size()) {
                    MultiLegPlan.Step step = plan.get(stepIndex);
                    OptionContract target = step.isCall ? call : put;
                    boolean alreadyPicked = target != null && accInstruments.contains(target.instrumentName);
                    if (target != null && !alreadyPicked) {
                        row.setOnClickListener(v -> onMultiLegContractPicked(target));
                    } else if (alreadyPicked) {
                        row.setBackgroundColor(Color.parseColor("#332E7CB1"));
                    }
                }
            } else {
                boolean isSameAsLeg1 = isSecondLeg && call != null && call.instrumentName.equals(leg1Instrument);

                if (SAME_ROW_STRATEGIES.contains(selectForStrategy)) {
                    if (call != null && put != null) {
                        row.setOnClickListener(v -> onStraddleRowPicked(call, put));
                    }
                } else if (selectForStrategy != null && !isSameAsLeg1) {
                    OptionContract target = isPutStrategy() ? put : call;
                    if (target != null) {
                        row.setOnClickListener(v -> onContractPicked(target));
                    }
                } else if (isSameAsLeg1) {
                    row.setBackgroundColor(Color.parseColor("#332E7CB1"));
                }
            }

            chainContainer.addView(row);
        }

        status.setText(getString(R.string.chain_loading_greeks, forExpiry.size()));

        DeriveApiClient.fetchGreeksFor(forExpiry, debugSample -> {
            if (!isAdded()) return;
            refreshRows();
            updateStatus();
        });
    }

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
        if ("protective_put".equals(selectForStrategy)) return true;
        if ("long_strangle".equals(selectForStrategy) && isSecondLeg) return true; // پایه دوم استرنگل = پوت
        return false;
    }

    /** یک پایه از استراتژی چندپایه انتخاب شد؛ یا سراغ پایه بعدی برو یا صفحه نتیجه را باز کن. */
    private void onMultiLegContractPicked(OptionContract c) {
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

        ArrayList<String> instruments = new ArrayList<>(accInstruments);
        ArrayList<Double> strikes = new ArrayList<>(accStrikes);
        ArrayList<Double> premiums = new ArrayList<>(accPremiums);

        instruments.add(c.instrumentName);
        strikes.add(c.strike);
        premiums.add(c.markPrice);

        List<MultiLegPlan.Step> plan = MultiLegPlan.getPlan(selectForStrategy);

        if (instruments.size() < plan.size()) {
            OptionChainFragment next = OptionChainFragment.newInstanceForMultiLeg(
                    selectForStrategy, instruments, strikes, premiums);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, next)
                    .addToBackStack(null)
                    .commit();
            return;
        }

        // همه پایه‌ها انتخاب شدند
        StrategyResultFragment fragment = StrategyResultFragment.newInstanceMultiLeg(
                selectForStrategy, instruments, strikes, premiums, spotPrice);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void onStraddleRowPicked(OptionContract call, OptionContract put) {
        if (Double.isNaN(call.markPrice) || Double.isNaN(put.markPrice)) {
            android.widget.Toast.makeText(getContext(),
                    R.string.chain_no_price, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (Double.isNaN(spotPrice)) {
            android.widget.Toast.makeText(getContext(),
                    R.string.chain_no_spot, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        StrategyResultFragment fragment = StrategyResultFragment.newInstanceTwoLeg(
                selectForStrategy,
                call.instrumentName, call.strike, call.markPrice,
                put.instrumentName, put.strike, put.markPrice,
                spotPrice);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
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

        if (isTwoLegStrategy() && !isSecondLeg) {
            OptionChainFragment secondLeg = OptionChainFragment.newInstanceForSecondLeg(
                    selectForStrategy, c.instrumentName, c.strike, c.markPrice);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, secondLeg)
                    .addToBackStack(null)
                    .commit();
            return;
        }

        if (isTwoLegStrategy() && isSecondLeg) {
            StrategyResultFragment fragment = StrategyResultFragment.newInstanceTwoLeg(
                    selectForStrategy,
                    leg1Instrument, leg1Strike, leg1Premium,
                    c.instrumentName, c.strike, c.markPrice,
                    spotPrice);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
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
