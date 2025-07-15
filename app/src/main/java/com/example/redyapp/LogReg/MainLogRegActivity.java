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
        // Check if user is already signed in and redirect if necessary
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            // User is already authenticated and verified, redirect to main activity
            Intent mainActivityIntent = new Intent(MainLogRegActivity.this, MainActivity.class);
            mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(mainActivityIntent);
            finish();
            return;
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
        loginButton.setOnClickListener(view -> {
            Intent loginIntent = new Intent(MainLogRegActivity.this, LoginActivity.class);
            startActivity(loginIntent);
        });

        // Register button
        TextView registerButton = (TextView)findViewById(R.id.register_text_clickable);
        registerButton.setOnClickListener(view -> {
            Intent registerIntent = new Intent(MainLogRegActivity.this, RegisterActivity.class);
            startActivity(registerIntent);
        });
    }

    // Function to handle sign-in result
    private void handleSignInResult(Intent data) {
        try {
            // The Task returned from this call is always completed, no need to attach a listener.
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
            if (account != null) {
                Log.d("Google Sign In", "Account successfully retrieved. ID: " + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } else {
                // This case is rare when getResult(ApiException.class) is used.
                Log.w("Google Sign In", "GoogleSignIn.getSignedInAccountFromIntent returned a null account.");
            }
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.e("Google Sign In Error", "signInResult:failed code=" + e.getStatusCode());
            Log.e("Google Sign In Error", "Error message: " + e.getMessage());

            // You can show a more specific error to the user
            String errorMessage = "Google Sign-In Failed. Error code: " + e.getStatusCode();
            Toast.makeText(MainLogRegActivity.this, errorMessage, Toast.LENGTH_LONG).show();
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
                        if (user != null) {
                            Log.d("Google Sign In", "signInWithCredential:success, user: " + user.getEmail());
                            // Google accounts are typically already verified, so proceed to main activity
                            Intent mainActivityIntent = new Intent(MainLogRegActivity.this, MainActivity.class);
                            mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(mainActivityIntent);
                            finish();
                        } else {
                            Log.e("Google Sign In", "Firebase user is null after successful authentication");
                            Toast.makeText(MainLogRegActivity.this, "Authentication error occurred.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Sign in fails
                        Log.e("Google Sign In", "signInWithCredential:failure", task.getException());
                        Toast.makeText(MainLogRegActivity.this, "Firebase Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


}
