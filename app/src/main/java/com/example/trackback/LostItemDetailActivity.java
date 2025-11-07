package com.example.trackback;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LostItemDetailActivity extends AppCompatActivity {

    private TextView itemLostText, categoryText, brandText, dateText, timeText,
            additionalInfoText, lastSeenText, moreInfoText, phoneNumberText,
            firstNameText, lastNameText, itemLabel, dateLabel, timeLabel, locationLabel;

    private ImageView itemImageView, editBtn;
    private LinearLayout backBtn, markAsFoundBtn;
    private FrameLayout deleteBtn;
    private String documentId;
    private String imageUrl;
    private String currentReportType;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lost_item_detail);

        // Initialize views
        itemLostText = findViewById(R.id.itemLostText);
        categoryText = findViewById(R.id.categoryText);
        brandText = findViewById(R.id.brandText);
        dateText = findViewById(R.id.dateText);
        timeText = findViewById(R.id.timeText);
        additionalInfoText = findViewById(R.id.additionalInfoText);
        lastSeenText = findViewById(R.id.lastSeenText);
        moreInfoText = findViewById(R.id.moreInfoText);
        phoneNumberText = findViewById(R.id.phoneNumberText);
        firstNameText = findViewById(R.id.firstNameText);
        lastNameText = findViewById(R.id.lastNameText);
        itemImageView = findViewById(R.id.itemImageView);

        // Initialize labels
        itemLabel = findViewById(R.id.itemLabel);
        dateLabel = findViewById(R.id.dateLabel);
        timeLabel = findViewById(R.id.timeLabel);
        locationLabel = findViewById(R.id.locationLabel);

        // Initialize buttons
        editBtn = findViewById(R.id.editBtn);
        backBtn = findViewById(R.id.backBtn);
        markAsFoundBtn = findViewById(R.id.markAsFoundBtn);
        deleteBtn = findViewById(R.id.deleteBtn);

        // Get extras from Intent
        documentId = getIntent().getStringExtra("documentId");

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
        phoneNumberText.setText(getIntent().getStringExtra("phoneNumber"));

        // Load image
        imageUrl = getIntent().getStringExtra("itemImageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.item_default)
                    .into(itemImageView);
        } else {
            itemImageView.setImageResource(R.drawable.item_default);
        }

        // Fetch data from Firestore if needed
        if (documentId != null && !documentId.isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("lostItems")
                    .document(documentId)
                    .get()
                    .addOnSuccessListener(this::updateItemFromFirestore)
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load report.", Toast.LENGTH_SHORT).show());
        }

        // Handle Back Button Click
        backBtn.setOnClickListener(v -> onBackPressed());

        // Handle Edit Button Click
        editBtn.setOnClickListener(v -> {
            if (documentId != null && !documentId.isEmpty()) {
                dialogLost_edit_Fragment editDialog = new dialogLost_edit_Fragment();
                Bundle args = new Bundle();
                args.putString("documentId", documentId);
                editDialog.setArguments(args);
                editDialog.show(getSupportFragmentManager(), "EditDialog");
            } else {
                Toast.makeText(this, "Document ID not found", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle Mark As Found Button Click
        markAsFoundBtn.setOnClickListener(v -> {
            if (documentId != null && !documentId.isEmpty()) {
                markItemAsFound();
            } else {
                Toast.makeText(this, "Document ID not found", Toast.LENGTH_SHORT).show();
            }
        });

        // Handle Delete Button Click
        deleteBtn.setOnClickListener(v -> {
            if (documentId != null && !documentId.isEmpty()) {
                deleteItem();
            } else {
                Toast.makeText(this, "Document ID not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateItemFromFirestore(DocumentSnapshot doc) {
        if (doc.exists()) {
            imageUrl = doc.getString("itemImageUrl");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.item_default)
                        .into(itemImageView);
            }

            currentReportType = doc.getString("reportType");

            // Update UI based on report type
            if (currentReportType != null) {
                if (currentReportType.equalsIgnoreCase("Found")) {
                    // Update labels for Found items
                    itemLabel.setText("Item Found: ");
                    dateLabel.setText("Date Found: ");
                    timeLabel.setText("Time Found: ");
                    locationLabel.setText("Found At: ");
                    itemLostText.setText(doc.getString("itemLost"));

                    // Hide "Mark As Found" button if already found
                    markAsFoundBtn.setVisibility(View.GONE);
                } else if (currentReportType.equalsIgnoreCase("Lost")) {
                    // Update labels for Lost items
                    itemLabel.setText("Item Lost: ");
                    dateLabel.setText("Date Lost: ");
                    timeLabel.setText("Time Lost: ");
                    locationLabel.setText("Lost At: ");
                    itemLostText.setText(doc.getString("itemLost"));

                    // Show "Mark As Found" button
                    markAsFoundBtn.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void markItemAsFound() {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Mark as Found")
                .setMessage("Are you sure you want to mark this item as found?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Update Firestore document
                    FirebaseFirestore.getInstance()
                            .collection("lostItems")
                            .document(documentId)
                            .update("reportType", "Found")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Item marked as found!", Toast.LENGTH_SHORT).show();

                                // Update UI
                                currentReportType = "Found";
                                String currentText = itemLostText.getText().toString();
                                if (currentText.startsWith("Item Lost: ")) {
                                    itemLostText.setText(currentText.replace("Item Lost: ", "Item Found: "));
                                }
                                markAsFoundBtn.setVisibility(View.GONE);

                                // Go back to refresh the list
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteItem() {
        // Show confirmation dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Post")
                .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete from Firestore
                    FirebaseFirestore.getInstance()
                            .collection("lostItems")
                            .document(documentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Post deleted successfully", Toast.LENGTH_SHORT).show();
                                // Go back to previous screen
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}