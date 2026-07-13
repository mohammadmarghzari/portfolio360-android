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

import java.util.List;

/**
 * گام دوم ویزارد: انتخاب استراتژی بر اساس دیدگاه بازار.
 * پس از انتخاب، کاربر به زنجیره آپشن می‌رود تا یک قرارداد واقعی انتخاب کند.
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

            card.setOnClickListener(v -> openContractPicker(option));

            listContainer.addView(card);
        }

        return view;
    }

    /** کاربر را به زنجیره آپشن می‌برد تا قرارداد واقعی را انتخاب کند. */
    private void openContractPicker(StrategyOption option) {
        if (!option.implemented) {
            Toast.makeText(getContext(), R.string.strategy_coming_soon_message,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        OptionChainFragment fragment = OptionChainFragment.newInstanceForStrategy(option.key);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
