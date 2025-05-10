package com.example.trackback;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class AddReportDialogFragment extends DialogFragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_report_dialog, container, false);

        Button reportLostBtn = view.findViewById(R.id.reportLostBtn);
        Button reportFoundBtn = view.findViewById(R.id.reportFoundBtn);

        reportLostBtn.setOnClickListener(v -> {
            dismiss();
            Intent intent = new Intent(getActivity(), ReportlostActivity.class);
            startActivity(intent);

        });

        reportFoundBtn.setOnClickListener(v -> {
            dismiss();
            Intent intent = new Intent(getActivity(), ReportfoundActivity.class);
            startActivity(intent);

        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}
