package com.mamad.portfolio360.network;

/**
 * نمایانگر یک قرارداد آپشن واقعی دریافت‌شده از API عمومی Derive.xyz.
 */
public class DeriveInstrument {
    public final String instrumentName;
    public final double strike;
    public final long expirationTimestampMillis;
    public final String optionType; // "call" یا "put"

    public DeriveInstrument(String instrumentName, double strike,
                             long expirationTimestampMillis, String optionType) {
        this.instrumentName = instrumentName;
        this.strike = strike;
        this.expirationTimestampMillis = expirationTimestampMillis;
        this.optionType = optionType;
    }
}
