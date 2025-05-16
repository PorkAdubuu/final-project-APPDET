package com.example.trackback;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.app.TimePickerDialog;
import android.widget.TimePicker;
import java.util.Calendar;

public class ReportlostActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportlost);

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
                "Electronics", "Documents", "Clothing", "Accessories", "Bags",
                "Wallets", "Keys", "Jewelry", "Books", "School Supplies",
                "ID Cards", "Mobile Phones", "Umbrellas", "Eyeglasses", "Others"
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

        findViewById(R.id.publishBtn).setOnClickListener(v -> publishLostItem(
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
                phoneNumber.getText().toString()

        ));
    }

    private void publishLostItem(String itemLost, String category, String brand, String date,
                                 String time, String additionalInfo, String lastSeen,
                                 String moreInfo, String firstName, String lastName, String phone) {

        String profileUrl = "";
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getPhotoUrl() != null) {
            profileUrl = mAuth.getCurrentUser().getPhotoUrl().toString();
        }

        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();  // <-- get current user's UID

            LostItem lostItem = new LostItem(itemLost, category, brand, date, time, additionalInfo,
                    lastSeen, moreInfo, firstName, lastName, phone, profileUrl, userId);

            db.collection("lostItems")
                    .add(lostItem)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Lost item reported successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to report item. Try again.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
        }
    }

    public static class LostItem {
        private String itemLost;
        private String category;
        private String brand;
        private String date;
        private String time;
        private String additionalInfo;
        private String lastSeen;
        private String moreInfo;
        private String firstName;
        private String lastName;
        private String phone;
        private String profileUrl;
        private String userId;  // added userId field
        private Timestamp timestamp;

        public LostItem() {
            // Default constructor required for Firestore
        }

        public LostItem(String itemLost, String category, String brand, String date, String time,
                        String additionalInfo, String lastSeen, String moreInfo, String firstName,
                        String lastName, String phone, String profileUrl, String userId) {
            this.itemLost = itemLost;
            this.category = category;
            this.brand = brand;
            this.date = date;
            this.time = time;
            this.additionalInfo = additionalInfo;
            this.lastSeen = lastSeen;
            this.moreInfo = moreInfo;
            this.firstName = firstName;
            this.lastName = lastName;
            this.phone = phone;
            this.profileUrl = profileUrl;
            this.userId = userId;
            this.timestamp = Timestamp.now();
        }

        // Getters
        public String getItemLost() { return itemLost; }
        public String getCategory() { return category; }
        public String getBrand() { return brand; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getAdditionalInfo() { return additionalInfo; }
        public String getLastSeen() { return lastSeen; }
        public String getMoreInfo() { return moreInfo; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getPhone() { return phone; }
        public String getProfileUrl() { return profileUrl; }
        public String getUserId() { return userId; }
        public Timestamp getTimestamp() { return timestamp; }
    }
}
