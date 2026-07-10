package com.mamad.portfolio360.calc;

/**
 * محاسبات استراتژی کاوردکال (Covered Call):
 * دارنده دارایی پایه، یک اختیار خرید (Call) روی آن می‌فروشد و پرمیوم دریافت می‌کند.
 *
 * ورودی‌ها:
 *   costBasis = قیمت خرید دارایی پایه (میانگین بهای تمام‌شده)
 *   spot      = قیمت لحظه‌ای فعلی دارایی
 *   strike    = قیمت اعمال کال فروخته‌شده
 *   premium   = پرمیوم دریافتی از فروش کال (به ازای هر واحد دارایی)
 *   days      = روز باقی‌مانده تا سررسید
 */
public class CoveredCall {

    public static class Result {
        public double breakeven;           // نقطه سر به سر
        public double maxProfitPerShare;   // حداکثر سود در صورت اعمال
        public double staticReturnPct;     // بازده ایستا (اگر اعمال نشود) به درصد
        public double ifCalledReturnPct;   // بازده در صورت اعمال به درصد
        public double annualizedReturnPct; // بازده سالانه‌شده (بر مبنای بازده ایستا)
        public double downsideProtectionPct; // درصد محافظت در برابر افت قیمت
    }

    public static Result compute(double costBasis, double spot, double strike,
                                  double premium, double days) {
        Result res = new Result();

        res.breakeven = costBasis - premium;

        res.maxProfitPerShare = (strike - costBasis) + premium;

        res.staticReturnPct = (premium / costBasis) * 100.0;

        double ifCalledProfit = (strike - costBasis) + premium;
        res.ifCalledReturnPct = (ifCalledProfit / costBasis) * 100.0;

        if (days > 0) {
            res.annualizedReturnPct = res.staticReturnPct * (365.0 / days);
        } else {
            res.annualizedReturnPct = 0;
        }

        res.downsideProtectionPct = (premium / spot) * 100.0;

        return res;
    }
}
