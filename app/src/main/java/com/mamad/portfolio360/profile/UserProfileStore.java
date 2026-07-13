package com.mamad.portfolio360.profile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * ذخیره‌ی نام نمایشی و عکس پروفایل روی سند users/{uid} در Firestore — نه در
 * Firebase Storage، چون Storage برای پروژه‌های تازه به پلن Blaze نیاز دارد که
 * برای حساب‌های ایرانی در دسترس نیست. عکس به‌صورت فشرده و Base64 داخل همان
 * سند ذخیره می‌شود (کوچک‌تر از محدودیت ۱ مگابایتی هر سند Firestore)، پس با
 * ورود از هر گوشی، پروفایل کامل بازیابی می‌شود.
 */
public class UserProfileStore {

    private static final int MAX_DIMENSION = 256;

    public interface LoadCallback {
        void onLoaded(String displayName, Bitmap photo);
        void onError();
    }

    public interface SaveCallback {
        void onSaved();
        void onError();
    }

    public static void load(LoadCallback callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            callback.onError();
            return;
        }
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String name = doc != null ? doc.getString("displayName") : null;
                    String photoBase64 = doc != null ? doc.getString("photoBase64") : null;
                    Bitmap bitmap = null;
                    if (photoBase64 != null) {
                        try {
                            byte[] bytes = Base64.decode(photoBase64, Base64.DEFAULT);
                            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        } catch (Exception ignored) {
                        }
                    }
                    callback.onLoaded(name, bitmap);
                })
                .addOnFailureListener(e -> callback.onError());
    }

    public static void saveDisplayName(String displayName, SaveCallback callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            callback.onError();
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("displayName", displayName);
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSaved())
                .addOnFailureListener(e -> callback.onError());
    }

    public static void savePhoto(Bitmap photo, SaveCallback callback) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            callback.onError();
            return;
        }

        Bitmap scaled = scaleDown(photo, MAX_DIMENSION);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, out);
        String base64 = Base64.encodeToString(out.toByteArray(), Base64.DEFAULT);

        Map<String, Object> data = new HashMap<>();
        data.put("photoBase64", base64);
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSaved())
                .addOnFailureListener(e -> callback.onError());
    }

    private static Bitmap scaleDown(Bitmap source, int maxDimension) {
        int width = source.getWidth();
        int height = source.getHeight();
        float scale = Math.min(1f, (float) maxDimension / Math.max(width, height));
        if (scale >= 1f) return source;
        return Bitmap.createScaledBitmap(source, Math.round(width * scale), Math.round(height * scale), true);
    }
}
