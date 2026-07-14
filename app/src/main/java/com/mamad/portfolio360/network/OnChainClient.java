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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * دریافت داده‌های آنچین/مشتقات (سنتیمنت بازار) از API عمومی و رایگان بایننس
 * فیوچرز — بدون نیاز به کلید. همان متریک‌هایی که سایت‌هایی مثل CryptoQuant و
 * Coinglass نشان می‌دهند (نسبت خرید/فروش تیکرها، نسبت لانگ/شورت، اوپن‌اینترست،
 * نرخ فاندینگ) اینجا به‌صورت زنده و رایگان در دسترس‌اند.
 *
 * چون درخواست از گوشی کاربر می‌رود، برای دسترسی معمولاً باید VPN روشن باشد.
 */
public class OnChainClient {

    private static final String BASE = "https://fapi.binance.com";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static class Point {
        public final long time;
        public final double value;

        public Point(long time, double value) {
            this.time = time;
            this.value = value;
        }
    }

    public interface Callback {
        void onSuccess(List<Point> points);
        void onError(String message);
    }

    public enum Metric { TAKER_RATIO, LONG_SHORT, OPEN_INTEREST, FUNDING }

    public static void fetch(Metric metric, String symbol, String period, Callback callback) {
        String url;
        String timeKey;
        String valueKey;
        double scale = 1.0;

        switch (metric) {
            case TAKER_RATIO:
                url = BASE + "/futures/data/takerlongshortRatio?symbol=" + symbol + "&period=" + period + "&limit=48";
                timeKey = "timestamp";
                valueKey = "buySellRatio";
                break;
            case LONG_SHORT:
                url = BASE + "/futures/data/globalLongShortAccountRatio?symbol=" + symbol + "&period=" + period + "&limit=48";
                timeKey = "timestamp";
                valueKey = "longShortRatio";
                break;
            case OPEN_INTEREST:
                url = BASE + "/futures/data/openInterestHist?symbol=" + symbol + "&period=" + period + "&limit=48";
                timeKey = "timestamp";
                valueKey = "sumOpenInterestValue";
                break;
            case FUNDING:
            default:
                url = BASE + "/fapi/v1/fundingRate?symbol=" + symbol + "&limit=48";
                timeKey = "fundingTime";
                valueKey = "fundingRate";
                scale = 100.0; // به درصد
                break;
        }

        final String fTimeKey = timeKey;
        final String fValueKey = valueKey;
        final double fScale = scale;

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android) Portfolio360")
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        int code = response.code();
                        mainHandler.post(() -> callback.onError("HTTP " + code));
                        return;
                    }
                    String body = response.body().string();
                    List<Point> points = parse(body, fTimeKey, fValueKey, fScale);
                    mainHandler.post(() -> callback.onSuccess(points));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                } finally {
                    response.close();
                }
            }
        });
    }

    private static List<Point> parse(String json, String timeKey, String valueKey, double scale) throws Exception {
        List<Point> out = new ArrayList<>();
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            long time = o.optLong(timeKey, 0);
            double value;
            try {
                value = Double.parseDouble(o.optString(valueKey, "0")) * scale;
            } catch (NumberFormatException e) {
                continue;
            }
            out.add(new Point(time, value));
        }
        return out;
    }
}
