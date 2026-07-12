package com.mamad.portfolio360.wizard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * برای استراتژی‌های سه/چهارپایه (آیرون کاندور، پروانه‌ای)، این کلاس مشخص می‌کند
 * کاربر باید چند پایه و به چه ترتیبی (کال/پوت، خرید/فروش، تعداد) انتخاب کند.
 * پس از انتخاب همه پایه‌ها، مستقیماً به PayoffEngine.Leg تبدیل می‌شوند —
 * نیازی به سازنده اختصاصی در PayoffEngine نیست.
 */
public class MultiLegPlan {

    public static class Step {
        public final boolean isCall;
        public final boolean isLong;
        public final double quantity;
        public final int hintRes; // شناسه رشته راهنما برای این قدم

        public Step(boolean isCall, boolean isLong, double quantity, int hintRes) {
            this.isCall = isCall;
            this.isLong = isLong;
            this.quantity = quantity;
            this.hintRes = hintRes;
        }
    }

    private static final Map<String, List<Step>> PLANS = new LinkedHashMap<>();

    static {
        // آیرون کاندور: خرید پوت دور (بیمه) → فروش پوت نزدیک → فروش کال نزدیک → خرید کال دور (بیمه)
        List<Step> ironCondor = new ArrayList<>();
        ironCondor.add(new Step(false, true, 1,
                com.mamad.portfolio360.R.string.ic_step1_buy_put));
        ironCondor.add(new Step(false, false, 1,
                com.mamad.portfolio360.R.string.ic_step2_sell_put));
        ironCondor.add(new Step(true, false, 1,
                com.mamad.portfolio360.R.string.ic_step3_sell_call));
        ironCondor.add(new Step(true, true, 1,
                com.mamad.portfolio360.R.string.ic_step4_buy_call));
        PLANS.put("iron_condor", ironCondor);

        // پروانه‌ای (با کال): خرید کال پایین → فروش ۲ کال وسط → خرید کال بالا
        List<Step> butterfly = new ArrayList<>();
        butterfly.add(new Step(true, true, 1,
                com.mamad.portfolio360.R.string.bf_step1_buy_lower));
        butterfly.add(new Step(true, false, 2,
                com.mamad.portfolio360.R.string.bf_step2_sell_middle));
        butterfly.add(new Step(true, true, 1,
                com.mamad.portfolio360.R.string.bf_step3_buy_upper));
        PLANS.put("butterfly", butterfly);
    }

    public static boolean isMultiLeg(String strategyKey) {
        return PLANS.containsKey(strategyKey);
    }

    public static List<Step> getPlan(String strategyKey) {
        return PLANS.get(strategyKey);
    }
}
