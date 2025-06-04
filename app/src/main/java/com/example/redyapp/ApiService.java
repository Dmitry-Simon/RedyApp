package com.example.redyapp;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * ApiService is an interface that defines the API endpoints for making network requests.
 * It includes a method for making a prediction request.
 * The response is expected to be a PredictionResponse object.
 */
public interface ApiService {
    // Endpoint for making a prediction request with a file
    @Multipart
    @POST("predict")
    Call<PredictionResponse> predictWatermelonSweetness(
            @Part MultipartBody.Part file
    );
}

/**
 * PredictionResponse is a data class that represents the response from the prediction endpoint.
 * It includes the predicted label and its confidence.
 * This class is used to deserialize the JSON response from the server.
 * @see ApiService for the API endpoint
 */
class PredictionResponse {
    String predicted_label;
    Double confidence;

    public String getPredictedLabel() { return predicted_label; }
    public void setPredictedLabel(String predicted_label) { this.predicted_label = predicted_label; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}
