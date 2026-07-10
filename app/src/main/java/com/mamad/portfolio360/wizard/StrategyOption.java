package com.mamad.portfolio360.wizard;

/**
 * نمایانگر یک استراتژی قابل انتخاب در لیست استراتژی‌ها (شبیه کارت‌های Derive.xyz).
 */
public class StrategyOption {
    public final String key;
    public final String title;
    public final String description;
    public final boolean implemented;

    public StrategyOption(String key, String title, String description, boolean implemented) {
        this.key = key;
        this.title = title;
        this.description = description;
        this.implemented = implemented;
    }
}
