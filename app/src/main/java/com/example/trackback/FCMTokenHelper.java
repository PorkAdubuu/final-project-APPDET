package com.example.trackback;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Helper class to register FCM tokens for push notifications
 * Call this after user login to ensure they receive notifications
 */
public class FCMTokenHelper {

    private static final String TAG = "FCMTokenHelper";

    /**
     * Register FCM token for the current user
     * Call this method after user successfully logs in
     */
    public static void registerFCMToken() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            Log.w(TAG, "User not authenticated, cannot register FCM token");
            return;
        }

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Failed to get FCM token", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);

                    // Save token to Firestore
                    saveTokenToFirestore(userId, token);
                });
    }

    private static void saveTokenToFirestore(String userId, String token) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "FCM token successfully saved to Firestore"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving FCM token to Firestore", e);

                    // If update fails (document might not exist), try set with merge
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .set(new java.util.HashMap<String, Object>() {{
                                put("fcmToken", token);
                            }}, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid2 ->
                                    Log.d(TAG, "FCM token saved with merge"))
                            .addOnFailureListener(e2 ->
                                    Log.e(TAG, "Failed to save FCM token with merge", e2));
                });
    }

    /**
     * Remove FCM token when user logs out
     */
    public static void unregisterFCMToken() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", com.google.firebase.firestore.FieldValue.delete())
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "FCM token removed from Firestore"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error removing FCM token", e));
    }
}