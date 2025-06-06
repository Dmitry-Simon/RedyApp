package com.example.redyapp;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An activity that displays a list of past prediction results from the local Room database.
 * It observes changes in the database and updates the UI accordingly.
 * It also handles user interactions, such as deleting history items.
 */
public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnHistoryItemInteractionListener {

    private HistoryAdapter historyAdapter;
    private HistoryDatabase historyDatabase;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Initialize the database instance
        historyDatabase = HistoryDatabase.getInstance(getApplicationContext());

        // Set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.history_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the adapter and pass 'this' as the listener for delete callbacks
        // *** THIS LINE IS THE FIX ***
        historyAdapter = new HistoryAdapter(this);
        recyclerView.setAdapter(historyAdapter);

        // Observe the LiveData from the database.
        // When the data changes (e.g., an item is added or deleted), the onChanged() method
        // will be triggered, and the UI will be updated.
        historyDatabase.historyDao().getAllHistoryItems().observe(this, historyItems -> {
            // Update the adapter's data set
            historyAdapter.setHistoryItems(historyItems);
        });

        // Set up the back button to close the activity
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
    }

    /**
     * This method is called from the HistoryAdapter when the user clicks the delete icon on an item.
     * @param historyItem The item to be deleted.
     */
    @Override
    public void onDeleteClicked(HistoryItem historyItem) {
        // Show a confirmation dialog before deleting
        new AlertDialog.Builder(this)
                .setTitle("Delete Recording")
                .setMessage("Are you sure you want to permanently delete this history item?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // If confirmed, proceed with deletion on a background thread
                    deleteItemFromDatabaseAndStorage(historyItem);
                })
                .setNegativeButton("Cancel", null) // Do nothing on cancel
                .show();
    }

    /**
     * Deletes the HistoryItem from the Room database and its associated audio file
     * from the device's local storage. This operation is performed on a background thread.
     * @param historyItem The item to delete.
     */
    private void deleteItemFromDatabaseAndStorage(final HistoryItem historyItem) {
        databaseExecutor.execute(() -> {
            // Step 1: Delete the record from the Room database
            historyDatabase.historyDao().delete(historyItem);

            // Step 2: Delete the associated audio file from local storage
            String localPath = historyItem.getLocalAudioPath();
            if (localPath != null && !localPath.isEmpty()) {
                File audioFile = new File(localPath);
                if (audioFile.exists()) {
                    if (audioFile.delete()) {
                        Log.d("HistoryActivity", "Successfully deleted local audio file: " + localPath);
                    } else {
                        Log.e("HistoryActivity", "Failed to delete local audio file: " + localPath);
                    }
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // It's crucial to release the MediaPlayer resources when the activity is no longer visible
        // to prevent audio from playing in the background.
        if (historyAdapter != null) {
            historyAdapter.releaseMediaPlayer();
        }
    }
}
