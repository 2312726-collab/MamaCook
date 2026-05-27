package com.example.mamacook.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.example.mamacook.R;
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

public class AccountActivity extends AppCompatActivity {

    private ImageView btnBack, imgAvatar;
    private TextView tvDisplayName, tvDisplayEmail, tvInfoName, tvInfoEmail, tvInfoPhone, tvInfoRole, tvInfoDate;
    private LinearLayout btnEditProfile, btnChangePassword;
    private com.google.android.material.button.MaterialButton btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentAvatarUrl;
    private Uri cameraUri;

    // Launcher xử lý kết quả cắt ảnh
    private final ActivityResultLauncher<CropImageContractOptions> cropImage =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri resultUri = result.getUriContent();
                    if (resultUri != null) {
                        Intent intent = new Intent(this, PreviewAvatarActivity.class);
                        intent.putExtra("IMAGE_URI", resultUri.toString());
                        intent.putExtra("IS_VIEW_ONLY", false);
                        startActivity(intent);
                    }
                } else if (result.getError() != null) {
                    Toast.makeText(this, "Lỗi cắt ảnh: " + result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

    // Launcher chọn ảnh từ thư viện
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    startCrop(uri);
                }
            });

    // Launcher chụp ảnh từ Camera
    private final ActivityResultLauncher<Uri> takePhoto =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraUri != null) {
                    startCrop(cameraUri);
                }
            });

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
    }

    private void startCrop(Uri uri) {
        CropImageOptions cropImageOptions = new CropImageOptions();
        cropImageOptions.guidelines = CropImageView.Guidelines.ON;
        cropImageOptions.aspectRatioX = 1;
        cropImageOptions.aspectRatioY = 1;
        cropImageOptions.fixAspectRatio = true;
        cropImageOptions.cropShape = CropImageView.CropShape.OVAL;
        cropImageOptions.toolbarColor = Color.WHITE;
        cropImageOptions.toolbarTitleColor = Color.BLACK;
        cropImageOptions.toolbarBackButtonColor = Color.BLACK;
        cropImageOptions.activityMenuIconColor = Color.BLACK;
        cropImageOptions.cropMenuCropButtonTitle = "Xong";
        cropImageOptions.progressBarColor = Color.parseColor("#6D4C41");

        cropImage.launch(new CropImageContractOptions(uri, cropImageOptions));
    }

    private void showAvatarOptions() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_change_avatar_options, null);
        bottomSheetDialog.setContentView(view);

        LinearLayout btnTakePhoto = view.findViewById(R.id.btn_take_photo);
        LinearLayout btnUpload = view.findViewById(R.id.btn_upload_photo);
        LinearLayout btnView = view.findViewById(R.id.btn_view_photo);

        btnTakePhoto.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            openCamera();
        });

        btnUpload.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build();
            pickMedia.launch(request);
        });

        btnView.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
                Intent intent = new Intent(this, PreviewAvatarActivity.class);
                intent.putExtra("IMAGE_URI", currentAvatarUrl);
                intent.putExtra("IS_VIEW_ONLY", true);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Bạn chưa có ảnh đại diện", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheetDialog.show();
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            cameraUri = FileProvider.getUriForFile(this, "com.example.mamacook.fileprovider", photoFile);
            takePhoto.launch(cameraUri);
        } catch (IOException e) {
            Toast.makeText(this, "Không thể tạo file ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
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
        if (firebaseUser == null) return;

        db.collection("nguoi_dung").document(firebaseUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user != null) displayData(user);
                    }
                });
    }

    private void displayData(User user) {
        currentAvatarUrl = user.getAnh_dai_dien();
        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
            Glide.with(this).load(currentAvatarUrl).placeholder(R.drawable.ic_nav_profile).into(imgAvatar);
        }

        tvDisplayName.setText(user.getHo_ten() != null ? user.getHo_ten() : "N/A");
        tvDisplayEmail.setText(user.getEmail());
        tvInfoName.setText(user.getHo_ten());
        tvInfoEmail.setText(user.getEmail());
        tvInfoPhone.setText(user.getSo_dien_thoai() != null && !user.getSo_dien_thoai().isEmpty() ? user.getSo_dien_thoai() : "Chưa cập nhật");
        
        String role = user.getRole();
        tvInfoRole.setText(role != null && role.equals("admin") ? "Quản trị viên" : "Người dùng");

        if (user.getNgay_tao() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvInfoDate.setText(sdf.format(user.getNgay_tao().toDate()));
        }
    }
}
