package com.example.trackback;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MatchNotificationHelper {

    private static final String TAG = "MatchNotificationHelper";
    private FirebaseFirestore db;

    public MatchNotificationHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public void sendMatchNotification(String userId, String itemName, String lostItemId,
                                      String foundItemId, String senderName, String senderProfileUrl) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        Date now = new Date();

        Map<String, Object> notification = new HashMap<>();
        notification.put("documentId", lostItemId);
        notification.put("fname", senderName.split(" ")[0]);
        notification.put("lastName", senderName.contains(" ") ? senderName.split(" ")[1] : "");
        notification.put("profileUrl", senderProfileUrl);
        notification.put("date", dateFormat.format(now));
        notification.put("time", timeFormat.format(now));
        notification.put("reportType", "Match");
        notification.put("read", false);
        notification.put("status", "active");
        notification.put("notificationType", "match");
        notification.put("matchId", lostItemId + "_" + foundItemId);
        notification.put("lostItemId", lostItemId);
        notification.put("foundItemId", foundItemId);
        notification.put("itemName", itemName);

        db.collection("users")
                .document(userId)
                .collection("notifications")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Match notification saved: " + documentReference.getId());
                    sendPushNotification(userId, itemName, lostItemId, foundItemId);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error saving match notification", e));
    }

    private void sendPushNotification(String userId, String itemName, String lostItemId, String foundItemId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String fcmToken = documentSnapshot.getString("fcmToken");
                    if (fcmToken != null && !fcmToken.isEmpty()) {
                        sendFCMNotification(fcmToken, itemName, lostItemId, foundItemId);
                    } else {
                        Log.w(TAG, "User has no FCM token");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error getting user FCM token", e));
    }

    private void sendFCMNotification(String token, String itemName, String lostItemId, String foundItemId) {
        new Thread(() -> {
            try {
                String serverKey = "YOUR_SERVER_KEY_HERE";

                URL url = new URL("https://fcm.googleapis.com/fcm/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "key=" + serverKey);
                conn.setDoOutput(true);

                JSONObject notification = new JSONObject();
                notification.put("title", "New Match Found!");
                notification.put("body", "We found a potential match for your " + itemName);

                JSONObject data = new JSONObject();
                data.put("type", "match");
                data.put("lostItemId", lostItemId);
                data.put("foundItemId", foundItemId);
                data.put("itemName", itemName);

                JSONObject message = new JSONObject();
                message.put("to", token);
                message.put("notification", notification);
                message.put("data", data);

                OutputStream os = conn.getOutputStream();
                os.write(message.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "FCM Response Code: " + responseCode);

            } catch (Exception e) {
                Log.e(TAG, "Error sending FCM notification", e);
            }
        }).start();
    }
}