package com.mamad.portfolio360.premium;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.mamad.portfolio360.R;
import com.mamad.portfolio360.fragments.CoveredCallFragment;
import com.mamad.portfolio360.fragments.ProtectivePutFragment;
import com.mamad.portfolio360.portfolio.PortfolioSetupFragment;
import com.mamad.portfolio360.wizard.OptionChainFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * هاب بخش اشتراکی: استراتژی‌های درآمدزای آپشن (کاوردکال، پروتکتیو پوت،
 * لانگ استرنگل) و ابزارهای حرفه‌ای پرتفوی، همه در یک‌جا. اگر کاربر اشتراک
 * فعال نداشته باشد، به‌جای باز شدن ابزار، صفحه راهنمای خرید اشتراک باز می‌شود.
 */
public class PremiumHubFragment extends Fragment {

    private static class Item {
        final String titleRes;
        final String descRes;
        final java.util.function.Supplier<Fragment> destination;

        Item(String titleRes, String descRes, java.util.function.Supplier<Fragment> destination) {
            this.titleRes = titleRes;
            this.descRes = descRes;
            this.destination = destination;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_premium_hub, container, false);

        bindStatusBanner(view);

        MaterialButton manageBtn = view.findViewById(R.id.btn_manage_subscription);
        manageBtn.setOnClickListener(v -> navigateTo(new SubscriptionInfoFragment()));

        buildItems(view);

        return view;
    }

    private void bindStatusBanner(View view) {
        TextView banner = view.findViewById(R.id.premium_status_banner);
        if (SubscriptionManager.isActive(requireContext())) {
            String until = new SimpleDateFormat("yyyy/MM/dd", Locale.US)
                    .format(new Date(SubscriptionManager.expiresAtMillis(requireContext())));
            banner.setText(getString(R.string.sub_status_active, until));
        } else {
            banner.setText(getString(R.string.sub_status_inactive));
        }
    }

    private void buildItems(View root) {
        LinearLayout container = root.findViewById(R.id.premium_item_container);
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        List<Item> items = new ArrayList<>();
        items.add(new Item(
                getString(R.string.premium_item_covered_call_title),
                getString(R.string.premium_item_covered_call_desc),
                CoveredCallFragment::new));
        items.add(new Item(
                getString(R.string.premium_item_protective_put_title),
                getString(R.string.premium_item_protective_put_desc),
                ProtectivePutFragment::new));
        items.add(new Item(
                getString(R.string.premium_item_long_strangle_title),
                getString(R.string.premium_item_long_strangle_desc),
                () -> OptionChainFragment.newInstanceForStrategy("long_strangle")));
        items.add(new Item(
                getString(R.string.premium_item_portfolio_title),
                getString(R.string.premium_item_portfolio_desc),
                PortfolioSetupFragment::new));

        for (Item item : items) {
            View cardView = inflater.inflate(R.layout.item_strategy_card, container, false);
            CardView card = (CardView) cardView;

            ((TextView) card.findViewById(R.id.text_title)).setText(item.titleRes);
            ((TextView) card.findViewById(R.id.text_description)).setText(item.descRes);
            card.findViewById(R.id.text_badge).setVisibility(View.GONE);

            card.setOnClickListener(v -> {
                if (SubscriptionManager.isActive(requireContext())) {
                    navigateTo(item.destination.get());
                } else {
                    Toast.makeText(getContext(), R.string.premium_locked_toast, Toast.LENGTH_SHORT).show();
                    navigateTo(new SubscriptionInfoFragment());
                }
            });

            container.addView(card);
        }
    }

    private void navigateTo(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
