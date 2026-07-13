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
import com.mamad.portfolio360.calc.InvestmentVerdict;
import com.mamad.portfolio360.calc.PayoffEngine;
import com.mamad.portfolio360.calc.ProtectivePut;
import com.mamad.portfolio360.wizard.PayoffChartRenderer;

import java.util.List;
import java.util.Locale;

/**
 * تب "پروتکتیو پوت": محاسبه نقطه سر به سر، حداکثر زیان تضمینی
 * و هزینه بیمه برای استراتژی خرید پوت محافظتی.
 */
public class ProtectivePutFragment extends Fragment {

    private TextInputEditText inputCostBasis, inputStrike, inputPremium, inputDays,
            inputExpectedReturn, inputRiskFreeRate;
    private TextView resultBreakeven, resultMaxLoss, resultProtectionLevel,
            resultInsuranceCost, resultAnnualizedCost, resultVerdict;
    private LineChart chart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_protective_put, container, false);

        inputCostBasis = view.findViewById(R.id.input_cost_basis);
        inputStrike = view.findViewById(R.id.input_strike);
        inputPremium = view.findViewById(R.id.input_premium);
        inputDays = view.findViewById(R.id.input_days);
        inputExpectedReturn = view.findViewById(R.id.input_expected_return);
        inputRiskFreeRate = view.findViewById(R.id.input_risk_free_rate);

        resultBreakeven = view.findViewById(R.id.result_breakeven);
        resultMaxLoss = view.findViewById(R.id.result_max_loss);
        resultProtectionLevel = view.findViewById(R.id.result_protection_level);
        resultInsuranceCost = view.findViewById(R.id.result_insurance_cost);
        resultAnnualizedCost = view.findViewById(R.id.result_annualized_cost);
        resultVerdict = view.findViewById(R.id.result_verdict);
        chart = view.findViewById(R.id.pp_payoff_chart);

        MaterialButton calculateButton = view.findViewById(R.id.btn_calculate);
        calculateButton.setOnClickListener(v -> calculate());

        calculate();

        return view;
    }

    private void calculate() {
        try {
            double costBasis = parse(inputCostBasis);
            double strike = parse(inputStrike);
            double premium = parse(inputPremium);
            double days = parse(inputDays);
            double expectedReturn = parse(inputExpectedReturn);
            double riskFreeRate = parse(inputRiskFreeRate);

            ProtectivePut.Result res = ProtectivePut.compute(costBasis, strike, premium, days);

            resultBreakeven.setText(getString(R.string.result_breakeven, fmt(res.breakeven)));
            resultMaxLoss.setText(getString(R.string.result_max_loss, fmt(res.maxLossPerShare)));
            resultProtectionLevel.setText(getString(R.string.result_protection_level, fmtPct(res.protectionLevelPct)));
            resultInsuranceCost.setText(getString(R.string.result_insurance_cost, fmtPct(res.insuranceCostPct)));
            resultAnnualizedCost.setText(getString(R.string.result_annualized_cost, fmtPct(res.annualizedInsuranceCostPct)));

            List<PayoffEngine.Leg> legs = PayoffEngine.protectivePut(costBasis, strike, premium, 1);
            PayoffEngine.Result payoff = PayoffEngine.compute(legs, costBasis, 220);
            List<PayoffEngine.Leg> baseline = PayoffEngine.spotOnly(costBasis, 1);
            PayoffChartRenderer.render(chart, requireContext(), payoff, baseline, costBasis, costBasis,
                    getString(R.string.chart_spot_label), getString(R.string.chart_breakeven_label));

            InvestmentVerdict.Result verdict = InvestmentVerdict.forProtectivePut(
                    res.annualizedInsuranceCostPct, res.protectionLevelPct, expectedReturn, riskFreeRate);
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
