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

import com.example.redyapp.History.HistoryActivity;
import com.example.redyapp.History.HistoryDatabase;
import com.example.redyapp.History.HistoryItem;
import com.example.redyapp.LogReg.MainLogRegActivity;
import com.example.redyapp.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MainActivity is the main screen of the RedyApp application.
 *
 * This activity handles:
 * 1. Audio recording for watermelon thump analysis
 * 2. File upload for existing audio files
 * 3. Displaying prediction results
 * 4. User authentication verification
 * 5. Navigation to other activities (Settings, History)
 *
 * The app uses Firebase for authentication and Retrofit for API communication.
 */
public class MainActivity extends AppCompatActivity {

    // User authentication object
    FirebaseUser user;
    // Database for saving prediction history
    private HistoryDatabase historyDatabase;
    // Thread executor for database operations to avoid blocking UI thread
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    // View binding for the activity layout
    private ActivityMainBinding binding;
    // MediaRecorder for capturing audio
    private MediaRecorder mediaRecorder;
    // File to store recorded audio
    private File audioOutputFile;
    // Flags to track application state
    private boolean isRecording = false;
    private boolean isDisplayingResult = false;
    // Duration for audio recording in milliseconds (5 seconds)
    private static final long RECORDING_DURATION = 5000;
    // Timer to automatically stop recording after the defined duration
    private CountDownTimer countDownTimer;
    // Tag for logging
    private static final String TAG = "MainActivity";

