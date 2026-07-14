package com.mamad.portfolio360.calc;

import com.mamad.portfolio360.network.EconEvent;

import java.util.List;

/**
 * تحلیل خودکار و ساده‌ی جهت‌گیری داده‌های اقتصادی منتشرشده: با مقایسه‌ی «مقدار
 * واقعی» با «انتظار بازار» برای هر رویداد مهم، یک جمع‌بندی می‌دهد که فضای کلی
 * انقباضی (Hawkish) است یا انبساطی (Dovish) — و این برای طلا/بیت‌کوین/سهام
 * چه معنایی دارد. این تحلیل صرفاً آموزشی است، نه توصیه‌ی سرمایه‌گذاری.
 */
public class MacroSentiment {

    public enum Lean { HAWKISH, DOVISH, MIXED, NO_DATA }

    public static class Result {
        public final Lean lean;
        public final String reasoning;

        Result(Lean lean, String reasoning) {
            this.lean = lean;
            this.reasoning = reasoning;
        }
    }

    public static Result analyze(List<EconEvent> events) {
        int score = 0;      // مثبت = انقباضی، منفی = انبساطی
        int released = 0;

        for (EconEvent e : events) {
            Double actual = num(e.actual);
            Double forecast = num(e.forecast);
            if (actual == null || forecast == null) continue;
            released++;
            score += direction(e.title, actual, forecast);
        }

        if (released == 0) {
            return new Result(Lean.NO_DATA,
                    "هنوز داده‌ی مهمی از رویدادهای این هفته منتشر نشده. به‌محض انتشار مقادیر واقعی، تحلیل خودکار همین‌جا ظاهر می‌شود.");
        }

        Lean lean = score >= 2 ? Lean.HAWKISH : score <= -2 ? Lean.DOVISH : Lean.MIXED;
        return new Result(lean, buildReasoning(lean, released));
    }

    /** جهت هر رویداد: +۱ انقباضی، −۱ انبساطی، ۰ خنثی. */
    private static int direction(String title, double actual, double forecast) {
        String t = title.toLowerCase();
        int cmp = Double.compare(actual, forecast);
        if (cmp == 0) return 0;
        boolean higher = cmp > 0;

        // نرخ بیکاری: بالاتر از انتظار = ضعف اقتصاد = انبساطی
        if (t.contains("unemployment")) return higher ? -1 : 1;

        // تورم: بالاتر از انتظار = انقباضی
        if (t.contains("inflation") || t.contains("cpi") || t.contains("ppi") || t.contains("pce")) {
            return higher ? 1 : -1;
        }

        // نرخ بهره فدرال رزرو: بالاتر = انقباضی
        if (t.contains("interest rate")) return higher ? 1 : -1;

        // اشتغال و رشد اقتصادی: قوی‌تر از انتظار = اقتصاد داغ = انقباضی/تقویت دلار
        if (t.contains("payroll") || t.contains("employment") || t.contains("jolts")
                || t.contains("gdp") || t.contains("retail") || t.contains("pmi")
                || t.contains("durable") || t.contains("spending") || t.contains("income")
                || t.contains("housing") || t.contains("building") || t.contains("sentiment")) {
            return higher ? 1 : -1;
        }
        return 0;
    }

    private static String buildReasoning(Lean lean, int released) {
        String disclaimer = "\n\n(این تحلیل خودکار و صرفاً آموزشی است، نه توصیه‌ی سرمایه‌گذاری.)";
        switch (lean) {
            case HAWKISH:
                return "بر اساس " + released + " داده‌ی منتشرشده‌ی این هفته، فضای کلی «انقباضی» (Hawkish) است — "
                        + "داده‌ها داغ‌تر از انتظار بازار آمدند (تورم بالاتر یا اقتصاد/اشتغال قوی‌تر). این معمولاً یعنی "
                        + "احتمال سیاست سخت‌گیرانه‌تر فدرال رزرو و تقویت دلار؛ که اغلب فشار نزولی روی طلا، بیت‌کوین و سهام می‌گذارد. "
                        + "با احتیاط بیشتری معامله کن." + disclaimer;
            case DOVISH:
                return "بر اساس " + released + " داده‌ی منتشرشده‌ی این هفته، فضای کلی «انبساطی» (Dovish) است — "
                        + "داده‌ها ضعیف‌تر از انتظار بازار آمدند. این معمولاً یعنی احتمال سیاست ملایم‌تر فدرال رزرو و تضعیف دلار؛ "
                        + "که اغلب به‌نفع طلا، بیت‌کوین و سهام است." + disclaimer;
            default:
                return "بر اساس " + released + " داده‌ی منتشرشده‌ی این هفته، سیگنال‌ها «ترکیبی/خنثی» بودند و جهت‌گیری واضحی "
                        + "دیده نمی‌شود. بهتر است منتظر رویدادهای مهم بعدی این هفته (به‌خصوص تصمیم نرخ بهره یا داده‌های تورم) بمانی." + disclaimer;
        }
    }

    /** تبدیل رشته‌هایی مثل «2.9%»، «1.41M»، «57K»، «-4.5%» به عدد؛ null اگر عددی نباشد. */
    private static Double num(String s) {
        if (s == null) return null;
        String cleaned = s.trim().replace(",", "").replace("%", "")
                .replace("K", "").replace("M", "").replace("B", "").trim();
        if (cleaned.isEmpty()) return null;
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
