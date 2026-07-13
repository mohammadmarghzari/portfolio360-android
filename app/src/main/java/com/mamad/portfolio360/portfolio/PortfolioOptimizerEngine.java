package com.mamad.portfolio360.portfolio;

import com.mamad.portfolio360.calc.MonteCarloEngine;
import com.mamad.portfolio360.calc.PortfolioReturns;

import java.util.Random;

/**
 * پیشنهاد وزن پرتفوی با جست‌وجوی مونت‌کارلو روی هزاران ترکیب وزنی تصادفی
 * (نمونه‌گیری یکنواخت روی سیمپلکس). برای هر ترکیب، بازده/نوسان سالانه‌شده و
 * نسبت شارپ (با نرخ بدون ریسک ورودی کاربر) محاسبه می‌شود؛ در پایان، بر اساس
 * سبک انتخابی کاربر (محافظه‌کار → کمترین نوسان، متعادل → بیشترین شارپ،
 * تهاجمی → بیشترین بازده) و در صورت وجود بازده هدف، بهترین ترکیب انتخاب می‌شود.
 */
public class PortfolioOptimizerEngine {

    public static final String STYLE_CONSERVATIVE = "conservative";
    public static final String STYLE_BALANCED = "balanced";
    public static final String STYLE_AGGRESSIVE = "aggressive";

    public static class Result {
        public String[] symbols = new String[0];
        public double[] weights = new double[0]; // جمع = ۱

        public double annualizedReturnPct;
        public double annualizedVolPct;
        public double sharpe;

        public double maxDrawdownPct;
        public boolean recovered;
        public int recoveryDays = -1; // اگر recovered=false، یعنی روزهای سپری‌شده بدون بازگشت به اوج قبلی

        public double medianForwardReturnPct;
        public double p5ForwardReturnPct;
        public double p95ForwardReturnPct;

        public double bestPossibleSharpe;
        public double bestPossibleMedianReturnPct;

        public boolean targetAchieved = true; // false یعنی هیچ ترکیبی به بازده هدف کاربر نرسید
        public int alignedDays;

        // نمونه‌ای از ترکیب‌های بررسی‌شده (حداکثر ~۴۰۰ نقطه) برای رسم نقشه ریسک/بازده
        public double[] cloudReturnPct = new double[0];
        public double[] cloudVolPct = new double[0];
    }

    public static Result optimize(PortfolioReturns.PerSymbolAligned aligned,
                                   double riskFreeRateAnnualPct,
                                   double targetReturnAnnualPct,
                                   String style,
                                   int numSimulations,
                                   long seed) {
        Result res = new Result();
        res.symbols = aligned.symbols;
        res.alignedDays = aligned.alignedDays;

        int nAssets = aligned.symbols.length;
        if (nAssets == 0 || aligned.returns[0].length < 30) return res;

        int days = aligned.returns[0].length;
        boolean hasTarget = targetReturnAnnualPct > 0;

        double[] bestWeights = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        double bestPossibleSharpeFound = Double.NEGATIVE_INFINITY;
        double bestReturnFound = Double.NEGATIVE_INFINITY;
        double[] maxReturnWeights = null;

        int cloudEvery = Math.max(1, numSimulations / 400);
        java.util.List<Double> cloudReturn = new java.util.ArrayList<>();
        java.util.List<Double> cloudVol = new java.util.ArrayList<>();

        Random rnd = new Random(seed);
        for (int s = 0; s < numSimulations; s++) {
            double[] w = randomSimplex(nAssets, rnd);
            double[] stats = portfolioAnnualStats(w, aligned.returns, days);
            double annReturnPct = stats[0];
            double annVolPct = stats[1];
            double sharpe = annVolPct > 1e-9 ? (annReturnPct - riskFreeRateAnnualPct) / annVolPct : 0;

            if (s % cloudEvery == 0) {
                cloudReturn.add(annReturnPct);
                cloudVol.add(annVolPct);
            }

            if (sharpe > bestPossibleSharpeFound) bestPossibleSharpeFound = sharpe;
            if (annReturnPct > bestReturnFound) {
                bestReturnFound = annReturnPct;
                maxReturnWeights = w;
            }

            if (hasTarget && annReturnPct < targetReturnAnnualPct) continue;

            double score = styleScore(style, annReturnPct, annVolPct, sharpe);
            if (score > bestScore) {
                bestScore = score;
                bestWeights = w;
            }
        }

        res.cloudReturnPct = toArray(cloudReturn);
        res.cloudVolPct = toArray(cloudVol);

        // اگر هیچ ترکیبی به بازده هدف نرسید، بدون فیلتر هدف دوباره امتیازدهی کن
        if (bestWeights == null) {
            res.targetAchieved = !hasTarget; // اگر اصلاً هدفی وارد نشده بود، این حالت طبیعی است نه شکست
            rnd = new Random(seed);
            for (int s = 0; s < numSimulations; s++) {
                double[] w = randomSimplex(nAssets, rnd);
                double[] stats = portfolioAnnualStats(w, aligned.returns, days);
                double score = styleScore(style, stats[0], stats[1],
                        stats[1] > 1e-9 ? (stats[0] - riskFreeRateAnnualPct) / stats[1] : 0);
                if (score > bestScore) {
                    bestScore = score;
                    bestWeights = w;
                }
            }
        }

        if (bestWeights == null) return res;

        res.weights = bestWeights;
        double[] finalStats = portfolioAnnualStats(bestWeights, aligned.returns, days);
        res.annualizedReturnPct = finalStats[0];
        res.annualizedVolPct = finalStats[1];
        res.sharpe = res.annualizedVolPct > 1e-9
                ? (res.annualizedReturnPct - riskFreeRateAnnualPct) / res.annualizedVolPct : 0;
        res.bestPossibleSharpe = bestPossibleSharpeFound;

        double[] combinedDaily = combineDaily(bestWeights, aligned.returns, days);
        computeDrawdownAndRecovery(combinedDaily, res);

        MonteCarloEngine.Result mc = MonteCarloEngine.simulate(combinedDaily, 252, 3000, seed + 1);
        res.medianForwardReturnPct = mc.medianPct;
        res.p5ForwardReturnPct = mc.p5Pct;
        res.p95ForwardReturnPct = mc.p95Pct;

        if (maxReturnWeights != null) {
            double[] maxReturnDaily = combineDaily(maxReturnWeights, aligned.returns, days);
            MonteCarloEngine.Result mcBest = MonteCarloEngine.simulate(maxReturnDaily, 252, 3000, seed + 2);
            res.bestPossibleMedianReturnPct = mcBest.medianPct;
        }

        return res;
    }

