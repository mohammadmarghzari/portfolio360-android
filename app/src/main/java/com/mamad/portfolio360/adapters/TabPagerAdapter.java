package com.mamad.portfolio360.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.mamad.portfolio360.fragments.CoveredCallFragment;
import com.mamad.portfolio360.fragments.GreeksFragment;
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
        // تب‌های ۰ و ۱ پیاده‌سازی شده؛ بقیه تب‌ها به‌مرور جایگزین می‌شوند
        if (position == 0) {
            return new GreeksFragment();
        }
        if (position == 1) {
            return new CoveredCallFragment();
        }
        return PlaceholderFragment.newInstance(tabTitles.get(position));
    }

    @Override
    public int getItemCount() {
        return tabTitles.size();
    }
}
