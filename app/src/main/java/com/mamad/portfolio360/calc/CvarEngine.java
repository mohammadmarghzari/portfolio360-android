package com.mamad.portfolio360.calc;

import com.mamad.portfolio360.network.HistoricalPoint;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * محاسبه VaR و CVaR تاریخی (Historical Simulation) برای یک پرتفوی
 * هم‌وزن از چند دارایی، بر اساس داده قیمت واقعی.
 *
 * روش: بازده روزانه هر دارایی → ترکیب هم‌وزن → مرتب‌سازی صعودی →
 * VaR = بازده در صدک ۵ام (بدترین ۵٪) → CVaR = میانگین همان دنباله بدترین ۵٪.
 */
public class CvarEngine {

    public static class Result {
        public int alignedDays;
        public double meanDailyReturnPct;
        public double annualizedReturnPct;
        public double annualizedVolPct;
        public double var95Pct;
        public double cvar95Pct;
        public double[] dailyReturns = new double[0]; // برای استفاده مجدد توسط مونت‌کارلو
        public Map<String, Double> weights = new HashMap<>();
    }

    public static Result computeEqualWeighted(Map<String, List<HistoricalPoint>> histories) {
        Result res = new Result();

        int n = histories.size();
        if (n == 0) return res;

        double weight = 1.0 / n;
        for (String symbol : histories.keySet()) res.weights.put(symbol, weight);

        PortfolioReturns.Aligned aligned = PortfolioReturns.computeEqualWeighted(histories);
        res.alignedDays = aligned.alignedDays;
        res.dailyReturns = aligned.dailyReturns;

        double[] portfolioReturns = aligned.dailyReturns;
        if (portfolioReturns.length < 1) return res;

        double mean = 0;
        for (double r : portfolioReturns) mean += r;
        mean /= portfolioReturns.length;

        double variance = 0;
        for (double r : portfolioReturns) variance += Math.pow(r - mean, 2);
        variance /= portfolioReturns.length;
        double stdDev = Math.sqrt(variance);

        res.meanDailyReturnPct = mean * 100.0;
        res.annualizedReturnPct = (Math.pow(1 + mean, 252) - 1) * 100.0;
        res.annualizedVolPct = stdDev * Math.sqrt(252) * 100.0;

        double[] sorted = portfolioReturns.clone();
        Arrays.sort(sorted);

        int varIndex = (int) Math.floor(0.05 * sorted.length);
        if (varIndex >= sorted.length) varIndex = sorted.length - 1;

        res.var95Pct = sorted[varIndex] * 100.0;

        double tailSum = 0;
        for (int i = 0; i <= varIndex; i++) tailSum += sorted[i];
        res.cvar95Pct = (tailSum / (varIndex + 1)) * 100.0;

        return res;
    }
}
