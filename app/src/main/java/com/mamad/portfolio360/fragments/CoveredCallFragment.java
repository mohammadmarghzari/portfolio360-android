package com.mamad.portfolio360.fragments;

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
import com.mamad.portfolio360.wizard.PayoffChartRenderer;

import java.util.List;
import java.util.Locale;

/**
 * تب "کاوردکال": محاسبه بازده، نقطه سر به سر و محافظت نزولی
 * برای استراتژی فروش کال پوشش‌داده‌شده (Covered Call).
 */
public class CoveredCallFragment extends Fragment {

    private TextInputEditText inputCostBasis, inputSpot, inputStrike, inputPremium, inputDays,
            inputExpectedReturn, inputRiskFreeRate;
    private TextView resultBreakeven, resultMaxProfit, resultStaticReturn,
            resultIfCalledReturn, resultAnnualizedReturn, resultDownsideProtection, resultVerdict;
    private LineChart chart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_covered_call, container, false);

        inputCostBasis = view.findViewById(R.id.input_cost_basis);
        inputSpot = view.findViewById(R.id.input_spot);
        inputStrike = view.findViewById(R.id.input_strike);
        inputPremium = view.findViewById(R.id.input_premium);
        inputDays = view.findViewById(R.id.input_days);
        inputExpectedReturn = view.findViewById(R.id.input_expected_return);
        inputRiskFreeRate = view.findViewById(R.id.input_risk_free_rate);

        resultBreakeven = view.findViewById(R.id.result_breakeven);
        resultMaxProfit = view.findViewById(R.id.result_max_profit);
        resultStaticReturn = view.findViewById(R.id.result_static_return);
        resultIfCalledReturn = view.findViewById(R.id.result_if_called_return);
        resultAnnualizedReturn = view.findViewById(R.id.result_annualized_return);
        resultDownsideProtection = view.findViewById(R.id.result_downside_protection);
        resultVerdict = view.findViewById(R.id.result_verdict);
        chart = view.findViewById(R.id.cc_payoff_chart);

        MaterialButton calculateButton = view.findViewById(R.id.btn_calculate);
        calculateButton.setOnClickListener(v -> calculate());

        calculate();

        return view;
    }

    private void calculate() {
        try {
            double costBasis = parse(inputCostBasis);
            double spot = parse(inputSpot);
            double strike = parse(inputStrike);
            double premium = parse(inputPremium);
            double days = parse(inputDays);
            double expectedReturn = parse(inputExpectedReturn);
            double riskFreeRate = parse(inputRiskFreeRate);

            CoveredCall.Result res = CoveredCall.compute(costBasis, spot, strike, premium, days);

            resultBreakeven.setText(getString(R.string.result_breakeven, fmt(res.breakeven)));
            resultMaxProfit.setText(getString(R.string.result_max_profit, fmt(res.maxProfitPerShare)));
            resultStaticReturn.setText(getString(R.string.result_static_return, fmtPct(res.staticReturnPct)));
            resultIfCalledReturn.setText(getString(R.string.result_if_called_return, fmtPct(res.ifCalledReturnPct)));
            resultAnnualizedReturn.setText(getString(R.string.result_annualized_return, fmtPct(res.annualizedReturnPct)));
            resultDownsideProtection.setText(getString(R.string.result_downside_protection, fmtPct(res.downsideProtectionPct)));

            List<PayoffEngine.Leg> legs = PayoffEngine.coveredCall(costBasis, strike, premium, 1);
            PayoffEngine.Result payoff = PayoffEngine.compute(legs, spot, 220);
            List<PayoffEngine.Leg> baseline = PayoffEngine.spotOnly(costBasis, 1);
            PayoffChartRenderer.render(chart, requireContext(), payoff, baseline, costBasis, spot,
                    getString(R.string.chart_spot_label), getString(R.string.chart_breakeven_label));

            InvestmentVerdict.Result verdict = InvestmentVerdict.forCoveredCall(
                    res.annualizedReturnPct, res.ifCalledReturnPct, expectedReturn, riskFreeRate);
            resultVerdict.setText(verdict.reasoning);

        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), R.string.input_error, Toast.LENGTH_SHORT).show();
        }
    }

    private double parse(TextInputEditText field) {
        String text = field.getText() != null ? field.getText().toString().trim() : "";
        if (text.isEmpty()) throw new NumberFormatException("empty");
        return Double.parseDouble(text);
    }

    private String fmt(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String fmtPct(double value) {
        return String.format(Locale.US, "%.2f٪", value);
    }
}
