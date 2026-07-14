package com.mamad.portfolio360.profile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.mamad.portfolio360.LoginActivity;
import com.mamad.portfolio360.R;
import com.mamad.portfolio360.premium.SubscriptionManager;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** صفحه‌ی پروفایل کاربر: عکس، نام نمایشی، وضعیت اشتراک و خروج از حساب — همه‌چیز روی سرور ذخیره می‌شود. */
public class ProfileFragment extends Fragment {

    private ImageView avatarView;
    private Bitmap pendingPhoto;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onImagePicked);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        avatarView = view.findViewById(R.id.profile_avatar);
        TextView emailView = view.findViewById(R.id.profile_email);
        TextView statusView = view.findViewById(R.id.profile_sub_status);
        TextInputEditText nameInput = view.findViewById(R.id.profile_name_input);
        MaterialButton saveNameButton = view.findViewById(R.id.btn_save_name);
        MaterialButton signOutButton = view.findViewById(R.id.btn_sign_out);

        String email = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;
        emailView.setText(email != null ? email : "—");

        SubscriptionManager.refresh(() -> {
            if (!isAdded()) return;
            if (SubscriptionManager.isPaidActive()) {
                String until = new SimpleDateFormat("yyyy/MM/dd", Locale.US)
                        .format(new Date(SubscriptionManager.expiresAtMillis(requireContext())));
                statusView.setText(getString(R.string.sub_status_active, until));
            } else if (SubscriptionManager.isInTrial()) {
                String until = new SimpleDateFormat("yyyy/MM/dd", Locale.US)
                        .format(new Date(SubscriptionManager.trialEndsAtMillis()));
                statusView.setText(getString(R.string.sub_status_trial, until));
            } else {
                statusView.setText(getString(R.string.sub_status_inactive));
            }
        });

        UserProfileStore.load(new UserProfileStore.LoadCallback() {
            @Override
            public void onLoaded(String displayName, Bitmap photo) {
                if (!isAdded()) return;
                if (displayName != null) nameInput.setText(displayName);
                if (photo != null) setAvatarBitmap(photo);
            }

            @Override
            public void onError() {
                // اگه سند هنوز وجود نداره، پروفایل خالی نمایش داده می‌شه — طبیعیه
            }
        });

        avatarView.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        saveNameButton.setOnClickListener(v -> {
            String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
            if (name.isEmpty()) {
                Toast.makeText(getContext(), R.string.profile_name_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            saveNameButton.setEnabled(false);
            UserProfileStore.saveDisplayName(name, new UserProfileStore.SaveCallback() {
                @Override
                public void onSaved() {
                    if (!isAdded()) return;
                    saveNameButton.setEnabled(true);
                    Toast.makeText(getContext(), R.string.profile_saved, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError() {
                    if (!isAdded()) return;
                    saveNameButton.setEnabled(true);
                    Toast.makeText(getContext(), R.string.profile_save_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });

        signOutButton.setOnClickListener(v -> signOut());

        return view;
    }

    private void onImagePicked(Uri uri) {
        if (uri == null || !isAdded()) return;
        try (InputStream input = requireContext().getContentResolver().openInputStream(uri)) {
            Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input);
            if (bitmap == null) return;
            pendingPhoto = bitmap;
            setAvatarBitmap(bitmap);

            UserProfileStore.savePhoto(bitmap, new UserProfileStore.SaveCallback() {
                @Override
                public void onSaved() {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), R.string.profile_photo_saved, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError() {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), R.string.profile_save_failed, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.profile_save_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void setAvatarBitmap(Bitmap bitmap) {
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);
        drawable.setCircular(true);
        avatarView.setImageDrawable(drawable);
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
