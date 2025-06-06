package com.example.redyapp;

import androidx.room.TypeConverter;
import java.util.Date;

/**
 * Type Converters for Room to handle data types it doesn't natively support.
 * In this case, it converts a Date object to a Long (timestamp) and back,
 * so that dates can be stored in the database.
 */
public class Converters {

    /**
     * Converts a Long timestamp from the database into a Date object.
     * @param value The Long timestamp.
     * @return A Date object, or null if the timestamp was null.
     */
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    /**
     * Converts a Date object into a Long timestamp to be stored in the database.
     * @param date The Date object.
     * @return A Long timestamp, or null if the date was null.
     */
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
