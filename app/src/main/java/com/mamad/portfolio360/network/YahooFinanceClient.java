package com.mamad.portfolio360.network;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * کلاینت دریافت داده تاریخی Yahoo Finance (endpoint غیررسمی chart API).
 * ساختار پاسخ: chart.result[0].timestamp[] و
 * chart.result[0].indicators.quote[0].close[]
 */
public class YahooFinanceClient {

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface RawCallback {
        void onSuccess(int httpCode, String rawBodySnippet);
        void onError(String message);
    }

    public interface HistoryCallback {
        void onSuccess(String symbol, List<HistoricalPoint> points);
        void onError(String symbol, String message);
    }

    /** تست خام قبلی — فقط برای دیباگ اولیه، بدون parse. */
    public static void testFetch(String symbol, RawCallback callback) {
        String url = buildUrl(symbol, "1mo");
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android)")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onError("اتصال برقرار نشد: " + e.getMessage()));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    int code = response.code();
                    String snippet;
                    try {
                        JSONObject json = new JSONObject(body);
                        snippet = json.toString(2);
                        if (snippet.length() > 1500) snippet = snippet.substring(0, 1500) + "\n... (بریده‌شده)";
                    } catch (Exception e) {
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

    /**
     * قیمت‌های بسته‌شدن روزانه یک نماد را برای بازه مشخص می‌گیرد و parse می‌کند.
     * range یکی از: "6mo", "1y", "2y", "3y" (اصطلاح Yahoo).
     */
    public static void fetchHistory(String symbol, String range, HistoryCallback callback) {
        String url = buildUrl(symbol, range);
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android)")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onError(symbol, "اتصال برقرار نشد: " + e.getMessage()));
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        String msg = "پاسخ نامعتبر (کد " + response.code() + ")";
                        mainHandler.post(() -> callback.onError(symbol, msg));
                        return;
                    }

                    String body = response.body().string();
                    JSONObject root = new JSONObject(body);
                    JSONObject chart = root.getJSONObject("chart");

                    if (!chart.isNull("error") && chart.has("error") && !chart.get("error").equals(JSONObject.NULL)) {
                        JSONObject err = chart.optJSONObject("error");
                        String msg = err != null ? err.optString("description", "خطای نامشخص Yahoo") : "خطای نامشخص Yahoo";
                        mainHandler.post(() -> callback.onError(symbol, msg));
                        return;
                    }

                    JSONArray results = chart.getJSONArray("result");
                    if (results.length() == 0) {
                        mainHandler.post(() -> callback.onError(symbol, "نتیجه‌ای یافت نشد"));
                        return;
                    }

                    JSONObject result = results.getJSONObject(0);
                    JSONArray timestamps = result.getJSONArray("timestamp");
                    JSONObject indicators = result.getJSONObject("indicators");
                    JSONArray quoteArr = indicators.getJSONArray("quote");
                    JSONObject quote = quoteArr.getJSONObject(0);
                    JSONArray closes = quote.getJSONArray("close");

                    List<HistoricalPoint> points = new ArrayList<>();
                    for (int i = 0; i < timestamps.length(); i++) {
                        if (closes.isNull(i)) continue; // برخی روزها ممکن است null باشند (تعطیلی)
                        long ts = timestamps.getLong(i);
                        double close = closes.getDouble(i);
                        points.add(new HistoricalPoint(ts, close));
                    }

                    if (points.isEmpty()) {
                        mainHandler.post(() -> callback.onError(symbol, "داده معتبری یافت نشد"));
                        return;
                    }

                    mainHandler.post(() -> callback.onSuccess(symbol, points));

                } catch (Exception e) {
                    String msg = "خطا در پردازش: " + e.getMessage();
                    mainHandler.post(() -> callback.onError(symbol, msg));
                } finally {
                    response.close();
                }
            }
        });
    }

    private static String buildUrl(String symbol, String range) {
        return "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol
                + "?range=" + range + "&interval=1d";
    }

    public interface MultiHistoryCallback {
        /** برای هر نماد که موفق بود صدا زده می‌شود؛ در پایان همه (موفق یا نه)، onComplete صدا زده می‌شود. */
        void onEachSuccess(String symbol, List<HistoricalPoint> points);
        void onEachError(String symbol, String message);
        void onComplete();
    }

    /** داده تاریخی چند نماد را هم‌زمان می‌گیرد و در پایان onComplete را صدا می‌زند. */
    public static void fetchMultipleHistories(List<String> symbols, String range, MultiHistoryCallback callback) {
        if (symbols.isEmpty()) {
            callback.onComplete();
            return;
        }

        java.util.concurrent.atomic.AtomicInteger remaining =
                new java.util.concurrent.atomic.AtomicInteger(symbols.size());

        for (String symbol : symbols) {
            fetchHistory(symbol, range, new HistoryCallback() {
                @Override
                public void onSuccess(String sym, List<HistoricalPoint> points) {
                    callback.onEachSuccess(sym, points);
                    if (remaining.decrementAndGet() == 0) callback.onComplete();
                }

                @Override
                public void onError(String sym, String message) {
                    callback.onEachError(sym, message);
                    if (remaining.decrementAndGet() == 0) callback.onComplete();
                }
            });
        }
    }
}
