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
    private EditText etName, etPhone;
    private TextView tvEmail, tvRole, tvDate;
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

        btnSave.setOnClickListener(v -> checkPhoneAndSave());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_edit_back);
        etName = findViewById(R.id.et_edit_name);
        etPhone = findViewById(R.id.et_edit_phone);
        tvEmail = findViewById(R.id.tv_edit_email);
        tvRole = findViewById(R.id.tv_edit_role);
        tvDate = findViewById(R.id.tv_edit_date);
        btnSave = findViewById(R.id.btn_save_profile);
    }

    private void setupLockedFields() {
        // Hiếu: Thiết lập thông báo "Không thể sửa!" khi click vào các trường bị khóa
        tvEmail.setOnClickListener(v -> Toast.makeText(this, "Không thể sửa!", Toast.LENGTH_SHORT).show());
        tvRole.setOnClickListener(v -> Toast.makeText(this, "Không thể sửa!", Toast.LENGTH_SHORT).show());
        tvDate.setOnClickListener(v -> Toast.makeText(this, "Không thể sửa!", Toast.LENGTH_SHORT).show());
    }

    private void loadUserData() {
        db.collection("nguoi_dung").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    etName.setText(user.getHo_ten());
                    etPhone.setText(user.getSo_dien_thoai());
                    tvEmail.setText(user.getEmail());
                    
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

    private void checkPhoneAndSave() {
        String newName = etName.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hiếu: Kiểm tra nếu số điện thoại không trống thì check trùng
        if (!TextUtils.isEmpty(newPhone)) {
            db.collection("nguoi_dung")
                    .whereEqualTo("so_dien_thoai", newPhone)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        boolean isDuplicate = false;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                            if (!doc.getId().equals(uid)) { // Trùng với người khác (không phải mình)
                                isDuplicate = true;
                                break;
                            }
                        }

                        if (isDuplicate) {
                            Toast.makeText(this, "Số điện thoại này đã được sử dụng", Toast.LENGTH_SHORT).show();
                        } else {
                            updateProfile(newName, newPhone);
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi kiểm tra: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            updateProfile(newName, "");
        }
    }

    private void updateProfile(String name, String phone) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("ho_ten", name);
        updates.put("so_dien_thoai", phone);

        db.collection("nguoi_dung").document(uid).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditAccountActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(EditAccountActivity.this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
