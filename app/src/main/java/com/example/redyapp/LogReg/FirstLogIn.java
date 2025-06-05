package com.example.redyapp.LogReg;

import static com.creativesphere.sababa.PagesFrags.HomeFragment.MATARA_KEY;
import static com.creativesphere.sababa.PagesFrags.HomeFragment.MATARA_PREFERENCE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.creativesphere.sababa.MainActivity;
import com.creativesphere.sababa.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class FirstLogIn extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    String matara;
    int mataraInt;

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


        setContentView(R.layout.first_login);

        EditText displayNameField = findViewById(R.id.display_name_field);
        EditText mataraField = findViewById(R.id.display_matara_field);
        Button registerButton = findViewById(R.id.register_button);


        registerButton.setOnClickListener(view -> {
            String displayName = displayNameField.getText().toString();
            sharedPreferences = getSharedPreferences(MATARA_PREFERENCE, Context.MODE_PRIVATE); // Initialize SharedPreferences

//            Object rawMatara = sharedPreferences.getAll().get(MATARA_KEY);
            // get matara from mataraField
            matara = mataraField.getText().toString();
            try {
                mataraInt = Integer.parseInt(mataraField.getText().toString());
            } catch (NumberFormatException e) {
                e.printStackTrace();
                // Handle invalid string
            }
            if (!displayName.matches("[א-ת ]+")) {
                Toast.makeText(this, "הכנס שם בעברית בסבבה", Toast.LENGTH_SHORT).show();
                return;
            } else if (displayName.length() < 2) {
                Toast.makeText(this, "הכנס שם ארוך יותר מתוו בסבבה", Toast.LENGTH_SHORT).show();
                return;
            } else if (displayName.length() > 16) {
                Toast.makeText(this, "הכנס שם קצר יותר מ16 תווים בסבבה", Toast.LENGTH_SHORT).show();
                return;
            }

            if (displayName.isEmpty()) {
                Toast.makeText(this, "לא סבבה להשאיר את השם ריק", Toast.LENGTH_SHORT).show();
                return;
            } else if (mataraInt == 0) {
                Toast.makeText(this, "לא סבבה להשאיר את המטרה ריקה", Toast.LENGTH_SHORT).show();
                return;
            } else if (mataraInt > 100) {
                Toast.makeText(this, "מטרה חייבת להיות עד 100", Toast.LENGTH_SHORT).show();
                return;
            } else if (mataraInt < 0) {
                Toast.makeText(this, "מטרה חייבת להיות מעל 0", Toast.LENGTH_SHORT).show();
                return;
            } else if (displayName.length() > 10) {
                Toast.makeText(this, "שם חייב להיות עד 10 תווים", Toast.LENGTH_SHORT).show();
                return;
            } else if (displayName.length() < 1) {
                Toast.makeText(this, "שם חייב להיות מעל תוו בודד", Toast.LENGTH_SHORT).show();
                return;
            } else if (matara.length() > 3) {
                Toast.makeText(this, "מטרה חייבת להיות עד 3 ספרות", Toast.LENGTH_SHORT).show();
                return;
            } else if (matara.length() < 1) {
                Toast.makeText(this, "מטרה חייבת להיות מעל ספרה בודדת", Toast.LENGTH_SHORT).show();
                return;
            }


            register(displayName, matara);
        });


    }

    private void register(String displayName, String matara) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();




        // Set display name in Firebase user profile
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();
        user.updateProfile(profileUpdates)
                .addOnCompleteListener(profileUpdateTask -> {
                    if (!profileUpdateTask.isSuccessful()) {
                        Toast.makeText(this, "Failed to update display name.", Toast.LENGTH_SHORT).show();
                    }
                });

        // set matara in shared preferences
        sharedPreferences.edit().putString(MATARA_KEY, matara).apply();





        SharedPreferences preferences = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        // set isFirstTime to false
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isFirstTime", false);
        editor.apply();

        // a shared preference to check if the user is to the first time for the first time
        SharedPreferences isFirstTime = getSharedPreferences("isFirstTime", MODE_PRIVATE);
        SharedPreferences.Editor editor1 = isFirstTime.edit();
        editor1.putBoolean("isFirstTimeFirstTime", true);
        editor1.apply();

        Intent mainActivityIntent = new Intent(FirstLogIn.this, MainActivity.class);
        mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainActivityIntent);
        finish();

    }
}
