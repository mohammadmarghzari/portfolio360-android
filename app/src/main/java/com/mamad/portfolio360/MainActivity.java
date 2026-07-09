package com.mamad.portfolio360;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.mamad.portfolio360.adapters.TabPagerAdapter;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        List<String> tabTitles = Arrays.asList(
                getString(R.string.tab_greeks),
                getString(R.string.tab_covered_call),
                getString(R.string.tab_protective_put),
                getString(R.string.tab_iron_condor),
                getString(R.string.tab_rolling_cc),
                getString(R.string.tab_black_litterman),
                getString(R.string.tab_monte_carlo),
                getString(R.string.tab_rebalancing),
                getString(R.string.tab_benchmark)
        );

        ViewPager2 viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        TabPagerAdapter adapter = new TabPagerAdapter(this, tabTitles);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (@NonNull TabLayout.Tab tab, int position) ->
                        tab.setText(tabTitles.get(position))
        ).attach();
    }
}
