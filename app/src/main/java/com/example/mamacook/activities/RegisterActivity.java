package com.example.mamacook.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import com.example.mamacook.R;
import com.example.mamacook.models.User;
import com.google.firebase.FirebaseException;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText etName, etEmail, etPhone, etOtp, etPassword, etConfirmPassword;
    private AppCompatButton btnRegister, btnSendOtp;
    private View btnNavLogin;

    private TextView tabEmail, tabPhone;
    private LinearLayout layoutEmailRegister, layoutPhoneRegister;
    private boolean isEmailMode = true;

    private String mVerificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 1. Ánh xạ View
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etOtp = findViewById(R.id.et_otp);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        
        btnRegister = findViewById(R.id.btn_register);
        btnSendOtp = findViewById(R.id.btn_send_otp);
        btnNavLogin = findViewById(R.id.btn_nav_login);

        tabEmail = findViewById(R.id.tab_email);
        tabPhone = findViewById(R.id.tab_phone);
        layoutEmailRegister = findViewById(R.id.layout_email_register);
        layoutPhoneRegister = findViewById(R.id.layout_phone_register);

        // 2. Sự kiện chuyển Tab
        tabEmail.setOnClickListener(v -> switchTab(true));
        tabPhone.setOnClickListener(v -> switchTab(false));

        // 3. Gửi OTP
        btnSendOtp.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            if (TextUtils.isEmpty(phone)) {
                Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
                return;
            }
            sendOtp(phone);
        });

        // 4. Nút Đăng ký
        btnRegister.setOnClickListener(v -> registerUser());

        btnNavLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        });
    }

    private void sendOtp(String phone) {
        String formattedPhone = phone;
        if (phone.startsWith("0")) {
            formattedPhone = "+84" + phone.substring(1);
        } else if (!phone.startsWith("+")) {
            formattedPhone = "+84" + phone;
        }

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(formattedPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {}

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(RegisterActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        mVerificationId = verId;
                        etOtp.setVisibility(View.VISIBLE);
                        Toast.makeText(RegisterActivity.this, "Mã OTP đã được gửi!", Toast.LENGTH_SHORT).show();
                    }
                }).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(password) || !password.equals(confirmPassword)) {
            Toast.makeText(this, "Thông tin không hợp lệ hoặc mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEmailMode) {
            String email = etEmail.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
            performRegister(email, password, name, "", email);
        } else {
            String otp = etOtp.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            if (TextUtils.isEmpty(otp)) {
                Toast.makeText(this, "Vui lòng nhập mã OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            // Xác thực OTP trước khi tạo account
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, otp);
            mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // OTP đúng -> Tạo account email ảo để đồng bộ login
                    String fakeEmail = phone + "@mamacook.com";
                    performRegister(fakeEmail, password, name, phone, "");
                } else {
                    Toast.makeText(this, "Mã OTP không chính xác!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void performRegister(String authEmail, String password, String name, String phone, String realEmail) {
        mAuth.createUserWithEmailAndPassword(authEmail, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveUserToFirestore(task.getResult().getUser().getUid(), name, phone, realEmail, password);
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "Tài khoản đã tồn tại!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Lỗi đăng ký: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserToFirestore(String uid, String name, String phone, String email, String pass) {
        User user = new User();
        user.setId_nguoi_dung(uid);
        user.setHo_ten(name);
        user.setSo_dien_thoai(phone);
        user.setEmail(email);
        user.setMat_khau(pass); // Lưu để demo
        user.setNgay_tao(Timestamp.now());
        user.setTrang_thai_tai_khoan("dang_hoat_dong");
        user.setVai_tro("user");

        db.collection("nguoi_dung").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                });
    }

    private void switchTab(boolean emailMode) {
        isEmailMode = emailMode;
        if (emailMode) {
            layoutEmailRegister.setVisibility(View.VISIBLE);
            layoutPhoneRegister.setVisibility(View.GONE);
            tabEmail.setBackgroundResource(R.drawable.bg_register_button);
            tabEmail.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tabPhone.setBackground(null);
            tabPhone.setTextColor(ContextCompat.getColor(this, R.color.brown_dark));
        } else {
            layoutEmailRegister.setVisibility(View.GONE);
            layoutPhoneRegister.setVisibility(View.VISIBLE);
            tabPhone.setBackgroundResource(R.drawable.bg_register_button);
            tabPhone.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tabEmail.setBackground(null);
            tabEmail.setTextColor(ContextCompat.getColor(this, R.color.brown_dark));
        }
    }
}
