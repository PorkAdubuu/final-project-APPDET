// ReportSuccessDialogFragment.java
package com.example.trackback;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class report_success_dialog extends DialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_report_success_dialog, container, false);

        // “Go back to home”
        Button backHome = root.findViewById(R.id.backhomeBtn);
        backHome.setOnClickListener(v -> {
            // close all other Activities and go straight to MainActivity
            Intent i = new Intent(requireContext(), MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            dismiss();
        });

        // simple pop-in animation for the white card
        View card = root.findViewById(R.id.contentContainer);
        card.setScaleX(0.85f);
        card.setScaleY(0.85f);
        card.setAlpha(0f);
        card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(250)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        // full-screen + transparent background so we see the blur image
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        // allow tapping outside to close
        setCancelable(true);
        getDialog().setCanceledOnTouchOutside(true);
    }
}
