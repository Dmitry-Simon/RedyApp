package com.example.redyapp;

// Create RetrofitClient.java
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit; // Import TimeUnit
// import com.yourdomain.redyapp.BuildConfig; // If you use BuildConfig.DEBUG

public class RetrofitClient {
    private static final String BASE_URL = "https://watermelon-api-96308048537.me-west1.run.app/";
    private static Retrofit retrofit = null;

    public static ApiService getInstance() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            // Example: Only log in debug builds
            // if (BuildConfig.DEBUG) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            // } else {
            //     loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
            // }

            OkHttpClient httpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS) // Connection timeout
                    .readTimeout(30, TimeUnit.SECONDS)    // Read timeout
                    .writeTimeout(30, TimeUnit.SECONDS)   // Write timeout (important for uploads)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(httpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
