package com.example.trackback;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;

public class HomeActivity extends AppCompatActivity {

    private FrameLayout overlay;
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private ListenerRegistration notifListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        overlay = findViewById(R.id.frame_overlay);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        db = FirebaseFirestore.getInstance();

        // Ensure user document exists
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users")
                    .document(user.getUid())
                    .set(new HashMap<>(), SetOptions.merge());
        }

        // Floating Action Button
        FloatingActionButton fabAdd = findViewById(R.id.floatingActionButtonAdd);
        fabAdd.setOnClickListener(view -> {
            AddReportDialogFragment dialog = new AddReportDialogFragment();
            dialog.show(getSupportFragmentManager(), "AddReportDialog");
        });

        // BottomNavigationView item selection listener
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
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

        // Show HomeFragment on first launch
        if (savedInstanceState == null) {
            overlay.setVisibility(View.VISIBLE);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_overlay, new HomeFragment())
                    .commit();
        }

        // Fetch and update notification badge count on startup
        startListeningUnreadNotifications();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            overlay.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    // Optional helper method to replace fragments with backstack
    private void replaceFragment(androidx.fragment.app.Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_main, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    // Update notification badge on BottomNavigationView
    public void updateNotificationBadge(int unreadCount) {
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

}
