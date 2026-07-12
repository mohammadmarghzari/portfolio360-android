package com.mamad.portfolio360.portfolio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * استراتژی بارِبل طالب: دارایی‌های انتخابی کاربر را بر اساس دسته‌بندی
 * به دو سبد «امن» (صندوق‌ها/ETF، به‌عنوان نماینده دارایی‌های پایدار) و
 * «پرریسک» (سهام تکی، ارز دیجیتال، کالا) تقسیم می‌کند و وزن ۹۰٪/۱۰٪
 * پیشنهاد می‌دهد.
 *
 * توجه: این یک ساده‌سازی آموزشی است — بارِبل اصیل طالب معمولاً از نقد/اوراق
 * خزانه کوتاه‌مدت برای سبد امن و اختیار معامله/دارایی‌های فوق‌نامتقارن برای
 * سبد پرریسک استفاده می‌کند؛ اینجا از دسته «صندوق‌ها» به‌عنوان نزدیک‌ترین
 * معادل موجود در فهرست دارایی‌ها استفاده شده است.
 */
public class BarbellEngine {

    public static class Result {
        public List<String> safeAssets = new ArrayList<>();
        public List<String> riskyAssets = new ArrayList<>();
        public Map<String, Double> weightPct = new LinkedHashMap<>();
        public boolean hasSafeBucket;
        public boolean hasRiskyBucket;
    }

    public static Result compute(Set<String> selectedSymbols) {
        Result res = new Result();
        Map<String, List<AssetCatalog.Asset>> catalog = AssetCatalog.all();

        for (String symbol : selectedSymbols) {
            String category = findCategory(symbol, catalog);
            if (AssetCatalog.CAT_FUNDS.equals(category)) {
                res.safeAssets.add(symbol);
            } else {
                res.riskyAssets.add(symbol);
            }
        }

        res.hasSafeBucket = !res.safeAssets.isEmpty();
        res.hasRiskyBucket = !res.riskyAssets.isEmpty();

        if (res.hasSafeBucket && res.hasRiskyBucket) {
            double safeEach = 90.0 / res.safeAssets.size();
            double riskyEach = 10.0 / res.riskyAssets.size();
            for (String s : res.safeAssets) res.weightPct.put(s, safeEach);
            for (String s : res.riskyAssets) res.weightPct.put(s, riskyEach);
        } else if (res.hasSafeBucket) {
            double safeEach = 90.0 / res.safeAssets.size();
            for (String s : res.safeAssets) res.weightPct.put(s, safeEach);
        } else if (res.hasRiskyBucket) {
            double each = 100.0 / res.riskyAssets.size();
            for (String s : res.riskyAssets) res.weightPct.put(s, each);
        }

        return res;
    }

    private static String findCategory(String symbol, Map<String, List<AssetCatalog.Asset>> catalog) {
        for (Map.Entry<String, List<AssetCatalog.Asset>> e : catalog.entrySet()) {
            for (AssetCatalog.Asset a : e.getValue()) {
                if (a.symbol.equals(symbol)) return e.getKey();
            }
        }
        return AssetCatalog.CAT_STOCKS;
    }
}
