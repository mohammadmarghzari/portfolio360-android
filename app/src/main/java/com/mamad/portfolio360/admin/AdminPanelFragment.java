package com.mamad.portfolio360.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mamad.portfolio360.R;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** پنل مدیریت داخل اپ: فقط برای ادمین — فعال‌سازی دستی اشتراک با ایمیل کاربر. */
public class AdminPanelFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_panel, container, false);

        TextInputEditText emailInput = view.findViewById(R.id.admin_target_email);
        RadioGroup planGroup = view.findViewById(R.id.admin_plan_group);
        MaterialButton activateButton = view.findViewById(R.id.btn_admin_activate);

        activateButton.setOnClickListener(v -> {
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim().toLowerCase() : "";
            if (email.isEmpty()) {
                Toast.makeText(getContext(), R.string.admin_email_required, Toast.LENGTH_SHORT).show();
                return;
            }

            int days = planDays(planGroup.getCheckedRadioButtonId());
            String planKey = planKey(planGroup.getCheckedRadioButtonId());

            activateButton.setEnabled(false);
            FirebaseFirestore.getInstance().collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!isAdded()) return;
                        if (snapshot.isEmpty()) {
                            activateButton.setEnabled(true);
                            Toast.makeText(getContext(), R.string.admin_user_not_found, Toast.LENGTH_LONG).show();
                            return;
                        }

                        QueryDocumentSnapshot userDoc = snapshot.iterator().next();
                        String uid = userDoc.getId();
                        long expiresAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days);

                        Map<String, Object> sub = new HashMap<>();
                        sub.put("active", true);
                        sub.put("expiresAt", expiresAt);
                        sub.put("plan", planKey);
                        sub.put("activatedAt", System.currentTimeMillis());

                        FirebaseFirestore.getInstance().collection("subscriptions").document(uid).set(sub)
                                .addOnSuccessListener(unused -> {
                                    if (!isAdded()) return;
                                    activateButton.setEnabled(true);
                                    String until = new java.text.SimpleDateFormat("yyyy/MM/dd", Locale.US)
                                            .format(new Date(expiresAt));
                                    Toast.makeText(getContext(),
                                            getString(R.string.admin_activation_success, email, until),
                                            Toast.LENGTH_LONG).show();
                                    emailInput.setText("");
                                })
                                .addOnFailureListener(e -> {
                                    if (!isAdded()) return;
                                    activateButton.setEnabled(true);
                                    Toast.makeText(getContext(), R.string.admin_activation_failed, Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        activateButton.setEnabled(true);
                        Toast.makeText(getContext(), R.string.admin_activation_failed, Toast.LENGTH_SHORT).show();
                    });
        });

        return view;
    }

    private int planDays(int checkedId) {
        if (checkedId == R.id.admin_plan_3m) return 90;
        if (checkedId == R.id.admin_plan_6m) return 180;
        if (checkedId == R.id.admin_plan_1y) return 365;
        return 30;
    }

    private String planKey(int checkedId) {
        if (checkedId == R.id.admin_plan_3m) return "3m";
        if (checkedId == R.id.admin_plan_6m) return "6m";
        if (checkedId == R.id.admin_plan_1y) return "1y";
        return "1m";
    }
}
