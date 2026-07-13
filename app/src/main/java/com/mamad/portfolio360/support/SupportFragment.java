package com.mamad.portfolio360.support;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.mamad.portfolio360.R;

/** صفحه‌ی راهنما: سوالات متداول + فرم ارسال انتقاد/پیشنهاد مستقیم به تلگرام ادمین. */
public class SupportFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_support, container, false);

        buildFaqList(view);

        TextInputEditText feedbackInput = view.findViewById(R.id.feedback_input);
        MaterialButton sendButton = view.findViewById(R.id.btn_send_feedback);

        sendButton.setOnClickListener(v -> {
            String message = feedbackInput.getText() != null ? feedbackInput.getText().toString().trim() : "";
            if (message.isEmpty()) {
                Toast.makeText(getContext(), R.string.support_feedback_empty, Toast.LENGTH_SHORT).show();
                return;
            }

            String email = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;

            sendButton.setEnabled(false);
            TelegramFeedbackClient.sendFeedback(email, message, new TelegramFeedbackClient.SendCallback() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) return;
                    sendButton.setEnabled(true);
                    feedbackInput.setText("");
                    Toast.makeText(getContext(), R.string.support_feedback_sent, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String errorMessage) {
                    if (!isAdded()) return;
                    sendButton.setEnabled(true);
                    Toast.makeText(getContext(), R.string.support_feedback_failed, Toast.LENGTH_SHORT).show();
                }
            });
        });

        return view;
    }

    private void buildFaqList(View root) {
        LinearLayout container = root.findViewById(R.id.faq_container);
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (FaqData.FaqItem item : FaqData.items()) {
            View card = inflater.inflate(R.layout.item_faq, container, false);
            TextView questionView = card.findViewById(R.id.faq_question);
            TextView answerView = card.findViewById(R.id.faq_answer);
            TextView toggleIcon = card.findViewById(R.id.faq_toggle_icon);
            View questionRow = card.findViewById(R.id.faq_question_row);

            questionView.setText(item.question);
            answerView.setText(item.answer);

            questionRow.setOnClickListener(v -> {
                boolean expanded = answerView.getVisibility() == View.VISIBLE;
                answerView.setVisibility(expanded ? View.GONE : View.VISIBLE);
                toggleIcon.setText(expanded ? "+" : "−");
            });

            container.addView(card);
        }
    }
}
