package com.mamad.portfolio360.calc;

import java.util.Locale;

/**
 * تولید نظر و استدلال درباره‌ی به‌صرفه بودن یک معامله‌ی آپشنی، با مقایسه‌ی
 * بازده/هزینه‌ی آن نسبت به بازده‌ی موردانتظار و نرخ بدون ریسکی که کاربر وارد
 * می‌کند. هدف، جایگزینی تصمیم کاربر نیست — فقط استدلال شفاف و قابل‌بررسی است.
 */
public class InvestmentVerdict {

    public enum Verdict { FAVORABLE, CAUTION, UNFAVORABLE }

    public static class Result {
        public final Verdict verdict;
        public final String reasoning;

        Result(Verdict verdict, String reasoning) {
            this.verdict = verdict;
            this.reasoning = reasoning;
        }
    }

    /** کاوردکال: مقایسه بازده سالانه‌شده (ایستا) با نرخ بدون ریسک، و بازده در صورت اعمال با انتظار کاربر. */
    public static Result forCoveredCall(double annualizedReturnPct, double ifCalledReturnPct,
                                         double expectedReturnPct, double riskFreeRatePct) {
        if (annualizedReturnPct < riskFreeRatePct) {
            return new Result(Verdict.UNFAVORABLE, String.format(Locale.US,
                    "بازده سالانه‌شده این کاوردکال (%.1f%%) از نرخ بدون ریسک (%.1f%%) کمتر است؛ یعنی بازده اضافه‌ای که در ازای پذیرش ریسک نگهداری دارایی و محدود شدن سقف سود می‌گیری، توجیه‌کننده نیست.",
                    annualizedReturnPct, riskFreeRatePct));
        }
        if (expectedReturnPct > ifCalledReturnPct) {
            return new Result(Verdict.CAUTION, String.format(Locale.US,
                    "بازده سالانه‌شده (%.1f%%) از نرخ بدون ریسک (%.1f%%) بهتر است، اما اگر واقعاً انتظار بازده %.1f%% داری، این استراتژی سقف سودت را روی %.1f%% (در صورت اعمال کال) محدود می‌کند — بخشی از رشد احتمالی دارایی را از دست می‌دهی.",
                    annualizedReturnPct, riskFreeRatePct, expectedReturnPct, ifCalledReturnPct));
        }
        return new Result(Verdict.FAVORABLE, String.format(Locale.US,
                "بازده سالانه‌شده (%.1f%%) هم از نرخ بدون ریسک (%.1f%%) بهتر است و هم با انتظار بازده‌ات (%.1f%%) همخوانی دارد؛ این کاوردکال نسبت به ورودی‌های تو به‌صرفه به‌نظر می‌رسد.",
                annualizedReturnPct, riskFreeRatePct, expectedReturnPct));
    }

    /** پروتکتیو پوت: مقایسه هزینه بیمه سالانه با بازده موردانتظار (سهمی که بیمه از بازده می‌بلعد). */
    public static Result forProtectivePut(double annualizedInsuranceCostPct, double protectionLevelPct,
                                           double expectedReturnPct, double riskFreeRatePct) {
        double costShareOfReturn = expectedReturnPct > 0 ? (annualizedInsuranceCostPct / expectedReturnPct) * 100.0 : Double.POSITIVE_INFINITY;

        if (costShareOfReturn > 50) {
            return new Result(Verdict.UNFAVORABLE, String.format(Locale.US,
                    "هزینه بیمه سالانه (%.1f%%) بخش بزرگی (~%.0f%%) از بازده موردانتظارت (%.1f%%) را می‌بلعد؛ این سطح از بیمه به‌لحاظ بازده به‌صرفه نیست، مگر اینکه نگرانی جدی از افت شدید قیمت داشته باشی.",
                    annualizedInsuranceCostPct, costShareOfReturn, expectedReturnPct));
        }
        if (costShareOfReturn > 25) {
            return new Result(Verdict.CAUTION, String.format(Locale.US,
                    "هزینه بیمه سالانه (%.1f%%) حدود %.0f%% از بازده موردانتظارت (%.1f%%) را می‌گیرد؛ اگر نگرانی مشخصی از افت قیمت داری قابل قبوله، وگرنه هزینه‌اش نسبتاً بالاست.",
                    annualizedInsuranceCostPct, costShareOfReturn, expectedReturnPct));
        }
        return new Result(Verdict.FAVORABLE, String.format(Locale.US,
                "هزینه بیمه سالانه (%.1f%%) نسبت به بازده موردانتظارت (%.1f%%) معقول است و در ازایش، ارزش دارایی تا سطح %.1f%% محافظت می‌شود.",
                annualizedInsuranceCostPct, expectedReturnPct, protectionLevelPct));
    }

    /**
     * استراتژی‌های خریدار پرمیوم خالص (مثل لانگ استرنگل/استرادل/لانگ کال): هزینه‌ی موقعیت
     * (سالانه‌شده به‌عنوان درصدی از قیمت دارایی) در برابر بازده موردانتظار و نرخ بدون ریسک.
     */
    public static Result forNetDebitStrategy(double annualizedCostPct, double requiredMovePct,
                                              double expectedReturnPct, double riskFreeRatePct) {
        double costShareOfReturn = expectedReturnPct > 0 ? (annualizedCostPct / expectedReturnPct) * 100.0 : Double.POSITIVE_INFINITY;

        if (costShareOfReturn > 70) {
            return new Result(Verdict.UNFAVORABLE, String.format(Locale.US,
                    "هزینه سالانه‌شده این موقعیت (%.1f%%) نسبت به بازده موردانتظارت (%.1f%%) بسیار بالاست و برای سودآوری باید قیمت حداقل %.1f%% حرکت کند؛ ریسک از دست دادن کل پرمیوم پرداختی زیاد است.",
                    annualizedCostPct, expectedReturnPct, requiredMovePct));
        }
        if (costShareOfReturn > 35) {
            return new Result(Verdict.CAUTION, String.format(Locale.US,
                    "این موقعیت برای سودآوری به حرکت حداقل %.1f%% قیمت نیاز دارد و هزینه‌اش (%.1f%% سالانه) بخش قابل‌توجهی از بازده موردانتظارت (%.1f%%) است — فقط اگر به نوسان شدید مطمئنی منطقی است.",
                    requiredMovePct, annualizedCostPct, expectedReturnPct));
        }
        return new Result(Verdict.FAVORABLE, String.format(Locale.US,
                "هزینه سالانه‌شده این موقعیت (%.1f%%) نسبت به بازده موردانتظارت (%.1f%%) معقول است و حرکت لازم برای سودآوری (%.1f%%) با دیدگاه تو سازگار به‌نظر می‌رسد.",
                annualizedCostPct, expectedReturnPct, requiredMovePct));
    }
}
