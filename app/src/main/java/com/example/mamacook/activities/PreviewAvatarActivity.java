package com.example.mamacook.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_avatar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        ImageView imgPreview = findViewById(R.id.img_preview_avatar);
        com.google.android.material.button.MaterialButton btnCancel = findViewById(R.id.btn_cancel_preview);
        com.google.android.material.button.MaterialButton btnSave = findViewById(R.id.btn_save_avatar);

        // Lấy URI ảnh từ Intent
        String uriString = getIntent().getStringExtra("IMAGE_URI");
        if (uriString != null) {
            imageUri = Uri.parse(uriString);
            imgPreview.setImageURI(imageUri);
        }

        btnCancel.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadAndSaveAvatar(btnSave);
            }
        });
    }

    private void uploadAndSaveAvatar(com.google.android.material.button.MaterialButton btnSave) {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        // 1. Hiếu: Lấy thông tin ảnh cũ từ Firestore để xóa sau khi tải ảnh mới thành công
        db.collection("nguoi_dung").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            String oldImageUrl = documentSnapshot.getString("anh_dai_dien");

            // Hiếu: Tạo tên file mới dựa trên thời gian để tránh lỗi cache của Glide
            String fileName = "avatar_" + System.currentTimeMillis() + ".jpg";
            StorageReference storageRef = storage.getReference().child("avatars/" + uid + "/" + fileName);

            try {
                // 2. Nén ảnh để tiết kiệm dung lượng (Hiếu)
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                if (bitmap == null) {
                    Toast.makeText(this, "Không thể xử lý tệp ảnh", Toast.LENGTH_SHORT).show();
                    return;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // Nén xuống chất lượng 70% để cân bằng giữa độ nét và dung lượng
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] data = baos.toByteArray();

                // 3. Tải lên Firebase Storage
                btnSave.setEnabled(false);
                btnSave.setText("Đang tải...");

                UploadTask uploadTask = storageRef.putBytes(data);
                uploadTask.addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();

                    // 4. Cập nhật link ảnh vào Firestore
                    db.collection("nguoi_dung").document(uid)
                            .update("anh_dai_dien", downloadUrl)
                            .addOnSuccessListener(aVoid -> {

                                // 5. Hiếu: Xóa ảnh cũ trên Storage để tiết kiệm bộ nhớ
                                if (oldImageUrl != null && oldImageUrl.contains("firebasestorage.googleapis.com")) {
                                    try {
                                        FirebaseStorage.getInstance().getReferenceFromUrl(oldImageUrl).delete();
                                    } catch (Exception ignored) {
                                    }
                                }

                                Toast.makeText(this, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnSave.setEnabled(true);
                                btnSave.setText("Lưu");
                                Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })).addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu");
                    Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

            } catch (FileNotFoundException e) {
                Toast.makeText(this, "Không tìm thấy tệp ảnh", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
