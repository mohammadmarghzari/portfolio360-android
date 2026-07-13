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
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * کلاینت API عمومی Derive.xyz (سابق Lyra).
 * فقط endpointهای عمومی؛ بدون نیاز به کلید یا احراز هویت.
 */
public class DeriveApiClient {

    private static final String BASE_URL = "https://api.lyra.finance/public/";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ---------- کال‌بک‌ها ----------

    public interface PriceCallback {
        void onSuccess(double spotPrice);
        void onError(String message);
    }

    public interface ChainCallback {
        void onSuccess(List<OptionContract> contracts);
        void onError(String message);
    }

    public interface GreeksCallback {
        /** وقتی همه تیکرها (موفق یا ناموفق) تمام شدند صدا زده می‌شود. */
        void onComplete(String debugSampleJson);
    }

    // ---------- قیمت لحظه‌ای ----------

    public static void fetchSpotPrice(String currency, PriceCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("instrument_name", currency.toUpperCase() + "-PERP");
        } catch (JSONException e) {
            post(() -> callback.onError("خطا در ساخت درخواست"));
            return;
        }

        post(BASE_URL + "get_ticker", body, new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                post(() -> callback.onError("اتصال برقرار نشد: " + e.getMessage()));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    JSONObject result = readResult(response);
                    double price = result.getDouble("index_price");
                    post(() -> callback.onSuccess(price));
                } catch (Exception e) {
                    post(() -> callback.onError(e.getMessage()));
                } finally {
                    response.close();
                }
            }
        });
    }

    // ---------- زنجیره آپشن ----------

    /**
     * همه قراردادهای آپشن فعال یک ارز را می‌گیرد و به OptionContract تبدیل می‌کند.
     * فیلدها طبق ساختار واقعی: option_details.{expiry, strike, option_type}
     */
    public static void fetchOptionChain(String currency, ChainCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("currency", currency.toUpperCase());
            body.put("expired", false);
            body.put("instrument_type", "option");
        } catch (JSONException e) {
            post(() -> callback.onError("خطا در ساخت درخواست"));
            return;
        }

        post(BASE_URL + "get_instruments", body, new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                post(() -> callback.onError("اتصال برقرار نشد: " + e.getMessage()));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    JSONArray array = readResultArray(response);
                    List<OptionContract> list = new ArrayList<>();

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.getJSONObject(i);

                        if (!item.optBoolean("is_active", true)) continue;

                        JSONObject details = item.optJSONObject("option_details");
                        if (details == null) continue;

                        String name = item.optString("instrument_name", "");
                        // strike به‌صورت رشته می‌آید
                        double strike = parseNum(details.opt("strike"));
                        // expiry بر حسب ثانیه است
                        long expiry = (long) parseNum(details.opt("expiry"));
                        String type = details.optString("option_type", "");

                        if (name.isEmpty() || strike <= 0 || expiry <= 0) continue;

                        boolean isCall = type.equalsIgnoreCase("C") || type.toLowerCase().startsWith("c");
                        list.add(new OptionContract(name, strike, expiry, isCall));
                    }

                    post(() -> callback.onSuccess(list));
                } catch (Exception e) {
                    post(() -> callback.onError("خطا در پردازش: " + e.getMessage()));
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * برای هر قرارداد، تیکر را می‌گیرد و گریک‌ها/قیمت را داخل همان شیء پر می‌کند.
     * وقتی همه تمام شدند onComplete صدا زده می‌شود.
     * نمونه JSON خام اولین پاسخ برای دیباگ برگردانده می‌شود.
     */
    public static void fetchGreeksFor(List<OptionContract> contracts, GreeksCallback callback) {
        if (contracts.isEmpty()) {
            post(() -> callback.onComplete("(هیچ قراردادی نبود)"));
            return;
        }

        AtomicInteger remaining = new AtomicInteger(contracts.size());
        StringBuilder sample = new StringBuilder();

        for (OptionContract contract : contracts) {
            JSONObject body = new JSONObject();
            try {
                body.put("instrument_name", contract.instrumentName);
            } catch (JSONException ignored) {
                if (remaining.decrementAndGet() == 0) post(() -> callback.onComplete(sample.toString()));
                continue;
            }

            post(BASE_URL + "get_ticker", body, new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (remaining.decrementAndGet() == 0) post(() -> callback.onComplete(sample.toString()));
                }

                @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try {
                        JSONObject result = readResult(response);

                        synchronized (sample) {
                            if (sample.length() == 0) {
                                sample.append(result.toString(2));
                            }
                        }

                        contract.markPrice = pick(result, "mark_price", "best_bid_price", "index_price");

                        // گریک‌ها ممکن است در سطح بالا یا داخل option_pricing باشند
                        JSONObject g = result.optJSONObject("option_pricing");
                        if (g == null) g = result.optJSONObject("greeks");
                        JSONObject src = (g != null) ? g : result;

                        contract.delta = pick(src, "delta");
                        contract.gamma = pick(src, "gamma");
                        contract.vega  = pick(src, "vega");
                        contract.theta = pick(src, "theta");
                        contract.iv    = pick(src, "iv", "mark_iv", "implied_volatility");

                        if (Double.isNaN(contract.markPrice) && g != null) {
                            contract.markPrice = pick(g, "mark_price");
                        }

                    } catch (Exception ignored) {
                        // این قرارداد نادیده گرفته می‌شود
                    } finally {
                        response.close();
                        if (remaining.decrementAndGet() == 0) {
                            post(() -> callback.onComplete(sample.toString()));
                        }
                    }
                }
            });
        }
    }

    // ---------- کمکی ----------

    private static void post(String url, JSONObject body, Callback cb) {
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        client.newCall(request).enqueue(cb);
    }

    private static void post(Runnable r) {
        mainHandler.post(r);
    }

    private static JSONObject readResult(Response response) throws IOException, JSONException {
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("پاسخ نامعتبر (کد " + response.code() + ")");
        }
        JSONObject json = new JSONObject(response.body().string());
        if (json.has("error")) {
            throw new IOException("خطای API: " + json.getJSONObject("error").optString("message", "نامشخص"));
        }
        return json.getJSONObject("result");
    }

    private static JSONArray readResultArray(Response response) throws IOException, JSONException {
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("پاسخ نامعتبر (کد " + response.code() + ")");
        }
        JSONObject json = new JSONObject(response.body().string());
        if (json.has("error")) {
            throw new IOException("خطای API: " + json.getJSONObject("error").optString("message", "نامشخص"));
        }
        Object result = json.get("result");
        if (result instanceof JSONArray) return (JSONArray) result;

        JSONObject obj = (JSONObject) result;
        if (obj.has("instruments")) return obj.getJSONArray("instruments");
        java.util.Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            Object v = obj.get(keys.next());
            if (v instanceof JSONArray) return (JSONArray) v;
        }
        throw new JSONException("آرایه‌ای در پاسخ پیدا نشد");
    }

    /** عدد را چه رشته باشد چه عدد، می‌خواند. */
    private static double parseNum(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** اولین کلید موجود از میان چند نام محتمل را برمی‌گرداند؛ در غیر این صورت NaN. */
    private static double pick(JSONObject obj, String... keys) {
        for (String key : keys) {
            if (obj.has(key) && !obj.isNull(key)) {
                double v = parseNum(obj.opt(key));
                if (v != 0 || obj.opt(key).toString().startsWith("0")) return v;
                return v;
            }
        }
        return Double.NaN;
    }
}
