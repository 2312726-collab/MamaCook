package com.example.mamacook.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.mamacook.R;
import com.example.mamacook.models.User;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AccountActivity extends AppCompatActivity {

    private ImageView btnBack, imgAvatar;
    private TextView tvDisplayName, tvDisplayEmail, tvInfoName, tvInfoEmail, tvInfoPhone, tvInfoRole, tvInfoDate;
    private LinearLayout btnEditProfile, btnChangePassword;
    private com.google.android.material.button.MaterialButton btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadUserData();

        btnBack.setOnClickListener(v -> finish());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, EditAccountActivity.class);
            startActivity(intent);
        });

        btnChangePassword.setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        imgAvatar.setOnClickListener(v -> showAvatarOptions());
        imgAvatar.setOnLongClickListener(v -> {
            showAvatarOptions();
            return true;
        });
    }

    private void showAvatarOptions() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_change_avatar_options, null);
        bottomSheetDialog.setContentView(view);

        LinearLayout btnUpload = view.findViewById(R.id.btn_upload_photo);
        LinearLayout btnTake = view.findViewById(R.id.btn_take_photo);
        LinearLayout btnView = view.findViewById(R.id.btn_view_photo);

        btnUpload.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Tính năng tải ảnh đang phát triển", Toast.LENGTH_SHORT).show();
        });

        btnTake.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Tính năng chụp ảnh đang phát triển", Toast.LENGTH_SHORT).show();
        });

        btnView.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Toast.makeText(this, "Tính năng xem ảnh đang phát triển", Toast.LENGTH_SHORT).show();
        });

        bottomSheetDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        imgAvatar = findViewById(R.id.img_avatar);
        tvDisplayName = findViewById(R.id.tv_display_name);
        tvDisplayEmail = findViewById(R.id.tv_display_email);
        tvInfoName = findViewById(R.id.tv_info_name);
        tvInfoEmail = findViewById(R.id.tv_info_email);
        tvInfoPhone = findViewById(R.id.tv_info_phone);
        tvInfoRole = findViewById(R.id.tv_info_role);
        tvInfoDate = findViewById(R.id.tv_info_date);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            finish();
            return;
        }

        db.collection("nguoi_dung")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) displayData(user);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void displayData(User user) {
        if (user.getAnh_dai_dien() != null && !user.getAnh_dai_dien().isEmpty()) {
            Glide.with(this).load(user.getAnh_dai_dien()).placeholder(R.drawable.ic_nav_profile).into(imgAvatar);
        }

        tvDisplayName.setText(user.getHo_ten() != null ? user.getHo_ten() : "N/A");
        
        // Hiếu: Luôn hiển thị Email (thật hoặc ảo) ở vị trí dưới tên
        tvDisplayEmail.setText(user.getEmail());

        // Các thông tin chi tiết bên dưới thẻ CardView
        tvInfoName.setText(user.getHo_ten());
        tvInfoEmail.setText(user.getEmail());
        
        if (user.getSo_dien_thoai() != null && !user.getSo_dien_thoai().isEmpty()) {
            tvInfoPhone.setText(user.getSo_dien_thoai());
        } else {
            tvInfoPhone.setText("Chưa cập nhật");
        }

        String role = user.getRole();
        if (role == null) role = "user";
        tvInfoRole.setText(role.equals("admin") ? "Quản trị viên" : "Người dùng");

        if (user.getNgay_tao() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvInfoDate.setText(sdf.format(user.getNgay_tao().toDate()));
        }
    }
}
