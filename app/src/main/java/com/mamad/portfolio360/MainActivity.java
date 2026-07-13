package com.mamad.portfolio360;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mamad.portfolio360.premium.SubscriptionManager;
import com.mamad.portfolio360.support.SupportFragment;
import com.mamad.portfolio360.wizard.HomeFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SubscriptionManager.refresh(null);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        FloatingActionButton fabSupport = findViewById(R.id.fab_support);
        fabSupport.setOnClickListener(v -> getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new SupportFragment())
                .addToBackStack(null)
                .commit());
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
