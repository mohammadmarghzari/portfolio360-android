package com.mamad.portfolio360;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * اپ همیشه با تم تیره‌ی حرفه‌ای (سبک صرافی) نمایش داده می‌شود، مستقل از تنظیم
 * روشن/تاریک گوشی — تا ظاهر یکدست و حرفه‌ای بماند.
 */
public class Portfolio360App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}
