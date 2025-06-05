package com.example.redyapp.LogReg;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.creativesphere.sababa.R;
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

        // Set layout direction to RTL
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // Make the Status Bar transparent
        getWindow().setStatusBarColor(Color.TRANSPARENT);


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
            showFeedback("כתובת האימייל אינה בפורמט הנכון.");
            return;
        }

        if (!isValidPassword(password)) {
            showFeedback("הסיסמה צריכה להכיל לפחות 8 תווים ולכלול שילוב של אותיות ומספרים.");
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
                                        SharedPreferences preferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
                                        boolean isFirstTime = preferences.getBoolean("isFirstTime", true);
                                        // set isFirstTime to true
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putBoolean("isFirstTime", true);
                                        editor.apply();
                                        // go to the next activity
                                        Intent loginIntent = new Intent(RegisterActivity.this, RegisterDone.class);
                                        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(loginIntent);

                                    } else {
                                        showFeedback("שליחת מייל נכשלה");
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

        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");

        return hasLetter && hasDigit;
    }



    private boolean isValidEmail(CharSequence email) {
        return (email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches());
    }
}
