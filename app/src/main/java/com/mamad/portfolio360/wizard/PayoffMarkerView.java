package com.mamad.portfolio360.wizard;

import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.mamad.portfolio360.R;

import java.util.Locale;

/**
 * حبابکی که هنگام لمس نمودار سود/زیان ظاهر می‌شود:
 * قیمت لمس‌شده، سود/زیان دلاری، و درصد آن نسبت به سرمایه اولیه را نشان می‌دهد.
 */
public class PayoffMarkerView extends MarkerView {

    private final TextView priceView;
    private final TextView dollarView;
    private final TextView percentView;
    private final double costBasis;

    private static final int COLOR_PROFIT = Color.parseColor("#22C55E");
    private static final int COLOR_LOSS = Color.parseColor("#EF4444");

    public PayoffMarkerView(Context context, double costBasis) {
        super(context, R.layout.marker_payoff);
        this.costBasis = costBasis;
        priceView = findViewById(R.id.marker_price);
        dollarView = findViewById(R.id.marker_dollar);
        percentView = findViewById(R.id.marker_percent);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        double price = e.getX();
        double payoff = e.getY();
        double percent = costBasis > 0 ? (payoff / costBasis) * 100.0 : 0;

        priceView.setText(String.format(Locale.US, "قیمت: %,.0f", price));

        boolean profit = payoff >= 0;
        int color = profit ? COLOR_PROFIT : COLOR_LOSS;
        String sign = profit ? "+" : "";

        dollarView.setTextColor(color);
        dollarView.setText(String.format(Locale.US, "%s%,.2f $", sign, payoff));

        percentView.setTextColor(color);
        percentView.setText(String.format(Locale.US, "%s%.2f%% نسبت به سرمایه", sign, percent));

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        // حبابک را کمی بالای نقطه لمس‌شده و وسط‌چین نشان بده
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 24f);
    }
}
