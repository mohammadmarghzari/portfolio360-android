package com.mamad.portfolio360.wizard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.mamad.portfolio360.R;
import com.mamad.portfolio360.portfolio.PortfolioSetupFragment;

/**
 * صفحه اصلی اپلیکیشن: انتخاب بین دو بخش مستقل —
 * «تحلیل و استراتژی آپشن» و «بهینه‌سازی پرتفوی».
 */
public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        CardView optionsCard = view.findViewById(R.id.card_options);
        optionsCard.setOnClickListener(v -> navigateTo(new MarketOutlookFragment()));

        CardView portfolioCard = view.findViewById(R.id.card_portfolio);
        portfolioCard.setOnClickListener(v -> navigateTo(new PortfolioSetupFragment()));

        return view;
    }

    private void navigateTo(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
