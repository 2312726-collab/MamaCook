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
    private LinearLayout btnEditProfile, btnChangePassword, btnAddMonAn;
    private LinearLayout btnAdminUsers, btnAdminStats, btnAdminReviews, btnAdminRecipes, btnAdminChat;
    private View dividerAdmin1, dividerAdmin2, dividerAdmin3, dividerAdmin4, dividerAdmin5;
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
            // Hiếu: Mở màn hình chỉnh sửa thông tin với hiệu ứng trượt
            Intent intent = new Intent(getActivity(), com.example.mamacook.activities.EditAccountActivity.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        btnChangePassword.setOnClickListener(v -> {
            // Hiếu: Mở màn hình đổi mật khẩu với hiệu ứng trượt
            Intent intent = new Intent(getActivity(), com.example.mamacook.activities.ChangePasswordActivity.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        btnAddMonAn.setOnClickListener(v -> {
            if (isAdded() && getContext() != null) {
                Intent intent = new Intent(requireContext(), com.example.mamacook.activities.AddEditMonAnActivity.class);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
            }
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
        btnAddMonAn = view.findViewById(R.id.btn_add_monan);
        btnAdminUsers = view.findViewById(R.id.btn_admin_users);
        btnAdminStats = view.findViewById(R.id.btn_admin_stats);
        btnAdminReviews = view.findViewById(R.id.btn_admin_reviews);
        btnAdminRecipes = view.findViewById(R.id.btn_admin_recipes);
        btnAdminChat = view.findViewById(R.id.btn_admin_chat);
        dividerAdmin1 = view.findViewById(R.id.divider_admin_1);
        dividerAdmin2 = view.findViewById(R.id.divider_admin_2);
        dividerAdmin3 = view.findViewById(R.id.divider_admin_3);
        dividerAdmin4 = view.findViewById(R.id.divider_admin_4);
        dividerAdmin5 = view.findViewById(R.id.divider_admin_5);
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
                        if (user != null) {
                            displayData(user);
                            checkUserRoleAndSetupAdminButton();
                        }
                    }
                });
    }

    private void checkUserRoleAndSetupAdminButton() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) return;

        db.collection("nguoi_dung")
                .document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (isAdded() && doc.exists()) {
                        String role = doc.getString("role");
                        String vaiTro = doc.getString("vai_tro");
                        if ("admin".equals(role) || "admin".equals(vaiTro)) {
                            // Show buttons
                            if (btnAdminUsers != null) btnAdminUsers.setVisibility(View.VISIBLE);
                            if (btnAdminStats != null) btnAdminStats.setVisibility(View.VISIBLE);
                            if (btnAdminReviews != null) btnAdminReviews.setVisibility(View.VISIBLE);
                            if (btnAdminRecipes != null) btnAdminRecipes.setVisibility(View.VISIBLE);
                            if (btnAdminChat != null) btnAdminChat.setVisibility(View.VISIBLE);
                            
                            if (dividerAdmin1 != null) dividerAdmin1.setVisibility(View.VISIBLE);
                            if (dividerAdmin2 != null) dividerAdmin2.setVisibility(View.VISIBLE);
                            if (dividerAdmin3 != null) dividerAdmin3.setVisibility(View.VISIBLE);
                            if (dividerAdmin4 != null) dividerAdmin4.setVisibility(View.VISIBLE);
                            if (dividerAdmin5 != null) dividerAdmin5.setVisibility(View.VISIBLE);

                            // Setup click listeners
                            btnAdminUsers.setOnClickListener(v -> {
                                startActivity(new Intent(getActivity(), com.example.mamacook.activities.QuanLyTaiKhoanActivity.class));
                                if (getActivity() != null) getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            });

                            btnAdminStats.setOnClickListener(v -> {
                                startActivity(new Intent(getActivity(), com.example.mamacook.activities.ThongKeAdminActivity.class));
                                if (getActivity() != null) getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            });

                            btnAdminReviews.setOnClickListener(v -> {
                                startActivity(new Intent(getActivity(), com.example.mamacook.activities.QuanLyDanhGiaActivity.class));
                                if (getActivity() != null) getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            });

                            btnAdminRecipes.setOnClickListener(v -> {
                                startActivity(new Intent(getActivity(), com.example.mamacook.activities.QuanLyMonAnActivity.class));
                                if (getActivity() != null) getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            });

                            btnAdminChat.setOnClickListener(v -> {
                                startActivity(new Intent(getActivity(), com.example.mamacook.activities.DanhSachChatActivity.class));
                                if (getActivity() != null) getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            });
                        }
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
        
        String role = user.getVai_tro();
        tvInfoRole.setText("admin".equals(role) ? "Quản trị viên" : "Người dùng");

        if (user.getNgay_tao() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvInfoDate.setText(sdf.format(user.getNgay_tao().toDate()));
        }
    }
}
