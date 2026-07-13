package com.mamad.portfolio360.calc;

import com.mamad.portfolio360.network.HistoricalPoint;

import java.util.List;

/**
 * محاسبات آماری پایه روی یک سری قیمت تاریخی — پیش‌نیاز CVaR و مونت‌کارلو
 * که در جلسات بعد روی همین متدها ساخته می‌شوند.
 */
public class ReturnStats {

    public static class Result {
        public int dataPoints;
        public double latestClose;
        public double totalReturnPct;      // بازده کل بازه
        public double annualizedReturnPct; // بازده سالانه‌شده
        public double annualizedVolPct;    // نوسان سالانه‌شده (انحراف معیار بازده روزانه)
        public double maxDrawdownPct;      // بیشترین افت از قله تا دره در بازه
    }

    /** بازده لگاریتمی روزانه را از سری قیمت‌ها محاسبه می‌کند. */
    public static double[] dailyLogReturns(List<HistoricalPoint> points) {
        double[] returns = new double[points.size() - 1];
        for (int i = 1; i < points.size(); i++) {
            double prev = points.get(i - 1).close;
            double curr = points.get(i).close;
            returns[i - 1] = Math.log(curr / prev);
        }
        return returns;
    }

    public static Result compute(List<HistoricalPoint> points) {
        Result res = new Result();
        res.dataPoints = points.size();

        if (points.size() < 2) return res;

        double first = points.get(0).close;
        double last = points.get(points.size() - 1).close;
        res.latestClose = last;
        res.totalReturnPct = (last / first - 1.0) * 100.0;

        double[] logReturns = dailyLogReturns(points);

        double mean = 0;
        for (double r : logReturns) mean += r;
        mean /= logReturns.length;

        double variance = 0;
        for (double r : logReturns) variance += Math.pow(r - mean, 2);
        variance /= logReturns.length;
        double dailyStdDev = Math.sqrt(variance);

        // تقریب معاملاتی: ۲۵۲ روز کاری در سال
        res.annualizedVolPct = dailyStdDev * Math.sqrt(252) * 100.0;
        res.annualizedReturnPct = (Math.exp(mean * 252) - 1.0) * 100.0;

        // حداکثر افت (Max Drawdown)
        double peak = points.get(0).close;
        double maxDD = 0;
        for (HistoricalPoint p : points) {
            if (p.close > peak) peak = p.close;
            double drawdown = (p.close - peak) / peak;
            if (drawdown < maxDD) maxDD = drawdown;
        }
        res.maxDrawdownPct = maxDD * 100.0;

        return res;
    }
}
