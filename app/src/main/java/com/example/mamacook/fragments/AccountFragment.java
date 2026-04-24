package com.example.mamacook.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.mamacook.R;
import com.example.mamacook.activities.MainActivity;
import com.example.mamacook.models.User;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AccountFragment extends Fragment {

    private ImageView imgAvatar;
    private TextView tvDisplayName, tvDisplayEmail, tvInfoName, tvInfoEmail, tvInfoPhone, tvInfoRole, tvInfoDate;
    private LinearLayout btnEditProfile, btnChangePassword;
    private com.google.android.material.button.MaterialButton btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews(view);
        loadUserData();

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });

        btnEditProfile.setOnClickListener(v -> {
            // Chuyển sang màn hình sửa (Giữ nguyên Activity vì đây là luồng phụ)
            Toast.makeText(getContext(), "Chức năng đang được cập nhật", Toast.LENGTH_SHORT).show();
        });

        btnChangePassword.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Chức năng đang được cập nhật", Toast.LENGTH_SHORT).show();
        });

        imgAvatar.setOnClickListener(v -> showAvatarOptions());

        return view;
    }

    private void showAvatarOptions() {
        if (getContext() == null) return;
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        // Lưu ý: Cần có layout_change_avatar_options.xml, nếu chưa có sẽ báo lỗi đỏ
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_change_avatar_options, null, false);
        bottomSheetDialog.setContentView(view);

        view.findViewById(R.id.btn_upload_photo).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Toast.makeText(getContext(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
        });

        bottomSheetDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    private void initViews(View view) {
        imgAvatar = view.findViewById(R.id.img_avatar);
        tvDisplayName = view.findViewById(R.id.tv_display_name);
        tvDisplayEmail = view.findViewById(R.id.tv_display_email);
        tvInfoName = view.findViewById(R.id.tv_info_name);
        tvInfoEmail = view.findViewById(R.id.tv_info_email);
        tvInfoPhone = view.findViewById(R.id.tv_info_phone);
        tvInfoRole = view.findViewById(R.id.tv_info_role);
        tvInfoDate = view.findViewById(R.id.tv_info_date);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnChangePassword = view.findViewById(R.id.btn_change_password);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) return;

        db.collection("nguoi_dung")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (isAdded() && doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) displayData(user);
                    }
                });
    }

    private void displayData(User user) {
        if (user.getAnh_dai_dien() != null && !user.getAnh_dai_dien().isEmpty()) {
            Glide.with(this).load(user.getAnh_dai_dien()).placeholder(R.drawable.ic_nav_profile).into(imgAvatar);
        }

        tvDisplayName.setText(user.getHo_ten());
        tvDisplayEmail.setText(user.getEmail());
        tvInfoName.setText(user.getHo_ten());
        tvInfoEmail.setText(user.getEmail());
        tvInfoPhone.setText(user.getSo_dien_thoai() != null ? user.getSo_dien_thoai() : "Chưa cập nhật");
        
        String role = user.getRole();
        tvInfoRole.setText("admin".equals(role) ? "Quản trị viên" : "Người dùng");

        if (user.getNgay_tao() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvInfoDate.setText(sdf.format(user.getNgay_tao().toDate()));
        }
    }
}
