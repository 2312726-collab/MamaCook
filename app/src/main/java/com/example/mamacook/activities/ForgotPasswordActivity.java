package com.example.mamacook.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
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

    private boolean isPhoneMethod = false;
    private String mVerificationId;
    private String phoneNumberInput; 

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

        btnEmail.setOnClickListener(v -> { isPhoneMethod = false; etInput.setHint("Nhập Email đã đăng ký"); showLayout(layoutInput); });
        btnPhone.setOnClickListener(v -> { isPhoneMethod = true; etInput.setHint("Nhập Số điện thoại"); showLayout(layoutInput); });

        btnSendCode.setOnClickListener(v -> {
            String input = etInput.getText().toString().trim();
            if (TextUtils.isEmpty(input)) return;
            if (isPhoneMethod) { phoneNumberInput = input; handleSendOtp(input); }
            else { handleSendEmailReset(input); }
        });

        btnVerify.setOnClickListener(v -> handleVerifyOtp());
        btnUpdate.setOnClickListener(v -> handleUpdatePassword());
        btnBack.setOnClickListener(v -> finish());
    }

    private void handleSendOtp(String phone) {
        String formattedPhone = phone.startsWith("0") ? "+84" + phone.substring(1) : (phone.startsWith("+") ? phone : "+84" + phone);
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(formattedPhone)
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
                        showLayout(layoutVerify);
                    }
                }).build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void handleVerifyOtp() {
        String code = etOtp.getText().toString().trim();
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) { showLayout(layoutReset); }
            else { Toast.makeText(this, "Mã OTP sai!", Toast.LENGTH_SHORT).show(); }
        });
    }

    private void handleUpdatePassword() {
        String newPass = etNewPass.getText().toString().trim();
        if (newPass.length() < 6) return;

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // 1. Cập nhật mật khẩu trên Firebase Authentication
            user.updatePassword(newPass).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // 2. Cập nhật mật khẩu trên Firestore để đồng bộ
                    updateFirestorePassword(newPass);
                } else {
                    Toast.makeText(this, "Lỗi cập nhật Auth: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateFirestorePassword(String newPass) {
        db.collection("nguoi_dung")
                .whereEqualTo("so_dien_thoai", phoneNumberInput)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        db.collection("nguoi_dung").document(docId).update("mat_khau", newPass)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Đã cập nhật mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                                    mAuth.signOut();
                                    finish();
                                });
                    }
                });
    }

    private void handleSendEmailReset(String email) {
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) { Toast.makeText(this, "Đã gửi link vào Email", Toast.LENGTH_SHORT).show(); finish(); }
        });
    }

    private void showLayout(LinearLayout layoutToShow) {
        layoutSelect.setVisibility(View.GONE);
        layoutInput.setVisibility(View.GONE);
        layoutVerify.setVisibility(View.GONE);
        layoutReset.setVisibility(View.GONE);
        layoutToShow.setVisibility(View.VISIBLE);
    }
}
