package com.mamad.portfolio360.premium;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.mamad.portfolio360.R;

/** راهنمای خرید واقعی هر دسته دارایی (سهام، آپشن، کالا) از پلتفرم‌های بین‌المللی، با تأکید بر نیاز به VPN. */
public class AssetGuideFragment extends Fragment {

    private static final String URL_STOCKS = "https://pancakeswap.finance/stocks?chain=eth&chainOut=eth&inputCurrency=0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48&outputCurrency=0xb365cd2588065f522d379ad19e903304f6b622c6&exactAmount=100";
    private static final String URL_OPTIONS = "https://app.derive.xyz/trade/options";
    private static final String URL_COMMODITIES = "https://app.ostium.com/trade";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_asset_guide, container, false);

        MaterialButton btnStocks = view.findViewById(R.id.btn_open_stocks);
        MaterialButton btnOptions = view.findViewById(R.id.btn_open_options);
        MaterialButton btnCommodities = view.findViewById(R.id.btn_open_commodities);

        btnStocks.setOnClickListener(v -> openUrl(URL_STOCKS));
        btnOptions.setOnClickListener(v -> openUrl(URL_OPTIONS));
        btnCommodities.setOnClickListener(v -> openUrl(URL_COMMODITIES));

        return view;
    }

    private void openUrl(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}
