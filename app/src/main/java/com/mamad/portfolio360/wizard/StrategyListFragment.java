package com.mamad.portfolio360.wizard;

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

import com.mamad.portfolio360.R;
import com.mamad.portfolio360.fragments.CoveredCallFragment;
import com.mamad.portfolio360.fragments.ProtectivePutFragment;

import java.util.List;

/**
 * صفحه دوم ویزارد: نمایش لیست استراتژی‌های پیشنهادی بر اساس دیدگاه بازار انتخابی
 * — مشابه بخش "Select a strategy" در Derive.xyz.
 */
public class StrategyListFragment extends Fragment {

    private static final String ARG_OUTLOOK = "arg_outlook";

    public static StrategyListFragment newInstance(String outlookKey) {
        StrategyListFragment fragment = new StrategyListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_OUTLOOK, outlookKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_strategy_list, container, false);

        String outlookKey = getArguments() != null ? getArguments().getString(ARG_OUTLOOK, "") : "";
        LinearLayout listContainer = view.findViewById(R.id.strategy_container);

        List<StrategyOption> options = StrategyMapping.getStrategiesFor(outlookKey);

        for (StrategyOption option : options) {
            View card = inflater.inflate(R.layout.item_strategy_card, listContainer, false);

            TextView title = card.findViewById(R.id.text_title);
            TextView description = card.findViewById(R.id.text_description);
            TextView badge = card.findViewById(R.id.text_badge);

            title.setText(option.title);
            description.setText(option.description);
            badge.setVisibility(option.implemented ? View.GONE : View.VISIBLE);

            card.setOnClickListener(v -> openStrategyDetail(option));

            listContainer.addView(card);
        }

        return view;
    }

    private void openStrategyDetail(StrategyOption option) {
        Fragment detailFragment = null;

        switch (option.key) {
            case "covered_call":
                detailFragment = new CoveredCallFragment();
                break;
            case "protective_put":
                detailFragment = new ProtectivePutFragment();
                break;
            default:
                // استراتژی‌های دیگر هنوز پیاده‌سازی نشده‌اند
                Toast.makeText(getContext(), R.string.strategy_coming_soon_message, Toast.LENGTH_SHORT).show();
                return;
        }

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }
}
