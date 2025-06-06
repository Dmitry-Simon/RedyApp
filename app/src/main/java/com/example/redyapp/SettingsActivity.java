package com.example.redyapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.redyapp.LogReg.MainLogRegActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Optional: Disable Dark Mode for consistency
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        Button logoutButton = findViewById(R.id.logout_button);
        // Set an OnClickListener for back_button to the previous activity
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(view -> {
            // Navigate back to the previous activity
            onBackPressed();
        });

        logoutButton.setOnClickListener(view -> {
            // Sign out the current user from Firebase Authentication
            FirebaseAuth.getInstance().signOut();

            // Provide user feedback
            Toast.makeText(SettingsActivity.this, "You have been logged out.", Toast.LENGTH_SHORT).show();

            // Create an intent to navigate to the login/registration screen
            Intent intent = new Intent(SettingsActivity.this, MainLogRegActivity.class);

            // Set flags to clear the activity stack. This prevents the user from
            // navigating back to the authenticated sections of the app after logging out.
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Finish the SettingsActivity so it's removed from the back stack
            finish();
        });
    }
}