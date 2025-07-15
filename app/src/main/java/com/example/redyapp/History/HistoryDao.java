package com.example.redyapp.History;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

/**
 * Data Access Object (DAO) for the HistoryItem table.
 * This interface defines all the database operations (queries, inserts, deletes)
 * that can be performed on the history data. Room will generate the implementation
 * for these methods at compile time.
 */
@Dao
public interface HistoryDao {

    /**
     * Inserts a new history item into the database. If there's a conflict, it replaces the old item.
     * @param historyItem The item to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HistoryItem historyItem);

    /**
     * Deletes a specific history item from the database.
     * @param historyItem The item to delete.
     */
    @Delete
    void delete(HistoryItem historyItem);

    /**
     * Retrieves all history items from the database, ordered by timestamp in descending order (newest first).
     * This method returns LiveData, which allows the UI (HistoryActivity) to automatically
     * update whenever the data in this table changes.
     * @return A LiveData list of all HistoryItems.
     */
    @Query("SELECT * FROM history_table ORDER BY timestamp DESC")
    LiveData<List<HistoryItem>> getAllHistoryItems();
}
