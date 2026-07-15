package com.mamad.portfolio360.macro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.mamad.portfolio360.R;
import com.mamad.portfolio360.calc.MacroSentiment;
import com.mamad.portfolio360.network.EconCalendarClient;
import com.mamad.portfolio360.network.EconEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * تقویم اقتصادی آمریکا: رویدادهای مهم (سه‌ستاره) این هفته را به‌صورت آنلاین
 * نمایش می‌دهد و در پایین، یک جمع‌بندی خودکار از جهت‌گیری داده‌ها ارائه می‌کند.
 */
public class EconCalendarFragment extends Fragment {

    private MaterialButton loadButton;
    private ProgressBar progress;
    private TextView message;
    private View analysisCard;
    private TextView analysisLean, analysisBody;
    private LinearLayout listContainer;
    private TextView updatedView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_econ_calendar, container, false);

        loadButton = view.findViewById(R.id.btn_econ_load);
        progress = view.findViewById(R.id.econ_progress);
        message = view.findViewById(R.id.econ_message);
        updatedView = view.findViewById(R.id.econ_updated);
        analysisCard = view.findViewById(R.id.econ_analysis_card);
        analysisLean = view.findViewById(R.id.econ_analysis_lean);
        analysisBody = view.findViewById(R.id.econ_analysis_body);
        listContainer = view.findViewById(R.id.econ_list_container);

        loadButton.setOnClickListener(v -> load());

        // به‌صورت خودکار همان لحظه‌ی باز شدن، داده‌ی زنده را می‌گیرد
        load();

        return view;
    }

    private void load() {
        listContainer.removeAllViews();
        analysisCard.setVisibility(View.GONE);
        message.setVisibility(View.GONE);
        updatedView.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
        loadButton.setEnabled(false);

        EconCalendarClient.fetchUsHighImpact(new EconCalendarClient.Callback() {
            @Override
            public void onSuccess(List<EconEvent> events) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                loadButton.setEnabled(true);

                if (events.isEmpty()) {
                    showMessage(getString(R.string.econ_empty));
                    return;
                }

                renderList(events);
                renderAnalysis(events);

                String now = new SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.US).format(new Date());
                updatedView.setText(getString(R.string.econ_updated_at, now));
                updatedView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded()) return;
                progress.setVisibility(View.GONE);
                loadButton.setEnabled(true);
                showMessage(getString(R.string.econ_error));
            }
        });
    }

    private void showMessage(String text) {
        message.setText(text);
        message.setVisibility(View.VISIBLE);
    }

    private void renderList(List<EconEvent> events) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        String currentDate = null;

        for (EconEvent e : events) {
            if (!e.dateLabel.equals(currentDate)) {
                currentDate = e.dateLabel;
                addDateHeader(prettyDate(e.dateLabel));
            }

            View row = inflater.inflate(R.layout.item_econ_event, listContainer, false);
            ((TextView) row.findViewById(R.id.econ_time)).setText(e.time.isEmpty() ? "—" : e.time);
            ((TextView) row.findViewById(R.id.econ_title)).setText(e.title);
            ((TextView) row.findViewById(R.id.econ_actual_value)).setText(valueOrDash(e.actual));
            ((TextView) row.findViewById(R.id.econ_forecast_value)).setText(valueOrDash(e.forecast));
            ((TextView) row.findViewById(R.id.econ_previous_value)).setText(valueOrDash(e.previous));
            listContainer.addView(row);
        }
    }

    private void addDateHeader(String label) {
        TextView header = new TextView(requireContext());
        header.setText(label);
        header.setTextColor(getResources().getColor(R.color.blueprint_primary));
        header.setTextSize(14f);
        header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
        int padTop = Math.round(14 * getResources().getDisplayMetrics().density);
        int padBottom = Math.round(8 * getResources().getDisplayMetrics().density);
        header.setPadding(0, padTop, 0, padBottom);
        listContainer.addView(header);
    }

    private void renderAnalysis(List<EconEvent> events) {
        MacroSentiment.Result result = MacroSentiment.analyze(events);
        analysisCard.setVisibility(View.VISIBLE);

        int leanRes;
        switch (result.lean) {
            case HAWKISH: leanRes = R.string.econ_lean_hawkish; break;
            case DOVISH: leanRes = R.string.econ_lean_dovish; break;
            case MIXED: leanRes = R.string.econ_lean_mixed; break;
            default: leanRes = R.string.econ_lean_no_data; break;
        }
        analysisLean.setText(getString(R.string.econ_analysis_header, getString(leanRes)));
        analysisBody.setText(result.reasoning);
    }

    private String valueOrDash(String v) {
        return v == null || v.trim().isEmpty() ? "—" : v.trim();
    }

    private String prettyDate(String yyyyMmDd) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(yyyyMmDd);
            return new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.US).format(d);
        } catch (Exception e) {
            return yyyyMmDd;
        }
    }
}
