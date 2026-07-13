package com.mamad.portfolio360.wizard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.mamad.portfolio360.R;
import com.mamad.portfolio360.network.DeriveApiClient;

import java.util.Locale;

/**
 * صفحه اول ویزارد: قیمت زنده ETH، دکمه ورود به زنجیره آپشن،
 * و انتخاب دیدگاه بازار (صعودی/نزولی/پرنوسان/خنثی/کسب درآمد).
 */
public class MarketOutlookFragment extends Fragment {

    private TextView textAsset;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_market_outlook, container, false);

        textAsset = view.findViewById(R.id.text_asset);
        loadSpotPrice();

        bindOutlookCard(view, R.id.card_bullish, StrategyMapping.OUTLOOK_BULLISH);
        bindOutlookCard(view, R.id.card_bearish, StrategyMapping.OUTLOOK_BEARISH);
        bindOutlookCard(view, R.id.card_volatile, StrategyMapping.OUTLOOK_VOLATILE);
        bindOutlookCard(view, R.id.card_neutral, StrategyMapping.OUTLOOK_NEUTRAL);
        bindOutlookCard(view, R.id.card_yield, StrategyMapping.OUTLOOK_YIELD);

        View chainButton = view.findViewById(R.id.btn_debug_options);
        chainButton.setOnClickListener(v -> openOptionChain());

        return view;
    }

    private void openOptionChain() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new OptionChainFragment())
                .addToBackStack(null)
                .commit();
    }

    private void loadSpotPrice() {
        textAsset.setText(R.string.outlook_asset_loading);

        DeriveApiClient.fetchSpotPrice("ETH", new DeriveApiClient.PriceCallback() {
            @Override
            public void onSuccess(double spotPrice) {
                if (!isAdded()) return;
                String formatted = String.format(Locale.US, "%,.2f", spotPrice);
                textAsset.setText(getString(R.string.outlook_asset_live, formatted));
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                textAsset.setText(getString(R.string.outlook_asset_error));
            }
        });
    }

    private void bindOutlookCard(View root, int cardId, String outlookKey) {
        CardView card = root.findViewById(cardId);
        card.setOnClickListener(v -> openStrategyList(outlookKey));
    }

    private void openStrategyList(String outlookKey) {
        StrategyListFragment fragment = StrategyListFragment.newInstance(outlookKey);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
