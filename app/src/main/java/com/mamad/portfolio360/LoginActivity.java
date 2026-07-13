package com.mamad.portfolio360;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * صفحه ورود/ثبت‌نام: با ایمیل و رمز عبور یا با جیمیل (Google Sign-In)، هر دو
 * از طریق Firebase Authentication — یعنی هر جیمیل دقیقاً یک حساب مستقل روی
 * سرور دارد (نه فقط روی همین گوشی).
 */
public class LoginActivity extends AppCompatActivity {

    private boolean signupMode = true;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::onGoogleSignInResult);

        TextView modeTitle = findViewById(R.id.login_mode_title);
        TextView switchMode = findViewById(R.id.login_switch_mode);
        TextInputEditText emailInput = findViewById(R.id.input_email);
        TextInputEditText passwordInput = findViewById(R.id.input_password);
        MaterialButton submitButton = findViewById(R.id.btn_login_submit);
        MaterialButton googleButton = findViewById(R.id.btn_google_signin);

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
                        goToMain();
                    } else {
                        Toast.makeText(this, R.string.login_error_invalid_credentials, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        googleButton.setOnClickListener(v -> googleSignInLauncher.launch(googleSignInClient.getSignInIntent()));
    }

    private void onGoogleSignInResult(androidx.activity.result.ActivityResult result) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            auth.signInWithCredential(credential).addOnCompleteListener(authTask -> {
                if (authTask.isSuccessful()) {
                    goToMain();
                } else {
                    Toast.makeText(this, describeError(authTask.getException()), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (ApiException e) {
            String code = com.google.android.gms.common.api.CommonStatusCodes.getStatusCodeString(e.getStatusCode());
            Toast.makeText(this, getString(R.string.login_google_failed) + " (" + e.getStatusCode() + " " + code + ")",
                    Toast.LENGTH_LONG).show();
        }
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
