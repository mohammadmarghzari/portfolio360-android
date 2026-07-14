package com.mamad.portfolio360;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

/**
 * صفحه ورود/ثبت‌نام با ایمیل و رمز عبور از طریق Firebase Authentication.
 * گزینه‌ی «مرا به خاطر بسپار» ایمیل و رمز را به‌صورت محلی ذخیره می‌کند تا
 * دفعه‌ی بعد نیازی به تایپ دوباره نباشد.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String PREFS = "login_prefs";
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";

    private boolean signupMode = true;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        TextView modeTitle = findViewById(R.id.login_mode_title);
        TextView switchMode = findViewById(R.id.login_switch_mode);
        TextInputEditText emailInput = findViewById(R.id.input_email);
        TextInputEditText passwordInput = findViewById(R.id.input_password);
        MaterialButton submitButton = findViewById(R.id.btn_login_submit);
        MaterialCheckBox rememberBox = findViewById(R.id.checkbox_remember);

        // پیش‌پرکردن از ذخیره‌ی قبلی
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_REMEMBER, false)) {
            emailInput.setText(prefs.getString(KEY_EMAIL, ""));
            passwordInput.setText(prefs.getString(KEY_PASSWORD, ""));
            rememberBox.setChecked(true);
            // اگر قبلاً حساب ساخته، پیش‌فرض را روی حالت ورود بگذار
            signupMode = false;
        }

        applyMode(modeTitle, switchMode, submitButton);

        switchMode.setOnClickListener(v -> {
            signupMode = !signupMode;
            applyMode(modeTitle, switchMode, submitButton);
        });

        submitButton.setOnClickListener(v -> {
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, R.string.login_error_invalid_email, Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(this, R.string.login_error_short_password, Toast.LENGTH_SHORT).show();
                return;
            }

            submitButton.setEnabled(false);
            if (signupMode) {
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    submitButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        saveCredentials(rememberBox.isChecked(), email, password);
                        goToMain();
                        return;
                    }
                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, R.string.login_error_account_exists, Toast.LENGTH_SHORT).show();
                        signupMode = false;
                        applyMode(modeTitle, switchMode, submitButton);
                    } else {
                        Toast.makeText(this, describeError(task.getException()), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    submitButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        saveCredentials(rememberBox.isChecked(), email, password);
                        goToMain();
                    } else {
                        Toast.makeText(this, R.string.login_error_invalid_credentials, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void saveCredentials(boolean remember, String email, String password) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        if (remember) {
            editor.putBoolean(KEY_REMEMBER, true)
                    .putString(KEY_EMAIL, email)
                    .putString(KEY_PASSWORD, password);
        } else {
            editor.clear();
        }
        editor.apply();
    }

    private String describeError(Exception e) {
        String message = e != null ? e.getLocalizedMessage() : null;
        return message != null ? message : getString(R.string.login_error_invalid_credentials);
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
