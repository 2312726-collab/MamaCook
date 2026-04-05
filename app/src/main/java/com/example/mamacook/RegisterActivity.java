package com.example.mamacook;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private AppCompatButton btnRegister;
    private TextView btnNavLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email); // Ô này nhập SĐT hoặc Email
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        btnNavLogin = findViewById(R.id.btn_nav_login);

        btnRegister.setOnClickListener(v -> registerUser());

        btnNavLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String input = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(input) || 
            TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Xử lý xác định là Email hay Số điện thoại
        String finalEmailForAuth = input;
        String phoneNumber = "";
        String realEmail = "";

        if (input.matches("\\d+")) { // Nếu chỉ chứa số -> Là số điện thoại
            if (input.length() < 10 || input.length() > 11) {
                Toast.makeText(this, "Số điện thoại không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
            phoneNumber = input;
            // Tạo một email ảo để Firebase Auth chấp nhận (vì đăng ký email/pass cần định dạng email)
            finalEmailForAuth = input + "@mamacook.com";
        } else { // Ngược lại coi là Email
            if (!Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                Toast.makeText(this, "Định dạng Email không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
            realEmail = input;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu nhập lại không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải từ 6 ký tự trở lên", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalPhoneNumber = phoneNumber;
        String finalRealEmail = realEmail;
        
        mAuth.createUserWithEmailAndPassword(finalEmailForAuth, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserToFirestore(mAuth.getCurrentUser(), name, password, finalRealEmail, finalPhoneNumber);
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(RegisterActivity.this, "Email hoặc số điện thoại này đã được đăng ký!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser firebaseUser, String name, String password, String email, String phone) {
        if (firebaseUser == null) return;

        String uid = firebaseUser.getUid();

        User user = new User();
        user.setId_nguoi_dung(uid);
        user.setHo_ten(name);
        user.setEmail(email);
        user.setSo_dien_thoai(phone);
        user.setMat_khau(password);
        user.setNgay_tao(Timestamp.now());
        user.setTrang_thai_tai_khoan("dang_hoat_dong");

        db.collection("nguoi_dung").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Lỗi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
