package com.mamad.portfolio360.calc;

import com.mamad.portfolio360.network.HistoricalPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * محاسبه VaR و CVaR تاریخی (Historical Simulation) برای یک پرتفوی
 * هم‌وزن از چند دارایی، بر اساس داده قیمت واقعی.
 *
 * روش: بازده روزانه هر دارایی → ترکیب هم‌وزن → مرتب‌سازی صعودی →
 * VaR = بازده در صدک ۵ام (بدترین ۵٪) → CVaR = میانگین همان دنباله بدترین ۵٪.
 */
public class CvarEngine {

    public static class Result {
        public int alignedDays;           // تعداد روزهای هم‌تراز بین همه دارایی‌ها
        public double meanDailyReturnPct;
        public double annualizedReturnPct;
        public double annualizedVolPct;
        public double var95Pct;           // بازده در صدک ۵ (منفی = زیان)
        public double cvar95Pct;          // میانگین بدترین ۵٪ (همیشه ≤ VaR)
        public Map<String, Double> weights = new HashMap<>();
    }

    /**
     * @param histories نگاشت نماد → لیست داده تاریخی آن (هر لیست باید صعودی بر اساس زمان باشد)
     */
    public static Result computeEqualWeighted(Map<String, List<HistoricalPoint>> histories) {
        Result res = new Result();

        int n = histories.size();
        if (n == 0) return res;

        double weight = 1.0 / n;
        for (String symbol : histories.keySet()) res.weights.put(symbol, weight);

        // ۱) هم‌ترازسازی بر اساس روز تقویمی (epoch روز = epochSeconds / 86400)
        TreeMap<Long, Map<String, Double>> byDay = new TreeMap<>();
        for (Map.Entry<String, List<HistoricalPoint>> e : histories.entrySet()) {
            for (HistoricalPoint p : e.getValue()) {
                long dayKey = p.epochSeconds / 86400L;
                byDay.computeIfAbsent(dayKey, k -> new HashMap<>()).put(e.getKey(), p.close);
            }
        }

        List<Long> alignedDays = new ArrayList<>();
        for (Map.Entry<Long, Map<String, Double>> e : byDay.entrySet()) {
            if (e.getValue().keySet().containsAll(histories.keySet())) {
                alignedDays.add(e.getKey());
            }
        }

        if (alignedDays.size() < 2) {
            res.alignedDays = alignedDays.size();
            return res;
        }

        // ۲) بازده روزانه هم‌وزن پرتفوی
        double[] portfolioReturns = new double[alignedDays.size() - 1];
        for (int i = 1; i < alignedDays.size(); i++) {
            Map<String, Double> prevPrices = byDay.get(alignedDays.get(i - 1));
            Map<String, Double> currPrices = byDay.get(alignedDays.get(i));

            double portRet = 0;
            for (String symbol : histories.keySet()) {
                double prev = prevPrices.get(symbol);
                double curr = currPrices.get(symbol);
                portRet += weight * (curr / prev - 1.0);
            }
            portfolioReturns[i - 1] = portRet;
        }

        res.alignedDays = alignedDays.size();

        // ۳) میانگین و نوسان
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

        // ۴) VaR/CVaR تاریخی در سطح اطمینان ۹۵٪
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
