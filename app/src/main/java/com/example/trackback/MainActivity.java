package com.example.trackback;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.AuthCredential;

// ✅ Insert for Notifications
import com.google.firebase.messaging.FirebaseMessaging;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // SUBSCRIBE USER TO TOPIC "allUsers" (for notifications)
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d("FCM", "Token: " + token);
                    } else {
                        Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                    }
                });

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))  // Your client ID from Firebase Console
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Sign-in button click listener
        findViewById(R.id.googleSignInBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });

        // Check if the user is already signed in
        if (mAuth.getCurrentUser() != null) {
            // The user is already signed in, go directly to HomeActivity or another screen
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching GoogleSignInClient.getSignInIntent()
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("MainActivity", "Google sign in failed", e);
                Toast.makeText(this, "Sign in failed!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d("MainActivity", "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String email = user.getEmail();
                            //  FIXED: Remove the empty string check
                            if (email != null && email.toLowerCase().endsWith("@umak.edu.ph")) {
                                //  Save user data to Firestore
                                saveUserToFirestore(user);
                                updateUI(user);
                            } else {
                                // Not a UMak email – sign out and show message
                                mAuth.signOut();
                                mGoogleSignInClient.signOut();
                                Toast.makeText(MainActivity.this, "Only UMak emails are allowed.", Toast.LENGTH_LONG).show();
                            }
                        }

                    } else {
                        Log.w("MainActivity", "signInWithCredential:failure", task.getException());
                        Toast.makeText(MainActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //NEW METHOD: Save user to Firestore
    // NEW METHOD: Save user to Firestore
    private void saveUserToFirestore(FirebaseUser user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", user.getDisplayName());
        userData.put("email", user.getEmail());
        userData.put("profileImageUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Save the basic user info first
        db.collection("users")
                .document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d("MainActivity", "User saved to Firestore");

                    // Now get and save the FCM token
                    FirebaseMessaging.getInstance().getToken()
                            .addOnCompleteListener(task -> {
                                if (!task.isSuccessful()) {
                                    Log.w("MainActivity", "Fetching FCM token failed", task.getException());
                                    return;
                                }

                                String token = task.getResult();
                                db.collection("users")
                                        .document(user.getUid())
                                        .update("fcmToken", token)
                                        .addOnSuccessListener(v -> {
                                            Log.d("MainActivity", "FCM token saved");
                                        })
                                        .addOnFailureListener(e -> Log.e("MainActivity", "Error saving FCM token", e));
                            });
                })
                .addOnFailureListener(e -> Log.e("MainActivity", "Error saving user", e));
    }

    private void updateUI(FirebaseUser user) {
        // Proceed to HomeActivity if user is valid (UMak check was done before this)
        startActivity(new Intent(MainActivity.this, HomeActivity.class));
        finish();
    }

}
