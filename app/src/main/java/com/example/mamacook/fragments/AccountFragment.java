package com.example.mamacook.fragments;

import android.content.Intent;
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
    private TextView tvInfoDob, tvInfoGender; // Hiếu thêm: Hai trường mới
    private LinearLayout btnEditProfile, btnChangePassword;
    private com.google.android.material.button.MaterialButton btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentAvatarUrl;
    private Uri cameraUri;
    private boolean isDataLoaded = false;

    private final ActivityResultLauncher<CropImageContractOptions> cropImage =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful() && result.getUriContent() != null && isAdded()) {
                    Intent intent = new Intent(getActivity(), PreviewAvatarActivity.class);
                    intent.putExtra("IMAGE_URI", result.getUriContent().toString());
                    intent.putExtra("IS_VIEW_ONLY", false);
                    startActivity(intent);
                }
            });

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) startCrop(uri);
            });

    private final ActivityResultLauncher<Uri> takePhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraUri != null) startCrop(cameraUri);
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

    private void initViews(View view) {
        imgAvatar = view.findViewById(R.id.img_avatar);
        tvDisplayName = view.findViewById(R.id.tv_display_name);
        tvDisplayEmail = view.findViewById(R.id.tv_display_email);
        tvInfoName = view.findViewById(R.id.tv_info_name);
        tvInfoEmail = view.findViewById(R.id.tv_info_email);
        tvInfoPhone = view.findViewById(R.id.tv_info_phone);
        tvInfoRole = view.findViewById(R.id.tv_info_role);
        tvInfoDate = view.findViewById(R.id.tv_info_date);
        tvInfoDob = view.findViewById(R.id.tv_info_dob);       // Ánh xạ ngày sinh
        tvInfoGender = view.findViewById(R.id.tv_info_gender); // Ánh xạ giới tính
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnChangePassword = view.findViewById(R.id.btn_change_password);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });

        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(getActivity(), com.example.mamacook.activities.EditAccountActivity.class)));
        btnChangePassword.setOnClickListener(v -> startActivity(new Intent(getActivity(), com.example.mamacook.activities.ChangePasswordActivity.class)));
        imgAvatar.setOnClickListener(v -> showAvatarOptions());
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) return;

        db.collection("nguoi_dung").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (isAdded() && doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) displayData(user);
                    }
                });
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
        
        // Hiển thị Ngày sinh và Giới tính
        tvInfoDob.setText(user.getNgay_sinh() != null && !user.getNgay_sinh().isEmpty() ? user.getNgay_sinh() : "Chưa cập nhật");
        tvInfoGender.setText(user.getGioi_tinh() != null && !user.getGioi_tinh().isEmpty() ? user.getGioi_tinh() : "Chưa cập nhật");

        tvInfoRole.setText("admin".equals(user.getRole()) ? "Quản trị viên" : "Người dùng");
        if (user.getNgay_tao() != null) {
            tvInfoDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(user.getNgay_tao().toDate()));
        }
    }

    private void startCrop(Uri uri) {
        CropImageOptions options = new CropImageOptions();
        options.cropShape = CropImageView.CropShape.OVAL;
        options.fixAspectRatio = true;
        options.aspectRatioX = 1;
        options.aspectRatioY = 1;
        cropImage.launch(new CropImageContractOptions(uri, options));
    }

    private void showAvatarOptions() {
        if (!isAdded() || getContext() == null) return;
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_change_avatar_options, null, false);
        bottomSheetDialog.setContentView(view);
        view.findViewById(R.id.btn_take_photo).setOnClickListener(v -> { bottomSheetDialog.dismiss(); openCamera(); });
        view.findViewById(R.id.btn_upload_photo).setOnClickListener(v -> { bottomSheetDialog.dismiss(); pickMedia.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()); });
        view.findViewById(R.id.btn_view_photo).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            if (currentAvatarUrl != null) {
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
            File photoFile = File.createTempFile("IMG_", ".jpg", getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            cameraUri = FileProvider.getUriForFile(requireContext(), "com.example.mamacook.fileprovider", photoFile);
            takePhoto.launch(cameraUri);
        } catch (IOException ignored) {}
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }
}
