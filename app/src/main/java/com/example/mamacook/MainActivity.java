package com.example.mamacook;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView btnNavRegister;
    private Button btnLoginMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        db = FirebaseFirestore.getInstance();
        
        btnNavRegister = findViewById(R.id.btn_nav_register);
        btnLoginMain = findViewById(R.id.btn_login_main);

        // Chuyển sang trang Đăng ký
        if (btnNavRegister != null) {
            btnNavRegister.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
            });
        }

        // Xử lý Đăng nhập để vào Home
        if (btnLoginMain != null) {
            btnLoginMain.setOnClickListener(v -> {
                // Tạm thời cho phép vào luôn Home. 
                // Sau này bạn có thể thêm code kiểm tra email/password tại đây.
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
                finish(); // Kết thúc màn hình đăng nhập
            });
        }
    }
}
