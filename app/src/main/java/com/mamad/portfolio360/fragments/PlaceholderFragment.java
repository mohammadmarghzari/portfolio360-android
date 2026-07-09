package com.mamad.portfolio360.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mamad.portfolio360.R;

/**
 * فرگمنت جایگزین موقت برای هر تب.
 * در مراحل بعدی، هر تب یک فرگمنت اختصاصی با منطق واقعی
 * (Black-Scholes، Monte Carlo، Black-Litterman و ...) جایگزین این کلاس می‌شود.
 */
public class PlaceholderFragment extends Fragment {

    private static final String ARG_TITLE = "arg_title";

    public static PlaceholderFragment newInstance(String title) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);

        String title = getArguments() != null ? getArguments().getString(ARG_TITLE, "") : "";

        TextView titleView = view.findViewById(R.id.placeholder_title);
        TextView bodyView = view.findViewById(R.id.placeholder_body);

        titleView.setText(title);
        bodyView.setText(getString(R.string.placeholder_text, title));

        return view;
    }
}
