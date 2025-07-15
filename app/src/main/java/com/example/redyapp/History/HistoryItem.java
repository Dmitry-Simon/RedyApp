package com.example.redyapp.History;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a single history item in the prediction history.
 * This class is a Room Entity, defining the structure of the "history_table".
 */
@Entity(tableName = "history_table")
public class HistoryItem implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String predictedLabel;
    private double confidence;
    private String localAudioPath; // Stores the absolute path to the audio file on the device
    private Date timestamp;

    /**
     * Constructs a new HistoryItem.
     * Room uses this constructor. The id is handled automatically.
     * @param predictedLabel The result of the prediction (e.g., "Sweet").
     * @param confidence The confidence score of the prediction.
     * @param localAudioPath The local file path where the recording is stored.
     * @param timestamp The date and time of the prediction.
     */
    public HistoryItem(String predictedLabel, double confidence, String localAudioPath, Date timestamp) {
        this.predictedLabel = predictedLabel;
        this.confidence = confidence;
        this.localAudioPath = localAudioPath;
        this.timestamp = timestamp;
    }

    // --- Getters and Setters ---
    // These are necessary for Room to automatically handle data.

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPredictedLabel() {
        return predictedLabel;
    }

    public void setPredictedLabel(String predictedLabel) {
        this.predictedLabel = predictedLabel;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getLocalAudioPath() {
        return localAudioPath;
    }

    public void setLocalAudioPath(String localAudioPath) {
        this.localAudioPath = localAudioPath;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
