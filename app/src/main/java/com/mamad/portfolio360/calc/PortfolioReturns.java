package com.mamad.portfolio360.calc;

import com.mamad.portfolio360.network.HistoricalPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * ابزار مشترک: داده تاریخی چند دارایی را بر اساس روز تقویمی هم‌تراز می‌کند
 * و بازده روزانه پرتفوی هم‌وزن را برمی‌گرداند. توسط CvarEngine و MonteCarloEngine
 * هر دو استفاده می‌شود تا منطق هم‌ترازسازی یک‌بار نوشته شود.
 */
public class PortfolioReturns {

    public static class Aligned {
        public double[] dailyReturns = new double[0];
        public int alignedDays;
    }

    public static Aligned computeEqualWeighted(Map<String, List<HistoricalPoint>> histories) {
        Aligned result = new Aligned();
        int n = histories.size();
        if (n == 0) return result;

        double weight = 1.0 / n;

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

        result.alignedDays = alignedDays.size();
        if (alignedDays.size() < 2) return result;

        double[] returns = new double[alignedDays.size() - 1];
        for (int i = 1; i < alignedDays.size(); i++) {
            Map<String, Double> prevPrices = byDay.get(alignedDays.get(i - 1));
            Map<String, Double> currPrices = byDay.get(alignedDays.get(i));

            double portRet = 0;
            for (String symbol : histories.keySet()) {
                double prev = prevPrices.get(symbol);
                double curr = currPrices.get(symbol);
                portRet += weight * (curr / prev - 1.0);
            }
            returns[i - 1] = portRet;
        }

        result.dailyReturns = returns;
        return result;
    }
}
