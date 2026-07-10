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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mamad.portfolio360.R;
import com.mamad.portfolio360.calc.BlackScholes;

import java.util.Locale;

/**
 * تب "آپشن پایه": فرم ورودی + محاسبه قیمت و یونانی‌ها با فرمول بلک-شولز.
 */
public class GreeksFragment extends Fragment {

    private TextInputEditText inputSpot, inputStrike, inputDays, inputVolatility, inputRate;
    private TextView resultCall, resultPut, resultDelta, resultGamma, resultTheta, resultVega, resultRho;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_greeks, container, false);

        inputSpot = view.findViewById(R.id.input_spot);
        inputStrike = view.findViewById(R.id.input_strike);
        inputDays = view.findViewById(R.id.input_days);
        inputVolatility = view.findViewById(R.id.input_volatility);
        inputRate = view.findViewById(R.id.input_rate);

        resultCall = view.findViewById(R.id.result_call_price);
        resultPut = view.findViewById(R.id.result_put_price);
        resultDelta = view.findViewById(R.id.result_delta);
        resultGamma = view.findViewById(R.id.result_gamma);
        resultTheta = view.findViewById(R.id.result_theta);
        resultVega = view.findViewById(R.id.result_vega);
        resultRho = view.findViewById(R.id.result_rho);

        MaterialButton calculateButton = view.findViewById(R.id.btn_calculate);
        calculateButton.setOnClickListener(v -> calculate());

        // محاسبه اولیه با مقادیر پیش‌فرض
        calculate();

        return view;
    }

    private void calculate() {
        try {
            double spot = parse(inputSpot);
            double strike = parse(inputStrike);
            double days = parse(inputDays);
            double volatilityPct = parse(inputVolatility);
            double ratePct = parse(inputRate);

            double T = days / 365.0;
            double sigma = volatilityPct / 100.0;
            double r = ratePct / 100.0;

            BlackScholes.Result res = BlackScholes.compute(spot, strike, T, r, sigma);

            resultCall.setText(getString(R.string.result_call_price, fmt(res.callPrice)));
            resultPut.setText(getString(R.string.result_put_price, fmt(res.putPrice)));
            resultDelta.setText(getString(R.string.result_delta, fmt(res.delta)));
            resultGamma.setText(getString(R.string.result_gamma, fmt(res.gamma)));
            resultTheta.setText(getString(R.string.result_theta, fmt(res.theta)));
            resultVega.setText(getString(R.string.result_vega, fmt(res.vega)));
            resultRho.setText(getString(R.string.result_rho, fmt(res.rho)));

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
        return String.format(Locale.US, "%.4f", value);
    }
}
