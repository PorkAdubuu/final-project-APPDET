package com.example.trackback;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class BroadcastNotificationHelper {

    private static final String TAG = "BroadcastNotification";
    private FirebaseFirestore db;
    private String currentUserId;
    private Context context;

    public BroadcastNotificationHelper(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.context = context;

        // Start listening to posts
        listenForNewPosts();
    }

    /**
     * Creates a new post in Firestore
     */
    public void createNewPost(String posterName, String posterProfileUrl,
                              String itemName, String reportType) {

        Map<String, Object> post = new HashMap<>();
        post.put("userId", currentUserId);
        post.put("userName", posterName);
        post.put("posterProfileUrl", posterProfileUrl);
        post.put("itemName", itemName);
        post.put("reportType", reportType);
        post.put("timestamp", System.currentTimeMillis());

        db.collection("posts")
                .add(post)
                .addOnSuccessListener(docRef ->
                        Log.d(TAG, "Post added with ID: " + docRef.getId()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error adding post", e));
    }

    /**
     * Listens for new posts from other users and shows local notifications
     */
    private void listenForNewPosts() {
        db.collection("posts")
                .orderBy("timestamp")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String posterId = doc.getString("userId");

                            // Skip posts by current user
                            if (!posterId.equals(currentUserId)) {
                                String posterName = doc.getString("userName");
                                String reportType = doc.getString("reportType");
                                String itemName = doc.getString("itemName");

                                showLocalNotification(posterName, reportType, itemName);
                            }
                        }
                    }
                });
    }

    /**
     * Shows a local notification for a new post
     */
    private void showLocalNotification(String posterName, String reportType, String itemName) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "post_notifications";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Post Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notif) // Replace with your app icon
                .setContentTitle("New " + reportType + " Item Posted")
                .setContentText(posterName + " posted: " + itemName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
