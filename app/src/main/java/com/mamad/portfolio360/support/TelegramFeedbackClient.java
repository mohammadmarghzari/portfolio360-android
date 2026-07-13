package com.mamad.portfolio360.support;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ارسال مستقیم پیام انتقاد/پیشنهاد کاربر به چت تلگرام مدیر، از طریق Bot API —
 * چون این تماس از گوشی کاربر انجام می‌شود (نه از سرور)، محدودیت شبکه‌ای وجود ندارد.
 */
public class TelegramFeedbackClient {

    private static final String BOT_TOKEN = "8606014493:AAEjxKqhVYiep071XOsohDh-jgqZkTf4k9c";
    private static final String ADMIN_CHAT_ID = "840723814";
    private static final String SEND_URL = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface SendCallback {
        void onSuccess();
        void onError(String message);
    }

    public static void sendFeedback(String userEmail, String message, SendCallback callback) {
        String text = "📝 انتقاد/پیشنهاد جدید از Portfolio360\n\n"
                + "کاربر: " + (userEmail != null ? userEmail : "نامشخص") + "\n\n"
                + message;

        RequestBody body = new FormBody.Builder()
                .add("chat_id", ADMIN_CHAT_ID)
                .add("text", text)
                .build();

        Request request = new Request.Builder().url(SEND_URL).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                boolean ok = response.isSuccessful();
                response.close();
                mainHandler.post(() -> {
                    if (ok) callback.onSuccess();
                    else callback.onError("Telegram API error");
                });
            }
        });
    }
}
