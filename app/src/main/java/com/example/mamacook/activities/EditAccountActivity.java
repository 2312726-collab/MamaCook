package com.example.mamacook.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mamacook.R;
import com.example.mamacook.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditAccountActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etName, etPhone, etEmail, etDob;
    private TextView tvRole, tvDate;
    private Spinner spnGender;
    private com.google.android.material.button.MaterialButton btnSave;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String uid;
    private final Calendar calendar = Calendar.getInstance();

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
        setupGenderSpinner();
        setupDatePicker();
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
        etDob = findViewById(R.id.et_edit_dob);
        spnGender = findViewById(R.id.spn_edit_gender);
        tvRole = findViewById(R.id.tv_edit_role);
        tvDate = findViewById(R.id.tv_edit_date);
        btnSave = findViewById(R.id.btn_save_profile);
    }

    private void setupGenderSpinner() {
        String[] genders = {"Nam", "Nữ", "Khác"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genders);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnGender.setAdapter(adapter);
    }

    private void setupDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            etDob.setText(sdf.format(calendar.getTime()));
        };

        etDob.setOnClickListener(v -> {
            new DatePickerDialog(this, dateSetListener, 
                calendar.get(Calendar.YEAR), 
                calendar.get(Calendar.MONTH), 
                calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupLockedFields() {
        tvRole.setOnClickListener(v -> Toast.makeText(this, "không thể chỉnh sửa", Toast.LENGTH_SHORT).show());
        tvDate.setOnClickListener(v -> Toast.makeText(this, "không thể chỉnh sửa", Toast.LENGTH_SHORT).show());
    }

    private void loadUserData() {
        db.collection("nguoi_dung").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    etName.setText(user.getHo_ten());
                    etPhone.setText(user.getSo_dien_thoai());
                    etEmail.setText(user.getEmail());
                    etDob.setText(user.getNgay_sinh());
                    
                    if (user.getGioi_tinh() != null) {
                        String g = user.getGioi_tinh();
                        if (g.equals("Nam")) spnGender.setSelection(0);
                        else if (g.equals("Nữ")) spnGender.setSelection(1);
                        else spnGender.setSelection(2);
                    }

                    tvRole.setText("admin".equals(user.getRole()) ? "Quản trị viên" : "Người dùng");
                    if (user.getNgay_tao() != null) {
                        tvDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(user.getNgay_tao().toDate()));
                    }
                }
            }
        });
    }

    private void checkDataAndSave() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String dob = etDob.getText().toString().trim();
        String gender = spnGender.getSelectedItem().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Họ tên và Email không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        updateProfile(name, phone, email, dob, gender);
    }

    private void updateProfile(String name, String phone, String email, String dob, String gender) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        user.updateEmail(email).addOnCompleteListener(task -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("ho_ten", name);
            updates.put("so_dien_thoai", phone);
            updates.put("email", email);
            updates.put("ngay_sinh", dob);
            updates.put("gioi_tinh", gender);

            db.collection("nguoi_dung").document(uid).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }
}
