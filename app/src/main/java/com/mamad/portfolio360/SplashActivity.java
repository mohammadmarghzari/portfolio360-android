package com.mamad.portfolio360;

import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(R.layout.activity_splash);

        View root              = findViewById(R.id.splashRoot);
        SplashChartView chart  = findViewById(R.id.splashChart);
        View glowCircle        = findViewById(R.id.glowCircle);
        TextView title         = findViewById(R.id.tvAppTitle);
        TextView subtitle      = findViewById(R.id.tvSubtitle);
        View divider           = findViewById(R.id.dividerLine);
        LinearLayout tags      = findViewById(R.id.tagRow);
        ProgressBar bar        = findViewById(R.id.loadingBar);

        chart.startReveal(1900);

        ObjectAnimator glowIn = ObjectAnimator.ofFloat(glowCircle, "alpha", 0f, 0.7f);
        glowIn.setDuration(700); glowIn.setStartDelay(150);

        AnimatorSet badges = buildBadgeAnimator(
                findViewById(R.id.badgeGoogl), 300,
                findViewById(R.id.badgeBtc), 480,
                findViewById(R.id.badgeEth), 660,
                findViewById(R.id.badgeNvda), 840,
                findViewById(R.id.badgeMeta), 1000
        );

        AnimatorSet titleSet = new AnimatorSet();
        titleSet.playTogether(
            ObjectAnimator.ofFloat(title, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(title, "translationY", 30f, 0f)
        );
        titleSet.setDuration(500); titleSet.setStartDelay(1250);

        AnimatorSet subSet = new AnimatorSet();
        subSet.playTogether(
            ObjectAnimator.ofFloat(subtitle, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(subtitle, "translationY", 20f, 0f)
        );
        subSet.setDuration(450); subSet.setStartDelay(1450);

        AnimatorSet divSet = new AnimatorSet();
        divSet.playTogether(
            ObjectAnimator.ofFloat(divider, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(divider, "scaleX", 0f, 1f)
        );
        divSet.setDuration(500); divSet.setStartDelay(1650);

        AnimatorSet tagSet = new AnimatorSet();
        tagSet.playTogether(
            ObjectAnimator.ofFloat(tags, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(tags, "translationY", 16f, 0f)
        );
        tagSet.setDuration(450); tagSet.setStartDelay(1900);

        ObjectAnimator barA = ObjectAnimator.ofFloat(bar, "alpha", 0f, 1f);
        barA.setDuration(400); barA.setStartDelay(2150);

        AnimatorSet all = new AnimatorSet();
        all.playTogether(glowIn, badges, titleSet, subSet, divSet, tagSet, barA);
        all.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ObjectAnimator out = ObjectAnimator.ofFloat(root, "alpha", 1f, 0f);
            out.setDuration(400);
            out.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator a) {
                    Class<?> next = com.mamad.portfolio360.auth.UserAccountManager.isLoggedIn(SplashActivity.this)
                            ? MainActivity.class : LoginActivity.class;
                    startActivity(new Intent(SplashActivity.this, next));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }
            });
            out.start();
        }, 3600);
    }

    /** هر نشان دارایی با کمی تأخیر نسبت به قبلی، با افکت overshoot ظاهر می‌شود. */
    private AnimatorSet buildBadgeAnimator(Object... iconsAndDelays) {
        java.util.List<android.animation.Animator> all = new java.util.ArrayList<>();

        for (int i = 0; i < iconsAndDelays.length; i += 2) {
            ImageView icon = (ImageView) iconsAndDelays[i];
            long delay = (long) (int) iconsAndDelays[i + 1];

            ObjectAnimator a = ObjectAnimator.ofFloat(icon, "alpha", 0f, 1f);
            ObjectAnimator sx = ObjectAnimator.ofFloat(icon, "scaleX", 0.3f, 1f);
            ObjectAnimator sy = ObjectAnimator.ofFloat(icon, "scaleY", 0.3f, 1f);

            AnimatorSet set = new AnimatorSet();
            set.playTogether(a, sx, sy);
            set.setDuration(550);
            set.setStartDelay(delay);
            set.setInterpolator(new OvershootInterpolator(1.6f));

            all.add(set);
        }

        AnimatorSet combined = new AnimatorSet();
        combined.playTogether(all);
        return combined;
    }

    @Override public void onBackPressed() {}
}
