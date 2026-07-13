package com.mamad.portfolio360.wizard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mamad.portfolio360.R;
import com.mamad.portfolio360.calc.CoveredCall;
import com.mamad.portfolio360.calc.InvestmentVerdict;
import com.mamad.portfolio360.calc.PayoffEngine;
import com.mamad.portfolio360.calc.ProtectivePut;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * صفحه نتیجه استراتژی: نمودار تعاملی سود/زیان در سررسید،
 * با قیمت خرید دارایی قابل‌ویرایش توسط کاربر.
 */
public class StrategyResultFragment extends Fragment {

    private static final String ARG_STRATEGY = "strategy";
    private static final String ARG_INSTRUMENT = "instrument";
    private static final String ARG_STRIKE = "strike";
    private static final String ARG_PREMIUM = "premium";
    private static final String ARG_SPOT = "spot";
    private static final String ARG_DAYS = "days";
    private static final String ARG_INSTRUMENT2 = "instrument2";
    private static final String ARG_STRIKE2 = "strike2";
    private static final String ARG_PREMIUM2 = "premium2";
    private static final String ARG_MULTI_INSTRUMENTS = "multi_instruments";
    private static final String ARG_MULTI_STRIKES = "multi_strikes";
    private static final String ARG_MULTI_PREMIUMS = "multi_premiums";

    /** استراتژی‌های اشتراکی که نظر/استدلال سرمایه‌گذاری برایشان نمایش داده می‌شود. */
    private static final Set<String> VERDICT_STRATEGIES = new HashSet<>(
            java.util.Arrays.asList("covered_call", "protective_put", "long_strangle"));

    private String strategyKey;
    private String instrumentName;
    private double strike;
    private double premium;
    private double spot;
    private double daysToExpiry = 30;

    private boolean hasSecondLeg;
    private String instrumentName2;
    private double strike2;
    private double premium2;

    private boolean isMultiLeg;
    private List<String> multiInstruments;
    private List<Double> multiStrikes;
    private List<Double> multiPremiums;

    private LineChart chart;
    private TextInputEditText inputCostBasis;
    private TextInputEditText inputExpectedReturn;
    private TextInputEditText inputRiskFreeRate;

    public static StrategyResultFragment newInstance(String strategyKey, String instrumentName,
                                                      double strike, double premium, double spot,
                                                      double daysToExpiry) {
        StrategyResultFragment f = new StrategyResultFragment();
        Bundle b = new Bundle();
        b.putString(ARG_STRATEGY, strategyKey);
        b.putString(ARG_INSTRUMENT, instrumentName);
        b.putDouble(ARG_STRIKE, strike);
        b.putDouble(ARG_PREMIUM, premium);
        b.putDouble(ARG_SPOT, spot);
        b.putDouble(ARG_DAYS, daysToExpiry);
        f.setArguments(b);
        return f;
    }

    public static StrategyResultFragment newInstanceTwoLeg(
            String strategyKey,
            String instrument1, double strike1, double premium1,
            String instrument2, double strikeB, double premiumB,
            double spot, double daysToExpiry) {
        StrategyResultFragment f = new StrategyResultFragment();
        Bundle b = new Bundle();
        b.putString(ARG_STRATEGY, strategyKey);
        b.putString(ARG_INSTRUMENT, instrument1);
        b.putDouble(ARG_STRIKE, strike1);
        b.putDouble(ARG_PREMIUM, premium1);
        b.putString(ARG_INSTRUMENT2, instrument2);
        b.putDouble(ARG_STRIKE2, strikeB);
        b.putDouble(ARG_PREMIUM2, premiumB);
        b.putDouble(ARG_SPOT, spot);
        b.putDouble(ARG_DAYS, daysToExpiry);
        f.setArguments(b);
        return f;
    }

