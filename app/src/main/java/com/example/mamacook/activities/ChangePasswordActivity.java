package com.example.mamacook.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mamacook.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etCurrentPassword, etNewPassword, etConfirmNewPassword;
    private TextView tvForgotPassword;
    private com.google.android.material.button.MaterialButton btnChangePassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        mAuth = FirebaseAuth.getInstance();

        initViews();

        btnBack.setOnClickListener(v -> finish());

        // Chuyển hướng sang màn hình Quên mật khẩu đã có sẵn
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(ChangePasswordActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        btnChangePassword.setOnClickListener(v -> validateAndChangePassword());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_change_pass_back);
        etCurrentPassword = findViewById(R.id.et_current_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmNewPassword = findViewById(R.id.et_confirm_new_password);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        btnChangePassword = findViewById(R.id.btn_change_password_submit);
    }

    private void validateAndChangePassword() {
        String currentPass = etCurrentPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(currentPass) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.length() < 6 || !newPass.matches(".*\\d.*")) {
            Toast.makeText(this, "Mật khẩu mới phải có tối thiểu 6 ký tự và bao gồm chữ số", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "Mật khẩu nhập lại không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);

            // Hiển thị trạng thái đang xử lý
            btnChangePassword.setEnabled(false);
            btnChangePassword.setText("Đang xử lý...");

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                        btnChangePassword.setEnabled(true);
                        btnChangePassword.setText("Cập nhật mật khẩu mới");
                        
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(ChangePasswordActivity.this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(ChangePasswordActivity.this, "Lỗi: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    btnChangePassword.setEnabled(true);
                    btnChangePassword.setText("Cập nhật mật khẩu mới");
                    Toast.makeText(ChangePasswordActivity.this, "Mật khẩu hiện tại không chính xác", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
