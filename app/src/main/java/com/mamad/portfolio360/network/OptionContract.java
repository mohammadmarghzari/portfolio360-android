package com.mamad.portfolio360.network;

/**
 * یک قرارداد آپشن واقعی از Derive.xyz.
 * ساختار بر اساس خروجی واقعی get_instruments:
 *   instrument_name: "ETH-20260713-1675-C"
 *   option_details: { expiry: 1783929600 (ثانیه), strike: "1675" (رشته), option_type: "C"|"P" }
 */
public class OptionContract {

    public final String instrumentName;
    public final double strike;
    public final long expirySeconds;
    public final boolean isCall;

    // گریک‌ها و قیمت — بعداً از get_ticker پر می‌شوند (تا آن زمان NaN)
    public double markPrice = Double.NaN;
    public double delta = Double.NaN;
    public double gamma = Double.NaN;
    public double vega = Double.NaN;
    public double theta = Double.NaN;
    public double iv = Double.NaN;

    public OptionContract(String instrumentName, double strike, long expirySeconds, boolean isCall) {
        this.instrumentName = instrumentName;
        this.strike = strike;
        this.expirySeconds = expirySeconds;
        this.isCall = isCall;
    }

    public boolean hasGreeks() {
        return !Double.isNaN(delta);
    }
}
