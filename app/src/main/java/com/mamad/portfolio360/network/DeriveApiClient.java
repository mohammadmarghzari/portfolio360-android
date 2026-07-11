package com.mamad.portfolio360.network;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public interface InstrumentsCallback {
        void onSuccess(List<DeriveInstrument> instruments);
        void onError(String message);
    }

    /**
     * قیمت لحظه‌ای (spot) دارایی را از طریق تیکر قرارداد دائمی (Perpetual) می‌گیرد.
     * برای اتریوم، نام قرارداد دائمی معمولاً "ETH-PERP" است.
     */
    public static void fetchSpotPrice(String currency, PriceCallback callback) {
        String instrumentName = currency.toUpperCase() + "-PERP";
        fetchTickerField(instrumentName, "index_price", callback);
    }

    /**
     * پرمیوم لحظه‌ای (mark price) یک قرارداد آپشن مشخص را می‌گیرد.
     */
    public static void fetchMarkPrice(String instrumentName, PriceCallback callback) {
        fetchTickerField(instrumentName, "mark_price", callback);
    }

    private static void fetchTickerField(String instrumentName, String fieldName, PriceCallback callback) {
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
                    double value = result.getDouble(fieldName);

                    postSuccess(callback, value);

                } catch (JSONException | IOException e) {
                    postError(callback, "خطا در پردازش پاسخ سرور: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * لیست قراردادهای آپشن فعال (غیرمنقضی) یک ارز را می‌گیرد.
     * currency مثلاً "ETH"، optionType یکی از "call" یا "put".
     */
    public static void fetchOptionInstruments(String currency, String optionType, InstrumentsCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("currency", currency.toUpperCase());
            body.put("expired", false);
            body.put("instrument_type", "option");
        } catch (JSONException e) {
            postInstrumentsError(callback, "خطا در ساخت درخواست");
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "get_instruments")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postInstrumentsError(callback, "اتصال به Derive.xyz برقرار نشد: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        postInstrumentsError(callback, "پاسخ نامعتبر از سرور (کد " + response.code() + ")");
                        return;
                    }

                    String raw = response.body().string();
                    JSONObject json = new JSONObject(raw);

                    if (json.has("error")) {
                        postInstrumentsError(callback, "خطای API: " + json.getJSONObject("error").optString("message", "نامشخص"));
                        return;
                    }

                    JSONArray array = extractInstrumentsArray(json);
                    List<DeriveInstrument> instruments = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.getJSONObject(i);
                        String type = item.optString("option_type", "");

                        if (optionType != null && !type.toLowerCase().startsWith(optionType.substring(0, 1).toLowerCase())) {
                            continue;
                        }

                        instruments.add(new DeriveInstrument(
                                item.optString("instrument_name", ""),
                                item.optDouble("strike", 0),
                                item.optLong("expiration_timestamp", 0),
                                type
                        ));
                    }

                    postInstrumentsSuccess(callback, instruments);

                } catch (JSONException | IOException e) {
                    postInstrumentsError(callback, "خطا در پردازش پاسخ سرور: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * پاسخ get_instruments ممکن است آرایه مستقیم زیر "result" باشد یا داخل یک
     * کلید تودرتو مثل "instruments" — هر دو حالت را پوشش می‌دهیم.
     */
    private static JSONArray extractInstrumentsArray(JSONObject json) throws JSONException {
        Object result = json.get("result");
        if (result instanceof JSONArray) {
            return (JSONArray) result;
        }
        JSONObject resultObj = json.getJSONObject("result");
        if (resultObj.has("instruments")) {
            return resultObj.getJSONArray("instruments");
        }
        // آخرین تلاش: اولین کلید آرایه‌ای داخل result را برگردان
        java.util.Iterator<String> keys = resultObj.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (resultObj.get(key) instanceof JSONArray) {
                return resultObj.getJSONArray(key);
            }
        }
        throw new JSONException("آرایه instruments در پاسخ پیدا نشد");
    }

    public interface RawListCallback {
        void onSuccess(String rawFirstItem, int totalCount);
        void onError(String message);
    }

    /**
     * برای شناخت دقیق ساختار پاسخ get_instruments، این متد فقط تعداد کل آپشن‌های
     * فعال یک دارایی و متن خام اولین آیتم را برمی‌گرداند (بدون parse دقیق فیلدها).
     * پس از بررسی خروجی واقعی، در جلسه بعد منطق کامل انتخاب strike پیاده می‌شود.
     */
    public static void fetchOptionInstrumentsRaw(String currency, RawListCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("currency", currency.toUpperCase());
            body.put("expired", false);
            body.put("instrument_type", "option");
        } catch (JSONException e) {
            postRawError(callback, "خطا در ساخت درخواست");
            return;
        }

        Request request = new Request.Builder()
                .url(BASE_URL + "get_instruments")
                .post(RequestBody.create(body.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                postRawError(callback, "اتصال برقرار نشد: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        postRawError(callback, "پاسخ نامعتبر (کد " + response.code() + ")");
                        return;
                    }

                    String raw = response.body().string();
                    JSONObject json = new JSONObject(raw);

                    if (json.has("error")) {
                        postRawError(callback, "خطای API: " + json.getJSONObject("error").optString("message", "نامشخص"));
                        return;
                    }

                    // نتیجه ممکن است آرایه مستقیم باشد یا داخل یک کلید دیگر
                    JSONArray instruments;
                    Object result = json.get("result");
                    if (result instanceof JSONArray) {
                        instruments = (JSONArray) result;
                    } else {
                        JSONObject resultObj = (JSONObject) result;
                        instruments = resultObj.optJSONArray("instruments");
                        if (instruments == null) {
                            postRawSuccess(callback, resultObj.toString(2), -1);
                            return;
                        }
                    }

                    if (instruments.length() == 0) {
                        postRawSuccess(callback, "(هیچ آپشنی برنگشت)", 0);
                        return;
                    }

                    String firstItem = instruments.getJSONObject(0).toString(2);
                    postRawSuccess(callback, firstItem, instruments.length());

                } catch (JSONException | IOException e) {
                    postRawError(callback, "خطا در پردازش پاسخ: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    private static void postRawSuccess(RawListCallback callback, String firstItem, int count) {
        mainHandler.post(() -> callback.onSuccess(firstItem, count));
    }

    private static void postRawError(RawListCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    private static void postSuccess(PriceCallback callback, double price) {
        mainHandler.post(() -> callback.onSuccess(price));
    }

    private static void postError(PriceCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }

    private static void postInstrumentsSuccess(InstrumentsCallback callback, List<DeriveInstrument> instruments) {
        mainHandler.post(() -> callback.onSuccess(instruments));
    }

    private static void postInstrumentsError(InstrumentsCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
