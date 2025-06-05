package com.example.redyapp.LogReg;

import android.animation.Animator;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.airbnb.lottie.LottieAnimationView;
import com.creativesphere.sababa.R;

public class RegisterDone extends AppCompatActivity {

    LottieAnimationView animationView;

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


        setContentView(R.layout.activity_register_done);

        animationView = findViewById(R.id.view11);
        animationView.playAnimation();
        animationView.setRepeatCount(0);

        // when tha animation finished then go back to the login activity
        animationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                // do nothing
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Intent intent = new Intent(RegisterDone.this, MainLogRegActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // do nothing
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // do nothing
            }
        });


    }
}
