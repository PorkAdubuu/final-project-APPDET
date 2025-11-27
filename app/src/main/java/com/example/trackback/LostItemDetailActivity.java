package com.example.trackback;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private String itemOwnerId; // NEW: To track the item owner

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

            // NEW: Get the item owner's user ID
            itemOwnerId = doc.getString("userId");

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

                                // NEW: Send system message to all conversations related to this item
                                sendSystemMessageToAllChats();

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

    // NEW METHOD: Send system message to all related chats
    // Add this method to your LostItemDetailActivity

    private void sendSystemMessageToAllChats() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Find all chats where the current user is involved
        db.collection("chats")
                .whereArrayContains("users", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot chatDoc : queryDocumentSnapshots) {
                        String chatId = chatDoc.getId();

                        // Get the users array
                        List<String> users = (List<String>) chatDoc.get("users");
                        if (users != null && users.size() == 2) {
                            // Determine the other user
                            String otherUserId = users.get(0).equals(currentUserId)
                                    ? users.get(1)
                                    : users.get(0);

                            // Send system message to this chat
                            sendSystemMessageToChat(chatId, currentUserId, otherUserId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MarkAsFound", "Failed to find chats: " + e.getMessage());
                });
    }

    private void sendSystemMessageToChat(String chatId, String senderId, String receiverId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create system message data
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("messageText", "The item has been marked as found"); // REMOVED ✓
        systemMessage.put("senderId", senderId);
        systemMessage.put("receiverId", receiverId);
        systemMessage.put("timestamp", com.google.firebase.Timestamp.now());
        systemMessage.put("isSystemMessage", true);
        systemMessage.put("isDelivered", true);

        // DEBUG: Log the system message data
        android.util.Log.d("SystemMessage", "Creating system message with data: " + systemMessage.toString());

        // Add message to the messages subcollection
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(systemMessage)
                .addOnSuccessListener(documentReference -> {
                    android.util.Log.d("SystemMessage", "System message successfully saved!");

                    // Update the chat's lastMessage
                    Map<String, Object> chatUpdate = new HashMap<>();
                    chatUpdate.put("lastMessage", "The item has been marked as found");
                    chatUpdate.put("lastMessageTime", com.google.firebase.Timestamp.now());

                    db.collection("chats")
                            .document(chatId)
                            .update(chatUpdate)
                            .addOnSuccessListener(aVoid -> {
                                android.util.Log.d("SystemMessage", "System message sent to chat: " + chatId);
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("SystemMessage", "Failed to send system message: " + e.getMessage());
                });
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