    public static StrategyResultFragment newInstanceMultiLeg(
            String strategyKey, ArrayList<String> instruments,
            ArrayList<Double> strikes, ArrayList<Double> premiums, double spot) {
        StrategyResultFragment f = new StrategyResultFragment();
        Bundle b = new Bundle();
        b.putString(ARG_STRATEGY, strategyKey);
        b.putStringArrayList(ARG_MULTI_INSTRUMENTS, instruments);
        b.putSerializable(ARG_MULTI_STRIKES, strikes);
        b.putSerializable(ARG_MULTI_PREMIUMS, premiums);
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
        strategyKey = args.getString(ARG_STRATEGY, "");
        spot = args.getDouble(ARG_SPOT);
        if (args.containsKey(ARG_DAYS)) daysToExpiry = args.getDouble(ARG_DAYS);

        isMultiLeg = args.containsKey(ARG_MULTI_INSTRUMENTS);

        String contractLabel;

        if (isMultiLeg) {
            multiInstruments = args.getStringArrayList(ARG_MULTI_INSTRUMENTS);
            //noinspection unchecked
            multiStrikes = (List<Double>) args.getSerializable(ARG_MULTI_STRIKES);
            //noinspection unchecked
            multiPremiums = (List<Double>) args.getSerializable(ARG_MULTI_PREMIUMS);
            contractLabel = multiInstruments != null ? String.join("  +  ", multiInstruments) : "";
        } else {
            instrumentName = args.getString(ARG_INSTRUMENT, "");
            strike = args.getDouble(ARG_STRIKE);
            premium = args.getDouble(ARG_PREMIUM);

            hasSecondLeg = args.containsKey(ARG_INSTRUMENT2);
            if (hasSecondLeg) {
                instrumentName2 = args.getString(ARG_INSTRUMENT2, "");
                strike2 = args.getDouble(ARG_STRIKE2);
                premium2 = args.getDouble(ARG_PREMIUM2);
            }
            contractLabel = hasSecondLeg ? (instrumentName + "  +  " + instrumentName2) : instrumentName;
        }

        chart = view.findViewById(R.id.payoff_chart);
        inputCostBasis = view.findViewById(R.id.input_cost_basis);
        inputExpectedReturn = view.findViewById(R.id.input_expected_return);
        inputRiskFreeRate = view.findViewById(R.id.input_risk_free_rate);

        View verdictInputsCard = view.findViewById(R.id.verdict_inputs_card);
        boolean showVerdict = VERDICT_STRATEGIES.contains(strategyKey);
        verdictInputsCard.setVisibility(showVerdict ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.result_verdict).setVisibility(showVerdict ? View.VISIBLE : View.GONE);

        // پیش‌فرض قیمت خرید = قیمت لحظه‌ای فعلی؛ کاربر می‌تواند تغییر دهد
        inputCostBasis.setText(String.format(Locale.US, "%.2f", spot));

        String title;
        if ("long_call".equals(strategyKey)) title = getString(R.string.strategy_long_call_title);
        else if ("bull_call_spread".equals(strategyKey)) title = getString(R.string.strategy_bull_spread_title);
        else if ("long_straddle".equals(strategyKey)) title = getString(R.string.strategy_straddle_title);
        else if ("long_strangle".equals(strategyKey)) title = getString(R.string.strategy_strangle_title);
        else if ("iron_condor".equals(strategyKey)) title = getString(R.string.strategy_iron_condor_title);
        else if ("butterfly".equals(strategyKey)) title = getString(R.string.strategy_butterfly_title);
        else if ("protective_put".equals(strategyKey)) title = getString(R.string.strategy_pp_title);
        else title = getString(R.string.strategy_cc_title);
        ((TextView) view.findViewById(R.id.result_strategy_name)).setText(title);
        ((TextView) view.findViewById(R.id.result_contract_name)).setText(contractLabel);

        MaterialButton recalcButton = view.findViewById(R.id.btn_recalculate);
        recalcButton.setOnClickListener(v -> recalculate(view));

        recalculate(view);

        return view;
    }

