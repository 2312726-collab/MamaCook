package com.example.mamacook.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.example.mamacook.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class PreviewAvatarActivity extends AppCompatActivity {

    private Uri imageUri;
    private ImageView imgPreview;
    private com.google.android.material.button.MaterialButton btnSave, btnAdjust;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private boolean isViewOnly = false;

    // Launcher xử lý kết quả khi người dùng căn chỉnh lại ảnh
    private final ActivityResultLauncher<CropImageContractOptions> cropImage =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    Uri resultUri = result.getUriContent();
                    if (resultUri != null) {
                        imageUri = resultUri;
                        imgPreview.setImageURI(imageUri);
                    }
                } else if (result.getError() != null) {
                    Toast.makeText(this, "Lỗi căn chỉnh: " + result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_avatar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews();

        // Lấy thông tin từ Intent
        String uriString = getIntent().getStringExtra("IMAGE_URI");
        isViewOnly = getIntent().getBooleanExtra("IS_VIEW_ONLY", false);

        if (uriString != null) {
            if (isViewOnly) {
                // Chế độ XEM: Ẩn các nút thao tác
                setupViewOnlyMode(uriString);
            } else {
                // Chế độ XÁC NHẬN: Cho phép căn chỉnh và lưu
                imageUri = Uri.parse(uriString);
                imgPreview.setImageURI(imageUri);
                btnAdjust.setVisibility(View.VISIBLE);
            }
        }

        findViewById(R.id.btn_cancel_preview).setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadAndSaveAvatar();
            }
        });

        // Xử lý khi nhấn nút Căn chỉnh lại ảnh
        btnAdjust.setOnClickListener(v -> {
            if (imageUri != null) {
                startReCrop(imageUri);
            }
        });
    }

    private void initViews() {
        imgPreview = findViewById(R.id.img_preview_avatar);
        btnSave = findViewById(R.id.btn_save_avatar);
        btnAdjust = findViewById(R.id.btn_adjust_avatar);
    }

    private void setupViewOnlyMode(String url) {
        TextView tvTitle = findViewById(R.id.tv_preview_title);
        tvTitle.setText("ẢNH ĐẠI DIỆN");
        btnSave.setVisibility(View.GONE);
        btnAdjust.setVisibility(View.GONE);
        
        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.ic_nav_profile)
                .into(imgPreview);
    }

    private void startReCrop(Uri uri) {
        CropImageOptions options = new CropImageOptions();
        options.guidelines = CropImageView.Guidelines.ON;
        options.fixAspectRatio = true;
        options.aspectRatioX = 1;
        options.aspectRatioY = 1;
        options.cropShape = CropImageView.CropShape.OVAL;
        options.toolbarColor = Color.WHITE;
        options.toolbarTitleColor = Color.BLACK;
        options.toolbarBackButtonColor = Color.BLACK;
        options.activityMenuIconColor = Color.BLACK;
        options.cropMenuCropButtonTitle = "Xong";
        options.progressBarColor = Color.parseColor("#6D4C41");

        cropImage.launch(new CropImageContractOptions(uri, options));
    }

    private void uploadAndSaveAvatar() {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        btnSave.setEnabled(false);
        btnSave.setText("Đang tải...");

        db.collection("nguoi_dung").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            String oldImageUrl = documentSnapshot.getString("anh_dai_dien");
            String fileName = "avatar_" + System.currentTimeMillis() + ".jpg";
            StorageReference storageRef = storage.getReference().child("avatars/" + uid + "/" + fileName);

            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                if (bitmap == null) {
                    resetSaveButton();
                    Toast.makeText(this, "Không thể xử lý tệp ảnh", Toast.LENGTH_SHORT).show();
                    return;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] data = baos.toByteArray();

                UploadTask uploadTask = storageRef.putBytes(data);
                uploadTask.addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    updateFirestore(uri.toString(), oldImageUrl, uid);
                })).addOnFailureListener(e -> {
                    resetSaveButton();
                    Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

            } catch (FileNotFoundException e) {
                resetSaveButton();
                Toast.makeText(this, "Không tìm thấy tệp ảnh", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFirestore(String downloadUrl, String oldImageUrl, String uid) {
        db.collection("nguoi_dung").document(uid)
                .update("anh_dai_dien", downloadUrl)
                .addOnSuccessListener(aVoid -> {
                    if (oldImageUrl != null && oldImageUrl.contains("firebasestorage.googleapis.com")) {
                        try {
                            FirebaseStorage.getInstance().getReferenceFromUrl(oldImageUrl).delete();
                        } catch (Exception ignored) {}
                    }
                    Toast.makeText(this, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    resetSaveButton();
                    Toast.makeText(this, "Lỗi cập nhật database: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void resetSaveButton() {
        btnSave.setEnabled(true);
        btnSave.setText("Lưu ảnh");
    }
}
