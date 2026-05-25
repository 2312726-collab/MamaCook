package com.example.mamacook.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private int dataLoadedCount = 0;
    private static final int TOTAL_REQUIRED_DATA = 2; 
    private boolean isNavigated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- KHỞI TẠO APP CHECK DEBUG ---
        FirebaseApp.initializeApp(this);
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance());
        // --------------------------------

        // Luôn kiểm tra Auth ngay lập tức
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // ĐÃ ĐĂNG NHẬP: Hiện màn hình Splash và tải trước dữ liệu
            setContentView(com.example.mamacook.R.layout.activity_splash);
            startPreloadingData();
            
            // Timeout đề phòng lỗi mạng
            new Handler().postDelayed(() -> {
                if (!isNavigated) navigateToHome();
            }, 5000);
            
        } else {
            // CHƯA ĐĂNG NHẬP: Vào thẳng MainActivity ngay lập tức để tránh lộ Home
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }
    }

    private void startPreloadingData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("mon_an").orderBy("ngay_tao", Query.Direction.DESCENDING).limit(10).get()
                .addOnCompleteListener(task -> checkDataLoadingProgress());

        db.collection("mon_an").orderBy("luot_xem", Query.Direction.DESCENDING).limit(10).get()
                .addOnCompleteListener(task -> checkDataLoadingProgress());
    }

    private synchronized void checkDataLoadingProgress() {
        dataLoadedCount++;
        if (dataLoadedCount >= TOTAL_REQUIRED_DATA) {
            navigateToHome();
        }
    }

    private void navigateToHome() {
        if (isNavigated) return;
        isNavigated = true;
        startActivity(new Intent(SplashActivity.this, HomeActivity.class));
        finish();
    }
}
