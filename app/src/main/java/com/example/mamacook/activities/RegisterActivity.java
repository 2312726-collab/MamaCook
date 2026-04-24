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

import com.example.mamacook.R;
import com.example.mamacook.models.User;
import com.google.firebase.FirebaseException;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    private EditText etName, etEmail, etPassword, etConfirmPassword, etPhone, etOtp;
    private LinearLayout layoutEmailRegister, layoutPhoneRegister;
    private AppCompatButton btnRegister, btnSendOtp;
    private View btnNavLogin;
    private TextView tabEmail, tabPhone;

    private boolean isPhoneRegistration = false;
    private String mVerificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etOtp = findViewById(R.id.et_otp);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        
        layoutEmailRegister = findViewById(R.id.layout_email_register);
        layoutPhoneRegister = findViewById(R.id.layout_phone_register);
        
        btnRegister = findViewById(R.id.btn_register);
        btnSendOtp = findViewById(R.id.btn_send_otp);
        btnNavLogin = findViewById(R.id.btn_nav_login);
        
        tabEmail = findViewById(R.id.tab_email);
        tabPhone = findViewById(R.id.tab_phone);

        tabPhone.setOnClickListener(v -> toggleRegistrationMode(true));
        tabEmail.setOnClickListener(v -> toggleRegistrationMode(false));

        btnRegister.setOnClickListener(v -> {
            if (isPhoneRegistration) verifyOtpAndRegister();
            else registerWithEmail();
        });

        btnSendOtp.setOnClickListener(v -> startPhoneVerification());

        btnNavLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
            finish();
        });
    }

    private void toggleRegistrationMode(boolean isPhone) {
        isPhoneRegistration = isPhone;
        if (isPhone) {
            // Chuyển giao diện sang Phone
            layoutEmailRegister.setVisibility(View.GONE);
            layoutPhoneRegister.setVisibility(View.VISIBLE);
            
            // Cập nhật UI Tab
            tabPhone.setBackgroundResource(R.drawable.bg_register_button);
            tabPhone.setTextColor(getResources().getColor(android.R.color.white));
            tabEmail.setBackground(null);
            tabEmail.setTextColor(getResources().getColor(R.color.black));
            
            btnRegister.setText("Xác thực & Đăng ký");
        } else {
            // Chuyển giao diện sang Email
            layoutEmailRegister.setVisibility(View.VISIBLE);
            layoutPhoneRegister.setVisibility(View.GONE);
            etOtp.setVisibility(View.GONE);

            // Cập nhật UI Tab
            tabEmail.setBackgroundResource(R.drawable.bg_register_button);
            tabEmail.setTextColor(getResources().getColor(android.R.color.white));
            tabPhone.setBackground(null);
            tabPhone.setTextColor(getResources().getColor(R.color.black));
            
            btnRegister.setText("Đăng Ký Ngay");
        }
    }

    private void registerWithEmail() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { etName.setError("Vui lòng nhập tên"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError("Email không hợp lệ"); return; }
        if (password.length() < 6 || !password.equals(confirm)) { Toast.makeText(this, "Kiểm tra lại mật khẩu", Toast.LENGTH_SHORT).show(); return; }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) saveUserToFirestore(mAuth.getCurrentUser(), name, email, "");
                    else handleAuthError(task.getException());
                });
    }

    private void startPhoneVerification() {
        String phone = etPhone.getText().toString().trim();
        String name = etName.getText().toString().trim();
        
        if (TextUtils.isEmpty(name)) { etName.setError("Bắt buộc nhập tên khi đăng ký SĐT"); return; }
        if (phone.length() < 10) { etPhone.setError("SĐT không hợp lệ"); return; }

        String phoneNumber = phone.startsWith("0") ? "+84" + phone.substring(1) : phone;

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        etOtp.setText(credential.getSmsCode());
                    }
                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(RegisterActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onCodeSent(@NonNull String verId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        mVerificationId = verId;
                        etOtp.setVisibility(View.VISIBLE);
                        Toast.makeText(RegisterActivity.this, "Đã gửi mã OTP", Toast.LENGTH_SHORT).show();
                    }
                }).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyOtpAndRegister() {
        String otp = etOtp.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(otp)) { etOtp.setError("Nhập mã OTP"); return; }
        if (password.length() < 6 || !password.equals(confirm)) { Toast.makeText(this, "Kiểm tra mật khẩu", Toast.LENGTH_SHORT).show(); return; }

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, otp);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                String phone = etPhone.getText().toString().trim();
                String name = etName.getText().toString().trim();
                
                // LIÊN KẾT: Tạo thông tin email để liên kết vào tài khoản SĐT vừa verify
                com.google.firebase.auth.AuthCredential emailCred = com.google.firebase.auth.EmailAuthProvider.getCredential(phone + "@mamacook.com", password);
                
                user.linkWithCredential(emailCred).addOnCompleteListener(linkTask -> {
                    // Dù link thành công hay thất bại (nếu đã tồn tại), ta vẫn lưu Firestore với UID hiện tại
                    saveUserToFirestore(user, name, "", phone);
                });
            } else {
                Toast.makeText(this, "Mã OTP sai", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleAuthError(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) Toast.makeText(this, "Tài khoản đã tồn tại!", Toast.LENGTH_SHORT).show();
        else Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
    }

    private void saveUserToFirestore(FirebaseUser firebaseUser, String name, String email, String phone) {
        if (firebaseUser == null) return;
        User user = new User();
        user.setId_nguoi_dung(firebaseUser.getUid());
        user.setHo_ten(name);
        user.setEmail(email);
        user.setSo_dien_thoai(phone);
        user.setNgay_tao(Timestamp.now());
        user.setTrang_thai_tai_khoan("dang_hoat_dong");
        user.setVai_tro("user");

        db.collection("nguoi_dung").document(firebaseUser.getUid()).set(user).addOnSuccessListener(aVoid -> {
            Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
            finish();
        });
    }
}
