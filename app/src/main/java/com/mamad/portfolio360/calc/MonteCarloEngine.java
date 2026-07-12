package com.mamad.portfolio360.calc;

import java.util.Arrays;
import java.util.Random;

/**
 * شبیه‌سازی مونت‌کارلو مسیر آینده پرتفوی، بر اساس میانگین و انحراف معیار
 * بازده روزانه واقعی (برآوردشده از داده تاریخی). هر مسیر با گام‌های روزانه
 * تصادفی نرمال (Box-Muller از طریق Random.nextGaussian) ساخته و مرکب می‌شود.
 */
public class MonteCarloEngine {

    public static class Result {
        public int simulations;
        public int horizonDays;
        public double p5Pct;      // بدترین ۵٪ مسیرها
        public double p25Pct;
        public double medianPct;
        public double p75Pct;
        public double p95Pct;     // بهترین ۵٪ مسیرها
        public double probabilityOfLossPct;
    }

    public static Result simulate(double[] dailyReturns, int horizonDays, int numSimulations, long seed) {
        Result res = new Result();
        res.simulations = numSimulations;
        res.horizonDays = horizonDays;

        if (dailyReturns.length < 5) return res; // داده ناکافی

        double mean = 0;
        for (double r : dailyReturns) mean += r;
        mean /= dailyReturns.length;

        double variance = 0;
        for (double r : dailyReturns) variance += Math.pow(r - mean, 2);
        variance /= dailyReturns.length;
        double stdDev = Math.sqrt(variance);

        Random rnd = new Random(seed);
        double[] finalReturns = new double[numSimulations];

        for (int s = 0; s < numSimulations; s++) {
            double cumulative = 1.0;
            for (int d = 0; d < horizonDays; d++) {
                double dailyReturn = mean + stdDev * rnd.nextGaussian();
                cumulative *= (1 + dailyReturn);
            }
            finalReturns[s] = cumulative - 1.0;
        }

        Arrays.sort(finalReturns);

        res.p5Pct = percentile(finalReturns, 0.05) * 100.0;
        res.p25Pct = percentile(finalReturns, 0.25) * 100.0;
        res.medianPct = percentile(finalReturns, 0.50) * 100.0;
        res.p75Pct = percentile(finalReturns, 0.75) * 100.0;
        res.p95Pct = percentile(finalReturns, 0.95) * 100.0;

        int lossCount = 0;
        for (double r : finalReturns) if (r < 0) lossCount++;
        res.probabilityOfLossPct = (lossCount * 100.0) / numSimulations;

        return res;
    }

    private static double percentile(double[] sortedAscending, double p) {
        int idx = (int) (p * sortedAscending.length);
        if (idx >= sortedAscending.length) idx = sortedAscending.length - 1;
        if (idx < 0) idx = 0;
        return sortedAscending[idx];
    }
}
