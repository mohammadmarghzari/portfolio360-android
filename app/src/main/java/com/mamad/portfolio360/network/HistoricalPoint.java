package com.mamad.portfolio360.network;

/** یک نقطه داده تاریخی: تاریخ (epoch ثانیه) و قیمت بسته‌شدن. */
public class HistoricalPoint {
    public final long epochSeconds;
    public final double close;

    public HistoricalPoint(long epochSeconds, double close) {
        this.epochSeconds = epochSeconds;
        this.close = close;
    }
}
