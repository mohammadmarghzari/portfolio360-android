package com.mamad.portfolio360.calc;

/**
 * پیاده‌سازی خالص جاوا فرمول بلک-شولز و یونانی‌ها (Greeks).
 * معادل منطق مدل بلک-شولز نسخه پایتونی Portfolio360 / Option Desk.
 * ورودی‌ها:
 *   S  = قیمت لحظه‌ای دارایی پایه
 *   K  = قیمت اعمال (Strike)
 *   T  = زمان تا سررسید بر حسب سال (مثلاً ۳۰ روز = 30/365)
 *   r  = نرخ بهره بدون ریسک (به‌صورت اعشاری، مثلاً 0.05 برای ۵٪)
 *   sigma = نوسان ضمنی سالانه (به‌صورت اعشاری، مثلاً 0.6 برای ۶۰٪)
 */
public class BlackScholes {

    public static class Result {
        public double callPrice;
        public double putPrice;
        public double delta;   // برای کال؛ دلتای پوت = delta - 1
        public double gamma;
        public double theta;   // به ازای هر روز (کال)
        public double vega;    // به ازای ۱٪ تغییر نوسان
        public double rho;     // به ازای ۱٪ تغییر نرخ بهره
    }

    /** تابع چگالی احتمال نرمال استاندارد (PDF) */
    private static double pdf(double x) {
        return Math.exp(-0.5 * x * x) / Math.sqrt(2 * Math.PI);
    }

    /** تابع توزیع تجمعی نرمال استاندارد (CDF) با تقریب Abramowitz-Stegun */
    private static double cdf(double x) {
        return 0.5 * (1.0 + erf(x / Math.sqrt(2)));
    }

    private static double erf(double x) {
        // تقریب عددی با دقت بالا (Abramowitz & Stegun 7.1.26)
        double sign = x < 0 ? -1 : 1;
        x = Math.abs(x);

        double a1 = 0.254829592;
        double a2 = -0.284496736;
        double a3 = 1.421413741;
        double a4 = -1.453152027;
        double a5 = 1.061405429;
        double p = 0.3275911;

        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);

        return sign * y;
    }

    public static Result compute(double S, double K, double T, double r, double sigma) {
        Result res = new Result();

        if (T <= 0 || sigma <= 0 || S <= 0 || K <= 0) {
            // در سررسید یا ورودی نامعتبر: مقادیر ذاتی برگردانده می‌شود
            res.callPrice = Math.max(S - K, 0);
            res.putPrice = Math.max(K - S, 0);
            return res;
        }

        double sqrtT = Math.sqrt(T);
        double d1 = (Math.log(S / K) + (r + 0.5 * sigma * sigma) * T) / (sigma * sqrtT);
        double d2 = d1 - sigma * sqrtT;

        double Nd1 = cdf(d1);
        double Nd2 = cdf(d2);
        double NmD1 = cdf(-d1);
        double NmD2 = cdf(-d2);

        res.callPrice = S * Nd1 - K * Math.exp(-r * T) * Nd2;
        res.putPrice = K * Math.exp(-r * T) * NmD2 - S * NmD1;

        res.delta = Nd1;
        res.gamma = pdf(d1) / (S * sigma * sqrtT);

        // تتا به ازای هر روز (تقسیم بر ۳۶۵)
        double theta = (-(S * pdf(d1) * sigma) / (2 * sqrtT)) - r * K * Math.exp(-r * T) * Nd2;
        res.theta = theta / 365.0;

        // وگا به ازای ۱٪ تغییر نوسان
        res.vega = S * pdf(d1) * sqrtT / 100.0;

        // رو به ازای ۱٪ تغییر نرخ بهره (کال)
        res.rho = K * T * Math.exp(-r * T) * Nd2 / 100.0;

        return res;
    }
}
