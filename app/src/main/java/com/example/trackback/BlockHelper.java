package com.example.trackback;

import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Helper class to check if users have blocked each other
 */
public class BlockHelper {

    /**
     * Check if currentUser has blocked targetUser
     */
    public static void isUserBlocked(String currentUserId, String targetUserId, BlockCheckCallback callback) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .collection("blockedUsers")
                .document(targetUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    callback.onResult(documentSnapshot.exists());
                })
                .addOnFailureListener(e -> {
                    callback.onResult(false);
                });
    }

    /**
     * Check if EITHER user has blocked the other (bidirectional check)
     */
    public static void areUsersBlocked(String userId1, String userId2, BlockCheckCallback callback) {
        // Check if user1 blocked user2
        isUserBlocked(userId1, userId2, isBlocked1 -> {
            if (isBlocked1) {
                callback.onResult(true);
                return;
            }

            // Check if user2 blocked user1
            isUserBlocked(userId2, userId1, callback::onResult);
        });
    }

    /**
     * Callback interface for block check results
     */
    public interface BlockCheckCallback {
        void onResult(boolean isBlocked);
    }
}