package com.example.trackback;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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
            // Display the user's name and email
            welcomeTextView.setText("Welcome " + user.getDisplayName() + "!");
        }
    }
}
