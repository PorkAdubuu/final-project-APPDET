package com.example.trackback;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;

import android.widget.FrameLayout;
import android.view.View;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private FrameLayout overlay;
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private ListenerRegistration notifListener;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        overlay = findViewById(R.id.frame_overlay);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize permission launcher for notifications (Android 13+)
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Log.d(TAG, "Notification permission granted");
                        initializeFcmToken();
                    } else {
                        Log.d(TAG, "Notification permission denied");
                    }
                }
        );

        // Ensure user document exists
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            db.collection("users")
                    .document(user.getUid())
                    .set(new HashMap<>(), SetOptions.merge());
        }

        // Request notification permission and initialize FCM
        requestNotificationPermission();
        initializeFcmToken();

        // Floating Action Button
        FloatingActionButton fabAdd = findViewById(R.id.floatingActionButtonAdd);
        if (fabAdd != null) {
            fabAdd.setOnClickListener(view -> {
                AddReportDialogFragment dialog = new AddReportDialogFragment();
                dialog.show(getSupportFragmentManager(), "AddReportDialog");
            });
        }

        // BottomNavigationView item selection listener
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    // Show HomeFragment
                    overlay.setVisibility(View.VISIBLE);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame_overlay, new HomeFragment())
                            .commit();
                    return true;
                } else if (itemId == R.id.nav_search) {
                    overlay.setVisibility(View.VISIBLE);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame_overlay, new ItemsFragment())
                            .commit();
                    return true;
                } else if (itemId == R.id.nav_add) {
                    AddReportDialogFragment dialog = new AddReportDialogFragment();
                    dialog.show(getSupportFragmentManager(), "AddReportDialog");
                    return true;
                } else if (itemId == R.id.nav_notif) {
                    overlay.setVisibility(View.VISIBLE);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame_overlay, new NotificationsFragment())
                            .commit();
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    overlay.setVisibility(View.VISIBLE);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame_overlay, new FragmentProfile())
                            .commit();
                    return true;
                } else {
                    return false;
                }
            });
        }

        // Show HomeFragment on first launch
        if (savedInstanceState == null) {
            // ✅ Check if opened from notification tap BEFORE showing default fragment
            if (!handleNotificationIntent(getIntent())) {
                // Only show home fragment if NOT opened from notification
                overlay.setVisibility(View.VISIBLE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_overlay, new HomeFragment())
                        .commit();
                if (bottomNavigationView != null) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                }
            }
        }

        // Fetch and update notification badge count on startup
        startListeningUnreadNotifications();
    }

    // ✅ NEW METHOD: Handle notification intent
    private boolean handleNotificationIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("openNotifications", false)) {
            // Open notification fragment
            overlay.setVisibility(View.VISIBLE);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_overlay, new NotificationsFragment())
                    .commit();
            if (bottomNavigationView != null) {
                bottomNavigationView.setSelectedItemId(R.id.nav_notif);
            }
            return true; // Return true to indicate we handled the intent
        }
        return false; // Return false if no notification intent
    }

    // ✅ NEW METHOD: Handle notification tap when app is already open
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    // Request notification permission for Android 13+
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    // Initialize and save FCM token
    private void initializeFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM token failed", task.getException());
                        return;
                    }

                    // Get FCM token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);

                    // Save token to Firestore
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        db.collection("users")
                                .document(user.getUid())
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid ->
                                        Log.d(TAG, "FCM token saved to Firestore"))
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to save FCM token, trying set", e);
                                    // If update fails, try set with merge
                                    HashMap<String, Object> data = new HashMap<>();
                                    data.put("fcmToken", token);
                                    db.collection("users")
                                            .document(user.getUid())
                                            .set(data, SetOptions.merge());
                                });
                    }
                });
    }

    @Override
    public void onBackPressed() {
        if (overlay != null && overlay.getVisibility() == View.VISIBLE) {
            // Check if we're on home fragment
            if (getSupportFragmentManager().findFragmentById(R.id.frame_overlay) instanceof HomeFragment) {
                // If on home, exit app
                super.onBackPressed();
            } else {
                // Go back to home fragment
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_overlay, new HomeFragment())
                        .commit();
                if (bottomNavigationView != null) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                }
            }
        } else {
            super.onBackPressed();
        }
    }

    // Update notification badge on BottomNavigationView
    public void updateNotificationBadge(int unreadCount) {
        if (bottomNavigationView == null) return;

        if (unreadCount > 0) {
            BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.nav_notif);
            badge.setVisible(true);
            badge.setNumber(unreadCount);
        } else {
            bottomNavigationView.removeBadge(R.id.nav_notif);
        }
    }

    // Fetch unread notification count from Firestore
    private void startListeningUnreadNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        notifListener = db.collection("users")
                .document(user.getUid())
                .collection("notifications")
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        updateNotificationBadge(0);
                        return;
                    }
                    if (value != null) {
                        int unreadCount = value.size();
                        updateNotificationBadge(unreadCount);
                    }
                });
    }

    private void stopListeningUnreadNotifications() {
        if (notifListener != null) {
            notifListener.remove();
            notifListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopListeningUnreadNotifications();
    }
}