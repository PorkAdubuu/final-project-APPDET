package com.example.trackback;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MatchDetailActivity extends AppCompatActivity {

    private TextView tvSimilarityScore, tvMatchReason, tvMatchQuality;
    private CardView cardMatchQuality;

    // Lost Item Views
    private ImageView imgLostItem;
    private TextView tvLostTitle, tvLostCategory, tvLostLocation, tvLostDescription, tvLostDate;
    private TextView tvLostReporterName, tvLostReporterEmail, tvLostReporterPhone;
    private Button btnContactLostReporter;
    private LinearLayout lostReporterSection;

    // Found Item Views
    private ImageView imgFoundItem;
    private TextView tvFoundTitle, tvFoundCategory, tvFoundLocation, tvFoundDescription, tvFoundDate;
    private TextView tvFoundReporterName, tvFoundReporterEmail, tvFoundReporterPhone;
    private Button btnContactFoundReporter;
    private LinearLayout foundReporterSection;

    // Match Analysis
    private LinearLayout matchAnalysisLayout;
    private Chip chipCategory, chipLocation, chipKeywords;

    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String lostItemId;
    private String foundItemId;
    private double similarityScore;
    private String matchReason;
    private String profileImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_detail);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initializeViews();
        getIntentData();
        loadMatchDetails();
    }

    private void initializeViews() {
        // Match Info
        tvSimilarityScore = findViewById(R.id.tvSimilarityScore);
        tvMatchReason = findViewById(R.id.tvMatchReason);
        tvMatchQuality = findViewById(R.id.tvMatchQuality);
        cardMatchQuality = findViewById(R.id.cardMatchQuality);

        // Lost Item
        imgLostItem = findViewById(R.id.imgLostItem);
        tvLostTitle = findViewById(R.id.tvLostTitle);
        tvLostCategory = findViewById(R.id.tvLostCategory);
        tvLostLocation = findViewById(R.id.tvLostLocation);
        tvLostDescription = findViewById(R.id.tvLostDescription);
        tvLostDate = findViewById(R.id.tvLostDate);
        tvLostReporterName = findViewById(R.id.tvLostReporterName);
        tvLostReporterEmail = findViewById(R.id.tvLostReporterEmail);
        tvLostReporterPhone = findViewById(R.id.tvLostReporterPhone);
        btnContactLostReporter = findViewById(R.id.btnContactLostReporter);
        lostReporterSection = findViewById(R.id.lostReporterSection);

        // Found Item
        imgFoundItem = findViewById(R.id.imgFoundItem);
        tvFoundTitle = findViewById(R.id.tvFoundTitle);
        tvFoundCategory = findViewById(R.id.tvFoundCategory);
        tvFoundLocation = findViewById(R.id.tvFoundLocation);
        tvFoundDescription = findViewById(R.id.tvFoundDescription);
        tvFoundDate = findViewById(R.id.tvFoundDate);
        tvFoundReporterName = findViewById(R.id.tvFoundReporterName);
        tvFoundReporterEmail = findViewById(R.id.tvFoundReporterEmail);
        tvFoundReporterPhone = findViewById(R.id.tvFoundReporterPhone);
        btnContactFoundReporter = findViewById(R.id.btnContactFoundReporter);
        foundReporterSection = findViewById(R.id.foundReporterSection);

        // Match Analysis
        matchAnalysisLayout = findViewById(R.id.matchAnalysisLayout);
        chipCategory = findViewById(R.id.chipCategory);
        chipLocation = findViewById(R.id.chipLocation);
        chipKeywords = findViewById(R.id.chipKeywords);

        progressBar = findViewById(R.id.progressBar);

        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void getIntentData() {
        Intent intent = getIntent();
        lostItemId = intent.getStringExtra("lostItemId");
        foundItemId = intent.getStringExtra("foundItemId");
        similarityScore = intent.getDoubleExtra("similarityScore", 0.0);
        matchReason = intent.getStringExtra("matchReason");
    }

    private void loadMatchDetails() {
        progressBar.setVisibility(View.VISIBLE);

        // Display basic match info
        displayMatchInfo();

        // Load lost item details
        db.collection("lostItems").document(lostItemId).get()
                .addOnSuccessListener(this::displayLostItem)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading lost item", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });

        // Load found item details
        db.collection("lostItems").document(foundItemId).get()
                .addOnSuccessListener(this::displayFoundItem)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading found item", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void displayMatchInfo() {
        // Similarity score
        int percentage = (int) (similarityScore * 100);
        tvSimilarityScore.setText(percentage + "% Match");

        // Match quality and color
        String quality;
        int backgroundColor;
        int textColor;

        if (percentage >= 70) {
            quality = "Excellent Match";
            backgroundColor = Color.parseColor("#E8F5E9");
            textColor = Color.parseColor("#2E7D32");
        } else if (percentage >= 50) {
            quality = "Strong Match";
            backgroundColor = Color.parseColor("#FFF9C4");
            textColor = Color.parseColor("#F57F17");
        } else if (percentage >= 40) {
            quality = "Good Match";
            backgroundColor = Color.parseColor("#E3F2FD");
            textColor = Color.parseColor("#1976D2");
        } else {
            quality = "Possible Match";
            backgroundColor = Color.parseColor("#F5F5F5");
            textColor = Color.parseColor("#616161");
        }

        tvMatchQuality.setText(quality);
        cardMatchQuality.setCardBackgroundColor(backgroundColor);
        tvMatchQuality.setTextColor(textColor);
        tvSimilarityScore.setTextColor(textColor);

        // Match reason
        if (matchReason != null && !matchReason.isEmpty()) {
            tvMatchReason.setText(matchReason);
        }
    }

    private void displayLostItem(DocumentSnapshot document) {
        if (!document.exists()) return;

        LostItem lostItem = document.toObject(LostItem.class);
        if (lostItem == null) return;

        // Title
        tvLostTitle.setText(lostItem.getItemLost());

        // Category
        if (lostItem.getCategory() != null) {
            tvLostCategory.setText(lostItem.getCategory());
            tvLostCategory.setVisibility(View.VISIBLE);
        } else {
            tvLostCategory.setVisibility(View.GONE);
        }

        // Location
        if (lostItem.getLastSeen() != null) {
            tvLostLocation.setText("Last seen: " + lostItem.getLastSeen());
            tvLostLocation.setVisibility(View.VISIBLE);
        } else {
            tvLostLocation.setVisibility(View.GONE);
        }

        // Description
        if (lostItem.getAdditionalInfo() != null && !lostItem.getAdditionalInfo().isEmpty()) {
            tvLostDescription.setText(lostItem.getAdditionalInfo());
            tvLostDescription.setVisibility(View.VISIBLE);
        } else {
            tvLostDescription.setVisibility(View.GONE);
        }

        // Date
        if (lostItem.getTimestamp() != null) {
            Date date = lostItem.getTimestamp().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
            tvLostDate.setText("Reported: " + sdf.format(date));
        }

        // Image
        if (lostItem.getItemImageUrl() != null && !lostItem.getItemImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(lostItem.getItemImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .into(imgLostItem);
        }

        // Reporter info
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        if (lostItem.getUserId() != null && !lostItem.getUserId().equals(currentUserId)) {
            displayReporterInfo(lostItem, tvLostReporterName, tvLostReporterEmail,
                    tvLostReporterPhone, btnContactLostReporter, lostReporterSection);
        } else {
            lostReporterSection.setVisibility(View.GONE);
        }

        progressBar.setVisibility(View.GONE);
    }

    private void displayFoundItem(DocumentSnapshot document) {
        if (!document.exists()) return;

        LostItem foundItem = document.toObject(LostItem.class);
        if (foundItem == null) return;

        // Title
        tvFoundTitle.setText(foundItem.getItemLost());

        // Category
        if (foundItem.getCategory() != null) {
            tvFoundCategory.setText(foundItem.getCategory());
            tvFoundCategory.setVisibility(View.VISIBLE);
        } else {
            tvFoundCategory.setVisibility(View.GONE);
        }

        // Location
        if (foundItem.getLastSeen() != null) {
            tvFoundLocation.setText("Found at: " + foundItem.getLastSeen());
            tvFoundLocation.setVisibility(View.VISIBLE);
        } else {
            tvFoundLocation.setVisibility(View.GONE);
        }

        // Description
        if (foundItem.getAdditionalInfo() != null && !foundItem.getAdditionalInfo().isEmpty()) {
            tvFoundDescription.setText(foundItem.getAdditionalInfo());
            tvFoundDescription.setVisibility(View.VISIBLE);
        } else {
            tvFoundDescription.setVisibility(View.GONE);
        }

        // Date
        if (foundItem.getTimestamp() != null) {
            Date date = foundItem.getTimestamp().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
            tvFoundDate.setText("Reported: " + sdf.format(date));
        }

        // Image
        if (foundItem.getItemImageUrl() != null && !foundItem.getItemImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(foundItem.getItemImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .into(imgFoundItem);
        }

        // Reporter info
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        if (foundItem.getUserId() != null && !foundItem.getUserId().equals(currentUserId)) {
            displayReporterInfo(foundItem, tvFoundReporterName, tvFoundReporterEmail,
                    tvFoundReporterPhone, btnContactFoundReporter, foundReporterSection);
        } else {
            foundReporterSection.setVisibility(View.GONE);
        }
    }

    private void displayReporterInfo(LostItem item, TextView nameView, TextView emailView,
                                     TextView phoneView, Button contactBtn, LinearLayout section) {
        // Name
        String fullName = item.getFirstName() + " " + item.getLastName();
        nameView.setText(fullName);

        // Email
        if (item.getEmail() != null && !item.getEmail().isEmpty()) {
            emailView.setText(item.getEmail());
            emailView.setVisibility(View.VISIBLE);
        } else {
            emailView.setVisibility(View.GONE);
        }

        // Phone
        if (item.getPhone() != null && !item.getPhone().isEmpty()) {
            phoneView.setText(item.getPhone());
            phoneView.setVisibility(View.VISIBLE);
        } else {
            phoneView.setVisibility(View.GONE);
        }

        // Contact button
        contactBtn.setOnClickListener(v -> showContactOptions(item));

        section.setVisibility(View.VISIBLE);
    }

    private void showContactOptions(LostItem item) {
        // Open chat with the reporter
        openChatWithReporter(item);
    }

    private void openChatWithReporter(LostItem item) {
        if (item.getUserId() == null || item.getUserId().isEmpty()) {
            Toast.makeText(this, "Unable to contact reporter", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "Please sign in to send messages", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if chat already exists
        db.collection("chatList")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String existingChatId = null;

                    // Look for existing chat with this user
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        java.util.List<String> participants = (java.util.List<String>) doc.get("participants");
                        if (participants != null && participants.contains(item.getUserId())) {
                            existingChatId = doc.getId();
                            break;
                        }
                    }

                    if (existingChatId != null) {
                        // Open existing chat
                        openChat(existingChatId, item);
                    } else {
                        // Create new chat
                        createNewChat(item);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error opening chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewChat(LostItem item) {
        String currentUserId = mAuth.getCurrentUser().getUid();

        // Create new chat document
        java.util.Map<String, Object> chatData = new java.util.HashMap<>();
        chatData.put("participants", java.util.Arrays.asList(currentUserId, item.getUserId()));
        chatData.put("lastMessage", "");
        chatData.put("lastSenderId", "");
        chatData.put("timestamp", com.google.firebase.Timestamp.now());
        chatData.put("unread", false);

        db.collection("chatList")
                .add(chatData)
                .addOnSuccessListener(documentReference -> {
                    openChat(documentReference.getId(), item);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error creating chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void openChat(String chatId, LostItem item) {

        db.collection("users")
                .document(item.getUserId())
                .get()
                .addOnSuccessListener(userDoc -> {
                    String profileUrl = null;
                    if (userDoc.exists()) {
                        profileUrl = userDoc.getString("profileUrl");
                    }

                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("chatId", chatId);
                    intent.putExtra("receiverId", item.getUserId());
                    intent.putExtra("receiverName", item.getFirstName() + " " + item.getLastName());
                    intent.putExtra("profileImageUrl", profileUrl);
                    startActivity(intent); 
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }
}