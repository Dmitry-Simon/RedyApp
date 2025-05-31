package com.example.redyapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.redyapp.databinding.ActivityMainBinding;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale; // For String.format

// Import for Retrofit (should already be there from previous steps)
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MediaRecorder mediaRecorder;
    private File audioOutputFile;
    private boolean isRecording = false;
    private boolean isDisplayingResult = false; // New flag to manage UI state
    private static final long RECORDING_DURATION = 5000;
    private CountDownTimer countDownTimer;
    private static final String TAG = "MainActivity";

    private final ActivityResultLauncher<String> requestRecordAudioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startRecordingFlow();
                } else {
                    Toast.makeText(this, "Recording permission denied.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> pickAudioFileLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            Log.d(TAG, "File selected: " + uri.toString());
                            File fileToUpload = getFileFromUri(uri);
                            if (fileToUpload != null && fileToUpload.exists()) {
                                setProcessingUIState("Uploading...");
                                uploadAudioFile(fileToUpload, true); // Pass true if it's an uploaded file for cleanup
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to process selected file.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d(TAG, "No file selected");
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setInitialUIState(); // Set initial UI

        binding.watermelonMic.setOnClickListener(view -> {
            if (isDisplayingResult) {
                setInitialUIState(); // Reset UI if results were shown
            } else if (!isRecording) {
                checkPermissionAndStartRecording();
            }
        });

        binding.watermelonMic.setOnLongClickListener(view -> {
            if (isRecording) {
                Toast.makeText(MainActivity.this, "Cannot upload while recording.", Toast.LENGTH_SHORT).show();
                return true;
            }
            if (isDisplayingResult) {
                setInitialUIState(); // Reset UI first if results were shown
            }
            showUploadFileDialog();
            return true;
        });

        binding.settings.setOnClickListener(view -> {
            Toast.makeText(MainActivity.this, "Settings clicked (Implement SettingsActivity)", Toast.LENGTH_SHORT).show();
        });

        binding.info.setOnClickListener(view -> {
            Toast.makeText(MainActivity.this, "Info clicked (Implement AboutActivity)", Toast.LENGTH_SHORT).show();
        });
    }

    private void setInitialUIState() {
        binding.textView3.setText("Tap to Record, Long Press to Upload");
        binding.textView.setText("Settings");
        binding.textView2.setText("About");
        // Ensure result views are hidden
        if (binding.textViewPredictionConfidence != null) binding.textViewPredictionConfidence.setVisibility(View.GONE);
        if (binding.textViewResultMessage != null) binding.textViewResultMessage.setVisibility(View.GONE);
        // Show initial elements like tip and menu rectangle if they were hidden
        if (binding.imageView4 != null) binding.imageView4.setVisibility(View.VISIBLE);
        // (Manage visibility of your "Tip" view similarly)

        binding.watermelonMic.setEnabled(true);
        isDisplayingResult = false;
    }

    private void setRecordingUIState() {
        binding.textView3.setText("Recording...");
        // Hide other elements if needed
        if (binding.imageView4 != null) binding.imageView4.setVisibility(View.GONE);
        if (binding.textViewPredictionConfidence != null) binding.textViewPredictionConfidence.setVisibility(View.GONE);
        if (binding.textViewResultMessage != null) binding.textViewResultMessage.setVisibility(View.GONE);
        binding.watermelonMic.setEnabled(false);
        isDisplayingResult = false;
    }

    private void setProcessingUIState(String message) {
        binding.textView3.setText(message);
        if (binding.imageView4 != null) binding.imageView4.setVisibility(View.GONE);
        if (binding.textViewPredictionConfidence != null) binding.textViewPredictionConfidence.setVisibility(View.GONE);
        if (binding.textViewResultMessage != null) binding.textViewResultMessage.setVisibility(View.GONE);
        binding.watermelonMic.setEnabled(false); // Disable while processing
        isDisplayingResult = false;
    }


    private void displayResultsOnMainActivity(String label, Double confidence) {
        String displayLabel = "N/A";
        if (label != null && !"Error".equals(label) && !label.isEmpty()) {
            displayLabel = label.substring(0, 1).toUpperCase(Locale.ROOT) + label.substring(1).toLowerCase(Locale.ROOT) + "!";
        } else if (label != null) {
            displayLabel = label;
        }

        binding.textView3.setText(displayLabel); // Main result label
        binding.textView3.setVisibility(View.VISIBLE);

        if (binding.textViewPredictionConfidence != null) {
            binding.textViewPredictionConfidence.setText(String.format(Locale.US, "%.1f%%", (confidence != null ? confidence : 0.0) * 100));
            binding.textViewPredictionConfidence.setVisibility(View.VISIBLE);
        }

        if (binding.textViewResultMessage != null) {
            binding.textViewResultMessage.setText("You can try again or substitute watermelon.");
            binding.textViewResultMessage.setVisibility(View.VISIBLE);
        }

        // Hide elements not part of result screen
        if (binding.imageView4 != null) binding.imageView4.setVisibility(View.GONE);
        // (Hide "Tip" view)

        binding.watermelonMic.setEnabled(true); // Allow tap to reset/retry
        isDisplayingResult = true; // Set flag
    }

    private void showUploadFileDialog() { /* ... same as before ... */
        new AlertDialog.Builder(this)
                .setTitle("Upload Audio")
                .setMessage("Do you want to upload an existing WAV file?")
                .setPositiveButton("Yes, Upload", (dialog, which) -> pickAudioFileLauncher.launch("audio/wav"))
                .setNegativeButton("No, Record", (dialog, which) -> dialog.dismiss())
                .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void checkPermissionAndStartRecording() { /* ... same as before ... */
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecordingFlow();
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(this, "Audio recording permission is needed.", Toast.LENGTH_LONG).show();
            requestRecordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            requestRecordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void startRecordingFlow() { /* ... same as before, but call setRecordingUIState ... */
        if (isRecording) return;
        setRecordingUIState(); // Update UI for recording state
        // ... rest of the startRecordingFlow logic ...
        File outputDir = getExternalCacheDir();
        if (outputDir == null) outputDir = getCacheDir();
        if (outputDir == null) {
            Toast.makeText(this, "Cannot access storage for recording.", Toast.LENGTH_LONG).show();
            resetToInitialStateAfterError();
            return;
        }
        audioOutputFile = new File(outputDir, "recorded_watermelon_thump.wav");

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            mediaRecorder = new MediaRecorder(this);
        } else {
            mediaRecorder = new MediaRecorder();
        }

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(audioOutputFile.getAbsolutePath());

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            // UI updated by setRecordingUIState()
            Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show();

            countDownTimer = new CountDownTimer(RECORDING_DURATION, 1000) {
                public void onTick(long millisUntilFinished) { /* ... */ }
                public void onFinish() { if (isRecording) stopRecording(); }
            }.start();

        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "MediaRecorder start/prepare failed: " + e.getMessage());
            Toast.makeText(this, "Recording failed to start.", Toast.LENGTH_SHORT).show();
            resetRecordingState(); // This already enables watermelonMic and resets text
        }
    }

    private void stopRecording() { /* ... same as before, but pass false for isUploadedFile ... */
        if (!isRecording && mediaRecorder == null) return;
        if (countDownTimer != null) countDownTimer.cancel();
        try {
            if (mediaRecorder != null) { mediaRecorder.stop(); mediaRecorder.release(); }
        } catch (RuntimeException e) { Log.e(TAG, "MediaRecorder stop() failed: " + e.getMessage()); }
        finally { mediaRecorder = null; isRecording = false; /* UI reset in upload or initial state */ }

        if (audioOutputFile != null && audioOutputFile.exists() && audioOutputFile.length() > 0) {
            setProcessingUIState("Predicting...");
            uploadAudioFile(audioOutputFile, false); // false because it's a recorded file
        } else {
            Log.w(TAG, "Recorded audio file issue.");
            Toast.makeText(this, "Audio file not created.", Toast.LENGTH_SHORT).show();
            setInitialUIState(); // Reset to initial state on error
        }
    }

    private void resetRecordingState() { /* ... same as before, calls setInitialUIState ... */
        if (mediaRecorder != null) {
            try { mediaRecorder.release(); } catch (Exception e) { Log.e(TAG, "Error releasing media recorder: " + e.getMessage());}
            mediaRecorder = null;
        }
        isRecording = false;
        setInitialUIState(); // Reset to initial UI
        if (countDownTimer != null) countDownTimer.cancel();
    }

    private void resetToInitialStateAfterError() {
        setInitialUIState();
        binding.watermelonMic.setEnabled(true);
    }

    @Override
    protected void onDestroy() { /* ... same as before ... */
        super.onDestroy();
        resetRecordingState();
    }

    private String getFileName(Uri uri) { /* ... same as before ... */
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) result = cursor.getString(nameIndex);
                }
            } catch (Exception e) { Log.e(TAG, "Error getting file name from URI", e); }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        return (result != null && result.toLowerCase().endsWith(".wav")) ? result : "uploaded_audio.wav";
    }

    private File getFileFromUri(Uri uri) { /* ... same as before ... */
        File tempFile = null;
        String fileName = getFileName(uri);
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            File cacheDir = getApplicationContext().getCacheDir();
            tempFile = new File(cacheDir, fileName);

            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024 * 4]; int read;
                while ((read = inputStream.read(buffer)) != -1) outputStream.write(buffer, 0, read);
                outputStream.flush();
            }
            inputStream.close();
            return tempFile;
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy file from URI: " + e.getMessage(), e);
            if (tempFile != null && tempFile.exists()) tempFile.delete();
            return null;
        }
    }

    // Modified to accept a boolean for cleanup logic
    private void uploadAudioFile(File file, boolean isUploadedFile) {
        if (file == null || !file.exists() || file.length() == 0L) {
            Toast.makeText(this, "File to upload is invalid.", Toast.LENGTH_SHORT).show();
            resetToInitialStateAfterError();
            return;
        }
        Log.d(TAG, "Uploading file: " + file.getName() + " (" + file.length() + " bytes)");
        // UI state already set to processing by caller

        RequestBody requestFile = RequestBody.create(file, MediaType.parse("audio/wav"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        ApiService apiService = RetrofitClient.getInstance();
        Call<PredictionResponse> call = apiService.predictWatermelonSweetness(body);

        call.enqueue(new Callback<PredictionResponse>() {
            @Override
            public void onResponse(@NonNull Call<PredictionResponse> call, @NonNull Response<PredictionResponse> response) {
                if (isUploadedFile && file.getParentFile().equals(getCacheDir())) { // Clean up temp file
                    file.delete();
                    Log.d(TAG, "Temporary uploaded file deleted: " + file.getName());
                }
                if (response.isSuccessful() && response.body() != null) {
                    displayResultsOnMainActivity(response.body().getPredictedLabel(), response.body().getConfidence());
                } else {
                    Log.e(TAG, "API Error or empty body. Code: " + response.code());
                    Toast.makeText(MainActivity.this, "Prediction failed. Code: " + response.code(), Toast.LENGTH_LONG).show();
                    setInitialUIState(); // Reset on API error
                }
            }

            @Override
            public void onFailure(@NonNull Call<PredictionResponse> call, @NonNull Throwable t) {
                if (isUploadedFile && file.getParentFile().equals(getCacheDir())) { // Clean up temp file
                    file.delete();
                    Log.d(TAG, "Temporary uploaded file deleted after failure: " + file.getName());
                }
                Log.e(TAG, "Network Failure: " + t.getMessage(), t);
                Toast.makeText(MainActivity.this, "Network request failed. Check logs.", Toast.LENGTH_LONG).show();
                setInitialUIState(); // Reset on network failure
            }
        });
    }
}