    private void recalculate(View view) {
        if (isMultiLeg) {
            recalculateMultiLeg(view);
            return;
        }

        boolean isPureOption = "long_call".equals(strategyKey) || "bull_call_spread".equals(strategyKey)
                || "long_straddle".equals(strategyKey) || "long_strangle".equals(strategyKey);

        View costBasisCard = view.findViewById(R.id.cost_basis_card);
        if (costBasisCard != null) {
            costBasisCard.setVisibility(isPureOption ? View.GONE : View.VISIBLE);
        }

        double costBasis = isPureOption ? spot : parseCostBasis();
        if (Double.isNaN(costBasis) || costBasis <= 0) {
            Toast.makeText(getContext(), R.string.input_error, Toast.LENGTH_SHORT).show();
            return;
        }

        List<PayoffEngine.Leg> legs;
        if ("bull_call_spread".equals(strategyKey)) {
            legs = PayoffEngine.bullCallSpread(strike, premium, strike2, premium2, 1);
        } else if ("long_straddle".equals(strategyKey)) {
            // پایه اول و دوم می‌توانند به هر ترتیبی کال/پوت باشند؛ بر اساس اسم قرارداد تشخیص می‌دهیم
            boolean firstIsCall = instrumentName.endsWith("-C");
            double callPremium = firstIsCall ? premium : premium2;
            double putPremium = firstIsCall ? premium2 : premium;
            legs = PayoffEngine.longStraddle(strike, callPremium, putPremium, 1);
        } else if ("long_strangle".equals(strategyKey)) {
            boolean firstIsCall = instrumentName.endsWith("-C");
            double callStrike = firstIsCall ? strike : strike2;
            double callPremium = firstIsCall ? premium : premium2;
            double putStrike = firstIsCall ? strike2 : strike;
            double putPremium = firstIsCall ? premium2 : premium;
            legs = PayoffEngine.longStrangle(putStrike, putPremium, callStrike, callPremium, 1);
        } else if ("long_call".equals(strategyKey)) {
            legs = PayoffEngine.longCall(strike, premium, 1);
        } else if ("protective_put".equals(strategyKey)) {
            legs = PayoffEngine.protectivePut(costBasis, strike, premium, 1);
        } else {
            legs = PayoffEngine.coveredCall(costBasis, strike, premium, 1);
        }

        PayoffEngine.Result result = PayoffEngine.compute(legs, spot, 220);

        // برای استراتژی خالص آپشنی، خط مرجع "بدون استراتژی" یعنی سود صفر (چیزی نگه‌داشته نشده)
        List<PayoffEngine.Leg> baseline = isPureOption
                ? new ArrayList<>()
                : PayoffEngine.spotOnly(costBasis, 1);

        setupChart(result, baseline, costBasis);
        bindKpis(view, result, strategyKey);
        bindExplanation(view, result, strategyKey);
        if (VERDICT_STRATEGIES.contains(strategyKey)) {
            bindVerdict(view, result, costBasis);
        }
    }

    private void bindVerdict(View view, PayoffEngine.Result r, double costBasis) {
        double expectedReturn = parseOrDefault(inputExpectedReturn, 25);
        double riskFreeRate = parseOrDefault(inputRiskFreeRate, 30);

        InvestmentVerdict.Result verdict;
        if ("covered_call".equals(strategyKey)) {
            CoveredCall.Result cc = CoveredCall.compute(costBasis, spot, strike, premium, daysToExpiry);
            verdict = InvestmentVerdict.forCoveredCall(cc.annualizedReturnPct, cc.ifCalledReturnPct, expectedReturn, riskFreeRate);
        } else if ("protective_put".equals(strategyKey)) {
            ProtectivePut.Result pp = ProtectivePut.compute(costBasis, strike, premium, daysToExpiry);
            verdict = InvestmentVerdict.forProtectivePut(pp.annualizedInsuranceCostPct, pp.protectionLevelPct, expectedReturn, riskFreeRate);
        } else { // long_strangle
            double totalCost = Math.abs(netPremiumSigned());
            double annualizedCostPct = (totalCost / spot) * 100.0 * (365.0 / Math.max(1, daysToExpiry));
            double nearestBreakevenDistance = Double.POSITIVE_INFINITY;
            for (Double be : r.breakevens) {
                nearestBreakevenDistance = Math.min(nearestBreakevenDistance, Math.abs(be - spot));
            }
            double requiredMovePct = Double.isInfinite(nearestBreakevenDistance) ? 0 : (nearestBreakevenDistance / spot) * 100.0;
            verdict = InvestmentVerdict.forNetDebitStrategy(annualizedCostPct, requiredMovePct, expectedReturn, riskFreeRate);
        }

        ((TextView) view.findViewById(R.id.result_verdict)).setText(verdict.reasoning);
    }

