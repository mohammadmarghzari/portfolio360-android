package com.mamad.portfolio360.wizard;

import java.util.ArrayList;
import java.util.List;

/**
 * بر اساس دیدگاه بازار انتخابی کاربر (صعودی/نزولی/پرنوسان/خنثی/کسب درآمد)،
 * لیست استراتژی‌های مرتبط را برمی‌گرداند — مشابه بخش "Select a strategy" در Derive.xyz.
 */
public class StrategyMapping {

    public static final String OUTLOOK_BULLISH = "bullish";
    public static final String OUTLOOK_BEARISH = "bearish";
    public static final String OUTLOOK_VOLATILE = "volatile";
    public static final String OUTLOOK_NEUTRAL = "neutral";
    public static final String OUTLOOK_YIELD = "yield";

    public static List<StrategyOption> getStrategiesFor(String outlook) {
        List<StrategyOption> list = new ArrayList<>();

        switch (outlook) {
            case OUTLOOK_YIELD:
                list.add(new StrategyOption(
                        "covered_call",
                        "کاوردکال (Covered Call)",
                        "با فروش کال روی دارایی خود، پرمیوم دریافت کنید.",
                        true));
                break;

            case OUTLOOK_BEARISH:
                list.add(new StrategyOption(
                        "protective_put",
                        "پروتکتیو پوت (Protective Put)",
                        "با خرید پوت، دارایی خود را در برابر افت شدید بیمه کنید.",
                        true));
                break;

            case OUTLOOK_NEUTRAL:
                list.add(new StrategyOption(
                        "iron_condor",
                        "آیرون کاندور (Iron Condor)",
                        "با فروش یک استرنگل و خرید بال‌های محافظ، از کم‌نوسانی سود ببرید.",
                        false));
                list.add(new StrategyOption(
                        "butterfly",
                        "پروانه‌ای (Butterfly)",
                        "از کم‌نوسانی نزدیک یک قیمت هدف مشخص سود ببرید.",
                        false));
                break;

            case OUTLOOK_VOLATILE:
                list.add(new StrategyOption(
                        "long_straddle",
                        "استرادل بلند (Long Straddle)",
                        "با خرید هم‌زمان کال و پوت روی یک قیمت اعمال، از نوسان شدید سود ببرید.",
                        false));
                list.add(new StrategyOption(
                        "long_strangle",
                        "استرنگل بلند (Long Strangle)",
                        "با هزینه کمتر، از نوسان شدید قیمت در هر جهتی سود ببرید.",
                        false));
                break;

            case OUTLOOK_BULLISH:
            default:
                list.add(new StrategyOption(
                        "long_call",
                        "خرید کال (Long Call)",
                        "با خرید کال، از افزایش قیمت با ریسک محدود سود ببرید.",
                        true));
                list.add(new StrategyOption(
                        "bull_call_spread",
                        "بول کال اسپرد (Bull Call Spread)",
                        "با ترکیب خرید و فروش کال، هزینه استراتژی صعودی را کاهش دهید.",
                        true));
                break;
        }

        return list;
    }
}
