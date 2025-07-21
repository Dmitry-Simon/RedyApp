# 🍉 RedyApp: Watermelon Ripeness Classification Frontend

Android app for classifying watermelon ripeness from tap audio using deep learning.  
**This is the frontend part of "Project Phase B" of the final project.**

[🔗 Link for download the APK](https://drive.google.com/file/d/18OC_MR-IxNMIf2QQ-rpTmzTqaw61SDS5/view?usp=sharing)
---
[🔗 Backend (FastAPI, ECAPA-TDNN) code](https://github.com/Dmitry-Simon/RedGreen)
---

## 🏗️ Project Structure

```
app/
  ├── src/main/java/com/example/redyapp/
      ├── MainActivity.java                 # Main recording & prediction screen
      ├── SettingsActivity.java             # App settings
      ├── ApiService.java                   # Retrofit API interface
      ├── RetrofitClient.java               # HTTP client config
      ├── PredictionResponse.java           # API response model
      ├── History/
          ├── HistoryActivity.java          # Prediction history screen
          ├── HistoryAdapter.java           # RecyclerView adapter
          ├── HistoryDatabase.java          # Room DB config
          ├── HistoryDao.java               # DB access object
          └── HistoryItem.java              # History data model
      └── LogReg/
          ├── MainLogRegActivity.java       # Login/Register entry
          ├── LoginActivity.java            # Login screen
          ├── RegisterActivity.java         # Registration screen
          └── ForgotPasswordActivity.java   # Password recovery
  ├── src/main/res/
      ├── layout/                           # XML layouts
      ├── values/                           # Strings, colors, styles
      └── drawable/                         # Icons, graphics
  └── build.gradle                          # Dependencies
```

---

## 🚀 Quick Start

### 1. Setup

- Install Android Studio (API 21+)
- Clone this repo and open in Android Studio

### 2. Configure API

- Set backend URL in `RetrofitClient.java`:
  ```java
  private static final String BASE_URL = "http://your-backend-server:8000/";
  ```

### 3. Build & Run

- Build and install:
  ```bash
  ./gradlew assembleDebug
  ./gradlew installDebug
  ```

### 4. Authentication

- Configure Firebase Auth in `google-services.json`
- Enable Email/Password in Firebase Console

---

## 📱 Features

- **Audio Recording:** Tap to record 5s watermelon thump
- **File Upload:** Long press mic to upload WAV file
- **Prediction:** Shows ripeness and confidence
- **History:** Local storage of predictions and audio
- **Authentication:** Email/password login, registration, password reset

---

## 🧠 Model & Features

- **Backend Model:** ECAPA-TDNN (see backend)
- **Input:** Mel spectrogram ([1, 64, 512])
- **Classes:** `low_sweet`, `sweet`, `un_sweet`, `very_sweet`
- **Audio:** 16kHz, 64 mel bands, 50Hz–8kHz, pre-emphasis α=0.97

---

## 🛠️ Utilities

- **Permissions:**
  ```xml
  <uses-permission android:name="android.permission.RECORD_AUDIO" />
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
  ```
- **Dependencies:**
  ```gradle
  implementation 'com.squareup.retrofit2:retrofit:2.9.0'
  implementation 'com.google.firebase:firebase-auth:21.1.0'
  implementation 'androidx.room:room-runtime:2.4.3'
  implementation 'com.google.android.material:material:1.7.0'
  ```

---

## 📄 API Response Example

```json
{
  "predicted_label": "sweet",
  "confidence": 0.87
}
```

---

## 📈 Performance Tracking

- History stored in local Room DB
- Audio files saved in app storage

---

Built with 🍉 by the Redy team.
