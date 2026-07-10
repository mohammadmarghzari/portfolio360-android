package com.mamad.portfolio360;

import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
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

        View root         = findViewById(R.id.splashRoot);
        View glowCircle   = findViewById(R.id.glowCircle);
        ImageView ethIcon = findViewById(R.id.ethIcon);
        TextView title    = findViewById(R.id.tvAppTitle);
        TextView subtitle = findViewById(R.id.tvSubtitle);
        View divider      = findViewById(R.id.dividerLine);
        LinearLayout tags = findViewById(R.id.tagRow);
        ProgressBar bar   = findViewById(R.id.loadingBar);

        ObjectAnimator glowIn = ObjectAnimator.ofFloat(glowCircle, "alpha", 0f, 0.7f);
        glowIn.setDuration(700); glowIn.setStartDelay(150);

        ObjectAnimator eA  = ObjectAnimator.ofFloat(ethIcon, "alpha", 0f, 1f);
        ObjectAnimator eSX = ObjectAnimator.ofFloat(ethIcon, "scaleX", 0.3f, 1f);
        ObjectAnimator eSY = ObjectAnimator.ofFloat(ethIcon, "scaleY", 0.3f, 1f);
        AnimatorSet ethSet = new AnimatorSet();
        ethSet.playTogether(eA, eSX, eSY);
        ethSet.setDuration(600); ethSet.setStartDelay(350);
        ethSet.setInterpolator(new OvershootInterpolator(1.4f));

        ObjectAnimator ethRot = ObjectAnimator.ofFloat(ethIcon, "rotation", 0f, 360f);
        ethRot.setDuration(2800); ethRot.setStartDelay(600);
        ethRot.setInterpolator(new DecelerateInterpolator(2f));

        AnimatorSet titleSet = new AnimatorSet();
        titleSet.playTogether(
            ObjectAnimator.ofFloat(title, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(title, "translationY", 30f, 0f)
        );
        titleSet.setDuration(500); titleSet.setStartDelay(850);

        AnimatorSet subSet = new AnimatorSet();
        subSet.playTogether(
            ObjectAnimator.ofFloat(subtitle, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(subtitle, "translationY", 20f, 0f)
        );
        subSet.setDuration(450); subSet.setStartDelay(1080);

        AnimatorSet divSet = new AnimatorSet();
        divSet.playTogether(
            ObjectAnimator.ofFloat(divider, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(divider, "scaleX", 0f, 1f)
        );
        divSet.setDuration(500); divSet.setStartDelay(1300);

        AnimatorSet tagSet = new AnimatorSet();
        tagSet.playTogether(
            ObjectAnimator.ofFloat(tags, "alpha", 0f, 1f),
            ObjectAnimator.ofFloat(tags, "translationY", 16f, 0f)
        );
        tagSet.setDuration(450); tagSet.setStartDelay(1600);

        ObjectAnimator barA = ObjectAnimator.ofFloat(bar, "alpha", 0f, 1f);
        barA.setDuration(400); barA.setStartDelay(1900);

        ValueAnimator pulse = ValueAnimator.ofFloat(1f, 1.1f, 1f);
        pulse.setDuration(2000);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setStartDelay(700);
        pulse.addUpdateListener(a -> {
            float s = (float) a.getAnimatedValue();
            glowCircle.setScaleX(s); glowCircle.setScaleY(s);
        });

        AnimatorSet all = new AnimatorSet();
        all.playTogether(glowIn, ethSet, ethRot, titleSet, subSet, divSet, tagSet, barA);
        all.start();
        pulse.start();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ObjectAnimator out = ObjectAnimator.ofFloat(root, "alpha", 1f, 0f);
            out.setDuration(400);
            out.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator a) {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }
            });
            out.start();
        }, 3200);
    }

    @Override public void onBackPressed() {}
}
