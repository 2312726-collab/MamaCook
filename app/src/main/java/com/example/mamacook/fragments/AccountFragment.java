package com.example.mamacook.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.example.mamacook.R;
import com.example.mamacook.activities.MainActivity;
import com.example.mamacook.activities.PreviewAvatarActivity;
import com.example.mamacook.models.User;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    private String currentAvatarUrl;
    private Uri cameraUri;
    private boolean isDataLoading = false;

    // 1. Launcher xử lý kết quả cắt ảnh
    private final ActivityResultLauncher<CropImageContractOptions> cropImage =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri resultUri = result.getUriContent();
                    if (resultUri != null && isAdded()) {
                        Intent intent = new Intent(getActivity(), PreviewAvatarActivity.class);
                        intent.putExtra("IMAGE_URI", resultUri.toString());
                        intent.putExtra("IS_VIEW_ONLY", false);
                        startActivity(intent);
                    }
                }
            });

    // 2. Launcher chọn ảnh từ thư viện
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) startCrop(uri);
            });

    // 3. Launcher chụp ảnh từ Camera
    private final ActivityResultLauncher<Uri> takePhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraUri != null) {
                    startCrop(cameraUri);
                }
            });

    // 4. Launcher xin quyền Camera
    private final ActivityResultLauncher<String> requestCameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    if (isAdded()) Toast.makeText(getContext(), "Bạn cần cấp quyền Camera để sử dụng tính năng này", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews(view);
        setupListeners();
        
        return view;
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });

        btnEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), com.example.mamacook.activities.EditAccountActivity.class));
        });

        btnChangePassword.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), com.example.mamacook.activities.ChangePasswordActivity.class));
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
    }

    private void showAvatarOptions() {
        if (!isAdded() || getContext() == null) return;
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_change_avatar_options, null, false);
        bottomSheetDialog.setContentView(view);

        // Nút Chụp ảnh
        view.findViewById(R.id.btn_take_photo).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA);
            }
        });

        // Nút Tải ảnh lên
        view.findViewById(R.id.btn_upload_photo).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        // Nút Xem ảnh
        view.findViewById(R.id.btn_view_photo).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
                Intent intent = new Intent(getActivity(), PreviewAvatarActivity.class);
                intent.putExtra("IMAGE_URI", currentAvatarUrl);
                intent.putExtra("IS_VIEW_ONLY", true);
                startActivity(intent);
            }
        });

        bottomSheetDialog.show();
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            if (getContext() != null) {
                cameraUri = FileProvider.getUriForFile(requireContext(), "com.example.mamacook.fileprovider", photoFile);
                takePhoto.launch(cameraUri);
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Không thể tạo file tạm", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
    }

    private void startCrop(Uri uri) {
        CropImageOptions options = new CropImageOptions();
        options.guidelines = CropImageView.Guidelines.ON;
        options.fixAspectRatio = true;
        options.aspectRatioX = 1;
        options.aspectRatioY = 1;
        options.cropShape = CropImageView.CropShape.OVAL;
        options.toolbarColor = Color.WHITE;
        options.toolbarTitleColor = Color.BLACK;
        options.cropMenuCropButtonTitle = "Xong";
        cropImage.launch(new CropImageContractOptions(uri, options));
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
        FirebaseUser userAuth = mAuth.getCurrentUser();
        if (userAuth == null || isDataLoading) return;

        isDataLoading = true;
        db.collection("nguoi_dung").document(userAuth.getUid()).get()
                .addOnSuccessListener(doc -> {
                    isDataLoading = false;
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
                })
                .addOnFailureListener(e -> isDataLoading = false);
    }

    private void displayData(User user) {
        if (!isAdded()) return;
        currentAvatarUrl = user.getAnh_dai_dien();
        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
            Glide.with(this).load(currentAvatarUrl).placeholder(R.drawable.ic_nav_profile).circleCrop().into(imgAvatar);
        }

        tvDisplayName.setText(user.getHo_ten() != null ? user.getHo_ten() : "--");
        tvDisplayEmail.setText(user.getEmail());
        tvInfoName.setText(user.getHo_ten());
        tvInfoEmail.setText(user.getEmail());
        tvInfoPhone.setText(user.getSo_dien_thoai() != null && !user.getSo_dien_thoai().isEmpty() ? user.getSo_dien_thoai() : "Chưa cập nhật");
        tvInfoRole.setText("admin".equals(user.getRole()) ? "Quản trị viên" : "Người dùng");
        if (user.getNgay_tao() != null) {
            tvInfoDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(user.getNgay_tao().toDate()));
        }
    }
}
