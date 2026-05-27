package com.example.mamacook.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mamacook.R;
import com.example.mamacook.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditAccountActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etName, etPhone, etEmail;
    private TextView tvRole, tvDate;
    private com.google.android.material.button.MaterialButton btnSave;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_account);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            finish();
            return;
        }
        uid = currentUser.getUid();

        initViews();
        setupLockedFields();
        loadUserData();

        btnBack.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> checkDataAndSave());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_edit_back);
        etName = findViewById(R.id.et_edit_name);
        etPhone = findViewById(R.id.et_edit_phone);
        etEmail = findViewById(R.id.et_edit_email);
        tvRole = findViewById(R.id.tv_edit_role);
        tvDate = findViewById(R.id.tv_edit_date);
        btnSave = findViewById(R.id.btn_save_profile);
    }

    private void setupLockedFields() {
        // Thông báo khi người dùng cố gắng sửa các trường bị khóa
        tvRole.setOnClickListener(v -> Toast.makeText(this, "không thể chỉnh sửa", Toast.LENGTH_SHORT).show());
        tvDate.setOnClickListener(v -> Toast.makeText(this, "không thể chỉnh sửa", Toast.LENGTH_SHORT).show());
    }

    private void loadUserData() {
        db.collection("nguoi_dung").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    etName.setText(user.getHo_ten());
                    etPhone.setText(user.getSo_dien_thoai());
                    etEmail.setText(user.getEmail());
                    
                    String role = user.getVai_tro();
                    tvRole.setText(role != null && role.equals("admin") ? "Quản trị viên" : "Người dùng");

                    if (user.getNgay_tao() != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        tvDate.setText(sdf.format(user.getNgay_tao().toDate()));
                    }
                }
            }
        });
    }

    private void checkDataAndSave() {
        String newName = etName.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(newEmail)) {
            Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra trùng số điện thoại (nếu có nhập)
        if (!TextUtils.isEmpty(newPhone)) {
            db.collection("nguoi_dung")
                    .whereEqualTo("so_dien_thoai", newPhone)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        boolean isDuplicate = false;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                            if (!doc.getId().equals(uid)) {
                                isDuplicate = true;
                                break;
                            }
                        }

                        if (isDuplicate) {
                            Toast.makeText(this, "Số điện thoại này đã được sử dụng", Toast.LENGTH_SHORT).show();
                        } else {
                            updateProfile(newName, newPhone, newEmail);
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi kiểm tra: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            updateProfile(newName, "", newEmail);
        }
    }

    private void updateProfile(String name, String phone, String email) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // 1. Cập nhật Email trong hệ thống Authentication trước
        user.updateEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 2. Nếu Auth thành công, cập nhật Firestore
                Map<String, Object> updates = new HashMap<>();
                updates.put("ho_ten", name);
                updates.put("so_dien_thoai", phone);
                updates.put("email", email);

                db.collection("nguoi_dung").document(uid).update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(EditAccountActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(EditAccountActivity.this, "Lỗi cập nhật Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                // Lỗi này thường do yêu cầu re-authenticate (người dùng cần đăng nhập lại gần đây để đổi email)
                Toast.makeText(this, "Lỗi cập nhật Email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
