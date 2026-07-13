package com.mamad.portfolio360.premium;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * وضعیت اشتراک کاربر واردشده را نگه می‌دارد. منبع واقعی، سند
 * subscriptions/{uid} در Firestore است که فقط تابع سرور (پس از تأیید رسید در
 * تلگرام) آن را می‌نویسد؛ این کلاس فقط می‌خواند و در حافظه کش می‌کند تا
 * بررسی وضعیت (isActive) در همه‌جای اپ همگام و سریع باشد.
 *
 * علاوه بر اشتراک پولی، ۱۰ روز اول بعد از ساخت حساب (بر مبنای تاریخ ساخت
 * حساب در فایربیس — نه چیزی که کلاینت بنویسد) کل اپ رایگان است.
 */
public class SubscriptionManager {

    private static final long TRIAL_DURATION_MS = 10L * 24 * 60 * 60 * 1000;

    private static volatile boolean cachedActive = false;
    private static volatile long cachedExpiresAt = 0L;
    private static volatile String lastError = null;

    /** true اگر اشتراک پولی فعال باشد یا هنوز داخل ۱۰ روز آزمایشی رایگان باشیم. */
    public static boolean isActive(Context context) {
        return isPaidActive() || isInTrial();
    }

    /** true فقط وقتی اشتراک پولی واقعی (نه دوره آزمایشی) فعال باشد. */
    public static boolean isPaidActive() {
        return cachedActive && cachedExpiresAt > System.currentTimeMillis();
    }

    /** true اگر هنوز داخل ۱۰ روز اول بعد از ساخت حساب باشیم. */
    public static boolean isInTrial() {
        long endsAt = trialEndsAtMillis();
        return endsAt > 0 && System.currentTimeMillis() < endsAt;
    }

    /** لحظه‌ی پایان دوره آزمایشی (میلی‌ثانیه)، یا ۰ اگر کاربر واردنشده باشد. */
    public static long trialEndsAtMillis() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getMetadata() == null) return 0L;
        long createdAt = user.getMetadata().getCreationTimestamp();
        return createdAt > 0 ? createdAt + TRIAL_DURATION_MS : 0L;
    }

    public static long expiresAtMillis(Context context) {
        return cachedExpiresAt;
    }

    /** پیام آخرین خطای واکشی از Firestore (برای عیب‌یابی)، یا null اگر آخرین واکشی موفق بود. */
    public static String lastError() {
        return lastError;
    }

    /** کش محلی را از روی Firestore به‌روز می‌کند؛ onDone (اگر داده شود) در هر حالت صدا زده می‌شود. */
    public static void refresh(Runnable onDone) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            cachedActive = false;
            cachedExpiresAt = 0L;
            lastError = "کاربر واردنشده (uid=null)";
            if (onDone != null) onDone.run();
            return;
        }

        FirebaseFirestore.getInstance().collection("subscriptions").document(uid).get()
                .addOnSuccessListener(doc -> {
                    lastError = null;
                    if (doc != null && doc.exists()) {
                        Boolean active = doc.getBoolean("active");
                        Long expiresAt = doc.getLong("expiresAt");
                        cachedActive = active != null && active;
                        cachedExpiresAt = expiresAt != null ? expiresAt : 0L;
                    } else {
                        cachedActive = false;
                        cachedExpiresAt = 0L;
                    }
                    if (onDone != null) onDone.run();
                })
                .addOnFailureListener(e -> {
                    lastError = e.getMessage();
                    if (onDone != null) onDone.run();
                });
    }
}
