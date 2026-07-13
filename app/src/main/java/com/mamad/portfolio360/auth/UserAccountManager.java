package com.mamad.portfolio360.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Patterns;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * حساب کاربری فعلاً به‌صورت محلی (روی همین گوشی) با ایمیل و رمز عبور نگه‌داری
 * می‌شود؛ رمز عبور هرگز خام ذخیره نمی‌شود (فقط هش SHA-256 آن). این پیاده‌سازی
 * موقتی است — پس از راه‌اندازی Firebase، با ورود واقعی از طریق جیمیل (و یک
 * حساب مستقل برای هر کاربر روی سرور) جایگزین می‌شود، بدون نیاز به تغییر در
 * کدهای فراخواننده (isLoggedIn / currentEmail).
 */
public class UserAccountManager {

    private static final String PREFS_NAME = "account_prefs";
    private static final String KEY_EMAIL = "account_email";
    private static final String KEY_PASSWORD_HASH = "account_password_hash";
    private static final String KEY_LOGGED_IN = "account_logged_in";

    public static boolean isLoggedIn(Context context) {
        return prefs(context).getBoolean(KEY_LOGGED_IN, false);
    }

    public static String currentEmail(Context context) {
        return prefs(context).getString(KEY_EMAIL, "");
    }

    public static boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /** true اگر قبلاً روی این گوشی حسابی با این ایمیل ساخته شده باشد. */
    public static boolean accountExists(Context context, String email) {
        String saved = prefs(context).getString(KEY_EMAIL, null);
        return saved != null && saved.equalsIgnoreCase(email);
    }

    public static void createAccount(Context context, String email, String password) {
        prefs(context).edit()
                .putString(KEY_EMAIL, email)
                .putString(KEY_PASSWORD_HASH, hash(password))
                .putBoolean(KEY_LOGGED_IN, true)
                .apply();
    }

    /** true اگر ایمیل/رمز با حساب ذخیره‌شده روی این گوشی مطابقت داشته باشد. */
    public static boolean login(Context context, String email, String password) {
        SharedPreferences p = prefs(context);
        String savedEmail = p.getString(KEY_EMAIL, null);
        String savedHash = p.getString(KEY_PASSWORD_HASH, null);
        if (savedEmail == null || savedHash == null) return false;
        if (!savedEmail.equalsIgnoreCase(email)) return false;
        if (!savedHash.equals(hash(password))) return false;

        p.edit().putBoolean(KEY_LOGGED_IN, true).apply();
        return true;
    }

    public static void logout(Context context) {
        prefs(context).edit().putBoolean(KEY_LOGGED_IN, false).apply();
    }

    private static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
