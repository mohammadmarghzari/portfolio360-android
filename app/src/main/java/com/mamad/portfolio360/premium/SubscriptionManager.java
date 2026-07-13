package com.mamad.portfolio360.premium;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * وضعیت اشتراک کاربر را نگه می‌دارد. فعلاً به‌صورت محلی (SharedPreferences) کار
 * می‌کند تا رابط کاربری بخش اشتراکی از همین حالا قابل استفاده باشد؛ وقتی پروژه
 * Firebase وصل شود، پیاده‌سازی این کلاس با خواندن وضعیت از Firestore (بر اساس
 * کاربر واردشده با جیمیل) جایگزین می‌شود، بدون نیاز به تغییر کدهای فراخواننده.
 */
public class SubscriptionManager {

    private static final String PREFS_NAME = "subscription_prefs";
    private static final String KEY_EXPIRES_AT = "expires_at_millis";

    public static boolean isActive(Context context) {
        long expiresAt = prefs(context).getLong(KEY_EXPIRES_AT, 0L);
        return expiresAt > System.currentTimeMillis();
    }

    public static long expiresAtMillis(Context context) {
        return prefs(context).getLong(KEY_EXPIRES_AT, 0L);
    }

    /** برای استفاده‌ی مدیر (تأیید دستی رسید) تا وقتی تأیید اشتراک از طریق Firestore وصل شود. */
    public static void activateForDays(Context context, int days) {
        long newExpiry = System.currentTimeMillis() + days * 24L * 60 * 60 * 1000;
        prefs(context).edit().putLong(KEY_EXPIRES_AT, newExpiry).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
