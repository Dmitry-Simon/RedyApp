package com.example.redyapp.LogReg;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Patterns;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.redyapp.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Disable Dark Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);

        // set the orientation to portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        EditText emailField = findViewById(R.id.email_field);
        EditText passwordField = findViewById(R.id.password_field);
        Button registerButton = findViewById(R.id.register_button);
        ImageButton backButton = findViewById(R.id.back_button);

        // Back button to the previous activity
        backButton.setOnClickListener(view -> {
            finish();
        });

        registerButton.setOnClickListener(view -> {
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();

            register(email, password);
        });
    }

    private void register(String email, String password) {
        if (!isValidEmail(email)) {
            showFeedback("The email address is not valid.");
            return;
        }

        if (!isValidPassword(password)) {
            showFeedback("The password must be at least 8 characters long and contain at least " +
                    "one letter and one digit.");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Send verification email
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        user.sendEmailVerification()
                                .addOnCompleteListener(verificationTask -> {
                                    if (verificationTask.isSuccessful()) {
                                        showFeedback("Registration successful! A verification email has been sent to " + email);
                                        // Redirect to the registration done activity
                                        Intent intent = new Intent(RegisterActivity.this, RegisterDone.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        showFeedback("The verification email could not be sent. Please try again later.");
                                    }
                                });
                        // log out the user
                        mAuth.signOut();
                    } else {
                        // If registration fails, display a detailed message to the user.
                        String errorMessage = task.getException().getMessage();
                        showFeedback(errorMessage);

                        // Apply the shake animation to the register button
                        Button registerButton = findViewById(R.id.register_button);
                        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake_animation);
                        registerButton.startAnimation(shake);
                    }
                });
    }

    private void showFeedback(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 8) return false;

        // Check if the password contains at least one letter and one digit
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");

        return hasLetter && hasDigit;
    }



    private boolean isValidEmail(CharSequence email) {
        return (email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches());
    }
}
