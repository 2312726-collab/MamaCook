package com.example.mamacook.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mamacook.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private int dataLoadedCount = 0;
    private static final int TOTAL_REQUIRED_DATA = 2; // Số lượng query cần hoàn thành
    private boolean isNavigated = false;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        tvStatus = findViewById(R.id.tvLoadingStatus);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Nếu đã đăng nhập, tiến hành load dữ liệu quan trọng trước khi vào Home
            startPreloadingData();
            
            // Đặt một giới hạn thời gian (Timeout) 10 giây để tránh treo ở Splash nếu mạng quá yếu
            new Handler().postDelayed(() -> {
                if (!isNavigated) {
                    Log.w(TAG, "Loading timeout, navigating to Home anyway.");
                    navigateToHome();
                }
            }, 10000);
            
        } else {
            // Nếu chưa đăng nhập, chỉ chờ 2 giây để hiện logo rồi vào màn hình đăng nhập
            new Handler().postDelayed(() -> {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }, 2000);
        }
    }

    private void startPreloadingData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Tải danh sách món ăn mới nhất
        db.collection("mon_an")
                .orderBy("ngay_tao", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    checkDataLoadingProgress();
                });

        // 2. Tải danh sách món ăn nổi bật
        db.collection("mon_an")
                .orderBy("luot_xem", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    checkDataLoadingProgress();
                });
    }

    private synchronized void checkDataLoadingProgress() {
        dataLoadedCount++;
        if (dataLoadedCount >= TOTAL_REQUIRED_DATA) {
            // Đã load xong hết các dữ liệu cần thiết
            navigateToHome();
        }
    }

    private void navigateToHome() {
        if (isNavigated) return;
        isNavigated = true;
        
        startActivity(new Intent(SplashActivity.this, HomeActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
