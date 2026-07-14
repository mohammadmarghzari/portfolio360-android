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
 * دریافت تقویم اقتصادی این هفته از فید عمومی و رایگان faireconomy (همان فیدی که
 * ابزارهای متاتریدر استفاده می‌کنند). نیازی به کلید یا احراز هویت ندارد. فقط
 * رویدادهای «مهم» (High impact) آمریکا فیلتر می‌شوند — همان‌هایی که می‌توانند
 * روی طلا، بیت‌کوین و بازارهای مالی نوسان ایجاد کنند.
 *
 * چون درخواست از گوشی کاربر ارسال می‌شود، برای دسترسی معمولاً باید VPN روشن باشد.
 */
public class EconCalendarClient {

    private static final String URL = "https://nfs.faireconomy.media/ff_calendar_thisweek.json";

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(List<EconEvent> events);
        void onError(String message);
    }

    public static void fetchThisWeekUsHighImpact(Callback callback) {
        Request request = new Request.Builder()
                .url(URL)
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
                    List<EconEvent> list = parse(body);
                    mainHandler.post(() -> callback.onSuccess(list));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                } finally {
                    response.close();
                }
            }
        });
    }

    private static List<EconEvent> parse(String json) throws Exception {
        List<EconEvent> out = new ArrayList<>();
        JSONArray arr = new JSONArray(json);
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String country = o.optString("country", "");
            String impact = o.optString("impact", "");
            if (!country.equalsIgnoreCase("USD")) continue;
            if (!impact.equalsIgnoreCase("High")) continue;

            EconEvent e = new EconEvent();
            e.title = o.optString("title", "");
            e.impact = impact;
            e.actual = o.optString("actual", "");
            e.forecast = o.optString("forecast", "");
            e.previous = o.optString("previous", "");

            String date = o.optString("date", "");
            if (date.length() >= 16) {
                e.dateLabel = date.substring(0, 10);
                e.time = date.substring(11, 16);
            } else {
                e.dateLabel = date;
                e.time = "";
            }
            out.add(e);
        }
        return out;
    }
}
