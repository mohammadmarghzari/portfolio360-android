package com.mamad.portfolio360.portfolio;

import java.util.ArrayList;
import java.util.List;

/** یک روش تحلیل/بهینه‌سازی پرتفوی (CVaR، مونت‌کارلو، بارِبل طالب و ...) */
public class OptimizationMethod {
    public final String key;
    public final String title;
    public final String description;
    public final boolean implemented;

    public OptimizationMethod(String key, String title, String description, boolean implemented) {
        this.key = key;
        this.title = title;
        this.description = description;
        this.implemented = implemented;
    }

    public static List<OptimizationMethod> all() {
        List<OptimizationMethod> list = new ArrayList<>();
        list.add(new OptimizationMethod(
                "cvar",
                "بهینه‌سازی CVaR",
                "کمینه‌سازی میانگین بدترین زیان‌های محتمل (Conditional Value at Risk) به‌جای صرفاً واریانس.",
                true));
        list.add(new OptimizationMethod(
                "monte_carlo",
                "شبیه‌سازی مونت‌کارلو",
                "هزاران مسیر تصادفی قیمتی برای برآورد توزیع بازده و ریسک آینده پرتفوی.",
                true));
        list.add(new OptimizationMethod(
                "stress_test",
                "آزمون استرس (Stress Test)",
                "بررسی عملکرد پرتفوی در بحران‌های تاریخی (۲۰۰۸، کرونا، و سایر دوره‌های پرنوسان).",
                true));
        list.add(new OptimizationMethod(
                "taleb_barbell",
                "بارِبل طالب (Barbell 90/10)",
                "۹۰٪ در دارایی‌های فوق‌ایمن و ۱۰٪ در دارایی‌های پرریسک/پرپتانسیل — به سبک نسیم طالب.",
                true));
        return list;
    }
}
