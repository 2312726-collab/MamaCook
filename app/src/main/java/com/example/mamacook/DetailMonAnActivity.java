package com.example.mamacook;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetailMonAnActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ImageView imgMonAn, btnCamera, btnFavoriteDetail;
    private TextView tvTen, tvRatingInfo, tvThoiGian, tvNguyenLieu, tvSummaryRating;
    private LinearLayout layoutBuocNau;
    private RecyclerView rvDanhGia;
    private BinhLuanNgangAdapter adapterBinhLuan;
    private List<DanhGia> danhSachBinhLuan = new ArrayList<>();
    private EditText etBinhLuan;
    private RatingBar rbChonSao;
    private ImageView btnGuiBinhLuan;
    private String currentDishId;
    private String currentUserId;
    private boolean isSaved = false;

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
        tvSummaryRating = findViewById(R.id.tv_summary_rating);
        layoutBuocNau = findViewById(R.id.layout_buoc_nau);
        rvDanhGia = findViewById(R.id.rv_danh_gia);
        etBinhLuan = findViewById(R.id.et_binh_luan);
        rbChonSao = findViewById(R.id.rb_chon_sao);
        btnGuiBinhLuan = findViewById(R.id.btn_gui_binh_luan);
        btnFavoriteDetail = findViewById(R.id.btn_favorite_detail);
        btnCamera = findViewById(R.id.btn_camera);

        // Cài đặt RecyclerView vuốt ngang
        adapterBinhLuan = new BinhLuanNgangAdapter(danhSachBinhLuan);
        rvDanhGia.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDanhGia.setAdapter(adapterBinhLuan);

        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> finish());

        currentDishId = getIntent().getStringExtra("ID_MON_AN");
        if (currentDishId != null) {
            fetchDishDetailsRealtime(currentDishId);
            fetchCommentsSmartRealtime(currentDishId); 
            checkIfSaved();
        }

        btnGuiBinhLuan.setOnClickListener(v -> postCommentToServerAI());
        if (btnFavoriteDetail != null) {
            btnFavoriteDetail.setOnClickListener(v -> toggleSaveRecipe());
        }
        
        if (btnCamera != null) {
            btnCamera.setOnClickListener(v -> Toast.makeText(this, "Tính năng ảnh đang cập nhật", Toast.LENGTH_SHORT).show());
        }
        if (tvSummaryRating != null) {
            tvSummaryRating.setOnClickListener(v -> Toast.makeText(this, "Xem tất cả đánh giá", Toast.LENGTH_SHORT).show());
        }
    }

    private void fetchDishDetailsRealtime(String id) {
        db.collection("mon_an").document(id).addSnapshotListener((doc, error) -> {
            if (doc != null && doc.exists()) {
                MonAn monAn = doc.toObject(MonAn.class);
                if (monAn != null) {
                    tvTen.setText(monAn.getTen_mon());
                    tvThoiGian.setText("⌛ " + monAn.getThoi_gian_nau() + " phút");
                    tvRatingInfo.setText("🕒 " + monAn.getRating() + " ⭐ " + monAn.getTong_luot_danh_gia() + " (" + monAn.getTong_luot_danh_gia() + ")");
                    tvSummaryRating.setText("⭐ " + monAn.getRating() + "/" + monAn.getTong_luot_danh_gia() + " >");
                    Glide.with(this).load(monAn.getHinh_anh()).placeholder(R.drawable.bg_splash).into(imgMonAn);

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
                            ((TextView) stepView.findViewById(R.id.tv_step_title)).setText("Bước " + buoc.so_thu_tu);
                            ((TextView) stepView.findViewById(R.id.tv_step_content)).setText(buoc.noi_dung_buoc);
                            layoutBuocNau.addView(stepView);
                        }
                    }
                }
            }
        });
    }

    private void fetchCommentsSmartRealtime(String dishId) {
        db.collection("danh_gia")
                .whereEqualTo("id_mon_an", dishId)
                .whereEqualTo("trang_thai", "hien_thi") 
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Loi_Firebase", "Lỗi: " + error.getMessage());
                        return;
                    }
                    if (value == null) return;
                    
                    danhSachBinhLuan.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        DanhGia dg = doc.toObject(DanhGia.class);
                        if (dg != null) danhSachBinhLuan.add(dg);
                    }
                    
                    Collections.sort(danhSachBinhLuan, (o1, o2) -> {
                        if (o1.getNgay_danh_gia() == null || o2.getNgay_danh_gia() == null) return 0;
                        return o2.getNgay_danh_gia().compareTo(o1.getNgay_danh_gia());
                    });

                    adapterBinhLuan.notifyDataSetChanged();
                });
    }

    private void postCommentToServerAI() {
        String noiDung = etBinhLuan.getText().toString().trim();
        float soSao = rbChonSao.getRating(); // Lấy số sao khách chấm

        if (TextUtils.isEmpty(noiDung)) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : (user != null ? user.getEmail() : "Người dùng");
        String avatar = (user != null && user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : "";

        Map<String, Object> data = new HashMap<>();
        data.put("id_mon_an", currentDishId);
        data.put("id_nguoi_dung", currentUserId);
        data.put("ho_ten", name);
        data.put("anh_dai_dien", avatar);
        data.put("noi_dung_danh_gia", noiDung);
        data.put("so_sao", soSao); // Đẩy sao lên Firebase
        data.put("trang_thai", "pending"); 
        data.put("ngay_danh_gia", FieldValue.serverTimestamp());

        db.collection("danh_gia").add(data);
        etBinhLuan.setText(""); 
        rbChonSao.setRating(5); // Reset về 5 sao
        Toast.makeText(this, "Đang gửi bình luận...", Toast.LENGTH_SHORT).show();
    }

    private void checkIfSaved() {
        if (currentUserId == null) return;
        db.collection("mon_da_luu").document(currentUserId + "_" + currentDishId).addSnapshotListener((doc, error) -> {
            if (doc != null) {
                boolean newSavedStatus = doc.exists();
                if (isSaved != newSavedStatus) { 
                    isSaved = newSavedStatus;
                    updateSaveButtonUI(true); 
                } else {
                    isSaved = newSavedStatus;
                    updateSaveButtonUI(false); 
                }
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
            btnFavoriteDetail.animate()
                    .scaleX(1.4f)
                    .scaleY(1.4f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        btnFavoriteDetail.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(150)
                                .start();
                    })
                    .start();
        }
    }

    private void toggleSaveRecipe() {
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String idLuu = currentUserId + "_" + currentDishId;
        if (isSaved) {
            db.collection("mon_da_luu").document(idLuu).delete();
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("id_nguoi_dung", currentUserId);
            data.put("id_mon_an", currentDishId);
            db.collection("mon_da_luu").document(idLuu).set(data);
        }
        
        btnFavoriteDetail.animate().scaleX(0.7f).scaleY(0.7f).setDuration(100).withEndAction(() -> {
            btnFavoriteDetail.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
        }).start();
    }
}
