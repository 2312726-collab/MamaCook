package com.example.mamacook.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mamacook.adapters.BinhLuanNgangAdapter;
import com.example.mamacook.R;
import com.example.mamacook.models.DanhGia;
import com.example.mamacook.models.MonAn;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetailMonAnActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ImageView imgMonAn, btnFavoriteDetail, btnAddAttachment, imgPreviewComment;
    private TextView tvTen, tvRatingInfo, tvThoiGian, tvNguyenLieu, tvDiemTrungBinh, tvXemTatCa;
    private LinearLayout layoutBuocNau;
    private RelativeLayout layoutPreviewImage;
    private RecyclerView rvDanhGia;
    private BinhLuanNgangAdapter adapterBinhLuan;
    private final List<DanhGia> danhSachBinhLuan = new ArrayList<>();
    private EditText etBinhLuan;
    private RatingBar rbChonSao;
    private ImageView btnGuiBinhLuan;
    private String currentDishId;
    private String currentUserId;
    private boolean isSaved = false;

    private Uri imageUri;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_mon_an);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        imgMonAn = findViewById(R.id.img_detail_mon_an);
        tvTen = findViewById(R.id.tv_detail_ten);
        tvRatingInfo = findViewById(R.id.tv_detail_rating_info);
        tvThoiGian = findViewById(R.id.tv_detail_thoi_gian);
        tvNguyenLieu = findViewById(R.id.tv_detail_nguyen_lieu);
        tvDiemTrungBinh = findViewById(R.id.tvDiemTrungBinh);
        tvXemTatCa = findViewById(R.id.tvXemTatCa);
        layoutBuocNau = findViewById(R.id.layout_buoc_nau);
        rvDanhGia = findViewById(R.id.rv_danh_gia);
        etBinhLuan = findViewById(R.id.et_binh_luan);
        rbChonSao = findViewById(R.id.rb_chon_sao);
        btnGuiBinhLuan = findViewById(R.id.btn_gui_binh_luan);
        btnFavoriteDetail = findViewById(R.id.btn_favorite_detail);
        btnAddAttachment = findViewById(R.id.btn_add_attachment);
        imgPreviewComment = findViewById(R.id.img_preview_comment);
        layoutPreviewImage = findViewById(R.id.layout_preview_image);

        adapterBinhLuan = new BinhLuanNgangAdapter(danhSachBinhLuan);
        rvDanhGia.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDanhGia.setAdapter(adapterBinhLuan);

        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> finish());

        currentDishId = getIntent().getStringExtra("ID_MON_AN");
        String intentImage = getIntent().getStringExtra("HINH_ANH");

        if (intentImage != null && !intentImage.isEmpty()) {
            if (intentImage.startsWith("http")) {
                Glide.with(this).load(intentImage).placeholder(R.drawable.bg_splash).into(imgMonAn);
            } else {
                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(intentImage);
                Glide.with(this).load(storageRef).placeholder(R.drawable.bg_splash).into(imgMonAn);
            }
        }

        if (currentDishId != null) {
            fetchDishDetailsRealtime(currentDishId);
            fetchReviewStatsRealtime(currentDishId);
            fetchLatestReviews(currentDishId);
            checkIfSaved();
            addToHistory(currentDishId);
        }

        initImageLaunchers();

        if (btnAddAttachment != null) {
            btnAddAttachment.setOnClickListener(v -> showAttachmentMenu());
        }

        findViewById(R.id.btn_remove_preview).setOnClickListener(v -> {
            imageUri = null;
            layoutPreviewImage.setVisibility(View.GONE);
        });

        if (btnFavoriteDetail != null) {
            btnFavoriteDetail.setOnClickListener(v -> toggleSaveRecipe());
        }

        btnGuiBinhLuan.setOnClickListener(v -> guiBinhLuan());

        if (tvXemTatCa != null) {
            tvXemTatCa.setOnClickListener(v -> {
                Intent intent = new Intent(DetailMonAnActivity.this, TatCaBinhLuanActivity.class);
                intent.putExtra("ID_MON_AN", currentDishId);
                startActivity(intent);
            });
        }
    }

    private void initImageLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        showPreview();
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {
                        showPreview();
                    }
                }
        );
    }

    private void showPreview() {
        if (imageUri != null) {
            imgPreviewComment.setImageURI(imageUri);
            layoutPreviewImage.setVisibility(View.VISIBLE);
        }
    }

    private void showAttachmentMenu() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_attachment, null);
        
        // Gemini: Fix lỗi ID btnChupHinh và btnChonAnh không khớp giữa layout và java
        view.findViewById(R.id.btnChupHinh).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            try {
                File photoFile = createImageFile();
                imageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                cameraLauncher.launch(imageUri);
            } catch (IOException ex) {
                Toast.makeText(this, "Lỗi tạo file ảnh", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btnChonAnh).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
    }

    private void fetchReviewStatsRealtime(String dishId) {
        db.collection("danh_gia")
                .whereEqualTo("id_mon_an", dishId)
                .whereEqualTo("trang_thai", "hien_thi")
                .addSnapshotListener(this, (value, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (error != null || value == null) return;
                    double totalStars = 0;
                    int count = value.size();
                    for (QueryDocumentSnapshot doc : value) {
                        Double stars = doc.getDouble("so_sao");
                        if (stars != null) totalStars += stars;
                    }
                    if (count > 0) {
                        double average = totalStars / count;
                        tvDiemTrungBinh.setText(String.format(Locale.getDefault(), "⭐ %.1f (%d đánh giá)", average, count));
                    } else {
                        tvDiemTrungBinh.setText("⭐ 0.0 (0 đánh giá)");
                    }
                });
    }

    private void fetchLatestReviews(String dishId) {
        db.collection("danh_gia")
                .whereEqualTo("id_mon_an", dishId)
                .whereEqualTo("trang_thai", "hien_thi")
                .orderBy("ngay_danh_gia", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener(this, (value, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (value != null) {
                        danhSachBinhLuan.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            DanhGia dg = doc.toObject(DanhGia.class);
                            danhSachBinhLuan.add(dg);
                        }
                        adapterBinhLuan.notifyDataSetChanged();
                    }
                });
    }

    private void guiBinhLuan() {
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }
        String noiDung = etBinhLuan.getText().toString().trim();
        float soSao = rbChonSao.getRating();
        if (TextUtils.isEmpty(noiDung)) {
            Toast.makeText(this, "Nhập nội dung!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang xử lý...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        if (imageUri != null) {
            StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                    .child("reviews/" + System.currentTimeMillis() + ".jpg");
            storageRef.putFile(imageUri).continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return storageRef.getDownloadUrl();
            }).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    saveReviewToFirestore(noiDung, soSao, task.getResult().toString());
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Lỗi upload!", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            saveReviewToFirestore(noiDung, soSao, null);
        }
    }

    private void saveReviewToFirestore(String content, float stars, String imageUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = (user != null && !TextUtils.isEmpty(user.getDisplayName())) ? user.getDisplayName() : "Người dùng";
        Map<String, Object> review = new HashMap<>();
        review.put("id_nguoi_dung", currentUserId);
        review.put("ten_nguoi_dung", name);
        review.put("id_mon_an", currentDishId);
        review.put("noi_dung", content);
        review.put("so_sao", stars);
        review.put("hinh_anh_url", imageUrl);
        review.put("trang_thai", "hien_thi");
        review.put("ngay_danh_gia", FieldValue.serverTimestamp());

        db.collection("danh_gia").add(review).addOnSuccessListener(documentReference -> {
            progressDialog.dismiss();
            etBinhLuan.setText("");
            imageUri = null;
            layoutPreviewImage.setVisibility(View.GONE);
            updateTotalRating(stars);
            Toast.makeText(this, "Đã gửi!", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            progressDialog.dismiss();
            Toast.makeText(this, "Lỗi lưu!", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateTotalRating(float newStar) {
        DocumentReference monAnRef = db.collection("mon_an").document(currentDishId);
        monAnRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                MonAn monAn = doc.toObject(MonAn.class);
                if (monAn != null) {
                    int count = monAn.getTong_luot_danh_gia();
                    double oldRating = monAn.getRating();
                    int newCount = count + 1;
                    double newRating = ((oldRating * count) + newStar) / newCount;
                    monAnRef.update("rating", newRating, "tong_luot_danh_gia", newCount);
                }
            }
        });
    }

    private void fetchDishDetailsRealtime(String id) {
        db.collection("mon_an").document(id).addSnapshotListener(this, (doc, error) -> {
            if (isFinishing() || isDestroyed()) return;
            if (doc != null && doc.exists()) {
                MonAn monAn = doc.toObject(MonAn.class);
                if (monAn != null) {
                    tvTen.setText(monAn.getTen_mon());
                    tvThoiGian.setText(String.format(Locale.getDefault(), "⌛ %d phút", monAn.getThoi_gian_nau()));
                    tvRatingInfo.setText(String.format(Locale.getDefault(), "🕒 %.1f ⭐ (%d)", monAn.getRating(), monAn.getTong_luot_danh_gia()));
                    String hinhAnh = monAn.getHinh_anh();
                    if (hinhAnh != null && !hinhAnh.isEmpty()) {
                        if (hinhAnh.startsWith("http")) {
                            Glide.with(this).load(hinhAnh).placeholder(R.drawable.bg_splash).into(imgMonAn);
                        } else {
                            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(hinhAnh);
                            Glide.with(this).load(storageRef).placeholder(R.drawable.bg_splash).into(imgMonAn);
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    if (monAn.getDanh_sach_nguyen_lieu() != null) {
                        for (MonAn.ChiTietNguyenLieu nl : monAn.getDanh_sach_nguyen_lieu()) {
                            sb.append("• ").append(nl.so_luong).append(nl.don_vi).append(" ").append(nl.ten_nguyen_lieu).append("\n");
                        }
                    }
                    tvNguyenLieu.setText(sb.toString());
                    layoutBuocNau.removeAllViews();
                    if (monAn.getDanh_sach_buoc_nau() != null) {
                        for (MonAn.BuocNau buoc : monAn.getDanh_sach_buoc_nau()) {
                            View stepView = LayoutInflater.from(this).inflate(R.layout.item_step_cook, layoutBuocNau, false);
                            ((TextView) stepView.findViewById(R.id.tv_step_title)).setText(String.format(Locale.getDefault(), "Bước %d", buoc.so_thu_tu));
                            ((TextView) stepView.findViewById(R.id.tv_step_content)).setText(buoc.noi_dung_buoc);
                            layoutBuocNau.addView(stepView);
                        }
                    }
                }
            }
        });
    }

    private void checkIfSaved() {
        if (currentUserId == null) return;
        db.collection("mon_da_luu").document(currentUserId + "_" + currentDishId).addSnapshotListener(this, (doc, error) -> {
            if (isFinishing() || isDestroyed()) return;
            if (doc != null) {
                isSaved = doc.exists();
                updateSaveButtonUI(false);
            }
        });
    }

    private void updateSaveButtonUI(boolean animate) {
        if (btnFavoriteDetail == null) return;
        if (isSaved) {
            btnFavoriteDetail.setColorFilter(Color.RED);
        } else {
            btnFavoriteDetail.setColorFilter(Color.WHITE);
        }
        if (animate) {
            btnFavoriteDetail.animate().scaleX(1.4f).scaleY(1.4f).setDuration(150).withEndAction(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    btnFavoriteDetail.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                }
            }).start();
        }
    }

    private void toggleSaveRecipe() {
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }
        String idLuu = currentUserId + "_" + currentDishId;
        if (isSaved) {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận")
                    .setMessage("Bỏ yêu thích?")
                    .setPositiveButton("Có", (dialog, which) -> db.collection("mon_da_luu").document(idLuu).delete())
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("id_nguoi_dung", currentUserId);
            data.put("id_mon_an", currentDishId);
            db.collection("mon_da_luu").document(idLuu).set(data);
        }
    }

    private void addToHistory(String dishId) {
        if (currentUserId == null) return;
        db.collection("lich_su_xem").whereEqualTo("id_nguoi_dung", currentUserId).whereEqualTo("id_mon_an", dishId).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (!querySnapshot.isEmpty()) {
                        db.collection("lich_su_xem").document(querySnapshot.getDocuments().get(0).getId())
                                .update("thoi_gian_xem", FieldValue.serverTimestamp());
                    } else {
                        Map<String, Object> h = new HashMap<>();
                        h.put("id_nguoi_dung", currentUserId);
                        h.put("id_mon_an", dishId);
                        h.put("thoi_gian_xem", FieldValue.serverTimestamp());
                        db.collection("lich_su_xem").add(h).addOnSuccessListener(ref -> limitHistoryTo15());
                    }
                });
    }

    private void limitHistoryTo15() {
        db.collection("lich_su_xem").whereEqualTo("id_nguoi_dung", currentUserId).orderBy("thoi_gian_xem", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (querySnapshot.size() > 15) {
                        for (int i = 15; i < querySnapshot.size(); i++) {
                            db.collection("lich_su_xem").document(querySnapshot.getDocuments().get(i).getId()).delete();
                        }
                    }
                });
    }
}
