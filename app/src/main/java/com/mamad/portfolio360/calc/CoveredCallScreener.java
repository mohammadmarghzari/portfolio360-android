package com.mamad.portfolio360.calc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * «بهینه‌یاب کاوردکال»: در یک بازه‌ی زمانی و بازده هدفی که کاربر مشخص می‌کند،
 * روی شبکه‌ای از قراردادهای کال خارج‌از‌پول (با سررسیدها و قیمت‌های اعمال مختلف)
 * می‌گردد و بهترین قرارداد را برای فروش کاوردکال پیدا می‌کند — بر اساس فرمول
 * بلک-شولز، بدون نیاز به داده‌ی زنده.
 *
 * منطق انتخاب:
 *   - از بین قراردادهایی که «بازده سالانه‌شده‌ی پرمیوم» آن‌ها به هدف کاربر می‌رسد،
 *     امن‌ترین را برمی‌گزیند: کمترین احتمال اعمال (بیشترین فاصله تا قیمت فعلی)،
 *     چون این یعنی رسیدن به درآمد هدف با کمترین ریسک از‌دست‌رفتن دارایی و کمترین
 *     محدود شدن سود صعودی.
 *   - اگر هیچ قراردادی به هدف نرسد، بالاترین بازده ممکن را معرفی می‌کند و اعلام
 *     می‌کند که هدف با نوسان فعلی دست‌یافتنی نیست.
 */
public class CoveredCallScreener {

    private static final int[] EXPIRY_GRID = {30, 45, 60, 75, 90};
    private static final int STRIKE_STEPS = 10;      // ۲.۵٪ تا ۲۵٪ بالای قیمت
    private static final double STRIKE_STEP_PCT = 0.025;

    public static class Candidate {
        public double strike;
        public int days;
        public double premium;
        public double staticAnnualPct;    // بازده سالانه‌شده صرفاً از پرمیوم
        public double ifCalledAnnualPct;  // بازده سالانه‌شده در صورت اعمال
        public double assignmentProbPct;  // احتمال تقریبی اعمال ≈ دلتا
        public double thetaPerDay;
        public double upsideBufferPct;    // فاصله‌ی استرایک تا قیمت فعلی
    }

    public static class Result {
        public Candidate best;
        public List<Candidate> topForChart = new ArrayList<>();
        public boolean targetMet;
        public boolean hasAny;
        public double spot, sigma, r;
    }

    public static Result screen(double spot, double volPct, double riskFreePct,
                                 int maxDays, double targetAnnualPct) {
        double sigma = volPct / 100.0;
        double r = riskFreePct / 100.0;

        List<Integer> expiries = new ArrayList<>();
        for (int d : EXPIRY_GRID) if (d <= maxDays) expiries.add(d);
        if (expiries.isEmpty()) expiries.add(Math.max(7, maxDays));

        List<Candidate> all = new ArrayList<>();
        for (int e : expiries) {
            double T = e / 365.0;
            for (int i = 1; i <= STRIKE_STEPS; i++) {
                double buffer = i * STRIKE_STEP_PCT;
                double K = spot * (1 + buffer);
                BlackScholes.Result bs = BlackScholes.compute(spot, K, T, r, sigma);
                if (bs.callPrice <= 0) continue;

                Candidate c = new Candidate();
                c.strike = K;
                c.days = e;
                c.premium = bs.callPrice;
                c.staticAnnualPct = (bs.callPrice / spot) * (365.0 / e) * 100.0;
                double ifCalledProfit = (K - spot) + bs.callPrice;
                c.ifCalledAnnualPct = (ifCalledProfit / spot) * (365.0 / e) * 100.0;
                c.assignmentProbPct = bs.delta * 100.0;
                c.thetaPerDay = bs.theta;
                c.upsideBufferPct = buffer * 100.0;
                all.add(c);
            }
        }

        Result result = new Result();
        result.spot = spot;
        result.sigma = sigma;
        result.r = r;
        result.hasAny = !all.isEmpty();
        if (all.isEmpty()) return result;

        List<Candidate> meeting = new ArrayList<>();
        for (Candidate c : all) if (c.staticAnnualPct >= targetAnnualPct) meeting.add(c);

        if (!meeting.isEmpty()) {
            result.targetMet = true;
            Collections.sort(meeting, (a, b) -> Double.compare(a.assignmentProbPct, b.assignmentProbPct));
            result.best = meeting.get(0);
            result.topForChart = pickForChart(meeting, result.best, true);
        } else {
            result.targetMet = false;
            Collections.sort(all, (a, b) -> Double.compare(b.staticAnnualPct, a.staticAnnualPct));
            result.best = all.get(0);
            result.topForChart = pickForChart(all, result.best, false);
        }

        return result;
    }

    /** یک نماینده به ازای هر سررسید انتخاب می‌کند تا نمودار مقایسه‌ی سررسیدها معنادار باشد. */
    private static List<Candidate> pickForChart(List<Candidate> pool, Candidate best, boolean byAssignment) {
        Map<Integer, Candidate> perExpiry = new LinkedHashMap<>();
        for (Candidate c : pool) {
            Candidate cur = perExpiry.get(c.days);
            if (cur == null) {
                perExpiry.put(c.days, c);
                continue;
            }
            boolean better = byAssignment
                    ? c.assignmentProbPct < cur.assignmentProbPct
                    : c.staticAnnualPct > cur.staticAnnualPct;
            if (better) perExpiry.put(c.days, c);
        }
        List<Candidate> chart = new ArrayList<>(perExpiry.values());
        if (!chart.contains(best)) chart.add(0, best);
        while (chart.size() > 5) chart.remove(chart.size() - 1);
        return chart;
    }

    /** ارزش پرمیوم قرارداد وقتی «daysRemaining» روز تا سررسید مانده (قیمت پایه ثابت). */
    public static double valueAtDaysRemaining(double spot, double strike, double daysRemaining,
                                              double r, double sigma) {
        if (daysRemaining <= 0) return Math.max(spot - strike, 0);
        double T = daysRemaining / 365.0;
        return BlackScholes.compute(spot, strike, T, r, sigma).callPrice;
    }
}
