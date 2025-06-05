package com.example.redyapp.LogReg;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.redyapp.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Disable Dark Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        // set the orientation to portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_forgot_pass);

        EditText emailField = findViewById(R.id.email_field);
        Button loginButton = findViewById(R.id.login_button);
        ImageButton backButton = findViewById(R.id.back_button);

        // Back button to the previous activity
        backButton.setOnClickListener(view -> {
            finish();
        });

        loginButton.setOnClickListener(view -> {
            String email = emailField.getText().toString();
            sendPasswordResetEmail(email);
        });
    }

    private void sendPasswordResetEmail(String email) {
        if (!email.isEmpty()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email);
                showFeedback("Success! A password reset email has been sent to " + email);
                finish();
            } else {
                showFeedback("Please enter a valid email address.");
            }
        }



    private void showFeedback(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

}
