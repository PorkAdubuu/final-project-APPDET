package com.example.trackback;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.bumptech.glide.Glide;
import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView welcomeTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        welcomeTextView = findViewById(R.id.welcomeTextView);

        // Get the current signed-in user
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            String fullName = user.getDisplayName();
            String firstName = fullName != null ? fullName.split(" ")[0] : "User";

            // Capitalize only the first letter
            if (!firstName.isEmpty()) {
                firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1).toLowerCase();
            }

            welcomeTextView.setText("Hi, " + firstName + "!");
        }

        ImageView profileImageView = findViewById(R.id.profileImageView);

        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(profileImageView);
        } else {
            Glide.with(this)
                    .load(R.drawable.default_avatar) // your fallback image
                    .circleCrop()
                    .into(profileImageView);
        }

        TextView dayTextView = findViewById(R.id.dayTextView);
        TextView monthTextView = findViewById(R.id.monthTextView);

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

        Date currentDate = new Date();

        dayTextView.setText(dayFormat.format(currentDate));
        monthTextView.setText(monthFormat.format(currentDate).toUpperCase());


    }
}
