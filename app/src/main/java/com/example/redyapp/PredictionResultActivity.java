package com.example.redyapp;
// PredictionResultActivity.java
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.redyapp.databinding.ActivityPredictionResultBinding;
import java.util.Locale;

public class PredictionResultActivity extends AppCompatActivity {
    private ActivityPredictionResultBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPredictionResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        String label = intent.getStringExtra("PREDICTION_LABEL");
        if (label == null || label.isEmpty()) {
            label = "Error";
        }

        double confidence = intent.getDoubleExtra("PREDICTION_CONFIDENCE", 0.0);

        String displayLabel = "N/A";
        if (!"Error".equals(label) && !label.isEmpty()) {
            displayLabel = label.substring(0, 1).toUpperCase(Locale.ROOT) + label.substring(1).toLowerCase(Locale.ROOT);
        } else {
            displayLabel = label;
        }

        binding.textViewPredictionLabel.setText(displayLabel);
        binding.textViewPredictionConfidence.setText(String.format(Locale.US, "%.1f%%", confidence * 100));

        // You might want to change the image based on the result too
        // For example, if "sweet", show a happy watermelon, if "not sweet", a sad one.
        // binding.imageViewResultWatermelon.setImageResource(R.drawable.your_result_specific_watermelon_image);

        binding.buttonTryAgain.setOnClickListener(v -> {
            finish(); // Goes back to MainActivity
        });
    }
}
