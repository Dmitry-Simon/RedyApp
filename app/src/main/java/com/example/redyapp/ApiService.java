package com.example.redyapp;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    @Multipart
    @POST("predict")
    Call<PredictionResponse> predictWatermelonSweetness(
            @Part MultipartBody.Part file
    );
}

class PredictionResponse {
    String predicted_label;
    Double confidence;

    public String getPredictedLabel() { return predicted_label; }
    public void setPredictedLabel(String predicted_label) { this.predicted_label = predicted_label; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
}
