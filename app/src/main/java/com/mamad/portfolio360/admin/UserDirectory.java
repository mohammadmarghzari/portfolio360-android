package com.mamad.portfolio360.admin;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * نگاشت ایمیل به uid برای هر کاربر، تا ادمین بتواند از داخل اپ (بدون Admin SDK)
 * با گرفتن ایمیل، حساب موردنظر را برای فعال‌سازی اشتراک پیدا کند.
 */
public class UserDirectory {

    /** هر بار که کاربر لاگین‌شده اپ را باز می‌کند، رکورد خودش را به‌روز نگه می‌دارد. */
    public static void syncCurrentUser() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getUid();
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (uid == null || email == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("email", email.toLowerCase());
        FirebaseFirestore.getInstance().collection("users").document(uid).set(data);
    }
}
