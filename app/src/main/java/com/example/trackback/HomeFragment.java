package com.example.trackback;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView welcomeTextView;
    private ImageView profileImageView;

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        welcomeTextView = view.findViewById(R.id.welcomeTextView);
        profileImageView = view.findViewById(R.id.profileImageView);
        TextView dayTextView = view.findViewById(R.id.dayTextView);
        TextView monthTextView = view.findViewById(R.id.monthTextView);

        if (user != null) {
            String fullName = user.getDisplayName();
            String firstName = fullName != null ? fullName.split(" ")[0] : "User";

            if (!firstName.isEmpty()) {
                firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1).toLowerCase();
            }

            welcomeTextView.setText("Hi, " + firstName + "!");
        }

        if (user != null && user.getPhotoUrl() != null) {
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .into(profileImageView);
        } else {
            Glide.with(this)
                    .load(R.drawable.default_avatar)
                    .circleCrop()
                    .into(profileImageView);
        }

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

        Date currentDate = new Date();

        dayTextView.setText(dayFormat.format(currentDate));
        monthTextView.setText(monthFormat.format(currentDate).toUpperCase());
    }
}
