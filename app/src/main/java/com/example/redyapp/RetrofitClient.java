package com.example.redyapp;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

/**
 * RetrofitClient is a singleton class that provides an instance of Retrofit for making network requests.
 * It configures the client with logging, connection, read, and write timeouts.
 */
public class RetrofitClient {
    private static final String BASE_URL = "https://watermelon-api-96308048537.me-west1.run.app/";
    private static Retrofit retrofit = null;

    /**
     * getInstance() is a static method that returns the Retrofit instance.
     * @return Retrofit instance
     */
    public static ApiService getInstance() {
        // If retrofit is null, create a new instance
        if (retrofit == null) {
            // Create a logging interceptor for debugging purposes and set the level to BODY
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Create an OkHttpClient with the logging interceptor and timeouts
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS) // Connection timeout
                    .readTimeout(30, TimeUnit.SECONDS)    // Read timeout
                    .writeTimeout(30, TimeUnit.SECONDS)   // Write timeout (important for uploads)
                    .build();

            // Build the Retrofit instance with the base URL, client, and Gson converter
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
