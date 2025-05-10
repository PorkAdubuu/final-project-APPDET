package com.example.trackback;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomeActivity extends AppCompatActivity {

    private FrameLayout overlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        overlay = findViewById(R.id.frame_overlay);


        //add button floating
        FloatingActionButton fabAdd = findViewById(R.id.floatingActionButtonAdd);

        // Set click listener to show the dialog
        fabAdd.setOnClickListener(view -> {
            AddReportDialogFragment dialog = new AddReportDialogFragment();
            dialog.show(getSupportFragmentManager(), "AddReportDialog");
        });
        // Initialize BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                overlay.setVisibility(View.VISIBLE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_overlay, new HomeFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            } else if (itemId == R.id.nav_search) {
                // Handle Profile and open ItemsFragment
                overlay.setVisibility(View.VISIBLE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_overlay, new ItemsFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            } else if (itemId == R.id.nav_add) {
                // Show the AddReportDialogFragment
                AddReportDialogFragment dialog = new AddReportDialogFragment();
                dialog.show(getSupportFragmentManager(), "AddReportDialog");
                return true;
            } else if (itemId == R.id.nav_notif) {
                // Handle Notifications
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Handle Profile and open FragmentProfile
                overlay.setVisibility(View.VISIBLE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.frame_overlay, new FragmentProfile())
                        .addToBackStack(null)
                        .commit();
                return true;
            } else {
                return false;
            }
        });


        // First launch - show HomeFragment and overlay
        if (savedInstanceState == null) {
            overlay.setVisibility(View.VISIBLE);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_overlay, new HomeFragment())
                    .addToBackStack(null)
                    .commit();
        }
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

    // Helper method to replace fragments
    private void replaceFragment(androidx.fragment.app.Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_main, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
