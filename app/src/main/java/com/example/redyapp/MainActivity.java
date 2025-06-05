package com.example.redyapp;

import android.Manifest;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.example.redyapp.LogReg.MainLogRegActivity;
import com.example.redyapp.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MainActivity is the entry point of the Redy App.
 * It handles audio recording, file uploads, and displays results of watermelon sweetness predictions.
 * This activity uses MediaRecorder for audio recording and Retrofit for network requests.
 * It also manages UI states for recording, processing, and displaying results.
 * The activity includes:
 * - Recording audio with a MediaRecorder
 * - Uploading audio files to a server for prediction
 * - Displaying prediction results
 */
public class MainActivity extends AppCompatActivity {

    FirebaseUser user;
    private ActivityMainBinding binding;
    private MediaRecorder mediaRecorder;
    private File audioOutputFile;
    private boolean isRecording = false;
    private boolean isDisplayingResult = false; // New flag to manage UI state
    private static final long RECORDING_DURATION = 5000;
    private CountDownTimer countDownTimer;
    private static final String TAG = "MainActivity";

    // Request permission launcher for recording audio
    private final ActivityResultLauncher<String> requestRecordAudioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startRecordingFlow();
                } else {
                    Toast.makeText(this, "Recording permission denied.", Toast.LENGTH_SHORT).show();
                }
            });

    // Activity result launcher for picking audio files
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

    /**
     * Called when the activity is starting. This is where most initialization
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Disable Dark Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        user = FirebaseAuth.getInstance().getCurrentUser();

        // Check user authentication status
        if (FirebaseAuth.getInstance().getCurrentUser() == null || !user.isEmailVerified()) {
            // Redirect to another activity (e.g., LoginActivity) if the user is not authenticated
            Intent intent = new Intent(this, MainLogRegActivity.class);
            startActivity(intent);
            finish();  // Close this activity so the user won't return to it when pressing back
            return;  // Prevent the rest of the code from executing
        }

        // Inflate the layout using View Binding and set the content view
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setInitialUIState(); // Set initial UI

        // Set up click listeners for the watermelon mic button
        binding.watermelonMic.setOnClickListener(view -> {
            if (isDisplayingResult) {
                setInitialUIState(); // Reset UI if results were shown
            } else if (!isRecording) {
                checkPermissionAndStartRecording();
            }
        });

        // Set up long click listener for the watermelon mic button to upload files
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

        // Set up click listeners for settings and info buttons
        binding.settings.setOnClickListener(view -> {
            Toast.makeText(MainActivity.this, "Settings clicked (Implement SettingsActivity)", Toast.LENGTH_SHORT).show();
        });

        // Placeholder for info button click, you can implement an AboutActivity later
        binding.info.setOnClickListener(view -> {
            Toast.makeText(MainActivity.this, "Info clicked (Implement AboutActivity)", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Called when the activity is about to be destroyed.
     * This is where you should clean up resources like MediaRecorder.
     */
    private void setInitialUIState() {
        // Crucial: check if binding is not null before accessing its views
        if (binding == null) {
            Log.w(TAG, "Binding is null in setInitialUIState, skipping UI updates.");
            return;
        }
        // Reset UI elements to initial state
        binding.textView3.setText("Tap to Record, Long Press to Upload");
        binding.textView.setText("Settings");
        binding.textView2.setText("About");
        // Ensure result views are hidden
        if (binding.textViewPredictionConfidence != null) binding.textViewPredictionConfidence.setVisibility(View.GONE);
        if (binding.textViewResultMessage != null) binding.textViewResultMessage.setVisibility(View.GONE);
        // Show initial elements like tip and menu rectangle if they were hidden
        if (binding.imageView4 != null) binding.imageView4.setVisibility(View.VISIBLE);

        // Enable watermelon mic button
        binding.watermelonMic.setEnabled(true);
        // Reset flag
        isDisplayingResult = false;
    }

    /**
     * Sets the UI state to indicate that recording is in progress.
     * This method updates the text and hides unnecessary elements.
     */
    private void setRecordingUIState() {
        // Add a null check before accessing binding views
        if (binding == null) {
            Log.w(TAG, "Binding is null in setRecordingUIState, cannot update UI.");
            return;
        }
        binding.textView3.setText("Recording...");
        // Hide other elements if needed
        if (binding.imageView4 != null) binding.imageView4.setVisibility(View.GONE);
        if (binding.textViewPredictionConfidence != null) binding.textViewPredictionConfidence.setVisibility(View.GONE);
        if (binding.textViewResultMessage != null) binding.textViewResultMessage.setVisibility(View.GONE);
        binding.watermelonMic.setEnabled(false);
        isDisplayingResult = false;
    }

    /**
     * Sets the UI state to indicate that processing is happening (e.g., uploading or predicting).
     * This method updates the text and hides unnecessary elements.
     * @param message The message to display while processing.
     */
    private void setProcessingUIState(String message) {
        // Add a null check before accessing binding views
        if (binding == null) {
            Log.w(TAG, "Binding is null in setProcessingUIState, cannot update UI.");
            return;
        }
        binding.textView3.setText(message);
        if (binding.imageView4 != null) binding.imageView4.setVisibility(View.GONE);
        if (binding.textViewPredictionConfidence != null) binding.textViewPredictionConfidence.setVisibility(View.GONE);
        if (binding.textViewResultMessage != null) binding.textViewResultMessage.setVisibility(View.GONE);
        binding.watermelonMic.setEnabled(false); // Disable while processing
        isDisplayingResult = false;
    }


    /**
     * Displays the results on the main activity after prediction.
     * This method updates the text views and hides unnecessary elements.
     * @param label The predicted label for the watermelon sweetness.
     * @param confidence The confidence level of the prediction.
     */
    private void displayResultsOnMainActivity(String label, Double confidence) {
        // Add a null check before accessing binding views
        if (binding == null) {
            Log.w(TAG, "Binding is null in displayResultsOnMainActivity, cannot update UI.");
            return;
        }

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

    /**
     * Shows a dialog to choose between uploading an existing WAV file or recording a new one.
     * This method is called when the user long-presses the watermelon mic button.
     */
    private void showUploadFileDialog() { /* ... same as before ... */
        new AlertDialog.Builder(this)
                .setTitle("Upload Audio")
                .setMessage("Do you want to upload an existing WAV file?")
                .setPositiveButton("Yes, Upload", (dialog, which) -> pickAudioFileLauncher.launch("audio/wav"))
                .setNegativeButton("No, Record", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Checks for audio recording permission and starts the recording flow if granted.
     * If permission is not granted, it requests the user for permission.
     */
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

    /**
     * Starts the recording flow, initializes MediaRecorder, and handles the recording process.
     * This method sets the UI state to indicate that recording is in progress.
     */
    private void startRecordingFlow() {
        if (isRecording) return;
        setRecordingUIState(); // Update UI for recording state
        // Initialize MediaRecorder
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

    /**
     * Stops the recording process, releases the MediaRecorder, and uploads the recorded audio file.
     * This method sets the UI state to indicate that processing is happening.
     */
    private void stopRecording() {
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

    /**
     * Resets the recording state, releases the MediaRecorder, and sets the UI to initial state.
     * This method is called when the activity is destroyed or when an error occurs.
     */
    private void resetRecordingState() {
        // Add a null check for binding here as well, since onDestroy calls this.
        // It prevents NullPointerException if onDestroy is called when binding is already null.
        if (mediaRecorder != null) {
            try { mediaRecorder.release(); } catch (Exception e) { Log.e(TAG, "Error releasing media recorder: " + e.getMessage());}
            mediaRecorder = null;
        }
        isRecording = false;
        // Only attempt to update UI if binding is still valid
        if (binding != null) {
            setInitialUIState(); // This method will also need a null check for binding
        }
        if (countDownTimer != null) countDownTimer.cancel();
    }

    /**
     * Resets the UI to the initial state after an error occurs.
     * This method is called when an error happens during recording or uploading.
     */
    private void resetToInitialStateAfterError() {
        if (binding == null) {
            Log.w(TAG, "Binding is null in resetToInitialStateAfterError, cannot update UI.");
            return;
        }
        setInitialUIState();
        binding.watermelonMic.setEnabled(true);
    }

    /**
     * Called when the activity is about to be destroyed.
     * This method resets the recording state and releases resources.
     * It ensures that the MediaRecorder is released and the binding reference is cleared to prevent memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Null-check binding before calling methods that use it
        // This is crucial because binding might already be null or its views invalid
        // when onDestroy is called, especially if the activity was never fully created
        // or if it's being destroyed by the system.
        if (binding != null) {
            resetRecordingState(); // This method now needs to safely handle a potentially null binding internally
            binding = null; // Important: Clear the binding reference to prevent memory leaks
        } else {
            // If binding was already null, ensure MediaRecorder is released anyway
            if (mediaRecorder != null) {
                try { mediaRecorder.release(); } catch (Exception e) { Log.e(TAG, "Error releasing media recorder: " + e.getMessage());}
                mediaRecorder = null;
            }
            if (countDownTimer != null) countDownTimer.cancel();
            isRecording = false;
        }
    }

    /**
     * Gets the file name from the URI, ensuring it ends with ".wav".
     * If the URI is a content URI, it queries the content resolver for the display name.
     * If not, it extracts the file name from the path.
     * @param uri The URI of the audio file.
     * @return The file name, or a default name if not found.
     */
    private String getFileName(Uri uri) {
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

    /**
     * Converts a URI to a File object by copying the content to a temporary file.
     * This method handles both content URIs and file URIs.
     * @param uri The URI of the audio file.
     * @return A File object pointing to the copied audio file, or null if an error occurs.
     */
    private File getFileFromUri(Uri uri) {
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

    /**
     * Uploads the audio file to the server for prediction.
     * This method creates a multipart request and handles the response.
     * @param file The audio file to upload.
     * @param isUploadedFile Indicates if this is an uploaded file (for cleanup).
     */
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

        call.enqueue(new Callback<>() {
            // Callback for handling the response from the server
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

            // Callback for handling network failures
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

