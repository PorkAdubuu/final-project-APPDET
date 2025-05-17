package com.example.trackback;

import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.EdgeToEdge;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.firestore.FirebaseFirestore;

public class LostItemDetailActivity extends AppCompatActivity {

    private TextView itemLostText, categoryText, brandText, dateText, timeText,
            additionalInfoText, lastSeenText, moreInfoText, firstNameText, lastNameText, phoneNumberText;

    private String documentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lost_item_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        FrameLayout deleteBtn = findViewById(R.id.deleteBtn);
        deleteBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Report")
                    .setMessage("Are you sure you want to delete this report?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (documentId != null && !documentId.isEmpty()) {
                            FirebaseFirestore.getInstance()
                                    .collection("lostItems")
                                    .document(documentId)
                                    .delete()
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(this, "Report deleted successfully.", Toast.LENGTH_SHORT).show();
                                        finish(); // Close this activity and go back
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Failed to delete report.", Toast.LENGTH_SHORT).show()
                                    );
                        } else {
                            Toast.makeText(this, "Invalid document ID.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Receive documentId from intent extras
        documentId = getIntent().getStringExtra("documentId");
        if (documentId == null || documentId.isEmpty()) {
            Toast.makeText(this, "Invalid or missing document ID", Toast.LENGTH_SHORT).show();
        } else {
            // documentId is valid, proceed normally
        }

        LinearLayout backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> onBackPressed());

        ImageView editBtn = findViewById(R.id.editBtn);
        editBtn.setOnClickListener(v -> {
            dialogLost_edit_Fragment dialogFragment = new dialogLost_edit_Fragment();

            // Pass documentId to dialog fragment
            Bundle args = new Bundle();
            args.putString("documentId", documentId);
            dialogFragment.setArguments(args);

            dialogFragment.show(getSupportFragmentManager(), "edit_dialog");
        });

        // Initialize TextViews (make sure IDs in XML match these)
        itemLostText = findViewById(R.id.itemLostText);
        categoryText = findViewById(R.id.categoryText);
        brandText = findViewById(R.id.brandText);
        dateText = findViewById(R.id.dateText);
        timeText = findViewById(R.id.timeText);
        additionalInfoText = findViewById(R.id.additionalInfoText);
        lastSeenText = findViewById(R.id.lastSeenText);
        moreInfoText = findViewById(R.id.moreInfoText);
        firstNameText = findViewById(R.id.firstNameText);
        lastNameText = findViewById(R.id.lastNameText);
        phoneNumberText = findViewById(R.id.phoneNumberText);

        // Get Intent extras and display
        itemLostText.setText(getIntent().getStringExtra("itemLost"));
        categoryText.setText(getIntent().getStringExtra("category"));
        brandText.setText(getIntent().getStringExtra("brand"));
        dateText.setText(getIntent().getStringExtra("date"));
        timeText.setText(getIntent().getStringExtra("time"));
        additionalInfoText.setText(getIntent().getStringExtra("additionalInfo"));
        lastSeenText.setText(getIntent().getStringExtra("lastSeen"));
        moreInfoText.setText(getIntent().getStringExtra("moreInfo"));
        firstNameText.setText(getIntent().getStringExtra("firstName"));
        lastNameText.setText(getIntent().getStringExtra("lastName"));
        phoneNumberText.setText(getIntent().getStringExtra("phone"));
    }
}
