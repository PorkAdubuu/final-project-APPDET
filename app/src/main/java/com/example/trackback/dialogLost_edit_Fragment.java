package com.example.trackback;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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

    private EditText itemLostText, brandText, additionalInfoText, lastSeenText,
            moreInfoText, firstNameText, lastNameText, phoneNumber, dateText, timeText;
    private AutoCompleteTextView autoComplete;
    private FrameLayout updateBtn;

    private String documentId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dialog_lost_edit_, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        itemLostText = view.findViewById(R.id.itemLostText);
        brandText = view.findViewById(R.id.brandText);
        additionalInfoText = view.findViewById(R.id.additionalInfoText);
        lastSeenText = view.findViewById(R.id.lastSeenText);
        moreInfoText = view.findViewById(R.id.moreInfoText);
        firstNameText = view.findViewById(R.id.firstNameText);
        lastNameText = view.findViewById(R.id.lastNameText);
        phoneNumber = view.findViewById(R.id.phoneNumber);
        dateText = view.findViewById(R.id.dateText);
        timeText = view.findViewById(R.id.timeText);
        autoComplete = view.findViewById(R.id.category);
        updateBtn = view.findViewById(R.id.updateBtn);

        ImageButton datePickerBtn = view.findViewById(R.id.datePicker);
        ImageButton timePickerBtn = view.findViewById(R.id.timePicker);

        // Disable manual input for date and time EditTexts
        dateText.setKeyListener(null);
        timeText.setKeyListener(null);

        // Setup dropdown categories
        String[] categories = {
                "Electronics", "Documents", "Clothing", "Accessories", "Bags",
                "Wallets", "Keys", "Jewelry", "Books", "School Supplies",
                "ID Cards", "Mobile Phones", "Umbrellas", "Eyeglasses", "Others"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, categories);
        autoComplete.setAdapter(adapter);

        // Date picker dialog
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

        // Time picker dialog
        timePickerBtn.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                    (view1, hourOfDay, minute) -> {
                        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                        timeText.setText(formattedTime);
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true);
            timePickerDialog.show();
        });

        // Get documentId from arguments and fetch data
        if (getArguments() != null) {
            documentId = getArguments().getString("documentId");
            if (documentId != null && !documentId.isEmpty()) {
                fetchReportData(documentId);
            } else {
                Toast.makeText(getContext(), "Invalid document ID", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        } else {
            Toast.makeText(getContext(), "No document ID provided", Toast.LENGTH_SHORT).show();
            dismiss();
        }

        // Update button click listener
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
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        itemLostText.setText(documentSnapshot.getString("itemLost"));
                        brandText.setText(documentSnapshot.getString("brand"));
                        additionalInfoText.setText(documentSnapshot.getString("additionalInfo"));
                        lastSeenText.setText(documentSnapshot.getString("lastSeen"));
                        moreInfoText.setText(documentSnapshot.getString("moreInfo"));
                        firstNameText.setText(documentSnapshot.getString("firstName"));
                        lastNameText.setText(documentSnapshot.getString("lastName"));
                        phoneNumber.setText(documentSnapshot.getString("phone"));
                        dateText.setText(documentSnapshot.getString("date"));
                        timeText.setText(documentSnapshot.getString("time"));
                        autoComplete.setText(documentSnapshot.getString("category"), false);
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
                "lastSeen", lastSeenText.getText().toString(),
                "moreInfo", moreInfoText.getText().toString(),
                "firstName", firstNameText.getText().toString(),
                "lastName", lastNameText.getText().toString(),
                "phone", phoneNumber.getText().toString(),
                "date", dateText.getText().toString(),
                "time", timeText.getText().toString(),
                "category", autoComplete.getText().toString()
        ).addOnSuccessListener(unused -> {
            Toast.makeText(getContext(), "Report updated successfully.", Toast.LENGTH_SHORT).show();

            // Close dialog
            dismiss();

            // Close LostItemDetailActivity to go back to FragmentProfile
            if (getActivity() != null) {
                getActivity().finish();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to update report.", Toast.LENGTH_SHORT).show()
        );
    }


}
