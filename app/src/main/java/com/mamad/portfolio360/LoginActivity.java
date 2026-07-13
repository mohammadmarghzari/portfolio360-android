package com.mamad.portfolio360;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mamad.portfolio360.auth.UserAccountManager;

/**
 * صفحه ورود/ثبت‌نام: فعلاً حساب به‌صورت محلی (روی همین گوشی) با ایمیل و رمز
 * عبور ساخته و بررسی می‌شود. دکمه «ورود با جیمیل» تا راه‌اندازی Firebase غیرفعال است.
 */
public class LoginActivity extends AppCompatActivity {

    private boolean signupMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TextView modeTitle = findViewById(R.id.login_mode_title);
        TextView switchMode = findViewById(R.id.login_switch_mode);
        TextInputEditText emailInput = findViewById(R.id.input_email);
        TextInputEditText passwordInput = findViewById(R.id.input_password);
        MaterialButton submitButton = findViewById(R.id.btn_login_submit);
        MaterialButton googleButton = findViewById(R.id.btn_google_signin);

        // اگر قبلاً روی این گوشی حساب ساخته شده، پیش‌فرض را روی «ورود» بگذار
        if (UserAccountManager.currentEmail(this) != null && !UserAccountManager.currentEmail(this).isEmpty()) {
            signupMode = false;
            emailInput.setText(UserAccountManager.currentEmail(this));
        }
        applyMode(modeTitle, switchMode, submitButton);

        switchMode.setOnClickListener(v -> {
            signupMode = !signupMode;
            applyMode(modeTitle, switchMode, submitButton);
        });

        submitButton.setOnClickListener(v -> {
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";

            if (!UserAccountManager.isValidEmail(email)) {
                Toast.makeText(this, R.string.login_error_invalid_email, Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, R.string.login_error_short_password, Toast.LENGTH_SHORT).show();
                return;
            }

            if (signupMode) {
                if (UserAccountManager.accountExists(this, email)) {
                    Toast.makeText(this, R.string.login_error_account_exists, Toast.LENGTH_SHORT).show();
                    signupMode = false;
                    applyMode(modeTitle, switchMode, submitButton);
                    return;
                }
                UserAccountManager.createAccount(this, email, password);
                goToMain();
            } else {
                if (UserAccountManager.login(this, email, password)) {
                    goToMain();
                } else {
                    Toast.makeText(this, R.string.login_error_invalid_credentials, Toast.LENGTH_SHORT).show();
                }
            }
        });

        googleButton.setOnClickListener(v ->
                Toast.makeText(this, R.string.login_google_soon, Toast.LENGTH_SHORT).show());
    }

    private void applyMode(TextView modeTitle, TextView switchMode, MaterialButton submitButton) {
        if (signupMode) {
            modeTitle.setText(R.string.login_title_signup);
            submitButton.setText(R.string.login_button_signup);
            switchMode.setText(R.string.login_switch_to_login);
        } else {
            modeTitle.setText(R.string.login_title_login);
            submitButton.setText(R.string.login_button_login);
            switchMode.setText(R.string.login_switch_to_signup);
        }
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
