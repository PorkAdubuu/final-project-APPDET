package com.example.trackback;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReportlostActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Uri selectedImageUri;
    private EditText fileNameText;
    private android.app.AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportlost);

        fileNameText = findViewById(R.id.fileNameText);
        ImageView uploadImageBtn = findViewById(R.id.uploadImageBtn);

        uploadImageBtn.setOnClickListener(v -> openImageChooser());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        EditText itemLostText = findViewById(R.id.itemLostText);
        EditText brandText = findViewById(R.id.brandText);
        EditText additionalInfoText = findViewById(R.id.additionalInfoText);
        EditText lastSeenText = findViewById(R.id.lastSeenText);
        EditText moreInfoText = findViewById(R.id.moreInfoText);
        EditText firstNameText = findViewById(R.id.firstNameText);
        EditText lastNameText = findViewById(R.id.lastNameText);
        EditText phoneNumber = findViewById(R.id.phoneNumber);



        EditText dateText = findViewById(R.id.dateText);
        ImageButton datePickerBtn = findViewById(R.id.datePicker);
        dateText.setKeyListener(null);

        EditText timeText = findViewById(R.id.timeText);
        ImageButton timePickerBtn = findViewById(R.id.timePicker);
        timeText.setKeyListener(null);

        String[] categories = {
                "Gadgets", "Personal Belongings", "Bags", "Accessories",
                "Clothing", "School Supplies", "Drinkware", "Others"
        };
        AutoCompleteTextView autoComplete = findViewById(R.id.category);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, categories);
        autoComplete.setAdapter(adapter);



        datePickerBtn.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date Lost")
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            picker.show(getSupportFragmentManager(), "DATE_PICKER");
            picker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String date = sdf.format(new Date(selection));
                dateText.setText(date);
            });
        });

        timePickerBtn.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, selectedHour, selectedMinute) -> {
                        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                        timeText.setText(formattedTime);
                    }, hour, minute, true);

            timePickerDialog.show();
        });

        LinearLayout backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> onBackPressed());

        Button cancelBtn = findViewById(R.id.cancelBtn);
        cancelBtn.setOnClickListener(v -> {
            itemLostText.setText("");
            autoComplete.setText("");
            brandText.setText("");
            dateText.setText("");
            timeText.setText("");
            additionalInfoText.setText("");
            lastSeenText.setText("");
            moreInfoText.setText("");
            firstNameText.setText("");
            lastNameText.setText("");
            phoneNumber.setText("");

        });

        findViewById(R.id.publishBtn).setOnClickListener(v -> {
            String userEmail = "";
            if (mAuth.getCurrentUser() != null) {
                userEmail = mAuth.getCurrentUser().getEmail();
            }

            publishLostItem(
                    itemLostText.getText().toString(),
                    autoComplete.getText().toString(),
                    brandText.getText().toString(),
                    dateText.getText().toString(),
                    timeText.getText().toString(),
                    additionalInfoText.getText().toString(),
                    lastSeenText.getText().toString(),
                    moreInfoText.getText().toString(),
                    firstNameText.getText().toString(),
                    lastNameText.getText().toString(),
                    phoneNumber.getText().toString(),
                    userEmail  // use email from FirebaseAuth here
            );
        });

    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), 101);
    }

    private void showLoadingDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.loading_dialog, null);
        builder.setView(view);
        builder.setCancelable(false);
        loadingDialog = builder.create();
        loadingDialog.show();

        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void publishLostItem(String itemLost, String category, String brand, String date,
                                 String time, String additionalInfo, String lastSeen,
                                 String moreInfo, String firstName, String lastName, String phone,
                                 String email) {  // added email param

        if (itemLost.isEmpty() || category.isEmpty() || brand.isEmpty() || date.isEmpty() ||
                time.isEmpty() || additionalInfo.isEmpty() || lastSeen.isEmpty() ||
                moreInfo.isEmpty() || firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty() || email.isEmpty()) {

            Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            String profileUrl = mAuth.getCurrentUser().getPhotoUrl() != null ?
                    mAuth.getCurrentUser().getPhotoUrl().toString() : "";

            if (selectedImageUri != null) {
                showLoadingDialog();

                String docId = db.collection("lostItems").document().getId();
                StorageReference storageRef = FirebaseStorage.getInstance()
                        .getReference("lost_images/" + docId + ".jpg");

                storageRef.putFile(selectedImageUri)
                        .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();

                            LostItem lostItem = new LostItem(
                                    itemLost, category, brand, date, time, additionalInfo,
                                    lastSeen, moreInfo, firstName, lastName, phone,
                                    email, profileUrl, imageUrl, userId, "Lost"
                            );

                            lostItem.setDocumentId(docId);

                            db.collection("lostItems").document(docId).set(lostItem)
                                    .addOnSuccessListener(aVoid -> {
                                        dismissLoadingDialog();
                                        showSuccessDialog();

                                        // Clear all fields except email (reset to logged user email)
                                        ((EditText) findViewById(R.id.itemLostText)).setText("");
                                        ((AutoCompleteTextView) findViewById(R.id.category)).setText("");
                                        ((EditText) findViewById(R.id.brandText)).setText("");
                                        ((EditText) findViewById(R.id.dateText)).setText("");
                                        ((EditText) findViewById(R.id.timeText)).setText("");
                                        ((EditText) findViewById(R.id.additionalInfoText)).setText("");
                                        ((EditText) findViewById(R.id.lastSeenText)).setText("");
                                        ((EditText) findViewById(R.id.moreInfoText)).setText("");
                                        ((EditText) findViewById(R.id.firstNameText)).setText("");
                                        ((EditText) findViewById(R.id.lastNameText)).setText("");
                                        ((EditText) findViewById(R.id.phoneNumber)).setText("");

                                        fileNameText.setText("");
                                        selectedImageUri = null;
                                    })
                                    .addOnFailureListener(e -> {
                                        dismissLoadingDialog();
                                        Toast.makeText(this, "Error uploading report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });

                        })).addOnFailureListener(e -> {
                            dismissLoadingDialog();
                            Toast.makeText(this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "Please select an image.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSuccessDialog() {
        report_success_dialog dialog = new report_success_dialog();
        dialog.show(getSupportFragmentManager(), "report_success");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                String fileName = selectedImageUri.getLastPathSegment();
                fileNameText.setText(fileName);
            }
        }
    }
}
