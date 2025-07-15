package com.example.redyapp.History;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.redyapp.Converters;

/**
 * The main database class for the application, built using Room.
 * This class is abstract and extends RoomDatabase. It lists the entities
 * that belong in the database and the DAOs that access them.
 *
 * It uses a Singleton pattern to prevent having multiple instances of the
 * database opened at the same time, which is an expensive operation.
 */
@Database(entities = {HistoryItem.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class}) // Register the TypeConverter for Date objects
public abstract class HistoryDatabase extends RoomDatabase {

    // Abstract method to get the DAO. Room will generate the implementation.
    public abstract HistoryDao historyDao();

    // Volatile instance to ensure visibility across threads.
    private static volatile HistoryDatabase INSTANCE;

    /**
     * Gets the singleton instance of the HistoryDatabase.
     *
     * @param context The application context.
     * @return The singleton HistoryDatabase instance.
     */
    public static HistoryDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            // Use a synchronized block to prevent race conditions during instantiation.
            synchronized (HistoryDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    HistoryDatabase.class, "history_database")
                            // NOTE: In a real production app, you would need to handle migrations
                            // for database schema changes. .fallbackToDestructiveMigration()
                            // is simple for development but deletes all data on schema change.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
