package com.mamad.portfolio360.network;

/** یک رویداد اقتصادی مهم آمریکا در تقویم (Economic Calendar). */
public class EconEvent {
    public String title;
    public String dateLabel;   // yyyy-MM-dd (تاریخ منبع)
    public String time;        // HH:mm
    public String impact;      // High
    public String actual;      // ممکن است خالی باشد (هنوز منتشر نشده)
    public String forecast;    // Consensus/Forecast
    public String previous;
}
