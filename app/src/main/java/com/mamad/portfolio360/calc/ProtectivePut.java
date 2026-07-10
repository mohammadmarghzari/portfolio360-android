package com.mamad.portfolio360.calc;

/**
 * محاسبات استراتژی پروتکتیو پوت (Protective Put):
 * دارنده دارایی پایه، یک اختیار فروش (Put) خریداری می‌کند تا در برابر افت شدید قیمت
 * بیمه شود. برخلاف کاوردکال، سقف سود محدود نمی‌شود، فقط هزینه بیمه (پرمیوم) پرداخت می‌شود.
 *
 * ورودی‌ها:
 *   costBasis = قیمت خرید دارایی پایه (میانگین بهای تمام‌شده)
 *   strike    = قیمت اعمال پوت خریداری‌شده (کف محافظت)
 *   premium   = پرمیوم پرداختی برای خرید پوت (به ازای هر واحد دارایی)
 *   days      = روز باقی‌مانده تا سررسید
 */
public class ProtectivePut {

    public static class Result {
        public double breakeven;             // نقطه سر به سر
        public double maxLossPerShare;        // حداکثر زیان تضمینی (کف بیمه‌شده)
        public double protectionLevelPct;     // درصد از ارزش دارایی که محافظت می‌شود
        public double insuranceCostPct;       // هزینه بیمه به درصد بهای تمام‌شده
        public double annualizedInsuranceCostPct; // هزینه بیمه سالانه‌شده
    }

    public static Result compute(double costBasis, double strike, double premium, double days) {
        Result res = new Result();

        res.breakeven = costBasis + premium;

        res.maxLossPerShare = (costBasis - strike) + premium;

        res.protectionLevelPct = (strike / costBasis) * 100.0;

        res.insuranceCostPct = (premium / costBasis) * 100.0;

        if (days > 0) {
            res.annualizedInsuranceCostPct = res.insuranceCostPct * (365.0 / days);
        } else {
            res.annualizedInsuranceCostPct = 0;
        }

        return res;
    }
}
