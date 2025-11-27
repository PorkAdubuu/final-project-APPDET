package com.example.trackback;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LostItemDetailsDialog extends DialogFragment {

    private static final String TAG = "LostItemDialog";
    private ListLostItem lostItem;

    public LostItemDetailsDialog() {
        // Required empty public constructor
    }

    public static LostItemDetailsDialog newInstance(ListLostItem item) {
        LostItemDetailsDialog dialog = new LostItemDetailsDialog();
        Bundle args = new Bundle();
        args.putSerializable("lostItem", item);
        dialog.setArguments(args);
        return dialog;
    }

    public static LostItemDetailsDialog newInstance(String documentId) {
        LostItemDetailsDialog dialog = new LostItemDetailsDialog();
        Bundle args = new Bundle();
        args.putString("documentId", documentId);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_lost_item_details, null);

        if (getArguments() != null) {
            if (getArguments().containsKey("lostItem")) {
                lostItem = (ListLostItem) getArguments().getSerializable("lostItem");
                setupDialogView(view);
            } else if (getArguments().containsKey("documentId")) {
                String documentId = getArguments().getString("documentId");
                fetchLostItemData(documentId, view);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        return dialog;
    }

    private void fetchLostItemData(String documentId, View view) {
        FirebaseFirestore.getInstance()
                .collection("lostItems")
                .document(documentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        lostItem = snapshot.toObject(ListLostItem.class);
                        if (lostItem != null) {
                            setupDialogView(view);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load item details", Toast.LENGTH_SHORT).show()
                );
    }

    private void setupDialogView(View view) {
        if (lostItem == null) {
            Toast.makeText(requireContext(), "Error: Item data not loaded", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        boolean isFound = lostItem.getReportType() != null && lostItem.getReportType().equalsIgnoreCase("Found");

        ImageView itemImageView = view.findViewById(R.id.itemImageView);
        Glide.with(requireContext())
                .load(lostItem.getItemImageUrl())
                .placeholder(R.drawable.item_default)
                .into(itemImageView);

        itemImageView.setOnClickListener(v -> {
            FullImagePreviewDialog previewDialog = new FullImagePreviewDialog(lostItem.getItemImageUrl());
            previewDialog.show(getParentFragmentManager(), "fullImagePreview");
        });

        ((TextView) view.findViewById(R.id.itemLabel)).setText("Item " + (isFound ? "Found:" : "Lost:"));
        ((TextView) view.findViewById(R.id.itemLostText)).setText(lostItem.getItemLost() != null ? lostItem.getItemLost() : "N/A");
        ((TextView) view.findViewById(R.id.dateLabel)).setText("Date " + (isFound ? "Found:" : "Lost:"));
        ((TextView) view.findViewById(R.id.dateText)).setText(lostItem.getDate() != null ? lostItem.getDate() : "N/A");
        ((TextView) view.findViewById(R.id.timeLabel)).setText("Time " + (isFound ? "Found:" : "Lost:"));
        ((TextView) view.findViewById(R.id.timeText)).setText(lostItem.getTime() != null ? lostItem.getTime() : "N/A");
        ((TextView) view.findViewById(R.id.locationLabel)).setText(isFound ? "Found At:" : "Lost At:");
        ((TextView) view.findViewById(R.id.lastSeenText)).setText(lostItem.getLastSeen() != null ? lostItem.getLastSeen() : "N/A");
        ((TextView) view.findViewById(R.id.categoryText)).setText(lostItem.getCategory() != null ? lostItem.getCategory() : "N/A");
        ((TextView) view.findViewById(R.id.brandText)).setText(lostItem.getBrand() != null ? lostItem.getBrand() : "N/A");
        ((TextView) view.findViewById(R.id.additionalInfoText)).setText(lostItem.getAdditionalInfo() != null ? lostItem.getAdditionalInfo() : "N/A");
        ((TextView) view.findViewById(R.id.moreInfoText)).setText(lostItem.getMoreInfo() != null ? lostItem.getMoreInfo() : "N/A");
        ((TextView) view.findViewById(R.id.accountFnameLname)).setText((lostItem.getFirstName() != null ? lostItem.getFirstName() : "") + " " + (lostItem.getLastName() != null ? lostItem.getLastName() : ""));
        ((TextView) view.findViewById(R.id.useremailadd)).setText(lostItem.getEmail() != null ? lostItem.getEmail() : "N/A");
        ((TextView) view.findViewById(R.id.phoneText)).setText(lostItem.getPhone() != null ? lostItem.getPhone() : "N/A");

        Glide.with(requireContext())
                .load(lostItem.getProfileUrl())
                .placeholder(R.drawable.def_prof)
                .circleCrop()
                .into((ImageView) view.findViewById(R.id.profileImageView));

        // --- Message Button ---
        Button messageButton = view.findViewById(R.id.messageBtn);

        if (messageButton != null) {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();

            // Check if logged in
            if (mAuth.getCurrentUser() == null) {
                messageButton.setVisibility(View.GONE);
                return;
            }

            String currentUserId = mAuth.getCurrentUser().getUid();
            String receiverId = lostItem.getUserId();

            // Hide message button if the current user is the owner of the post
            if (receiverId == null || receiverId.isEmpty() || currentUserId.equals(receiverId)) {
                messageButton.setVisibility(View.GONE);
            } else {
                messageButton.setVisibility(View.VISIBLE);

                messageButton.setOnClickListener(v -> {
                    String receiverName = ((lostItem.getFirstName() != null ? lostItem.getFirstName() : "") + " " + (lostItem.getLastName() != null ? lostItem.getLastName() : "")).trim();
                    String receiverProfileUrl = lostItem.getProfileUrl() != null ? lostItem.getProfileUrl() : "";

                    if (receiverName.isEmpty()) receiverName = "User";

                    try {
                        Intent intent = new Intent(requireContext(), ChatActivity.class);
                        intent.putExtra("receiverId", receiverId);
                        intent.putExtra("receiverName", receiverName);
                        intent.putExtra("profileImageUrl", receiverProfileUrl);
                        startActivity(intent);
                        dismiss();
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to open chat", e);
                        Toast.makeText(requireContext(), "Error opening chat.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        // --- TEST BUTTON: Send Item Found Message ---
        // You can add a temporary test button or use an existing button
        // For testing, let's add it to any button click - replace with your actual button ID

        Button testButton = view.findViewById(R.id.messageBtn); // TEMPORARILY using message button for testing
        if (testButton != null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            testButton.setOnLongClickListener(v -> {
                Log.d(TAG, "Test button long-clicked - sending Item Found notifications");
                Toast.makeText(requireContext(), "Sending Item Found notifications...", Toast.LENGTH_SHORT).show();
                sendItemFoundNotifications();
                return true;
            });
        }
    }

    private void sendItemFoundNotifications() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null");
            return;
        }

        Log.d(TAG, "Starting to send Item Found notifications for user: " + currentUserId);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Find all chat conversations involving current user
        firestore.collection("chatList")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Found " + querySnapshot.size() + " chat conversations");

                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(requireContext(), "No conversations found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int[] sentCount = {0};
                    querySnapshot.forEach(document -> {
                        Map<String, Object> data = document.getData();
                        if (data == null) {
                            Log.w(TAG, "Document data is null");
                            return;
                        }

                        String user1Id = (String) data.get("user1Id");
                        String user2Id = (String) data.get("user2Id");

                        Log.d(TAG, "Processing chat: user1=" + user1Id + ", user2=" + user2Id);

                        // Determine the other user
                        String otherUserId = currentUserId.equals(user1Id) ? user2Id : user1Id;
                        String otherUserName = currentUserId.equals(user1Id)
                                ? (String) data.get("user2Name")
                                : (String) data.get("user1Name");
                        String otherUserProfile = currentUserId.equals(user1Id)
                                ? (String) data.get("user2ProfileUrl")
                                : (String) data.get("user1ProfileUrl");

                        Log.d(TAG, "Sending to: " + otherUserId + " (" + otherUserName + ")");

                        // Send "Item Found" message
                        sendItemFoundMessage(otherUserId, otherUserName, otherUserProfile);
                        sentCount[0]++;
                    });

                    Toast.makeText(requireContext(),
                            "Sending Item Found to " + sentCount[0] + " user(s)",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query chatList", e);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendItemFoundMessage(String receiverId, String receiverName, String receiverProfileUrl) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getUid();

        if (currentUserId == null || receiverId == null) {
            Log.e(TAG, "Cannot send message: currentUserId or receiverId is null");
            return;
        }

        Log.d(TAG, "Creating Item Found message from " + currentUserId + " to " + receiverId);

        // Create the message
        String messageText = "Item Found";
        Timestamp now = Timestamp.now();

        Message message = new Message(currentUserId, receiverId, messageText, now);

        // Add to messages collection
        firestore.collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Message added successfully: " + documentReference.getId());

                    // Get current user info
                    firestore.collection("users").document(currentUserId)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                String currentUserName = "";
                                String currentUserProfile = "";

                                if (snapshot.exists()) {
                                    String firstName = snapshot.getString("firstName");
                                    String lastName = snapshot.getString("lastName");
                                    currentUserName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                                    currentUserProfile = snapshot.getString("profileImageUrl");

                                    Log.d(TAG, "Current user info: " + currentUserName);
                                }

                                // Update chatList
                                updateChatList(currentUserId, receiverId, currentUserName, currentUserProfile,
                                        receiverName, receiverProfileUrl, messageText, now);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to get current user info", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add message", e);
                    Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateChatList(String currentUserId, String receiverId,
                                String currentUserName, String currentUserProfile,
                                String receiverName, String receiverProfileUrl,
                                String lastMessage, Timestamp timestamp) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Generate chat ID
        String chatId = currentUserId.compareTo(receiverId) < 0
                ? currentUserId + "_" + receiverId
                : receiverId + "_" + currentUserId;

        Log.d(TAG, "Updating chatList with ID: " + chatId);

        // Prepare chatList data
        Map<String, Object> chatData = new HashMap<>();
        chatData.put("participants", Arrays.asList(currentUserId, receiverId));
        chatData.put("lastMessage", lastMessage);
        chatData.put("timestamp", timestamp);
        chatData.put("unread", true);
        chatData.put("lastSenderId", currentUserId);

        // Add user details
        if (currentUserId.compareTo(receiverId) < 0) {
            chatData.put("user1Id", currentUserId);
            chatData.put("user1Name", currentUserName);
            chatData.put("user1ProfileUrl", currentUserProfile);
            chatData.put("user2Id", receiverId);
            chatData.put("user2Name", receiverName);
            chatData.put("user2ProfileUrl", receiverProfileUrl);
        } else {
            chatData.put("user1Id", receiverId);
            chatData.put("user1Name", receiverName);
            chatData.put("user1ProfileUrl", receiverProfileUrl);
            chatData.put("user2Id", currentUserId);
            chatData.put("user2Name", currentUserName);
            chatData.put("user2ProfileUrl", currentUserProfile);
        }

        // Update chatList
        firestore.collection("chatList")
                .document(chatId)
                .set(chatData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "ChatList updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update chatList", e);
                });
    }
}