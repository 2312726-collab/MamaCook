package com.example.mamacook;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetailMonAnActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ImageView imgMonAn, btnCamera, btnFavoriteDetail;
    private TextView tvTen, tvRatingInfo, tvThoiGian, tvNguyenLieu, tvDiemTrungBinh, tvXemTatCa;
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
        tvDiemTrungBinh = findViewById(R.id.tvDiemTrungBinh);
        tvXemTatCa = findViewById(R.id.tvXemTatCa); // NODE 2: Ánh xạ ID chuẩn
        layoutBuocNau = findViewById(R.id.layout_buoc_nau);
        rvDanhGia = findViewById(R.id.rv_danh_gia);
        etBinhLuan = findViewById(R.id.et_binh_luan);
        rbChonSao = findViewById(R.id.rb_chon_sao);
        btnGuiBinhLuan = findViewById(R.id.btn_gui_binh_luan);
        btnFavoriteDetail = findViewById(R.id.btn_favorite_detail);
        btnCamera = findViewById(R.id.btn_camera);

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

        btnGuiBinhLuan.setOnClickListener(v -> guiBinhLuan());

        // NODE 2: Cài đặt sự kiện click chuyển sang TatCaBinhLuanActivity
        if (tvXemTatCa != null) {
            tvXemTatCa.setOnClickListener(v -> {
                Intent intent = new Intent(DetailMonAnActivity.this, TatCaBinhLuanActivity.class);
                intent.putExtra("ID_MON_AN", currentDishId);
                startActivity(intent);
            });
        }

        if (btnFavoriteDetail != null) {
            btnFavoriteDetail.setOnClickListener(v -> toggleSaveRecipe());
        }
    }

    private void fetchDishDetailsRealtime(String id) {
        db.collection("mon_an").document(id).addSnapshotListener((doc, error) -> {
            if (doc != null && doc.exists()) {
                MonAn monAn = doc.toObject(MonAn.class);
                if (monAn != null) {
                    tvTen.setText(monAn.getTen_mon());
                    tvThoiGian.setText("⌛ " + monAn.getThoi_gian_nau() + " phút");
                    tvRatingInfo.setText("🕒 " + monAn.getRating() + " ⭐ " + monAn.getTong_luot_danh_gia());
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
                    if (error != null || value == null) return;

                    List<DanhGia> allComments = new ArrayList<>();
                    float totalStars = 0;
                    int count = value.size();

                    for (QueryDocumentSnapshot doc : value) {
                        DanhGia dg = doc.toObject(DanhGia.class);
                        allComments.add(dg);
                        totalStars += dg.getSo_sao();
                    }

                    if (count > 0) {
                        float average = totalStars / count;
                        tvDiemTrungBinh.setText(String.format(Locale.getDefault(), "⭐ %.1f (%d đánh giá)", average, count));
                    } else {
                        tvDiemTrungBinh.setText("⭐ 0.0 (0 đánh giá)");
                    }

                    Collections.sort(allComments, (o1, o2) -> {
                        if (o1.getNgay_danh_gia() == null || o2.getNgay_danh_gia() == null) return 0;
                        return o2.getNgay_danh_gia().compareTo(o1.getNgay_danh_gia());
                    });

                    danhSachBinhLuan.clear();
                    for (int i = 0; i < Math.min(5, allComments.size()); i++) {
                        danhSachBinhLuan.add(allComments.get(i));
                    }
                    adapterBinhLuan.notifyDataSetChanged();
                });
    }

    private void guiBinhLuan() {
        String noiDung = etBinhLuan.getText().toString().trim();
        float soSao = rbChonSao.getRating();

        if (TextUtils.isEmpty(noiDung) || soSao == 0) {
            Toast.makeText(this, "Vui lòng nhập nội dung và chọn sao!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = (user != null && !TextUtils.isEmpty(user.getDisplayName())) ? user.getDisplayName() : "Người dùng";

        Map<String, Object> comment = new HashMap<>();
        comment.put("id_mon_an", currentDishId);
        comment.put("noi_dung", noiDung);
        comment.put("so_sao", soSao);
        comment.put("trang_thai", "cho_duyet");
        comment.put("ten_nguoi_dung", name);
        comment.put("ngay_danh_gia", FieldValue.serverTimestamp());

        db.collection("danh_gia").add(comment).addOnSuccessListener(docRef -> {
            etBinhLuan.setText("");
            rbChonSao.setRating(5);
            Toast.makeText(DetailMonAnActivity.this, "Gửi thành công! Đang chờ AI duyệt bài.", Toast.LENGTH_SHORT).show();
        });
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
