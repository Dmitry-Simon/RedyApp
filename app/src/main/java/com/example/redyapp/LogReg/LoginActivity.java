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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.creativesphere.sababa.R;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView forgotPassword;

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


        setContentView(R.layout.activity_login);



        mAuth = FirebaseAuth.getInstance();

        EditText emailField = findViewById(R.id.email_field);
        EditText passwordField = findViewById(R.id.password_field);
        Button loginButton = findViewById(R.id.login_button);
        ImageButton backButton = findViewById(R.id.back_button);
        forgotPassword = findViewById(R.id.forgot_password);

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent forgotPasswordIntent = new Intent(LoginActivity.this, ForgotPassword.class);
                startActivity(forgotPasswordIntent);
            }
        });

        // Back button to the previous activity
        backButton.setOnClickListener(view -> {
            finish();
        });

        loginButton.setOnClickListener(view -> {
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();
            signIn(email, password);
        });

        TextView registerButton = (TextView)findViewById(R.id.register_text_clickable);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(registerIntent);
            }
        });
    }

    private void signIn(String email, String password) {
        if (!isValidEmail(email)) {
            showFeedback("המייל כתוב בצורה שגויה");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user.isEmailVerified()) {
                            SharedPreferences preferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
                            boolean isFirstTime = preferences.getBoolean("isFirstTime", true);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putBoolean("isFirstTime", true);
                            editor.apply();
                            // Redirect to the specific activity for first-time users
                            Intent firstTimeLoginIntent = new Intent(LoginActivity.this, FirstLogIn.class);
                            firstTimeLoginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(firstTimeLoginIntent);
                            finish();

                        } else {
                            showFeedback("עוד לא אושר המייל, אנא אשרו");
                        }
                    } else {
                        // Check the type of exception and provide a corresponding Hebrew message
                        String errorMessage = getErrorMessage(task.getException());
                        showFeedback(errorMessage);

                        // Apply the shake animation to the login button
                        Button loginButton = findViewById(R.id.login_button);
                        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake_animation);
                        loginButton.startAnimation(shake);
                    }
                });
    }

    private String getErrorMessage(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidUserException) {
            return "המשתמש או הסיסמה אינם נכונים";
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return "הסיסמה שגויה";
        } else {
            return "שגיאה בהתחברות, אנא נסה שוב";
        }
    }

    private void showFeedback(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    private boolean isValidEmail(CharSequence email) {
        return (email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches());
    }

}
