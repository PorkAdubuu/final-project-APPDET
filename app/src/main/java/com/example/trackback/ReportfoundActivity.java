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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReportfoundActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Uri selectedImageUri;
    private EditText fileNameText;
    private android.app.AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportfound);

        fileNameText = findViewById(R.id.fileNameText);
        ImageView uploadImageBtn = findViewById(R.id.uploadImageBtn);

        uploadImageBtn.setOnClickListener(v -> openImageChooser());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        EditText itemLostText = findViewById(R.id.itemLostText);
        EditText brandText = findViewById(R.id.brandText);
        EditText additionalInfoText = findViewById(R.id.additionalInfoText);
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

        // Category dropdown
        String[] categories = {
                "Gadgets", "Personal Belongings", "Bags", "Accessories",
                "Clothing", "School Supplies", "Drinkware", "Others"
        };
        AutoCompleteTextView autoComplete = findViewById(R.id.category);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, categories);
        autoComplete.setAdapter(categoryAdapter);

        // Location dropdown
        AutoCompleteTextView locationDropdown = findViewById(R.id.locationDropdown);

        if (locationDropdown != null) {
            String[] locations = {
                    "Umak Oval", "HPSB", "Admin Building", "Academic Building 1",
                    "Academic Building 2", "Library", "Cafeteria"
            };

            ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(
                    this,
                    R.layout.dropdown_item,  // Use consistent layout
                    locations
            );

            locationDropdown.setAdapter(locationAdapter);
        } else {
            Log.e("ReportfoundActivity", "locationDropdown not found in layout!");
        }

        datePickerBtn.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date Found")
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
            moreInfoText.setText("");
            firstNameText.setText("");
            lastNameText.setText("");
            phoneNumber.setText("");
            if (locationDropdown != null) {
                locationDropdown.setText("");
            }
            fileNameText.setText("");
            selectedImageUri = null;
        });

        findViewById(R.id.publishBtn).setOnClickListener(v -> {
            String userEmail = "";
            if (mAuth.getCurrentUser() != null) {
                userEmail = mAuth.getCurrentUser().getEmail();
            }

            String location = "";
            if (locationDropdown != null) {
                location = locationDropdown.getText().toString();
            }

            publishFoundItem(
                    itemLostText.getText().toString(),
                    autoComplete.getText().toString(),
                    brandText.getText().toString(),
                    dateText.getText().toString(),
                    timeText.getText().toString(),
                    additionalInfoText.getText().toString(),
                    location,  // Use location from dropdown
                    moreInfoText.getText().toString(),
                    firstNameText.getText().toString(),
                    lastNameText.getText().toString(),
                    phoneNumber.getText().toString(),
                    userEmail
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

    private void publishFoundItem(String itemFound, String category, String brand, String date,
                                  String time, String additionalInfo, String foundAt,
                                  String moreInfo, String firstName, String lastName, String phone,
                                  String email) {

        // Validation
        if (itemFound.isEmpty() || category.isEmpty() || brand.isEmpty() || date.isEmpty() ||
                time.isEmpty() || additionalInfo.isEmpty() || foundAt.isEmpty() ||
                moreInfo.isEmpty() || firstName.isEmpty() || lastName.isEmpty() ||
                phone.isEmpty() || email.isEmpty()) {

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
                        .getReference("found_images/" + docId + ".jpg");

                storageRef.putFile(selectedImageUri)
                        .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();

                            LostItem foundItem = new LostItem(
                                    itemFound, category, brand, date, time, additionalInfo,
                                    foundAt, moreInfo, firstName, lastName, phone,
                                    email, profileUrl, imageUrl, userId, "FOUND"  // CHANGED: Use "FOUND" uppercase
                            );

                            foundItem.setDocumentId(docId);

                            db.collection("lostItems").document(docId).set(foundItem)
                                    .addOnSuccessListener(aVoid -> {
                                        // Create notification for Found item
                                        NotificationModel notification = new NotificationModel(
                                                docId,
                                                firstName,
                                                lastName,
                                                profileUrl,
                                                date,
                                                time,
                                                "Found",
                                                false,
                                                "active"
                                        );

                                        // Send notification to all users except current user
                                        db.collection("users").get().addOnSuccessListener(querySnapshot -> {
                                                    for (DocumentSnapshot userDoc : querySnapshot.getDocuments()) {
                                                        String otherUserId = userDoc.getId();

                                                        if (!otherUserId.equals(userId)) {
                                                            String notifId = db.collection("users")
                                                                    .document(otherUserId)
                                                                    .collection("notifications")
                                                                    .document().getId();

                                                            notification.setNotificationDocId(notifId);

                                                            db.collection("users").document(otherUserId)
                                                                    .collection("notifications").document(notifId)
                                                                    .set(notification)
                                                                    .addOnSuccessListener(aVoid2 -> {
                                                                        Log.d("Notification", "Notification sent to user: " + otherUserId);
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Log.e("Notification", "Failed to send notification to user: " + otherUserId, e);
                                                                    });
                                                        }
                                                    }

                                                    dismissLoadingDialog();
                                                    showSuccessDialog();

                                                    // Clear all fields
                                                    ((EditText) findViewById(R.id.itemLostText)).setText("");
                                                    ((AutoCompleteTextView) findViewById(R.id.category)).setText("");
                                                    ((EditText) findViewById(R.id.brandText)).setText("");
                                                    ((EditText) findViewById(R.id.dateText)).setText("");
                                                    ((EditText) findViewById(R.id.timeText)).setText("");
                                                    ((EditText) findViewById(R.id.additionalInfoText)).setText("");
                                                    ((EditText) findViewById(R.id.moreInfoText)).setText("");
                                                    ((EditText) findViewById(R.id.firstNameText)).setText("");
                                                    ((EditText) findViewById(R.id.lastNameText)).setText("");
                                                    ((EditText) findViewById(R.id.phoneNumber)).setText("");

                                                    AutoCompleteTextView locationDropdown = findViewById(R.id.locationDropdown);
                                                    if (locationDropdown != null) {
                                                        locationDropdown.setText("");
                                                    }

                                                    fileNameText.setText("");
                                                    selectedImageUri = null;
                                                })
                                                .addOnFailureListener(e -> {
                                                    dismissLoadingDialog();
                                                    Log.e("Notification", "Error getting users for notifications", e);
                                                    Toast.makeText(this, "Error sending notifications: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                });
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            String fileName = selectedImageUri.getLastPathSegment();
            fileNameText.setText(fileName);
        }
    }

    private void showSuccessDialog() {
        report_success_dialog dialog = new report_success_dialog();
        dialog.show(getSupportFragmentManager(), "report_success");
    }
}