package com.mamad.portfolio360.premium;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * وضعیت اشتراک کاربر واردشده را نگه می‌دارد. منبع واقعی، سند
 * subscriptions/{uid} در Firestore است که فقط تابع سرور (پس از تأیید رسید در
 * تلگرام) آن را می‌نویسد؛ این کلاس فقط می‌خواند و در حافظه کش می‌کند تا
 * بررسی وضعیت (isActive) در همه‌جای اپ همگام و سریع باشد.
 */
public class SubscriptionManager {

    private static volatile boolean cachedActive = false;
    private static volatile long cachedExpiresAt = 0L;

    public static boolean isActive(Context context) {
        return cachedActive && cachedExpiresAt > System.currentTimeMillis();
    }

    public static long expiresAtMillis(Context context) {
        return cachedExpiresAt;
    }

    /** کش محلی را از روی Firestore به‌روز می‌کند؛ onDone (اگر داده شود) در هر حالت صدا زده می‌شود. */
    public static void refresh(Runnable onDone) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            cachedActive = false;
            cachedExpiresAt = 0L;
            if (onDone != null) onDone.run();
            return;
        }

        FirebaseFirestore.getInstance().collection("subscriptions").document(uid).get()
                .addOnSuccessListener(doc -> {
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
                    if (onDone != null) onDone.run();
                });
    }
}