    private static double styleScore(String style, double annReturnPct, double annVolPct, double sharpe) {
        if (STYLE_CONSERVATIVE.equals(style)) return -annVolPct;
        if (STYLE_AGGRESSIVE.equals(style)) return annReturnPct;
        return sharpe; // balanced (پیش‌فرض)
    }

    private static double[] combineDaily(double[] weights, double[][] returnsBySymbol, int days) {
        double[] combined = new double[days];
        for (int a = 0; a < weights.length; a++) {
            double w = weights[a];
            double[] assetReturns = returnsBySymbol[a];
            for (int d = 0; d < days; d++) combined[d] += w * assetReturns[d];
        }
        return combined;
    }

    /** {بازده سالانه‌شده٪, نوسان سالانه‌شده٪} برای یک ترکیب وزنی معین. */
    private static double[] portfolioAnnualStats(double[] weights, double[][] returnsBySymbol, int days) {
        double[] combined = combineDaily(weights, returnsBySymbol, days);

        double mean = 0;
        for (double r : combined) mean += r;
        mean /= days;

        double variance = 0;
        for (double r : combined) variance += Math.pow(r - mean, 2);
        variance /= days;
        double stdDev = Math.sqrt(variance);

        double annReturnPct = (Math.pow(1 + mean, 252) - 1) * 100.0;
        double annVolPct = stdDev * Math.sqrt(252) * 100.0;
        return new double[]{annReturnPct, annVolPct};
    }

    /** حداکثر افت (peak-to-trough) و تعداد روزهای بازگشت به اوج قبلی، روی منحنی ارزش خالص بک‌تست‌شده. */
    private static void computeDrawdownAndRecovery(double[] dailyReturns, Result res) {
        double[] nav = new double[dailyReturns.length + 1];
        nav[0] = 1.0;
        for (int i = 0; i < dailyReturns.length; i++) nav[i + 1] = nav[i] * (1 + dailyReturns[i]);

        double runningPeak = nav[0];
        double maxDD = 0;
        double peakAtTrough = nav[0];
        int troughIndex = -1;

        for (int i = 1; i < nav.length; i++) {
            if (nav[i] > runningPeak) runningPeak = nav[i];
            double drawdown = (nav[i] - runningPeak) / runningPeak;
            if (drawdown < maxDD) {
                maxDD = drawdown;
                troughIndex = i;
                peakAtTrough = runningPeak;
            }
        }

        res.maxDrawdownPct = maxDD * 100.0;
        if (troughIndex < 0) return;

        int recoverIndex = -1;
        for (int i = troughIndex + 1; i < nav.length; i++) {
            if (nav[i] >= peakAtTrough) {
                recoverIndex = i;
                break;
            }
        }

        if (recoverIndex >= 0) {
            res.recovered = true;
            res.recoveryDays = recoverIndex - troughIndex;
        } else {
            res.recovered = false;
            res.recoveryDays = nav.length - 1 - troughIndex;
        }
    }

    private static double[] toArray(java.util.List<Double> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = list.get(i);
        return arr;
    }

    /** بردار وزن تصادفی با توزیع یکنواخت روی سیمپلکس (جمع = ۱، همه غیرمنفی)، از طریق نمونه‌گیری نمایی نرمال‌شده. */
    private static double[] randomSimplex(int n, Random rnd) {
        double[] w = new double[n];
        double sum = 0;
        for (int i = 0; i < n; i++) {
            w[i] = -Math.log(1.0 - rnd.nextDouble());
            sum += w[i];
        }
        for (int i = 0; i < n; i++) w[i] /= sum;
        return w;
    }
}
