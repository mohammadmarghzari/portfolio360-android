package com.mamad.portfolio360.wizard;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
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
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mamad.portfolio360.R;
import com.mamad.portfolio360.calc.PayoffEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private static final String ARG_INSTRUMENT2 = "instrument2";
    private static final String ARG_STRIKE2 = "strike2";
    private static final String ARG_PREMIUM2 = "premium2";

    private static final int COLOR_STRATEGY = Color.parseColor("#38BDF8");
    private static final int COLOR_BASELINE = Color.parseColor("#64748B");
    private static final int COLOR_GRID = Color.parseColor("#1E293B");
    private static final int COLOR_AXIS_TEXT = Color.parseColor("#94A3B8");

    private String strategyKey;
    private String instrumentName;
    private double strike;
    private double premium;
    private double spot;

    private boolean hasSecondLeg;
    private String instrumentName2;
    private double strike2;
    private double premium2;

    private LineChart chart;
    private TextInputEditText inputCostBasis;

    public static StrategyResultFragment newInstance(String strategyKey, String instrumentName,
                                                      double strike, double premium, double spot) {
        StrategyResultFragment f = new StrategyResultFragment();
        Bundle b = new Bundle();
        b.putString(ARG_STRATEGY, strategyKey);
        b.putString(ARG_INSTRUMENT, instrumentName);
        b.putDouble(ARG_STRIKE, strike);
        b.putDouble(ARG_PREMIUM, premium);
        b.putDouble(ARG_SPOT, spot);
        f.setArguments(b);
        return f;
    }

    public static StrategyResultFragment newInstanceTwoLeg(
            String strategyKey,
            String instrument1, double strike1, double premium1,
            String instrument2, double strikeB, double premiumB,
            double spot) {
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
        instrumentName = args.getString(ARG_INSTRUMENT, "");
        strike = args.getDouble(ARG_STRIKE);
        premium = args.getDouble(ARG_PREMIUM);
        spot = args.getDouble(ARG_SPOT);

        hasSecondLeg = args.containsKey(ARG_INSTRUMENT2);
        if (hasSecondLeg) {
            instrumentName2 = args.getString(ARG_INSTRUMENT2, "");
            strike2 = args.getDouble(ARG_STRIKE2);
            premium2 = args.getDouble(ARG_PREMIUM2);
        }

        chart = view.findViewById(R.id.payoff_chart);
        inputCostBasis = view.findViewById(R.id.input_cost_basis);

        // پیش‌فرض قیمت خرید = قیمت لحظه‌ای فعلی؛ کاربر می‌تواند تغییر دهد
        inputCostBasis.setText(String.format(Locale.US, "%.2f", spot));

        String title;
        if ("long_call".equals(strategyKey)) title = getString(R.string.strategy_long_call_title);
        else if ("bull_call_spread".equals(strategyKey)) title = getString(R.string.strategy_bull_spread_title);
        else if ("protective_put".equals(strategyKey)) title = getString(R.string.strategy_pp_title);
        else title = getString(R.string.strategy_cc_title);
        ((TextView) view.findViewById(R.id.result_strategy_name)).setText(title);
        String contractLabel = hasSecondLeg ? (instrumentName + "  +  " + instrumentName2) : instrumentName;
        ((TextView) view.findViewById(R.id.result_contract_name)).setText(contractLabel);

        MaterialButton recalcButton = view.findViewById(R.id.btn_recalculate);
        recalcButton.setOnClickListener(v -> recalculate(view));

        recalculate(view);

        return view;
    }

    private void recalculate(View view) {
        boolean isPureOption = "long_call".equals(strategyKey) || "bull_call_spread".equals(strategyKey);

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

        List<Entry> strategyEntries = new ArrayList<>();
        List<Entry> baselineEntries = new ArrayList<>();

        for (int i = 0; i < result.prices.length; i++) {
            float x = (float) result.prices[i];
            strategyEntries.add(new Entry(x, (float) result.payoffs[i]));
            baselineEntries.add(new Entry(x, (float) PayoffEngine.totalPayoff(baselineLegs, result.prices[i])));
        }

        LineDataSet strategySet = new LineDataSet(strategyEntries, "");
        strategySet.setColor(COLOR_STRATEGY);
        strategySet.setLineWidth(2.6f);
        strategySet.setDrawCircles(false);
        strategySet.setDrawValues(false);
        strategySet.setMode(LineDataSet.Mode.LINEAR);
        strategySet.setDrawFilled(true);
        strategySet.setFillAlpha(70);
        strategySet.setHighlightEnabled(true);          // فقط این خط قابل لمس/هایلایت است
        strategySet.setDrawHorizontalHighlightIndicator(false);
        strategySet.setHighlightLineWidth(1f);
        strategySet.setHighLightColor(Color.parseColor("#64748B"));

        strategySet.setFillDrawable(new android.graphics.drawable.Drawable() {
            @Override public void draw(@NonNull android.graphics.Canvas canvas) {
                Paint paint = new Paint();
                android.graphics.Rect b = getBounds();
                paint.setShader(new LinearGradient(0, b.top, 0, b.bottom,
                        new int[]{
                                Color.argb(120, 34, 197, 94),
                                Color.argb(20, 34, 197, 94),
                                Color.argb(20, 239, 68, 68),
                                Color.argb(120, 239, 68, 68)},
                        new float[]{0f, 0.48f, 0.52f, 1f},
                        Shader.TileMode.CLAMP));
                canvas.drawRect(b, paint);
            }
            @Override public void setAlpha(int alpha) {}
            @Override public void setColorFilter(@Nullable android.graphics.ColorFilter cf) {}
            @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        });

        LineDataSet baselineSet = new LineDataSet(baselineEntries, "");
        baselineSet.setColor(COLOR_BASELINE);
        baselineSet.setLineWidth(1.4f);
        baselineSet.setDrawCircles(false);
        baselineSet.setDrawValues(false);
        baselineSet.enableDashedLine(12f, 8f, 0f);
        baselineSet.setDrawFilled(false);
        baselineSet.setHighlightEnabled(false);          // این خط قابل لمس نیست

        chart.setData(new LineData(baselineSet, strategySet));

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);
        chart.setExtraOffsets(4, 8, 4, 30);
        chart.setTouchEnabled(true);
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setHighlightPerTapEnabled(true);
        chart.setHighlightPerDragEnabled(true);          // کشیدن انگشت روی نمودار هم کار کند

        chart.setMarker(new PayoffMarkerView(requireContext(), costBasis));

        XAxis x = chart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setTextColor(COLOR_AXIS_TEXT);
        x.setGridColor(COLOR_GRID);
        x.setAxisLineColor(COLOR_GRID);
        x.setTextSize(9f);
        x.setLabelCount(5, false);
        x.removeAllLimitLines();
        x.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return String.format(Locale.US, "%,.0f", value);
            }
        });

        YAxis left = chart.getAxisLeft();
        left.setTextColor(COLOR_AXIS_TEXT);
        left.setGridColor(COLOR_GRID);
        left.setAxisLineColor(COLOR_GRID);
        left.setTextSize(9f);
        left.setDrawZeroLine(true);
        left.setZeroLineColor(Color.parseColor("#475569"));
        left.setZeroLineWidth(1.2f);
        left.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return String.format(Locale.US, "%,.0f", value);
            }
        });

        chart.getAxisRight().setEnabled(false);

        LimitLine spotLine = new LimitLine((float) spot, getString(R.string.chart_spot_label));
        spotLine.setLineColor(Color.parseColor("#FBBF24"));
        spotLine.setLineWidth(1.4f);
        spotLine.enableDashedLine(8f, 6f, 0f);
        spotLine.setTextColor(Color.parseColor("#FBBF24"));
        spotLine.setTextSize(9f);
        spotLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        x.addLimitLine(spotLine);

        for (Double be : result.breakevens) {
            LimitLine beLine = new LimitLine(be.floatValue(), getString(R.string.chart_breakeven_label));
            beLine.setLineColor(Color.parseColor("#A78BFA"));
            beLine.setLineWidth(1.1f);
            beLine.enableDashedLine(6f, 5f, 0f);
            beLine.setTextColor(Color.parseColor("#A78BFA"));
            beLine.setTextSize(9f);
            beLine.setLabelPosition(LimitLine.LimitLabelPosition.LEFT_BOTTOM);
            x.addLimitLine(beLine);
        }

        chart.animateX(500);
        chart.invalidate();
    }

    /** پرمیوم خالص استراتژی: مثبت یعنی دریافتی (اعتباری)، منفی یعنی پرداختی (بدهکاری). */
    private double netPremiumSigned() {
        switch (strategyKey) {
            case "covered_call":
                return premium;                       // دریافت پرمیوم فروش کال
            case "bull_call_spread":
                return -(premium - premium2);          // پرداخت خالص: خرید گران‌تر منهای فروش
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
