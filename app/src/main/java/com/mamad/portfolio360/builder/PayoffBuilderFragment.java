package com.mamad.portfolio360.builder;

import android.os.Bundle;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.mamad.portfolio360.R;
import com.mamad.portfolio360.calc.PayoffEngine;
import com.mamad.portfolio360.wizard.PayoffChartRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ماشین‌حساب سریع و آزاد آپشن: کاربر هر تعداد قرارداد (دارایی پایه/کال/پوت،
 * خرید یا فروش) که بخواهد اضافه می‌کند و نمودار سود/زیان ترکیبی همه‌شان را
 * بلافاصله می‌بیند — برخلاف ویزارد راهنما، اینجا هیچ استراتژی از پیش تعیین‌شده‌ای نیست.
 */
public class PayoffBuilderFragment extends Fragment {

    private static final int MAX_LEGS = 12;

    private LineChart chart;
    private TextInputEditText spotInput;
    private TextView emptyHint;
    private View kpiRow1, kpiRow2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payoff_builder, container, false);

        chart = view.findViewById(R.id.builder_payoff_chart);
        spotInput = view.findViewById(R.id.builder_spot_input);
        emptyHint = view.findViewById(R.id.builder_empty_hint);
        kpiRow1 = view.findViewById(R.id.builder_kpi_row1);
        kpiRow2 = view.findViewById(R.id.builder_kpi_row2);
        LinearLayout legs = view.findViewById(R.id.builder_legs_container);

        MaterialButton addButton = view.findViewById(R.id.btn_add_leg);
        addButton.setOnClickListener(v -> addLeg(legs));

        MaterialButton calcButton = view.findViewById(R.id.btn_builder_calculate);
        calcButton.setOnClickListener(v -> calculate(legs));

        addLeg(legs);

        return view;
    }

    private void addLeg(LinearLayout legsContainer) {
        if (legsContainer.getChildCount() >= MAX_LEGS) {
            Toast.makeText(getContext(), R.string.builder_max_legs_toast, Toast.LENGTH_SHORT).show();
            return;
        }

        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_builder_leg, legsContainer, false);

        MaterialButtonToggleGroup typeGroup = row.findViewById(R.id.leg_type_group);
        TextInputLayout strikeLayout = row.findViewById(R.id.leg_strike_layout);
        typeGroup.check(R.id.leg_type_call);
        typeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            strikeLayout.setVisibility(checkedId == R.id.leg_type_spot ? View.GONE : View.VISIBLE);
        });

        MaterialButtonToggleGroup sideGroup = row.findViewById(R.id.leg_side_group);
        sideGroup.check(R.id.leg_side_buy);

        MaterialButton removeButton = row.findViewById(R.id.btn_remove_leg);
        removeButton.setOnClickListener(v -> {
            legsContainer.removeView(row);
            updateEmptyState(legsContainer);
        });

        legsContainer.addView(row);
        updateEmptyState(legsContainer);
    }

    private void updateEmptyState(LinearLayout legsContainer) {
        boolean hasLegs = legsContainer.getChildCount() > 0;
        emptyHint.setVisibility(hasLegs ? View.GONE : View.VISIBLE);
        if (!hasLegs) {
            kpiRow1.setVisibility(View.GONE);
            kpiRow2.setVisibility(View.GONE);
            chart.clear();
        }
    }

    private void calculate(LinearLayout legsContainer) {
        double spot = parse(spotInput, Double.NaN);
        if (Double.isNaN(spot) || spot <= 0) {
            Toast.makeText(getContext(), R.string.input_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (legsContainer.getChildCount() == 0) {
            Toast.makeText(getContext(), R.string.builder_no_legs, Toast.LENGTH_SHORT).show();
            return;
        }

        List<PayoffEngine.Leg> legs = new ArrayList<>();
        double netPremium = 0; // مثبت = دریافتی خالص، منفی = پرداختی خالص

        for (int i = 0; i < legsContainer.getChildCount(); i++) {
            View row = legsContainer.getChildAt(i);

            MaterialButtonToggleGroup typeGroup = row.findViewById(R.id.leg_type_group);
            MaterialButtonToggleGroup sideGroup = row.findViewById(R.id.leg_side_group);
            TextInputEditText strikeInput = row.findViewById(R.id.leg_strike_input);
            TextInputEditText priceInput = row.findViewById(R.id.leg_price_input);
            TextInputEditText qtyInput = row.findViewById(R.id.leg_qty_input);

            int typeId = typeGroup.getCheckedButtonId();
            PayoffEngine.LegType type = typeId == R.id.leg_type_spot ? PayoffEngine.LegType.SPOT
                    : typeId == R.id.leg_type_put ? PayoffEngine.LegType.PUT
                    : PayoffEngine.LegType.CALL;

            boolean isLong = sideGroup.getCheckedButtonId() == R.id.leg_side_buy;
            double strike = type == PayoffEngine.LegType.SPOT ? 0 : parse(strikeInput, Double.NaN);
            double price = parse(priceInput, Double.NaN);
            double qty = parse(qtyInput, 1);

            if ((type != PayoffEngine.LegType.SPOT && Double.isNaN(strike)) || Double.isNaN(price) || qty <= 0) {
                Toast.makeText(getContext(), R.string.input_error, Toast.LENGTH_SHORT).show();
                return;
            }

            legs.add(new PayoffEngine.Leg(type, isLong, strike, price, qty));
            netPremium += (isLong ? -price : price) * qty;
        }

        PayoffEngine.Result result = PayoffEngine.compute(legs, spot, 220);
        PayoffChartRenderer.render(chart, requireContext(), result, new ArrayList<>(), spot, spot,
                getString(R.string.chart_spot_label), getString(R.string.chart_breakeven_label));

        bindKpis(result, netPremium);
    }

    private void bindKpis(PayoffEngine.Result r, double netPremium) {
        kpiRow1.setVisibility(View.VISIBLE);
        kpiRow2.setVisibility(View.VISIBLE);

        ((TextView) requireView().findViewById(R.id.builder_kpi_max_profit))
                .setText(r.profitUnlimited ? getString(R.string.kpi_unlimited) : fmt(r.maxProfit));
        ((TextView) requireView().findViewById(R.id.builder_kpi_max_loss))
                .setText(r.lossUnlimited ? getString(R.string.kpi_unlimited_loss) : fmt(r.maxLoss));

        TextView breakeven = requireView().findViewById(R.id.builder_kpi_breakeven);
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

        TextView premView = requireView().findViewById(R.id.builder_kpi_premium);
        premView.setText((netPremium >= 0 ? "+" : "−") + fmt(Math.abs(netPremium)));
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
}
