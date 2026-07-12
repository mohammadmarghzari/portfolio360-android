package com.mamad.portfolio360.network;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * کلاینت تستی برای بررسی دسترسی به داده تاریخی Yahoo Finance.
 * Yahoo Finance یک API رسمی و مستند عمومی ندارد؛ این endpoint غیررسمی
 * (chart API) به‌طور گسترده استفاده می‌شود اما هیچ تضمین پایداری‌ای ندارد.
 * هدف این کلاس فقط تایید دسترسی‌پذیری از اندروید، پیش از ساخت منطق کامل است.
 */
public class YahooFinanceClient {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface RawCallback {
        void onSuccess(int httpCode, String rawBodySnippet);
        void onError(String message);
    }

    /**
     * تست خام: داده روزانه یک نماد را برای بازه ۱ ماهه می‌گیرد و
     * فقط کد HTTP و بخشی از پاسخ خام را برمی‌گرداند (بدون parse).
     */
    public static void testFetch(String symbol, RawCallback callback) {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/"
                + symbol + "?range=1mo&interval=1d";

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android)")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onError("اتصال برقرار نشد: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    int code = response.code();

                    String snippet;
                    try {
                        JSONObject json = new JSONObject(body);
                        snippet = json.toString(2);
                        if (snippet.length() > 1500) snippet = snippet.substring(0, 1500) + "\n... (بریده‌شده)";
                    } catch (Exception parseErr) {
                        snippet = body.length() > 800 ? body.substring(0, 800) : body;
                    }

                    String finalSnippet = snippet;
                    mainHandler.post(() -> callback.onSuccess(code, finalSnippet));

                } catch (IOException e) {
                    mainHandler.post(() -> callback.onError("خطا در خواندن پاسخ: " + e.getMessage()));
                } finally {
                    response.close();
                }
            }
        });
    }
}
