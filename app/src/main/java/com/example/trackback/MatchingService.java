package com.example.trackback;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class MatchingService {

    private static final String TAG = "MatchingService";
    private FirebaseFirestore db;
    private static final double SIMILARITY_THRESHOLD = 0.3;
    private MatchNotificationHelper notificationHelper;

    public MatchingService() {
        db = FirebaseFirestore.getInstance();
        notificationHelper = new MatchNotificationHelper();
    }

    public void findMatchesForLostItem(String lostItemId, OnMatchesFoundListener listener) {
        Log.d(TAG, "Finding matches for lost item: " + lostItemId);

        db.collection("lostItems").document(lostItemId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    LostItem lostItemData = documentSnapshot.toObject(LostItem.class);
                    if (lostItemData == null) {
                        Log.e(TAG, "Lost item not found");
                        listener.onError("Lost item not found");
                        return;
                    }

                    Item lostItem = convertLostItemToItem(lostItemData);
                    lostItem.setId(documentSnapshot.getId());

                    Log.d(TAG, "Lost item loaded: " + lostItem.getTitle());

                    db.collection("lostItems")
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<Item> foundItems = new ArrayList<>();

                                Log.d(TAG, "Total documents in lostItems: " + querySnapshot.size());

                                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    Boolean isDeleted = doc.getBoolean("isDeleted");
                                    if (isDeleted != null && isDeleted) {
                                        continue;
                                    }

                                    String reportType = doc.getString("reportType");
                                    Log.d(TAG, "Checking document: " + doc.getId() + ", reportType: " + reportType);

                                    if (reportType != null &&
                                            (reportType.equalsIgnoreCase("FOUND") || reportType.equalsIgnoreCase("Found"))) {

                                        LostItem foundItemData = doc.toObject(LostItem.class);
                                        if (foundItemData != null) {
                                            Item foundItem = convertLostItemToItem(foundItemData);
                                            foundItem.setId(doc.getId());
                                            foundItems.add(foundItem);
                                            Log.d(TAG, "Found item added: " + foundItem.getTitle());
                                        }
                                    }
                                }

                                Log.d(TAG, "Total found items: " + foundItems.size());

                                List<ItemMatch> matches = CosineSimilarityMatcher.findMatches(
                                        lostItem, foundItems, SIMILARITY_THRESHOLD
                                );

                                Log.d(TAG, "Matches found: " + matches.size());

                                if (!matches.isEmpty()) {
                                    sendMatchNotifications(lostItemData, matches);
                                }

                                listener.onMatchesFound(matches);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error getting found items", e);
                                listener.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting lost item", e);
                    listener.onError(e.getMessage());
                });
    }

    public void findMatchesForUser(String userId, OnMatchesFoundListener listener) {
        Log.d(TAG, "Finding matches for user: " + userId);

        db.collection("lostItems")
                .get()
                .addOnSuccessListener(lostSnapshot -> {
                    List<Item> lostItems = new ArrayList<>();

                    Log.d(TAG, "Total documents in lostItems: " + lostSnapshot.size());

                    for (DocumentSnapshot doc : lostSnapshot.getDocuments()) {
                        Boolean isDeleted = doc.getBoolean("isDeleted");
                        if (isDeleted != null && isDeleted) {
                            continue;
                        }

                        String docUserId = doc.getString("userId");
                        String reportType = doc.getString("reportType");

                        Log.d(TAG, "Document: " + doc.getId() + ", userId: " + docUserId + ", reportType: " + reportType);

                        if (docUserId != null && docUserId.equals(userId) &&
                                reportType != null &&
                                (reportType.equalsIgnoreCase("LOST") || reportType.equalsIgnoreCase("Lost"))) {

                            LostItem lostItemData = doc.toObject(LostItem.class);
                            if (lostItemData != null) {
                                Item item = convertLostItemToItem(lostItemData);
                                item.setId(doc.getId());
                                lostItems.add(item);
                                Log.d(TAG, "User's lost item added: " + item.getTitle());
                            }
                        }
                    }

                    Log.d(TAG, "User's total lost items: " + lostItems.size());

                    if (lostItems.isEmpty()) {
                        Log.d(TAG, "No lost items found for user");
                        listener.onError("No lost items found");
                        return;
                    }

                    db.collection("lostItems")
                            .get()
                            .addOnSuccessListener(foundSnapshot -> {
                                List<Item> foundItems = new ArrayList<>();

                                for (DocumentSnapshot doc : foundSnapshot.getDocuments()) {
                                    Boolean isDeleted = doc.getBoolean("isDeleted");
                                    if (isDeleted != null && isDeleted) {
                                        continue;
                                    }

                                    String reportType = doc.getString("reportType");

                                    if (reportType != null &&
                                            (reportType.equalsIgnoreCase("FOUND") || reportType.equalsIgnoreCase("Found"))) {

                                        LostItem foundItemData = doc.toObject(LostItem.class);
                                        if (foundItemData != null) {
                                            Item item = convertLostItemToItem(foundItemData);
                                            item.setId(doc.getId());
                                            foundItems.add(item);
                                        }
                                    }
                                }

                                Log.d(TAG, "Total found items: " + foundItems.size());

                                List<ItemMatch> allMatches = new ArrayList<>();
                                for (Item lostItem : lostItems) {
                                    Log.d(TAG, "Finding matches for: " + lostItem.getTitle());
                                    List<ItemMatch> matches = CosineSimilarityMatcher.findMatches(
                                            lostItem, foundItems, SIMILARITY_THRESHOLD
                                    );
                                    Log.d(TAG, "Matches for " + lostItem.getTitle() + ": " + matches.size());
                                    allMatches.addAll(matches);
                                }

                                Log.d(TAG, "Total matches found: " + allMatches.size());
                                listener.onMatchesFound(allMatches);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error getting found items", e);
                                listener.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user's lost items", e);
                    listener.onError(e.getMessage());
                });
    }

    private void sendMatchNotifications(LostItem lostItem, List<ItemMatch> matches) {
        for (ItemMatch match : matches) {
            db.collection("lostItems")
                    .document(match.getFoundItem().getId())
                    .get()
                    .addOnSuccessListener(foundDoc -> {
                        LostItem foundItem = foundDoc.toObject(LostItem.class);
                        if (foundItem != null) {
                            String senderName = foundItem.getFirstName() + " " + foundItem.getLastName();

                            notificationHelper.sendMatchNotification(
                                    lostItem.getUserId(),
                                    lostItem.getItemLost(),
                                    match.getLostItem().getId(),
                                    match.getFoundItem().getId(),
                                    senderName,
                                    foundItem.getProfileUrl()
                            );
                        }
                    });
        }
    }

    private Item convertLostItemToItem(LostItem lostItem) {
        Item item = new Item();
        item.setUserId(lostItem.getUserId());
        item.setType(lostItem.getReportType());
        item.setTitle(lostItem.getItemLost());

        String fullDescription = lostItem.getAdditionalInfo();
        if (lostItem.getBrand() != null && !lostItem.getBrand().isEmpty()) {
            fullDescription = (fullDescription != null ? fullDescription + " " : "") + lostItem.getBrand();
        }
        item.setDescription(fullDescription);

        item.setCategory(lostItem.getCategory());
        item.setLocation(lostItem.getLastSeen());
        item.setTimestamp(lostItem.getTimestamp());
        item.setImageUrl(lostItem.getItemImageUrl());

        return item;
    }

    public interface OnMatchesFoundListener {
        void onMatchesFound(List<ItemMatch> matches);
        void onError(String error);
    }
}