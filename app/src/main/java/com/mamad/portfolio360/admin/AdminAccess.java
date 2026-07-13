package com.mamad.portfolio360.admin;

import com.google.firebase.auth.FirebaseAuth;

/** تشخیص اینکه آیا کاربر لاگین‌شده‌ی فعلی، ادمین (مالک اپ) است یا نه. */
public class AdminAccess {

    private static final String ADMIN_EMAIL = "mohammadmarghzari1@gmail.com";

    public static boolean isAdmin() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return false;
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        return email != null && email.equalsIgnoreCase(ADMIN_EMAIL);
    }
}