    /**
     * Launcher for requesting audio recording permission
     * Uses the new ActivityResult API introduced in AndroidX
     */
    private final ActivityResultLauncher<String> requestRecordAudioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startRecordingFlow();
                } else {
                    Toast.makeText(this, "Recording permission denied.", Toast.LENGTH_SHORT).show();
                }
            });

    /**
     * Launcher for selecting audio files from device storage
     * Uses the ActivityResult API to handle the file selection callback
     */
    private final ActivityResultLauncher<String> pickAudioFileLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            Log.d(TAG, "File selected: " + uri.toString());
                            File fileToUpload = getFileFromUri(uri);
                            if (fileToUpload != null && fileToUpload.exists()) {
                                setProcessingUIState("Uploading...");
                                uploadAudioFile(fileToUpload, true);
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to process selected file.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d(TAG, "No file selected");
                        }
                    });

    /**
     * Initializes the activity, sets up authentication, UI, and event listeners
     *
     * This method:
     * 1. Forces light mode for consistent UI experience
     * 2. Checks for user authentication, redirecting to login if needed
     * 3. Initializes the database connection
     * 4. Sets up view binding and UI components
     * 5. Configures click listeners for all interactive elements
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Force light mode for the app
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Get current Firebase user
        user = FirebaseAuth.getInstance().getCurrentUser();

        // Redirect to login screen if user is not authenticated or email not verified
        if (user == null || !user.isEmailVerified()) {
            Intent intent = new Intent(this, MainLogRegActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Initialize the history database
        historyDatabase = HistoryDatabase.getInstance(this);

        // Set up view binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set the initial UI state
        setInitialUIState();

        // Set up main microphone button click listener
        binding.watermelonMic.setOnClickListener(view -> {
            if (isDisplayingResult) {
                setInitialUIState();
            } else if (!isRecording) {
                checkPermissionAndStartRecording();
            }
        });

        // Set up long press listener for file upload option
        binding.watermelonMic.setOnLongClickListener(view -> {
            if (isRecording) {
                Toast.makeText(MainActivity.this, "Cannot upload while recording.", Toast.LENGTH_SHORT).show();
                return true;
            }
            if (isDisplayingResult) {
                setInitialUIState();
            }
            showUploadFileDialog();
            return true;
        });

        // Set up settings button click listener
        binding.settings.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Set up info button click listener
        binding.info.setOnClickListener(view -> {
            Toast.makeText(MainActivity.this, "Info clicked (Implement AboutActivity)", Toast.LENGTH_SHORT).show();
        });

        // Set up click listener for the History button
        binding.history.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Sets the UI to its initial state when the app starts or after results are cleared
     *
     * This method:
     * - Shows the instruction text
     * - Hides the prediction result area
     * - Enables the microphone button
     * - Resets the display state flag
     */
    private void setInitialUIState() {
        if (binding == null) {
            Log.w(TAG, "Binding is null in setInitialUIState, skipping UI updates.");
            return;
        }
        // Show initial instruction text
        binding.textViewInstruction.setVisibility(View.VISIBLE);

        // Hide the result area
        binding.imageView4.setVisibility(View.GONE);
        binding.textViewPredictionResult.setVisibility(View.GONE);
        binding.textViewPredictionConfidence.setVisibility(View.GONE);

        // Enable mic button and reset flag
        binding.watermelonMic.setEnabled(true);
        isDisplayingResult = false;
    }

    /**
     * Updates the UI to reflect that recording is in progress
     *
     * This method:
     * - Changes instruction text to "Recording..."
     * - Hides any prediction results
     * - Disables the microphone button to prevent multiple recordings
     */
    private void setRecordingUIState() {
        if (binding == null) {
            Log.w(TAG, "Binding is null in setRecordingUIState, cannot update UI.");
            return;
        }
        binding.textViewInstruction.setText("Recording...");
        if (binding.imageView4 != null) binding.imageView4.setVisibility(View.GONE);
        if (binding.textViewPredictionConfidence != null) binding.textViewPredictionConfidence.setVisibility(View.GONE);
        if (binding.textViewPredictionResult  != null) binding.textViewPredictionResult.setVisibility(View.GONE);
        binding.watermelonMic.setEnabled(false);
        isDisplayingResult = false;
    }

    /**
     * Updates the UI to show that processing is happening (uploading or predicting)
     *
     * @param message The message to display to the user (e.g., "Uploading...", "Predicting...")
     */
    private void setProcessingUIState(String message) {
        if (binding == null) {
            Log.w(TAG, "Binding is null in setProcessingUIState, cannot update UI.");
            return;
        }
        binding.textViewInstruction.setText(message);
        if (binding.imageView4 != null) binding.imageView4.setVisibility(View.GONE);
        if (binding.textViewPredictionConfidence != null) binding.textViewPredictionConfidence.setVisibility(View.GONE);
        if (binding.textViewPredictionResult  != null) binding.textViewPredictionResult.setVisibility(View.GONE);
        binding.watermelonMic.setEnabled(false);
        isDisplayingResult = false;
    }

    /**
     * Displays the prediction results on the main activity screen
     *
     * @param label The predicted label for the watermelon (e.g., "sweet", "not sweet")
     * @param confidence The confidence level of the prediction (0.0-1.0)
     */
    private void displayResultsOnMainActivity(String label, Double confidence) {
        if (binding == null) {
            Log.w(TAG, "Binding is null in displayResultsOnMainActivity, cannot update UI.");
            return;
        }

        // Hide initial instruction text
        binding.textViewInstruction.setVisibility(View.GONE);

        // Show the result area
        binding.imageView4.setVisibility(View.VISIBLE);
        binding.textViewPredictionResult.setVisibility(View.VISIBLE);
        binding.textViewPredictionConfidence.setVisibility(View.VISIBLE);

        // Set the result text
        String displayLabel = "N/A";
        if (label != null && !"Error".equals(label) && !label.isEmpty()) {
            // Capitalize first letter for better display
            displayLabel = label.substring(0, 1).toUpperCase(Locale.ROOT) + label.substring(1).toLowerCase(Locale.ROOT) + "!";
        } else if (label != null) {
            displayLabel = label;
        }

        binding.textViewPredictionResult.setText(displayLabel);
        binding.textViewPredictionConfidence.setText(String.format(Locale.US, "%.1f%%", (confidence != null ? confidence : 0.0) * 100));

        // Enable mic button to allow for a new recording
        binding.watermelonMic.setEnabled(true);
        isDisplayingResult = true; // Set flag to know that results are being shown
    }

    /**
     * Shows a dialog asking if the user wants to upload an existing WAV file
     * This is triggered by a long press on the microphone button
     */
    private void showUploadFileDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Upload Audio")
                .setMessage("Do you want to upload an existing WAV file?")
                .setPositiveButton("Yes, Upload", (dialog, which) -> pickAudioFileLauncher.launch("audio/wav"))
                .setNegativeButton("No, Record", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * Checks for audio recording permission and starts recording if granted
     * Otherwise, requests the necessary permission
     */
    private void checkPermissionAndStartRecording() {
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
     * Initiates the audio recording process using MediaRecorder
     *
     * This method:
     * 1. Sets up the recording UI state
     * 2. Creates an output file for the recording
     * 3. Configures and starts the MediaRecorder
     * 4. Sets up a timer to automatically stop recording after the set duration
     */
    private void startRecordingFlow() {
        if (isRecording) return;
        setRecordingUIState();

        // Find an appropriate directory to store the temporary recording
        File outputDir = getExternalCacheDir();
        if (outputDir == null) outputDir = getCacheDir();
        if (outputDir == null) {
            Toast.makeText(this, "Cannot access storage for recording.", Toast.LENGTH_LONG).show();
            resetToInitialStateAfterError();
            return;
        }

        // Create output file for the recording
        audioOutputFile = new File(outputDir, "recorded_watermelon_thump.wav");

        // Initialize MediaRecorder with appropriate constructor based on Android version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            mediaRecorder = new MediaRecorder(this);
        } else {
            mediaRecorder = new MediaRecorder();
        }

        // Configure MediaRecorder settings
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(audioOutputFile.getAbsolutePath());

        try {
            // Start recording
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show();

            // Set timer to automatically stop recording after defined duration
            countDownTimer = new CountDownTimer(RECORDING_DURATION, 1000) {
                public void onTick(long millisUntilFinished) { /* ... */ }
                public void onFinish() { if (isRecording) stopRecording(); }
            }.start();
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "MediaRecorder start/prepare failed: " + e.getMessage());
            Toast.makeText(this, "Recording failed to start.", Toast.LENGTH_SHORT).show();
            resetRecordingState();
        }
    }

    /**
     * Stops the audio recording process and processes the recorded file
     * This is called either when the timer completes or when the user manually stops recording
     */
    private void stopRecording() {
        if (!isRecording || mediaRecorder == null) return;
        if (countDownTimer != null) countDownTimer.cancel();
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
        } catch (RuntimeException e) {
            Log.e(TAG, "MediaRecorder stop() failed: " + e.getMessage());
        } finally {
            mediaRecorder = null;
            isRecording = false;
        }
        if (audioOutputFile != null && audioOutputFile.exists() && audioOutputFile.length() > 0) {
            setProcessingUIState("Predicting...");
            uploadAudioFile(audioOutputFile, false);
        } else {
            Log.w(TAG, "Recorded audio file issue.");
            Toast.makeText(this, "Audio file not created.", Toast.LENGTH_SHORT).show();
            setInitialUIState();
        }
    }

    /**
     * Cleans up resources used by the MediaRecorder and resets the UI state
     * Called when recording is finished or if there's an error during recording
     */
    private void resetRecordingState() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing media recorder: " + e.getMessage());
            }
            mediaRecorder = null;
        }
        isRecording = false;
        if (binding != null) {
            setInitialUIState();
        }
        if (countDownTimer != null) countDownTimer.cancel();
    }

    /**
     * Resets the UI to its initial state after an error occurs
     * Makes sure the mic button is enabled so the user can try again
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
     * Cleans up resources when the activity is being destroyed
     * Ensures MediaRecorder and CountDownTimer are properly released
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            resetRecordingState();
            binding = null;
        } else {
            if (mediaRecorder != null) {
                try {
                    mediaRecorder.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing media recorder: " + e.getMessage());
                }
                mediaRecorder = null;
            }
            if (countDownTimer != null) countDownTimer.cancel();
            isRecording = false;
        }
    }

    /**
     * Extracts a filename from a content URI
     *
     * @param uri The URI to extract the filename from
     * @return The extracted filename or a default name if extraction fails
     */
    private String getFileName(Uri uri) {
        String result = null;
        // Try to get the display name from the content provider
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) result = cursor.getString(nameIndex);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name from URI", e);
            }
        }
        // If we couldn't get the name from content provider, try to extract it from the path
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) result = result.substring(cut + 1);
            }
        }
        // Ensure the file has a .wav extension
        return (result != null && result.toLowerCase().endsWith(".wav")) ? result : "uploaded_audio.wav";
    }

    /**
     * Converts a content URI to a File by copying the content to a temporary file
     *
     * @param uri The URI of the file to convert
     * @return A File object pointing to the copied file, or null if the operation fails
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
                byte[] buffer = new byte[1024 * 4];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
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
     * Uploads an audio file to the prediction API and handles the response
     * Optimized for faster processing with reduced file copying overhead
     *
     * @param file The audio file to upload
     * @param isUploadedFile Flag indicating if this is a user-uploaded file (true) or a recorded file (false)
     */
    private void uploadAudioFile(File file, boolean isUploadedFile) {
        // Validate file existence and size
        if (file == null || !file.exists() || file.length() == 0L) {
            Toast.makeText(this, "File to upload is invalid.", Toast.LENGTH_SHORT).show();
            resetToInitialStateAfterError();
            return;
        }

        // For performance, create the API request first, then handle file copying in background
        RequestBody requestFile = RequestBody.create(file, MediaType.parse("audio/wav"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        // Get API service and make the prediction request immediately
        ApiService apiService = RetrofitClient.getInstance();
        Call<PredictionResponse> call = apiService.predictWatermelonSweetness(body);

        // Execute the request asynchronously
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<PredictionResponse> call, @NonNull Response<PredictionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Process successful response
                    PredictionResponse prediction = response.body();
                    displayResultsOnMainActivity(prediction.getPredictedLabel(), prediction.getConfidence());

                    // Copy file to persistent storage in background after successful prediction
                    databaseExecutor.execute(() -> {
                        File persistentAudioFile = copyAudioToPersistentStorage(file);
                        if (persistentAudioFile != null) {
                            saveHistoryToDatabase(prediction.getPredictedLabel(), prediction.getConfidence(), persistentAudioFile.getAbsolutePath());
                        } else {
                            Log.w(TAG, "Failed to save audio file to persistent storage for history");
                        }
                    });
                } else {
                    // Handle API error
                    Log.e(TAG, "API Error or empty body. Code: " + response.code());
                    Toast.makeText(MainActivity.this, "Prediction failed. Code: " + response.code(), Toast.LENGTH_LONG).show();
                    setInitialUIState();
                }

                // Clean up temporary file if it was a user upload
                if (isUploadedFile && file.getParentFile() != null && file.getParentFile().equals(getCacheDir())) {
                    file.delete();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PredictionResponse> call, @NonNull Throwable t) {
                // Handle network failure
                Log.e(TAG, "Network Failure: " + t.getMessage(), t);
                Toast.makeText(MainActivity.this, "Network request failed.", Toast.LENGTH_LONG).show();
                setInitialUIState();
            }
        });
    }

    /**
     * Copies an audio file to the app's persistent storage for history tracking
     *
     * @param sourceFile The source audio file to copy
     * @return The new file in persistent storage, or null if copying failed
     */
    private File copyAudioToPersistentStorage(File sourceFile) {
        File persistentDir = new File(getFilesDir(), "history_audio");
        if (!persistentDir.exists()) {
            persistentDir.mkdirs();
        }
        String fileName = "rec_" + System.currentTimeMillis() + ".wav";
        File destinationFile = new File(persistentDir, fileName);
        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = new FileOutputStream(destinationFile)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy file to persistent storage", e);
            return null;
        }
        return destinationFile;
    }

    /**
     * Saves a prediction result to the history database
     * Uses a background thread to avoid blocking the UI thread
     *
     * @param label The predicted label (e.g., "sweet", "not sweet")
     * @param confidence The confidence level of the prediction (0.0-1.0)
     * @param localAudioPath Path to the saved audio file on device storage
     */
    private void saveHistoryToDatabase(String label, Double confidence, String localAudioPath) {
        databaseExecutor.execute(() -> {
            HistoryItem historyItem = new HistoryItem(label, confidence, localAudioPath, new Date());
            historyDatabase.historyDao().insert(historyItem);
            Log.d(TAG, "History item saved to local Room database.");
        });
    }
}
