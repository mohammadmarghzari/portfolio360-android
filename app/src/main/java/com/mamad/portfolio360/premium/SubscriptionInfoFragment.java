package com.mamad.portfolio360.premium;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.google.firebase.auth.FirebaseAuth;
import com.mamad.portfolio360.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * صفحه راهنمای خرید اشتراک: پلن‌ها، شماره کارت، و دکمه ارسال رسید به ربات تلگرام.
 * دکمه ارسال رسید با شناسه کاربری (uid) به ربات وصل می‌شود تا سرور بداند تأیید
 * مدیر برای کدام حساب است؛ تأیید و فعال‌سازی نهایی در تلگرام و به‌صورت خودکار
 * (از طریق Cloud Function) انجام می‌شود.
 */
public class SubscriptionInfoFragment extends Fragment {

    private static final String TELEGRAM_BOT_URL = "https://t.me/portfolio360bot";

    private static class Plan {
        final String duration;
        final String price;
        final String note;
        final int badgeRes;

        Plan(String duration, String price, String note, int badgeRes) {
            this.duration = duration;
            this.price = price;
            this.note = note;
            this.badgeRes = badgeRes;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_subscription_info, container, false);

        bindStatusBanner(view);
        buildPlanCards(view);
        SubscriptionManager.refresh(() -> {
            if (isAdded()) bindStatusBanner(view);
        });

        TextView cardNumber = view.findViewById(R.id.sub_card_number);
        MaterialButton copyBtn = view.findViewById(R.id.btn_copy_card);
        copyBtn.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("card_number", cardNumber.getText().toString().replace(" ", ""));
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), R.string.sub_card_copied, Toast.LENGTH_SHORT).show();
        });

        MaterialButton sendReceiptBtn = view.findViewById(R.id.btn_send_receipt);
        sendReceiptBtn.setOnClickListener(v -> {
            String uid = FirebaseAuth.getInstance().getUid();
            String url = uid != null ? TELEGRAM_BOT_URL + "?start=" + uid : TELEGRAM_BOT_URL;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        });

        return view;
    }

    private void bindStatusBanner(View view) {
        TextView banner = view.findViewById(R.id.sub_status_banner);
        if (SubscriptionManager.isActive(requireContext())) {
            String until = new SimpleDateFormat("yyyy/MM/dd", Locale.US)
                    .format(new Date(SubscriptionManager.expiresAtMillis(requireContext())));
            banner.setText(getString(R.string.sub_status_active, until));
        } else {
            banner.setText(getString(R.string.sub_status_inactive));
        }
    }

    private void buildPlanCards(View root) {
        LinearLayout container = root.findViewById(R.id.sub_plans_container);
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        Plan[] plans = {
                new Plan(getString(R.string.sub_plan_1m), "۱۰۰,۰۰۰ تومان", getString(R.string.sub_plan_note_monthly), 0),
                new Plan(getString(R.string.sub_plan_3m), "۳۰۰,۰۰۰ تومان", getString(R.string.sub_plan_note_monthly), 0),
                new Plan(getString(R.string.sub_plan_6m), "۶۰۰,۰۰۰ تومان", getString(R.string.sub_plan_note_monthly), R.string.sub_badge_popular),
                new Plan(getString(R.string.sub_plan_1y), "۱,۰۰۰,۰۰۰ تومان", getString(R.string.sub_plan_note_yearly), R.string.sub_badge_best_value),
        };

        for (Plan plan : plans) {
            View card = inflater.inflate(R.layout.item_plan_card, container, false);
            ((TextView) card.findViewById(R.id.plan_duration)).setText(plan.duration);
            ((TextView) card.findViewById(R.id.plan_price)).setText(plan.price);
            ((TextView) card.findViewById(R.id.plan_price_note)).setText(plan.note);

            TextView badge = card.findViewById(R.id.plan_badge);
            if (plan.badgeRes != 0) {
                badge.setText(plan.badgeRes);
                badge.setVisibility(View.VISIBLE);
            }

            container.addView(card);
        }
    }
}
