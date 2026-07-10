package com.mamad.portfolio360.network;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * کلاینت ساده برای اتصال به API عمومی Derive.xyz (سابق Lyra Chain).
 * مستندات: https://docs.derive.xyz
 *
 * نکته: این کلاینت فقط از endpointهای عمومی (public) استفاده می‌کند که نیاز به
 * احراز هویت یا کلید API ندارند.
 */
public class DeriveApiClient {

    private static final String BASE_URL = "https://api.lyra.finance/public/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface PriceCallback {
        void onSuccess(double spotPrice);
        void onError(String message);
    }

    /**
     * قیمت لحظه‌ای (spot) دارایی را از طریق تیکر قرارداد دائمی (Perpetual) می‌گیرد.
     * برای اتریوم، نام قرارداد دائمی معمولاً "ETH-PERP" است.
     */
    public static void fetchSpotPrice(String currency, PriceCallback callback) {
        String instrumentName = currency.toUpperCase() + "-PERP";

        JSONObject body = new JSONObject();
        try {
            body.put("instrument_name", instrumentName);
        } catch (JSONException e) {
            postError(callback, "خطا در ساخت درخواست");
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "get_ticker")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postError(callback, "اتصال به Derive.xyz برقرار نشد: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        postError(callback, "پاسخ نامعتبر از سرور (کد " + response.code() + ")");
                        return;
                    }

                    String raw = response.body().string();
                    JSONObject json = new JSONObject(raw);

                    if (json.has("error")) {
                        postError(callback, "خطای API: " + json.getJSONObject("error").optString("message", "نامشخص"));
                        return;
                    }

                    JSONObject result = json.getJSONObject("result");
                    double indexPrice = result.getDouble("index_price");

                    postSuccess(callback, indexPrice);

                } catch (JSONException | IOException e) {
                    postError(callback, "خطا در پردازش پاسخ سرور: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    private static void postSuccess(PriceCallback callback, double price) {
        mainHandler.post(() -> callback.onSuccess(price));
    }

    private static void postError(PriceCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
