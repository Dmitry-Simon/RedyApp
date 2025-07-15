package com.example.redyapp;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

/**
 * RetrofitClient is a singleton class that provides an instance of Retrofit for making network requests.
 * It configures the client with optimized timeouts, minimal logging, and connection pooling for better performance.
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
            // Create a logging interceptor with minimal logging for production performance
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            // Use HEADERS instead of BODY to reduce logging overhead while keeping debugging capability
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);

            // Create an OkHttpClient with optimized settings for faster API calls
            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    // Reduced timeouts for faster failure detection and retry
                    .connectTimeout(10, TimeUnit.SECONDS) // Reduced from 30s
                    .readTimeout(15, TimeUnit.SECONDS)    // Reduced from 30s
                    .writeTimeout(20, TimeUnit.SECONDS)   // Slightly reduced but kept higher for file uploads
                    // Add connection pooling optimization
                    .connectionPool(new okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
                    // Enable response compression
                    .retryOnConnectionFailure(true)
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
