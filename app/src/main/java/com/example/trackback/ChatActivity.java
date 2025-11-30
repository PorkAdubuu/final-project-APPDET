package com.example.trackback;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
    private String chatId; // NEW: Store the chat document ID
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

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Force adjustResize
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        );

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

        // NEW: Generate chatId
        chatId = currentUserId.compareTo(receiverId) < 0
                ? currentUserId + "_" + receiverId
                : receiverId + "_" + currentUserId;

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

        editMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                recyclerView.postDelayed(() -> {
                    editMessage.requestFocus();
                    if (messageAdapter.getItemCount() > 0) {
                        recyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                    }
                }, 300);
            }
        });

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        handleImageSelected(uri);
                    }
                }
        );

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
                                        sendImageMessage(imageUri);
                                    }
                                });
                    }
                });
    }

    private void sendImageMessage(Uri imageUri) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage("Sending image...");
        builder.setCancelable(false);
        loadingDialog = builder.create();
        loadingDialog.show();

        String fileName = "chat_images/" + currentUserId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(fileName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        String imageUrl = downloadUri.toString();
                        Map<String, Object> messageData = new HashMap<>();
                        messageData.put("senderId", currentUserId);
                        messageData.put("receiverId", receiverId);
                        messageData.put("messageText", ""); // Changed from "message" to "messageText"
                        messageData.put("imageUrl", imageUrl);
                        messageData.put("timestamp", Timestamp.now());
                        messageData.put("isDelivered", true);
                        messageData.put("isSystemMessage", false); // NEW

                        // NEW: Save to subcollection
                        firestore.collection("chats")
                                .document(chatId)
                                .collection("messages")
                                .add(messageData)
                                .addOnSuccessListener(documentReference -> {
                                    if (loadingDialog != null && loadingDialog.isShowing()) {
                                        loadingDialog.dismiss();
                                    }
                                    Toast.makeText(this, "Image sent!", Toast.LENGTH_SHORT).show();
                                    if (messages.size() > 0) {
                                        recyclerView.post(() -> recyclerView.smoothScrollToPosition(messages.size() - 1));
                                    }
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
                });
    }

    private void markMessagesAsRead() {
        // NEW: Mark messages as read in subcollection
        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereEqualTo("senderId", receiverId)
                .whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("isDelivered", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                        doc.getReference().update(
                                "isDelivered", true,
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

    private void actuallySendMessage(String text) {
        Message message = new Message(currentUserId, receiverId, text, Timestamp.now());

        // NEW: Save to subcollection
        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message);

        if (messages.size() > 0) {
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(messages.size() - 1));
        }
        updateChatList(text);
    }

    // UPDATED: Load messages from subcollection
    private void loadMessages() {
        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        Log.e("ChatActivity", "Error loading messages: " + error);
                        return;
                    }

                    for (DocumentChange dc : value.getDocumentChanges()) {
                        Message msg = new Message(); // Create empty message

                        // Manually map all fields from Firestore
                        Map<String, Object> data = dc.getDocument().getData();

                        msg.setMessageText((String) data.get("messageText"));
                        msg.setSenderId((String) data.get("senderId"));
                        msg.setReceiverId((String) data.get("receiverId"));
                        msg.setImageUrl((String) data.get("imageUrl"));
                        msg.setTimestamp((Timestamp) data.get("timestamp"));

                        // CRITICAL: Explicitly handle isSystemMessage
                        Boolean isSystemMessage = (Boolean) data.get("isSystemMessage");
                        msg.setSystemMessage(isSystemMessage != null && isSystemMessage);

                        Boolean isDelivered = (Boolean) data.get("isDelivered");
                        msg.setDelivered(isDelivered != null && isDelivered);

                        // LOG FOR DEBUGGING
                        Log.d("ChatActivity", "=================================");
                        Log.d("ChatActivity", "Loading message from Firestore:");
                        Log.d("ChatActivity", "  Text: " + msg.getMessageText());
                        Log.d("ChatActivity", "  isSystemMessage from Firestore: " + data.get("isSystemMessage"));
                        Log.d("ChatActivity", "  isSystemMessage() method: " + msg.isSystemMessage());
                        Log.d("ChatActivity", "  SenderId: " + msg.getSenderId());
                        Log.d("ChatActivity", "=================================");

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
                                for (int i = 0; i < messages.size(); i++) {
                                    if (messages.get(i).equals(msg)) {
                                        messages.set(i, msg);
                                        break;
                                    }
                                }
                                break;
                            case REMOVED:
                                messages.removeIf(m -> m.equals(msg));
                                break;
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

        // NEW: Save to subcollection
        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message);

        if (messages.size() > 0) {
            recyclerView.post(() -> {
                recyclerView.smoothScrollToPosition(messages.size() - 1);
            });
        }
        updateChatList(text);
    }

    private void checkIfBlockedBeforeSending(String messageText) {
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

    // NEW: Update to use "chats" collection instead of "chatList"
    private void updateChatList(String lastMessage) {
        android.util.Log.d("ChatActivity", "updateChatList() called");
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

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

                    // Create/Update chat document in "chats" collection
                    Map<String, Object> chatData = new HashMap<>();
                    chatData.put("users", Arrays.asList(currentUserId, receiverId));
                    chatData.put("lastMessage", lastMessage);
                    chatData.put("lastMessageTime", Timestamp.now());

                    firestore.collection("chats")
                            .document(chatId)
                            .set(chatData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                android.util.Log.d("ChatActivity", "Chat updated successfully");
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("ChatActivity", "Error updating chat", e);
                            });

                    // Also update chatList for compatibility
                    boolean currentUserIsUser1 = currentUserId.compareTo(receiverId) < 0;
                    Map<String, Object> chatListData = new HashMap<>();
                    chatListData.put("participants", Arrays.asList(currentUserId, receiverId));
                    chatListData.put("lastMessage", lastMessage);
                    chatListData.put("timestamp", Timestamp.now());
                    chatListData.put("lastSenderId", currentUserId);

                    if (currentUserIsUser1) {
                        chatListData.put("user1Id", currentUserId);
                        chatListData.put("user1Name", currentUserName);
                        chatListData.put("user1ProfileUrl", currentUserProfileUrl);
                        chatListData.put("user2Id", receiverId);
                        chatListData.put("user2Name", receiverName != null ? receiverName : "User");
                        chatListData.put("user2ProfileUrl", receiverProfileUrl != null ? receiverProfileUrl : "");
                    } else {
                        chatListData.put("user1Id", receiverId);
                        chatListData.put("user1Name", receiverName != null ? receiverName : "User");
                        chatListData.put("user1ProfileUrl", receiverProfileUrl != null ? receiverProfileUrl : "");
                        chatListData.put("user2Id", currentUserId);
                        chatListData.put("user2Name", currentUserName);
                        chatListData.put("user2ProfileUrl", currentUserProfileUrl);
                    }

                    chatListData.put("unread", true);

                    firestore.collection("chatList")
                            .document(chatId)
                            .set(chatListData, com.google.firebase.firestore.SetOptions.merge());
                });
    }

    private void markChatAsRead() {
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