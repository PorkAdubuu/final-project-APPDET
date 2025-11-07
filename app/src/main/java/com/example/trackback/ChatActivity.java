package com.example.trackback;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText editMessage;
    private ImageButton btnSend;
    private TextView receiverNameTextView;
    private ImageView profileImageView;
    private ImageView backBtn;
    private ImageView uploadImageBtn;
    private MessageAdapter messageAdapter;
    private List<Message> messages;
    private String currentUserId;
    private String receiverId;
    private Uri selectedImageUri;
    private android.app.AlertDialog loadingDialog;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private boolean isBlocked = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        recyclerView = findViewById(R.id.recyclerView);
        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);
        receiverNameTextView = findViewById(R.id.receiverNameTextView);
        profileImageView = findViewById(R.id.profileImageView);
        backBtn = findViewById(R.id.backBtn);
        uploadImageBtn = findViewById(R.id.uploadImageBtn);

        backBtn.setOnClickListener(v -> onBackPressed());

        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        currentUserId = FirebaseAuth.getInstance().getUid();
        receiverId = getIntent().getStringExtra("receiverId");
        String receiverName = getIntent().getStringExtra("receiverName");
        String profileImageUrl = getIntent().getStringExtra("profileImageUrl");

        if (receiverId == null) {
            finish();
            return;
        }

        markChatAsRead();
        markMessagesAsRead();

        receiverNameTextView.setText(receiverName);
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.circle_outline)
                    .into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.circle_outline);
        }

        messages = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messages, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        // Initialize image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        handleImageSelected(uri);
                    }
                }
        );

        // Set up upload image button click listener
        uploadImageBtn.setOnClickListener(v -> openGallery());

        loadMessages();

        btnSend.setOnClickListener(v -> {
            String text = editMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                checkIfBlockedBeforeSending(text);
            }
        });
    }

    private void openGallery() {
        try {
            imagePickerLauncher.launch("image/*");
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open gallery", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void handleImageSelected(Uri imageUri) {
        // Check if blocked before sending image
        checkIfBlockedBeforeSendingImage(imageUri);
    }

    private void checkIfBlockedBeforeSendingImage(Uri imageUri) {
        firestore.collection("users")
                .document(currentUserId)
                .collection("blockedUsers")
                .document(receiverId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Toast.makeText(this, "You blocked this account. Unblock to send messages.", Toast.LENGTH_SHORT).show();
                    } else {
                        firestore.collection("users")
                                .document(receiverId)
                                .collection("blockedUsers")
                                .document(currentUserId)
                                .get()
                                .addOnSuccessListener(otherDoc -> {
                                    if (otherDoc.exists()) {
                                        Toast.makeText(this, "You can't send a message. This user has blocked you.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        // Not blocked, proceed to upload
                                        sendImageMessage(imageUri);
                                    }
                                });
                    }
                });
    }

    private void sendImageMessage(Uri imageUri) {
        // Show loading dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage("Sending image...");
        builder.setCancelable(false);
        loadingDialog = builder.create();
        loadingDialog.show();

        // Create a unique filename
        String fileName = "chat_images/" + currentUserId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(fileName);

        // Upload image
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        // Create message with image URL
                        String imageUrl = downloadUri.toString();
                        Map<String, Object> messageData = new HashMap<>();
                        messageData.put("senderId", currentUserId);
                        messageData.put("receiverId", receiverId);
                        messageData.put("message", ""); // Empty text for image messages
                        messageData.put("imageUrl", imageUrl);
                        messageData.put("timestamp", Timestamp.now());
                        messageData.put("isRead", false);

                        firestore.collection("messages")
                                .add(messageData)
                                .addOnSuccessListener(documentReference -> {
                                    if (loadingDialog != null && loadingDialog.isShowing()) {
                                        loadingDialog.dismiss();
                                    }
                                    Toast.makeText(this, "Image sent!", Toast.LENGTH_SHORT).show();
                                    // Scroll to bottom
                                    if (messages.size() > 0) {
                                        recyclerView.post(() -> recyclerView.smoothScrollToPosition(messages.size() - 1));
                                    }
                                    // Update chat list
                                    updateChatList("📷 Image");
                                })
                                .addOnFailureListener(e -> {
                                    if (loadingDialog != null && loadingDialog.isShowing()) {
                                        loadingDialog.dismiss();
                                    }
                                    Toast.makeText(this, "Failed to send image", Toast.LENGTH_SHORT).show();
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    if (loadingDialog != null && loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                })
                .addOnProgressListener(snapshot -> {
                    // Optional: show upload progress
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    android.util.Log.d("ChatActivity", "Upload is " + progress + "% done");
                });
    }

    private void markMessagesAsRead() {
        firestore.collection("messages")
                .whereEqualTo("senderId", receiverId)
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                        doc.getReference().update(
                                "isRead", true,
                                "readAt", Timestamp.now()
                        );
                    }
                    android.util.Log.d("ChatActivity", "Marked " + querySnapshot.size() + " messages as read");
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        markMessagesAsRead();
    }

    private void checkIfBlockedStatus() {
        firestore.collection("users")
                .document(currentUserId)
                .collection("blockedUsers")
                .document(receiverId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        disableMessaging("You blocked this user.");
                    } else {
                        firestore.collection("users")
                                .document(receiverId)
                                .collection("blockedUsers")
                                .document(currentUserId)
                                .get()
                                .addOnSuccessListener(otherDoc -> {
                                    if (otherDoc.exists()) {
                                        disableMessaging("This user blocked you.");
                                    } else {
                                        enableMessaging();
                                    }
                                });
                    }
                });
    }

    private void disableMessaging(String reason) {
        isBlocked = true;
        editMessage.setEnabled(false);
        btnSend.setEnabled(false);
        uploadImageBtn.setEnabled(false);
        editMessage.setHint(reason);
        Toast.makeText(this, reason, Toast.LENGTH_SHORT).show();
    }

    private void enableMessaging() {
        isBlocked = false;
        editMessage.setEnabled(true);
        btnSend.setEnabled(true);
        uploadImageBtn.setEnabled(true);
        editMessage.setHint("Type a message...");
    }

    private void actuallySendMessage(String text) {
        Message message = new Message(currentUserId, receiverId, text, Timestamp.now());
        firestore.collection("messages").add(message);
        if (messages.size() > 0) {
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(messages.size() - 1));
        }
        updateChatList(text);
    }

    private void loadMessages() {
        firestore.collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    for (DocumentChange dc : value.getDocumentChanges()) {
                        Message msg = dc.getDocument().toObject(Message.class);
                        if ((msg.getSenderId().equals(currentUserId) && msg.getReceiverId().equals(receiverId)) ||
                                (msg.getSenderId().equals(receiverId) && msg.getReceiverId().equals(currentUserId))) {
                            switch (dc.getType()) {
                                case ADDED:
                                    boolean exists = false;
                                    for (Message existingMsg : messages) {
                                        if (existingMsg.equals(msg)) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (!exists) {
                                        messages.add(msg);
                                    }
                                    break;
                                case MODIFIED:
                                    break;
                                case REMOVED:
                                    messages.removeIf(m -> m.equals(msg));
                                    break;
                            }
                        }
                    }
                    messageAdapter.notifyDataSetChanged();
                    if (messages.size() > 0) {
                        recyclerView.post(() -> {
                            recyclerView.smoothScrollToPosition(messages.size() - 1);
                        });
                    }
                });
    }

    private void sendMessage(String text) {
        Message message = new Message(currentUserId, receiverId, text, Timestamp.now());
        firestore.collection("messages").add(message);
        if (messages.size() > 0) {
            recyclerView.post(() -> {
                recyclerView.smoothScrollToPosition(messages.size() - 1);
            });
        }
        updateChatList(text);
    }

    private void checkIfBlockedBeforeSending(String messageText) {
        String receiverId = this.receiverId;
        String currentUserId = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("users")
                .document(currentUserId)
                .collection("blockedUsers")
                .document(receiverId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Toast.makeText(this, "You blocked this account. Unblock to send messages.", Toast.LENGTH_SHORT).show();
                    } else {
                        firestore.collection("users")
                                .document(receiverId)
                                .collection("blockedUsers")
                                .document(currentUserId)
                                .get()
                                .addOnSuccessListener(otherDoc -> {
                                    if (otherDoc.exists()) {
                                        Toast.makeText(this, "You can't send a message. This user has blocked you.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        sendMessage(messageText);
                                        editMessage.setText("");
                                    }
                                });
                    }
                });
    }

    private void updateChatList(String lastMessage) {
        android.util.Log.d("ChatActivity", "updateChatList() called");
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String chatId = currentUserId.compareTo(receiverId) < 0 ? currentUserId + "_" + receiverId : receiverId + "_" + currentUserId;
        String receiverName = getIntent().getStringExtra("receiverName");
        String receiverProfileUrl = getIntent().getStringExtra("profileImageUrl");

        firestore.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(currentUserDoc -> {
                    String currentUserName = "User";
                    String currentUserProfileUrl = "";
                    if (currentUserDoc.exists()) {
                        currentUserName = currentUserDoc.getString("fullname");
                        if (currentUserName == null || currentUserName.isEmpty()) {
                            currentUserName = currentUserDoc.getString("fullName");
                        }
                        if (currentUserName == null || currentUserName.isEmpty()) {
                            currentUserName = currentUserDoc.getString("name");
                        }
                        if (currentUserName == null) {
                            currentUserName = "User";
                        }
                        currentUserProfileUrl = currentUserDoc.getString("profileImageUrl");
                        if (currentUserProfileUrl == null) {
                            currentUserProfileUrl = "";
                        }
                    }

                    android.util.Log.d("ChatActivity", "Current user name: " + currentUserName);
                    android.util.Log.d("ChatActivity", "Receiver name: " + receiverName);

                    boolean currentUserIsUser1 = currentUserId.compareTo(receiverId) < 0;
                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("participants", Arrays.asList(currentUserId, receiverId));
                    chatData.put("lastMessage", lastMessage);
                    chatData.put("timestamp", Timestamp.now());
                    chatData.put("lastSenderId", currentUserId);

                    if (currentUserIsUser1) {
                        chatData.put("user1Id", currentUserId);
                        chatData.put("user1Name", currentUserName);
                        chatData.put("user1ProfileUrl", currentUserProfileUrl);
                        chatData.put("user2Id", receiverId);
                        chatData.put("user2Name", receiverName != null ? receiverName : "User");
                        chatData.put("user2ProfileUrl", receiverProfileUrl != null ? receiverProfileUrl : "");
                    } else {
                        chatData.put("user1Id", receiverId);
                        chatData.put("user1Name", receiverName != null ? receiverName : "User");
                        chatData.put("user1ProfileUrl", receiverProfileUrl != null ? receiverProfileUrl : "");
                        chatData.put("user2Id", currentUserId);
                        chatData.put("user2Name", currentUserName);
                        chatData.put("user2ProfileUrl", currentUserProfileUrl);
                    }

                    chatData.put("unread", true);

                    android.util.Log.d("ChatActivity", "Saving to Firestore with user1Name: " + chatData.get("user1Name") + ", user2Name: " + chatData.get("user2Name"));

                    firestore.collection("chatList")
                            .document(chatId)
                            .set(chatData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                android.util.Log.d("ChatActivity", "Chat list updated successfully");
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("ChatActivity", "Error updating chat list", e);
                            });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ChatActivity", "Error fetching current user data", e);
                });
    }

    private void markChatAsRead() {
        String chatId = currentUserId.compareTo(receiverId) < 0 ? currentUserId + "_" + receiverId : receiverId + "_" + currentUserId;
        firestore.collection("chatList")
                .document(chatId)
                .update("unread", false)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("ChatActivity", "Chat marked as read");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ChatActivity", "Failed to mark as read", e);
                });
    }
}
