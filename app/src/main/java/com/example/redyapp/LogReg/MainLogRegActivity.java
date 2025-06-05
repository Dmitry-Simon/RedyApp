package com.example.redyapp.LogReg;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.redyapp.MainActivity;
import com.example.redyapp.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainLogRegActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> signInActivityResultLauncher;

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Disable Dark Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);

        // set the orientation to portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_logreg);

        mAuth = FirebaseAuth.getInstance();
        // logout the user if he is already logged in
        if (mAuth.getCurrentUser() != null) {
            mAuth.signOut();
        }

        // Handling the result of the Google Sign-In Intent
        signInActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleSignInResult(result.getData());
                    } else {
                        // Handle sign-in cancellation or failure
                        Toast.makeText(MainLogRegActivity.this, "Sign-In Cancelled or Failed", Toast.LENGTH_SHORT).show();
                        // log the cancellation
                        Log.d("Google Sign In", "Sign-In Cancelled or Failed");
                        // log the result code
                        Log.d("Google Sign In", "Result Code: " + result.getResultCode());
                        // log the data
                        Log.d("Google Sign In", "Data: " + (result.getData() != null ? result.getData().toString() : "null"));
                    }
                }
        );


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Button signInButton = findViewById(R.id.sign_in_google_button);
        signInButton.setOnClickListener(view -> signIn());

        // Login button
        Button loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent loginIntent = new Intent(MainLogRegActivity.this, LoginActivity.class);
                startActivity(loginIntent);
            }
        });

        // Register button
        TextView registerButton = (TextView)findViewById(R.id.register_text_clickable);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent registerIntent = new Intent(MainLogRegActivity.this, RegisterActivity.class);
                startActivity(registerIntent);
            }
        });
    }

    // Function to handle sign-in result
    private void handleSignInResult(Intent data) {
        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken());
            }
        } catch (ApiException e) {
            Log.e("Google Sign In Error", "signInResult:failed code=" + e.getStatusCode() + ", message: " + e.getMessage());
            Toast.makeText(MainLogRegActivity.this, "Google Sign-In Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInActivityResultLauncher.launch(signInIntent);
    }


    private void firebaseAuthWithGoogle(String idToken) {
        mAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        FirebaseUser user = mAuth.getCurrentUser();
                        assert user != null;
                        Log.d("Google Sign In", "signInWithCredential:success, user: " + user.getEmail());
                        // Check if the user is verified
                        if (user.isEmailVerified()) {
                            // Redirect to the main activity
                            Intent mainActivityIntent = new Intent(MainLogRegActivity.this, MainActivity.class);
                            mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(mainActivityIntent);
                        } else {
                            Toast.makeText(MainLogRegActivity.this, "Please verify your email before proceeding.", Toast.LENGTH_SHORT).show();
                        }

                        finish();
                    } else {
                        // Sign in fails
                        Toast.makeText(MainLogRegActivity.this, "Firebase Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


}

