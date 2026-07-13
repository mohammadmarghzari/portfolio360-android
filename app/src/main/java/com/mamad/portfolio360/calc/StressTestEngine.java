package com.mamad.portfolio360.calc;

import java.util.ArrayList;
import java.util.List;

/**
 * آزمون استرس: افت پرتفوی را تحت چند سناریوی شناخته‌شده تاریخی برآورد می‌کند.
 *
 * توجه صادقانه: چون داده تاریخی این اپ حداکثر ۳ سال گذشته را می‌گیرد، امکان
 * شبیه‌سازی مستقیم رفتار واقعیِ دارایی‌های انتخابی در بحران‌های ۲۰۰۸ یا کرونا
 * وجود ندارد (آن بازه‌ها خارج از دامنه داده قابل‌دریافت هستند). این ماژول
 * به‌جای آن، درصد افت مستند و شناخته‌شده شاخص S&P 500 در آن بحران‌ها را
 * به‌عنوان یک سناریوی آموزشی/تخمینی به کل پرتفوی اعمال می‌کند — نه یک
 * شبیه‌سازی دقیق مبتنی بر رفتار واقعی دارایی‌های انتخابی.
 */
public class StressTestEngine {

    public static class Scenario {
        public final String key;
        public final String titleFa;
        public final String periodFa;
        public final double shockPct; // درصد افت (منفی)

        public Scenario(String key, String titleFa, String periodFa, double shockPct) {
            this.key = key;
            this.titleFa = titleFa;
            this.periodFa = periodFa;
            this.shockPct = shockPct;
        }
    }

    public static List<Scenario> scenarios() {
        List<Scenario> list = new ArrayList<>();
        // درصدهای افت peak-to-trough شاخص S&P 500 — ارقام عمومی و مستند
        list.add(new Scenario("gfc2008", "بحران مالی جهانی", "اکتبر ۲۰۰۷ تا مارس ۲۰۰۹", -57.0));
        list.add(new Scenario("covid2020", "کرونا (COVID-19)", "فوریه تا مارس ۲۰۲۰", -34.0));
        list.add(new Scenario("bear2022", "بازار نزولی", "ژانویه تا اکتبر ۲۰۲۲", -25.0));
        return list;
    }
}
