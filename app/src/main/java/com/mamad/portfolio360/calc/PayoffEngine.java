package com.mamad.portfolio360.calc;

import java.util.ArrayList;
import java.util.List;

/**
 * موتور عمومی محاسبه سود/زیان استراتژی‌های آپشن.
 *
 * هر استراتژی از چند «پایه» (Leg) ساخته می‌شود:
 *   - دارایی پایه (SPOT)
 *   - اختیار خرید (CALL)
 *   - اختیار فروش (PUT)
 * هر پایه یا خریداری شده (long، ضریب +۱) یا فروخته شده (short، ضریب −۱) است.
 *
 * سود/زیان کل در هر قیمت فرضی S، جمع سود/زیان همه پایه‌هاست.
 */
public class PayoffEngine {

    public enum LegType { SPOT, CALL, PUT }

    public static class Leg {
        public final LegType type;
        public final boolean isLong;      // true = خرید، false = فروش
        public final double strike;       // برای SPOT استفاده نمی‌شود
        public final double price;        // قیمت خرید دارایی، یا پرمیوم آپشن
        public final double quantity;

        public Leg(LegType type, boolean isLong, double strike, double price, double quantity) {
            this.type = type;
            this.isLong = isLong;
            this.strike = strike;
            this.price = price;
            this.quantity = quantity;
        }

        /** سود/زیان این پایه در قیمت سررسید S */
        public double payoffAt(double s) {
            double intrinsic;
            switch (type) {
                case SPOT:
                    intrinsic = s - price;      // ارزش فعلی منهای بهای تمام‌شده
                    break;
                case CALL:
                    intrinsic = Math.max(s - strike, 0) - price;
                    break;
                case PUT:
                    intrinsic = Math.max(strike - s, 0) - price;
                    break;
                default:
                    intrinsic = 0;
            }
            double sign = isLong ? 1 : -1;
            // در فروش، پرمیوم دریافت می‌شود؛ فرمول بالا با ضریب −۱ خودش این را اعمال می‌کند
            return sign * intrinsic * quantity;
        }
    }

    public static class Result {
        public double[] prices;            // محور افقی: قیمت فرضی دارایی
        public double[] payoffs;           // محور عمودی: سود/زیان
        public List<Double> breakevens = new ArrayList<>();
        public double maxProfit;           // اگر بی‌نهایت باشد: Double.POSITIVE_INFINITY
        public double maxLoss;             // اگر بی‌نهایت باشد: Double.NEGATIVE_INFINITY
        public double payoffAtSpot;        // سود/زیان در قیمت فعلی بازار
        public boolean profitUnlimited;
        public boolean lossUnlimited;
    }

    /**
     * منحنی سود/زیان را در بازه‌ای حول قیمت لحظه‌ای محاسبه می‌کند.
     *
     * @param legs  پایه‌های استراتژی
     * @param spot  قیمت لحظه‌ای فعلی دارایی
     * @param steps تعداد نقاط منحنی (هرچه بیشتر، صاف‌تر)
     */
    public static Result compute(List<Leg> legs, double spot, int steps) {
        Result res = new Result();

        // بازه نمایش: ۵۰٪ پایین‌تر تا ۵۰٪ بالاتر از قیمت لحظه‌ای،
        // اما مطمئن می‌شویم همه قیمت‌های اعمال داخل بازه باشند.
        double lo = spot * 0.5;
        double hi = spot * 1.5;
        for (Leg leg : legs) {
            if (leg.type == LegType.SPOT) continue;
            lo = Math.min(lo, leg.strike * 0.75);
            hi = Math.max(hi, leg.strike * 1.25);
        }
        if (lo < 0) lo = 0;

        res.prices = new double[steps];
        res.payoffs = new double[steps];

        double step = (hi - lo) / (steps - 1);
        double maxP = Double.NEGATIVE_INFINITY;
        double minP = Double.POSITIVE_INFINITY;

        for (int i = 0; i < steps; i++) {
            double s = lo + i * step;
            double p = totalPayoff(legs, s);
            res.prices[i] = s;
            res.payoffs[i] = p;
            if (p > maxP) maxP = p;
            if (p < minP) minP = p;
        }

        res.maxProfit = maxP;
        res.maxLoss = minP;
        res.payoffAtSpot = totalPayoff(legs, spot);

        // تشخیص بی‌نهایت بودن: شیب منحنی در دو انتها
        double slopeRight = totalPayoff(legs, hi) - totalPayoff(legs, hi * 0.98);
        double slopeLeft = totalPayoff(legs, lo * 1.02) - totalPayoff(legs, lo);
        res.profitUnlimited = slopeRight > 1e-6;
        res.lossUnlimited = slopeLeft > 1e-6;   // شیب مثبت به سمت راست یعنی هرچه پایین‌تر، زیان بیشتر

        // نقاط سر به سر: جایی که منحنی از صفر عبور می‌کند
        for (int i = 1; i < steps; i++) {
            double a = res.payoffs[i - 1];
            double b = res.payoffs[i];
            if ((a < 0 && b >= 0) || (a > 0 && b <= 0)) {
                // درون‌یابی خطی
                double x1 = res.prices[i - 1];
                double x2 = res.prices[i];
                double be = x1 + (x2 - x1) * (0 - a) / (b - a);
                res.breakevens.add(be);
            }
        }

        return res;
    }

    public static double totalPayoff(List<Leg> legs, double s) {
        double sum = 0;
        for (Leg leg : legs) sum += leg.payoffAt(s);
        return sum;
    }

    // ---------- سازنده‌های استراتژی ----------

    /** کاوردکال: نگهداری دارایی + فروش کال */
    public static List<Leg> coveredCall(double costBasis, double strike, double premium, double qty) {
        List<Leg> legs = new ArrayList<>();
        legs.add(new Leg(LegType.SPOT, true, 0, costBasis, qty));
        legs.add(new Leg(LegType.CALL, false, strike, premium, qty));
        return legs;
    }

    /** پروتکتیو پوت: نگهداری دارایی + خرید پوت */
    public static List<Leg> protectivePut(double costBasis, double strike, double premium, double qty) {
        List<Leg> legs = new ArrayList<>();
        legs.add(new Leg(LegType.SPOT, true, 0, costBasis, qty));
        legs.add(new Leg(LegType.PUT, true, strike, premium, qty));
        return legs;
    }

    /** خرید کال ساده */
    public static List<Leg> longCall(double strike, double premium, double qty) {
        List<Leg> legs = new ArrayList<>();
        legs.add(new Leg(LegType.CALL, true, strike, premium, qty));
        return legs;
    }

    /** خرید پوت ساده */
    public static List<Leg> longPut(double strike, double premium, double qty) {
        List<Leg> legs = new ArrayList<>();
        legs.add(new Leg(LegType.PUT, true, strike, premium, qty));
        return legs;
    }

    /** فقط نگهداری دارایی — برای مقایسه «بدون استراتژی» */
    public static List<Leg> spotOnly(double costBasis, double qty) {
        List<Leg> legs = new ArrayList<>();
        legs.add(new Leg(LegType.SPOT, true, 0, costBasis, qty));
        return legs;
    }
}