    private double parseOrDefault(TextInputEditText field, double fallback) {
        String text = field.getText() != null ? field.getText().toString().trim() : "";
        if (text.isEmpty()) return fallback;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** محاسبه و نمایش برای استراتژی‌های چندپایه عمومی (آیرون کاندور، پروانه‌ای). */
    private void recalculateMultiLeg(View view) {
        View costBasisCard = view.findViewById(R.id.cost_basis_card);
        if (costBasisCard != null) costBasisCard.setVisibility(View.GONE);

        List<MultiLegPlan.Step> plan = MultiLegPlan.getPlan(strategyKey);
        List<PayoffEngine.Leg> legs = new ArrayList<>();
        double netPremium = 0; // مثبت = دریافتی خالص، منفی = پرداختی خالص

        for (int i = 0; i < plan.size(); i++) {
            MultiLegPlan.Step step = plan.get(i);
            double strikeI = multiStrikes.get(i);
            double premiumI = multiPremiums.get(i);

            legs.add(new PayoffEngine.Leg(
                    step.isCall ? PayoffEngine.LegType.CALL : PayoffEngine.LegType.PUT,
                    step.isLong, strikeI, premiumI, step.quantity));

            netPremium += (step.isLong ? -premiumI : premiumI) * step.quantity;
        }

        PayoffEngine.Result result = PayoffEngine.compute(legs, spot, 220);
        List<PayoffEngine.Leg> baseline = new ArrayList<>();

        setupChart(result, baseline, spot);
        bindMultiLegKpis(view, result, netPremium);
        bindMultiLegExplanation(view, result, netPremium);
    }

    private void bindMultiLegKpis(View view, PayoffEngine.Result r, double netPremium) {
        TextView maxProfit = view.findViewById(R.id.kpi_max_profit);
        TextView maxLoss = view.findViewById(R.id.kpi_max_loss);
        TextView breakeven = view.findViewById(R.id.kpi_breakeven);
        TextView prem = view.findViewById(R.id.kpi_premium);

        maxProfit.setText(r.profitUnlimited ? getString(R.string.kpi_unlimited) : fmt(r.maxProfit));
        maxLoss.setText(r.lossUnlimited ? getString(R.string.kpi_unlimited_loss) : fmt(r.maxLoss));

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

        prem.setText((netPremium >= 0 ? "+" : "−") + fmt(Math.abs(netPremium)));
    }

    private void bindMultiLegExplanation(View view, PayoffEngine.Result r, double netPremium) {
        TextView tv = view.findViewById(R.id.result_explanation);

        String be = r.breakevens.isEmpty() ? "—"
                : (r.breakevens.size() >= 2
                    ? (fmt(r.breakevens.get(0)) + " و " + fmt(r.breakevens.get(1)))
                    : fmt(r.breakevens.get(0)));

        // مرتب‌سازی قیمت‌های اعمال برای نمایش خوانا
        List<Double> sortedStrikes = new ArrayList<>(multiStrikes);
        java.util.Collections.sort(sortedStrikes);

        String text;
        if ("iron_condor".equals(strategyKey)) {
            text = getString(R.string.explain_iron_condor,
                    fmt(sortedStrikes.get(0)), fmt(sortedStrikes.get(1)),
                    fmt(sortedStrikes.get(2)), fmt(sortedStrikes.get(3)),
                    fmt(Math.abs(netPremium)), be);
        } else { // butterfly
            text = getString(R.string.explain_butterfly,
                    fmt(sortedStrikes.get(0)), fmt(sortedStrikes.get(1)), fmt(sortedStrikes.get(2)),
                    fmt(Math.abs(netPremium)), fmt(r.maxProfit), be);
        }
        tv.setText(text);
    }

    private double parseCostBasis() {
        String text = inputCostBasis.getText() != null ? inputCostBasis.getText().toString().trim() : "";
        if (text.isEmpty()) return Double.NaN;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private void setupChart(PayoffEngine.Result result, List<PayoffEngine.Leg> baselineLegs, double costBasis) {
        PayoffChartRenderer.render(chart, requireContext(), result, baselineLegs, costBasis, spot,
                getString(R.string.chart_spot_label), getString(R.string.chart_breakeven_label));
    }

    /** پرمیوم خالص استراتژی: مثبت یعنی دریافتی (اعتباری)، منفی یعنی پرداختی (بدهکاری). */
    private double netPremiumSigned() {
        switch (strategyKey) {
            case "covered_call":
                return premium;                       // دریافت پرمیوم فروش کال
            case "bull_call_spread":
                return -(premium - premium2);          // پرداخت خالص: خرید گران‌تر منهای فروش
            case "long_straddle":
            case "long_strangle":
                return -(premium + premium2);           // پرداخت هر دو پرمیوم
            case "long_call":
            case "protective_put":
            default:
                return -premium;                       // پرداخت پرمیوم خرید
        }
    }

    private void bindKpis(View view, PayoffEngine.Result r, String strategyKey) {
        TextView maxProfit = view.findViewById(R.id.kpi_max_profit);
        TextView maxLoss = view.findViewById(R.id.kpi_max_loss);
        TextView breakeven = view.findViewById(R.id.kpi_breakeven);
        TextView prem = view.findViewById(R.id.kpi_premium);

        maxProfit.setText(r.profitUnlimited ? getString(R.string.kpi_unlimited) : fmt(r.maxProfit));
        maxLoss.setText(r.lossUnlimited ? getString(R.string.kpi_unlimited_loss) : fmt(r.maxLoss));

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

        double net = netPremiumSigned();
        prem.setText((net >= 0 ? "+" : "−") + fmt(Math.abs(net)));
    }

    private void bindExplanation(View view, PayoffEngine.Result r, String strategyKey) {
        TextView tv = view.findViewById(R.id.result_explanation);
        String be = r.breakevens.isEmpty() ? "—" : fmt(r.breakevens.get(0));
        String text;

        if ("bull_call_spread".equals(strategyKey)) {
            double lowerStrike = Math.min(strike, strike2);
            double higherStrike = Math.max(strike, strike2);
            double netDebit = Math.abs(netPremiumSigned());
            text = getString(R.string.explain_bull_call_spread,
                    fmt(lowerStrike), fmt(higherStrike), fmt(netDebit), fmt(r.maxProfit), be);
        } else if ("long_straddle".equals(strategyKey)) {
            double totalCost = Math.abs(netPremiumSigned());
            String beText = r.breakevens.size() >= 2
                    ? (fmt(r.breakevens.get(0)) + " و " + fmt(r.breakevens.get(1)))
                    : be;
            text = getString(R.string.explain_long_straddle, fmt(strike), fmt(totalCost), beText);
        } else if ("long_strangle".equals(strategyKey)) {
            double lowerStrike = Math.min(strike, strike2);
            double higherStrike = Math.max(strike, strike2);
            double totalCost = Math.abs(netPremiumSigned());
            String beText = r.breakevens.size() >= 2
                    ? (fmt(r.breakevens.get(0)) + " و " + fmt(r.breakevens.get(1)))
                    : be;
            text = getString(R.string.explain_long_strangle,
                    fmt(lowerStrike), fmt(higherStrike), fmt(totalCost), beText);
        } else if ("long_call".equals(strategyKey)) {
            text = getString(R.string.explain_long_call, fmt(strike), fmt(premium), be);
        } else if ("protective_put".equals(strategyKey)) {
            text = getString(R.string.explain_protective_put,
                    fmt(strike), fmt(premium), fmt(Math.abs(r.maxLoss)), be);
        } else {
            text = getString(R.string.explain_covered_call,
                    fmt(strike), fmt(premium), fmt(r.maxProfit), be);
        }
        tv.setText(text);
    }

    private String fmt(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "—";
        return String.format(Locale.US, "%,.2f", v);
    }
}
