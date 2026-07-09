package com.mamad.portfolio360.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.mamad.portfolio360.fragments.PlaceholderFragment;

import java.util.List;

public class TabPagerAdapter extends FragmentStateAdapter {

    private final List<String> tabTitles;

    public TabPagerAdapter(@NonNull FragmentActivity activity, List<String> tabTitles) {
        super(activity);
        this.tabTitles = tabTitles;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // در آینده: بسته به position، فرگمنت اختصاصی هر تب برگردانده می‌شود
        // (مثلاً GreeksFragment، MonteCarloFragment و غیره)
        return PlaceholderFragment.newInstance(tabTitles.get(position));
    }

    @Override
    public int getItemCount() {
        return tabTitles.size();
    }
}
