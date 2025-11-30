package com.example.trackback;

import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class dialogLost_edit_Fragment extends DialogFragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // DROPDOWNS
    private AutoCompleteTextView autoComplete;
    private AutoCompleteTextView lastSeenDropdown;

    // TEXT FIELDS
    private EditText itemLostText, brandText, additionalInfoText,
            moreInfoText, firstNameText, lastNameText, phoneNumber, dateText, timeText;

    // BUTTON
    private Button updateBtn;

    private String documentId;

    // LABELS
    private TextView itemLabel, dateLabel, timeLabel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dialog_lost_edit_, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // LABELS
        itemLabel = view.findViewById(R.id.itemLabel);
        dateLabel = view.findViewById(R.id.dateLabel);
        timeLabel = view.findViewById(R.id.timeLabel);

        // INPUT FIELDS
        itemLostText = view.findViewById(R.id.itemLostText);
        brandText = view.findViewById(R.id.brandText);
        additionalInfoText = view.findViewById(R.id.additionalInfoText);
        moreInfoText = view.findViewById(R.id.moreInfoText);
        firstNameText = view.findViewById(R.id.firstNameText);
        lastNameText = view.findViewById(R.id.lastNameText);
        phoneNumber = view.findViewById(R.id.phoneNumber);
        dateText = view.findViewById(R.id.dateText);
        timeText = view.findViewById(R.id.timeText);

        // DROPDOWNS
        autoComplete = view.findViewById(R.id.category);
        lastSeenDropdown = view.findViewById(R.id.lastSeenDropdown);

        // UPDATE BUTTON
        updateBtn = view.findViewById(R.id.updateBtn);

        // CATEGORY DROPDOWN
        String[] categories = {
                "Gadgets", "Personal Belongings", "Bags", "Accessories",
                "Clothing", "School Supplies", "Drinkware", "Others"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, categories);
        autoComplete.setAdapter(adapter);

        // LAST SEEN DROPDOWN
        String[] locations = {
                "Umak Oval", "HPSB", "Admin Building", "Academic Building 1",
                "Academic Building 2", "Library", "Cafeteria"
        };
        ArrayAdapter<String> locationAdapter =
                new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, locations);
        lastSeenDropdown.setAdapter(locationAdapter);

        // DATE PICKER
        ImageButton datePickerBtn = view.findViewById(R.id.datePicker);
        dateText.setKeyListener(null);
        datePickerBtn.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date Lost")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            picker.show(getParentFragmentManager(), "DATE_PICKER");
            picker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                dateText.setText(sdf.format(new Date(selection)));
            });
        });

        // TIME PICKER
        ImageButton timePickerBtn = view.findViewById(R.id.timePicker);
        timeText.setKeyListener(null);
        timePickerBtn.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog dialog = new TimePickerDialog(requireContext(),
                    (pickerView, hour, minute) -> {
                        String formatted = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                        timeText.setText(formatted);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true);
            dialog.show();
        });

        // LOAD DATA
        if (getArguments() != null) {
            documentId = getArguments().getString("documentId");

            if (documentId != null && !documentId.isEmpty()) {
                fetchReportData(documentId);
            } else {
                Toast.makeText(getContext(), "Invalid document ID", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        }

        // UPDATE BUTTON CLICK
        updateBtn.setOnClickListener(v -> {
            if (documentId != null && !documentId.isEmpty()) {
                updateReport(documentId);
            } else {
                Toast.makeText(getContext(), "Cannot update: no document ID", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            Objects.requireNonNull(getDialog().getWindow()).setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void fetchReportData(String docId) {
        db.collection("lostItems").document(docId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {

                        itemLostText.setText(document.getString("itemLost"));
                        brandText.setText(document.getString("brand"));
                        additionalInfoText.setText(document.getString("additionalInfo"));

                        // SET LAST SEEN DROPDOWN
                        lastSeenDropdown.setText(document.getString("lastSeen"), false);

                        moreInfoText.setText(document.getString("moreInfo"));
                        firstNameText.setText(document.getString("firstName"));
                        lastNameText.setText(document.getString("lastName"));
                        phoneNumber.setText(document.getString("phone"));
                        dateText.setText(document.getString("date"));
                        timeText.setText(document.getString("time"));
                        autoComplete.setText(document.getString("category"), false);

                        String type = document.getString("reportType");
                        if (type != null) {
                            if (type.equalsIgnoreCase("Found")) {
                                itemLabel.setText("Item Found");
                                dateLabel.setText("Date Found");
                                timeLabel.setText("Time Found");
                            } else {
                                itemLabel.setText("Item Lost");
                                dateLabel.setText("Date Lost");
                                timeLabel.setText("Time Lost");
                            }
                        }

                    } else {
                        Toast.makeText(getContext(), "Report not found.", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load data.", Toast.LENGTH_SHORT).show();
                    dismiss();
                });
    }

    private void updateReport(String docId) {
        db.collection("lostItems").document(docId).update(
                "itemLost", itemLostText.getText().toString(),
                "brand", brandText.getText().toString(),
                "additionalInfo", additionalInfoText.getText().toString(),
                "lastSeen", lastSeenDropdown.getText().toString(),   // FIXED
                "moreInfo", moreInfoText.getText().toString(),
                "firstName", firstNameText.getText().toString(),
                "lastName", lastNameText.getText().toString(),
                "phone", phoneNumber.getText().toString(),
                "date", dateText.getText().toString(),
                "time", timeText.getText().toString(),
                "category", autoComplete.getText().toString()
        ).addOnSuccessListener(unused -> {
            Toast.makeText(getContext(), "Report updated successfully.", Toast.LENGTH_SHORT).show();
            dismiss();

            if (getActivity() != null) {
                getActivity().finish();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to update report.", Toast.LENGTH_SHORT).show()
        );
    }
}
