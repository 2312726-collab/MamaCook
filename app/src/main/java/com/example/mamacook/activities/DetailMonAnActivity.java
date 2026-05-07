package com.example.mamacook.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mamacook.adapters.BinhLuanNgangAdapter;
import com.example.mamacook.R;
import com.example.mamacook.models.DanhGia;
import com.example.mamacook.models.MonAn;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetailMonAnActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ImageView imgMonAn, btnFavoriteDetail;
    private TextView tvTen, tvRatingInfo, tvThoiGian, tvNguyenLieu, tvDiemTrungBinh, tvXemTatCa;
    private LinearLayout layoutBuocNau;
    private RecyclerView rvDanhGia;
    private BinhLuanNgangAdapter adapterBinhLuan;
    private final List<DanhGia> danhSachBinhLuan = new ArrayList<>();
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
            fetchCommentsSmartRealtime(currentDishId);
            checkIfSaved();
            addToHistory(currentDishId);
        }

        btnFavoriteDetail.setOnClickListener(v -> toggleSaveRecipe());

        btnGuiBinhLuan.setOnClickListener(v -> guiBinhLuan());
        if (btnFavoriteDetail != null) {
            btnFavoriteDetail.setOnClickListener(v -> toggleSaveRecipe());
        }

        // NODE 2: Cài đặt sự kiện click chuyển sang TatCaBinhLuanActivity
        if (tvXemTatCa != null) {
            tvXemTatCa.setOnClickListener(v -> {
                Intent intent = new Intent(DetailMonAnActivity.this, TatCaBinhLuanActivity.class);
                intent.putExtra("ID_MON_AN", currentDishId);
                startActivity(intent);
            });
        }
    }

    private void guiBinhLuan() {
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đánh giá!", Toast.LENGTH_SHORT).show();
            return;
        }

        String noiDung = etBinhLuan.getText().toString().trim();
        float soSaoMoi = rbChonSao.getRating();

        if (TextUtils.isEmpty(noiDung) || soSaoMoi == 0) {
            Toast.makeText(this, "Vui lòng nhập nội dung và chọn sao!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = (user != null && !TextUtils.isEmpty(user.getDisplayName())) ? user.getDisplayName() : "Người dùng";

        DocumentReference monAnRef = db.collection("mon_an").document(currentDishId);

        monAnRef.get().addOnSuccessListener(documentSnapshot -> {
            if (isFinishing() || isDestroyed()) return;
            if (documentSnapshot.exists()) {
                MonAn monAn = documentSnapshot.toObject(MonAn.class);
                if (monAn == null) return;

                double ratingHienTai = monAn.getRating();
                int tongLuotHienTai = monAn.getTong_luot_danh_gia();

                int tongLuotMoi = tongLuotHienTai + 1;
                double ratingMoi = ((ratingHienTai * tongLuotHienTai) + soSaoMoi) / tongLuotMoi;

                Map<String, Object> comment = new HashMap<>();

                comment.put("id_mon_an", currentDishId);
                comment.put("noi_dung", noiDung);
                comment.put("so_sao", soSaoMoi);

                comment.put("trang_thai", "hien_thi");

                comment.put("ten_nguoi_dung", name);

                comment.put("ngay_danh_gia", FieldValue.serverTimestamp());

                WriteBatch batch = db.batch();
                DocumentReference commentRef = db.collection("danh_gia").document();
                batch.set(commentRef, comment);
                batch.update(monAnRef, "rating", ratingMoi, "tong_luot_danh_gia", tongLuotMoi);

                batch.commit().addOnSuccessListener(aVoid -> {
                    if (isFinishing() || isDestroyed()) return;
                    etBinhLuan.setText("");
                    rbChonSao.setRating(5);
                    Toast.makeText(DetailMonAnActivity.this, "Đánh giá thành công!", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void fetchDishDetailsRealtime(String id) {
        // Thêm tham số 'this' vào addSnapshotListener để tự động hủy lắng nghe khi Activity đóng
        db.collection("mon_an").document(id).addSnapshotListener(this, (doc, error) -> {
            if (isFinishing() || isDestroyed()) return;
            if (doc != null && doc.exists()) {
                MonAn monAn = doc.toObject(MonAn.class);
                if (monAn != null) {
                    tvTen.setText(monAn.getTen_mon());
                    tvThoiGian.setText(String.format(Locale.getDefault(), "⌛ %d phút", monAn.getThoi_gian_nau()));
                    tvRatingInfo.setText(String.format(Locale.getDefault(), "🕒 %.1f ⭐ (%d)", monAn.getRating(), monAn.getTong_luot_danh_gia()));

                    // Kiểm tra an toàn trước khi gọi Glide
                    if (!isFinishing() && !isDestroyed()) {
                        Glide.with(this).load(monAn.getHinh_anh()).placeholder(R.drawable.bg_splash).into(imgMonAn);
                    }
                    tvThoiGian.setText("⌛ " + monAn.getThoi_gian_nau() + " phút");

                    String hinhAnh = monAn.getHinh_anh();
                    if (hinhAnh != null && !hinhAnh.isEmpty()) {
                        if (hinhAnh.startsWith("http")) {
                            Glide.with(this).load(hinhAnh).into(imgMonAn);
                        } else {
                            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(hinhAnh);
                            Glide.with(this).load(storageRef).into(imgMonAn);
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

    private void fetchCommentsSmartRealtime(String dishId) {
        db.collection("danh_gia")
                .whereEqualTo("id_mon_an", dishId)
                .whereEqualTo("trang_thai", "hien_thi")
                .addSnapshotListener(this, (value, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (error != null || value == null) return;
                    List<DanhGia> allComments = new ArrayList<>();
                    float totalStars = 0;
                    for (QueryDocumentSnapshot doc : value) {
                        DanhGia dg = doc.toObject(DanhGia.class);
                        totalStars += dg.getSo_sao();
                    }
                    int count = allComments.size();
                    if (count > 0) {
                        float average = totalStars / count;
                        String info = String.format(Locale.getDefault(), "⭐ %.1f (%d đánh giá)", average, count);
                        tvDiemTrungBinh.setText(info);
                        tvRatingInfo.setText(info);
                    } else {
                        tvDiemTrungBinh.setText("⭐ 0.0 (0 đánh giá)");
                        tvRatingInfo.setText("⭐ 0.0 (0 đánh giá)");
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
                btnFavoriteDetail.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
            }).start();
        }
    }

    private void toggleSaveRecipe() {
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để lưu món ăn!", Toast.LENGTH_SHORT).show();
            return;
        }

        String idLuu = currentUserId + "_" + currentDishId;

        if (isSaved) {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận")
                    .setMessage("Bạn có chắc chắn muốn bỏ yêu thích món ăn này không?")
                    .setPositiveButton("Có", (dialog, which) -> {
                        db.collection("mon_da_luu").document(idLuu).delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Đã xóa khỏi món ăn yêu thích!", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("id_nguoi_dung", currentUserId);
            data.put("id_mon_an", currentDishId);
            data.put("id_luu", idLuu);
            db.collection("mon_da_luu").document(idLuu).set(data)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã thêm vào yêu thích!", Toast.LENGTH_SHORT).show();
                    });
        }
        updateSaveButtonUI(true);
    }

    private void addToHistory(String dishId) {
        if (currentUserId == null) return;
        db.collection("lich_su_xem")
                .whereEqualTo("id_nguoi_dung", currentUserId)
                .whereEqualTo("id_mon_an", dishId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (!querySnapshot.isEmpty()) {
                        String docId = querySnapshot.getDocuments().get(0).getId();
                        db.collection("lich_su_xem").document(docId)
                                .update("thoi_gian_xem", FieldValue.serverTimestamp());
                    } else {
                        Map<String, Object> history = new HashMap<>();
                        history.put("id_nguoi_dung", currentUserId);
                        history.put("id_mon_an", dishId);
                        history.put("thoi_gian_xem", FieldValue.serverTimestamp());
                        db.collection("lich_su_xem").add(history)
                                .addOnSuccessListener(documentReference -> limitHistoryTo15());
                    }
                });
    }

    private void limitHistoryTo15() {
        db.collection("lich_su_xem")
                .whereEqualTo("id_nguoi_dung", currentUserId)
                .orderBy("thoi_gian_xem", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (querySnapshot.size() > 15) {
                        for (int i = 15; i < querySnapshot.size(); i++) {
                            db.collection("lich_su_xem")
                                    .document(querySnapshot.getDocuments().get(i).getId())
                                    .delete();
                        }
                    }
                });
    }
}
