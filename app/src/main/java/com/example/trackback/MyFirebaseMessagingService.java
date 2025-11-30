package com.example.trackback;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String MESSAGES_CHANNEL_ID = "messages_channel";
    private static final String ITEMS_CHANNEL_ID = "item_notifications_channel";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM Token: " + token);

        // Save token to Firestore when user is logged in
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId != null) {
            saveFcmToken(userId, token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        Log.d(TAG, "Message received from: " + message.getFrom());

        // Check if message contains data payload
        if (message.getData().size() > 0) {
            Log.d(TAG, "Message data: " + message.getData());

            String type = message.getData().get("type");
            String title = message.getData().get("title");
            String body = message.getData().get("body");

            // Handle different notification types
            if ("message".equals(type)) {
                String senderId = message.getData().get("senderId");
                String senderName = message.getData().get("senderName");
                showMessageNotification(title, body, senderId, senderName);
            } else if ("item_notification".equals(type)) {
                String documentId = message.getData().get("documentId");
                String reportType = message.getData().get("reportType");
                showItemNotification(title, body, documentId, reportType);
            } else {
                // Default handling for backward compatibility
                String senderId = message.getData().get("senderId");
                String senderName = message.getData().get("senderName");
                showMessageNotification(title, body, senderId, senderName);
            }
        }

        // Check if message contains notification payload
        if (message.getNotification() != null) {
            Log.d(TAG, "Notification: " + message.getNotification().getBody());
            showMessageNotification(
                    message.getNotification().getTitle(),
                    message.getNotification().getBody(),
                    null,
                    null
            );
        }
    }

    private void showMessageNotification(String title, String body, String senderId, String senderName) {
        createMessagesNotificationChannel();

        // Create intent to open MessagesActivity or specific chat
        Intent intent = new Intent(this, MessagesActivity.class);
        if (senderId != null) {
            intent.putExtra("senderId", senderId);
            intent.putExtra("senderName", senderName);
            intent.putExtra("openChat", true);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MESSAGES_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_message)
                .setContentTitle(title != null ? title : "New Message")
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void showItemNotification(String title, String body, String documentId, String reportType) {
        createItemsNotificationChannel();

        // Open HomeActivity and navigate to notification fragment
        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("openNotifications", true);
        intent.putExtra("documentId", documentId);
        intent.putExtra("reportType", reportType);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ITEMS_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notif)  // Make sure this icon exists or use another one
                .setContentTitle(title != null ? title : "New Item Posted")
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createMessagesNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    MESSAGES_CHANNEL_ID,
                    "Messages",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for new messages");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void createItemsNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    ITEMS_CHANNEL_ID,
                    "Lost & Found Items",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for new lost and found items");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void saveFcmToken(String userId, String token) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save FCM token", e));
    }
}