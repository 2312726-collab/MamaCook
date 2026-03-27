package com.example.mamacook;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // KIỂM TRA TRẠNG THÁI ĐĂNG NHẬP
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                
                if (currentUser != null) {
                    // Nếu đã đăng nhập -> Vào thẳng Home
                    startActivity(new Intent(SplashActivity.this, HomeActivity.class));
                } else {
                    // Nếu chưa đăng nhập -> Vào trang Login (MainActivity)
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                }
                finish(); 
            }
        }, 3000);
    }
}
