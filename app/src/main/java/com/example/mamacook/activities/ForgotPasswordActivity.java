package com.example.mamacook.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.mamacook.R;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class ForgotPasswordActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout layoutSelect, layoutInput, layoutVerify, layoutReset;
    private AppCompatButton btnEmail, btnPhone, btnSendCode, btnVerify, btnUpdate;
    private EditText etInput, etOtp, etNewPass, etConfirmNew;
    private View btnBack;

    private String mVerificationId;
    private boolean isPhoneMethod = false;
    private String userAccountInput = ""; // Lưu lại SĐT hoặc Email

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        layoutSelect = findViewById(R.id.layout_select_method);
        layoutInput = findViewById(R.id.layout_input_info);
        layoutVerify = findViewById(R.id.layout_verify_otp);
        layoutReset = findViewById(R.id.layout_reset_password);

        btnEmail = findViewById(R.id.btn_method_email);
        btnPhone = findViewById(R.id.btn_method_phone);
        btnSendCode = findViewById(R.id.btn_send_code);
        btnVerify = findViewById(R.id.btn_verify_forgot);
        btnUpdate = findViewById(R.id.btn_update_password);

        etInput = findViewById(R.id.et_forgot_input);
        etOtp = findViewById(R.id.et_otp_forgot);
        etNewPass = findViewById(R.id.et_new_password);
        etConfirmNew = findViewById(R.id.et_confirm_new_password);
        btnBack = findViewById(R.id.btn_back_to_login);

        btnEmail.setOnClickListener(v -> { isPhoneMethod = false; showInputLayout("Nhập Email"); });
        btnPhone.setOnClickListener(v -> { isPhoneMethod = true; showInputLayout("Nhập Số điện thoại"); });

        btnSendCode.setOnClickListener(v -> handleSendCode());
        btnVerify.setOnClickListener(v -> handleVerifyCode());
        btnUpdate.setOnClickListener(v -> handleUpdatePassword());

        btnBack.setOnClickListener(v -> finish());
    }

    private void showInputLayout(String hint) {
        layoutSelect.setVisibility(View.GONE);
        layoutInput.setVisibility(View.VISIBLE);
        etInput.setHint(hint);
    }

    private void handleSendCode() {
        userAccountInput = etInput.getText().toString().trim();
        if (TextUtils.isEmpty(userAccountInput)) return;

        if (isPhoneMethod) {
            String phoneNumber = userAccountInput.startsWith("0") ? "+84" + userAccountInput.substring(1) : userAccountInput;
            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {}
                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) { Toast.makeText(ForgotPasswordActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show(); }
                        @Override
                        public void onCodeSent(@NonNull String verId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                            mVerificationId = verId;
                            layoutInput.setVisibility(View.GONE);
                            layoutVerify.setVisibility(View.VISIBLE);
                        }
                    }).build();
            PhoneAuthProvider.verifyPhoneNumber(options);
        } else {
            mAuth.sendPasswordResetEmail(userAccountInput).addOnCompleteListener(task -> {
                if (task.isSuccessful()) { Toast.makeText(this, "Link đổi mật khẩu đã gửi vào Email", Toast.LENGTH_LONG).show(); finish(); }
                else { Toast.makeText(this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show(); }
            });
        }
    }

    private void handleVerifyCode() {
        String code = etOtp.getText().toString().trim();
        if (TextUtils.isEmpty(code)) return;
        
        // Xác thực mã OTP và đăng nhập tạm thời để có quyền đổi mật khẩu
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                layoutVerify.setVisibility(View.GONE);
                layoutReset.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Mã OTP không chính xác!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleUpdatePassword() {
        String newPass = etNewPass.getText().toString().trim();
        String confirm = etConfirmNew.getText().toString().trim();

        if (newPass.length() < 6 || !newPass.equals(confirm)) {
            Toast.makeText(this, "Mật khẩu không khớp hoặc quá ngắn", Toast.LENGTH_SHORT).show();
            return;
        }
// QUân thêm phần này để đổi mật khẩu trên Firebase Authentication
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // 1. Cập nhật mật khẩu trên Firebase Authentication trước
            user.updatePassword(newPass).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // 2. Sau đó mới cập nhật vào Firestore để đồng bộ
                    updatePasswordInFirestore(newPass);
                } else {
                    // Nếu lỗi do hết hạn session, yêu cầu login lại (hiếm gặp sau khi vừa verify OTP)
                    Toast.makeText(this, "Lỗi xác thực: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else if (!isPhoneMethod) {
            // Đối với Email, thường dùng link reset nên không cần xử lý update pass thủ công ở đây
            // Nhưng nếu bạn muốn tự làm giao diện thì cần logic re-authenticate
            Toast.makeText(this, "Vui lòng kiểm tra Email để đổi mật khẩu", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePasswordInFirestore(String newPass) {
        // Tìm user theo SĐT hoặc Email để cập nhật mật khẩu trong Firestore
        String field = isPhoneMethod ? "so_dien_thoai" : "email";
        
        db.collection("nguoi_dung")
                .whereEqualTo(field, userAccountInput)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        db.collection("nguoi_dung").document(docId)
                                .update("mat_khau", newPass)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_LONG).show();
                                    mAuth.signOut(); // Đăng xuất để yêu cầu login lại bằng pass mới
                                    finish();
                                });
                    }
                });
    }
}
