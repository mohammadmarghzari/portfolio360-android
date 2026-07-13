package com.mamad.portfolio360.calc;

import com.mamad.portfolio360.network.HistoricalPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * ابزار مشترک: داده تاریخی چند دارایی را بر اساس روز تقویمی هم‌تراز می‌کند.
 * توسط CvarEngine، MonteCarloEngine و PortfolioOptimizerEngine استفاده می‌شود
 * تا منطق هم‌ترازسازی یک‌بار نوشته شود.
 */
public class PortfolioReturns {

    public static class Aligned {
        public double[] dailyReturns = new double[0];
        public int alignedDays;
    }

    /** بازده روزانه هر دارایی به‌طور جداگانه، بر اساس روزهای هم‌تراز مشترک. */
    public static class PerSymbolAligned {
        public String[] symbols = new String[0];
        public double[][] returns = new double[0][]; // [assetIndex][dayIndex]
        public int alignedDays;
    }

    public static PerSymbolAligned alignPerSymbol(Map<String, List<HistoricalPoint>> histories) {
        PerSymbolAligned result = new PerSymbolAligned();
        int n = histories.size();
        if (n == 0) return result;

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

        result.symbols = histories.keySet().toArray(new String[0]);
        result.returns = new double[result.symbols.length][alignedDays.size() - 1];

        for (int a = 0; a < result.symbols.length; a++) {
            String symbol = result.symbols[a];
            for (int i = 1; i < alignedDays.size(); i++) {
                double prev = byDay.get(alignedDays.get(i - 1)).get(symbol);
                double curr = byDay.get(alignedDays.get(i)).get(symbol);
                result.returns[a][i - 1] = curr / prev - 1.0;
            }
        }

        return result;
    }

    public static Aligned computeEqualWeighted(Map<String, List<HistoricalPoint>> histories) {
        Aligned result = new Aligned();

        PerSymbolAligned perSymbol = alignPerSymbol(histories);
        result.alignedDays = perSymbol.alignedDays;
        if (perSymbol.symbols.length == 0) return result;

        double weight = 1.0 / perSymbol.symbols.length;
        int days = perSymbol.returns[0].length;
        double[] combined = new double[days];

        for (double[] assetReturns : perSymbol.returns) {
            for (int d = 0; d < days; d++) combined[d] += weight * assetReturns[d];
        }

        result.dailyReturns = combined;
        return result;
    }
}
