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
    private ImageView imgMonAn, btnFavoriteDetail, btnAddToPlan;
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
    private boolean isInPlan = false;
    private MonAn currentMonAn;

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
        btnAddToPlan = findViewById(R.id.btn_add_to_plan);

        adapterBinhLuan = new BinhLuanNgangAdapter(danhSachBinhLuan);
        rvDanhGia.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvDanhGia.setAdapter(adapterBinhLuan);

        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

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
            checkIfInPlan();
            addToHistory(currentDishId);
        }

        if (btnFavoriteDetail != null) {
            btnFavoriteDetail.setOnClickListener(v -> toggleSaveRecipe());
        }

        if (btnAddToPlan != null) {
            btnAddToPlan.setOnClickListener(v -> toggleCookingPlan());
        }

        btnGuiBinhLuan.setOnClickListener(v -> guiBinhLuan());

        if (tvXemTatCa != null) {
            tvXemTatCa.setOnClickListener(v -> {
                Intent intent = new Intent(DetailMonAnActivity.this, TatCaBinhLuanActivity.class);
                intent.putExtra("ID_MON_AN", currentDishId);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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

    private void checkIfInPlan() {
        if (currentUserId == null) return;
        db.collection("ke_hoach_nau_an")
                .document(currentUserId + "_" + currentDishId)
                .addSnapshotListener(this, (doc, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (doc != null) {
                        isInPlan = doc.exists();
                        updatePlanButtonUI();
                    }
                });
    }

    private void updatePlanButtonUI() {
        if (btnAddToPlan == null) return;
        if (isInPlan) {
            btnAddToPlan.setColorFilter(Color.parseColor("#FFEB3B")); // Màu vàng cho món trong kế hoạch
        } else {
            btnAddToPlan.setColorFilter(Color.WHITE);
        }
    }

    private void toggleCookingPlan() {
        if (currentUserId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentMonAn == null) return;

        String idPlan = currentUserId + "_" + currentDishId;
        if (isInPlan) {
            db.collection("ke_hoach_nau_an").document(idPlan).delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã xóa khỏi kế hoạch!", Toast.LENGTH_SHORT).show());
        } else {
            // Hiện Dialog chọn buổi
            String[] types = {"Sáng", "Trưa", "Tối"};
            new AlertDialog.Builder(this)
                    .setTitle("Chọn buổi nấu ăn")
                    .setItems(types, (dialog, which) -> {
                        String mealType = "";
                        if (which == 0) mealType = "Sang";
                        else if (which == 1) mealType = "Trua";
                        else mealType = "Toi";
                        
                        saveToCookingPlan(idPlan, mealType);
                    })
                    .show();
        }
    }

    private void saveToCookingPlan(String idPlan, String mealType) {
        Map<String, Object> plan = new HashMap<>();
        plan.put("id_nguoi_dung", currentUserId);
        plan.put("id_mon_an", currentDishId);
        plan.put("ten_mon", currentMonAn.getTen_mon());
        plan.put("hinh_anh", currentMonAn.getHinh_anh());
        plan.put("ngay_lap_ke_hoach", FieldValue.serverTimestamp());
        plan.put("trang_thai", "dang_di_cho");
        plan.put("buoi", mealType);

        List<Map<String, Object>> listNL = new ArrayList<>();
        if (currentMonAn.getDanh_sach_nguyen_lieu() != null) {
            for (MonAn.ChiTietNguyenLieu nl : currentMonAn.getDanh_sach_nguyen_lieu()) {
                Map<String, Object> item = new HashMap<>();
                item.put("ten_nguyen_lieu", nl.ten_nguyen_lieu);
                item.put("so_luong", nl.so_luong);
                item.put("don_vi", nl.don_vi);
                item.put("da_mua", false);
                listNL.add(item);
            }
        }
        plan.put("danh_sach_nguyen_lieu", listNL);

        db.collection("ke_hoach_nau_an").document(idPlan).set(plan)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã thêm vào kế hoạch " + (mealType.equals("Sang") ? "Sáng" : mealType.equals("Trua") ? "Trưa" : "Tối") + "!", Toast.LENGTH_SHORT).show());
    }

    private void fetchDishDetailsRealtime(String id) {
        db.collection("mon_an").document(id).addSnapshotListener(this, (doc, error) -> {
            if (isFinishing() || isDestroyed()) return;
            if (doc != null && doc.exists()) {
                currentMonAn = doc.toObject(MonAn.class);
                if (currentMonAn != null) {
                    currentMonAn.setId_mon_an(doc.getId());
                    tvTen.setText(currentMonAn.getTen_mon());
                    tvThoiGian.setText(String.format(Locale.getDefault(), "⌛ %d phút", currentMonAn.getThoi_gian_nau()));
                    tvRatingInfo.setText(String.format(Locale.getDefault(), "🕒 %.1f ⭐ (%d)", currentMonAn.getRating(), currentMonAn.getTong_luot_danh_gia()));

                    if (!isFinishing() && !isDestroyed()) {
                        String hinhAnh = currentMonAn.getHinh_anh();
                        if (hinhAnh != null && !hinhAnh.isEmpty()) {
                            if (hinhAnh.startsWith("http")) {
                                Glide.with(this).load(hinhAnh).placeholder(R.drawable.bg_splash).into(imgMonAn);
                            } else {
                                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(hinhAnh);
                                Glide.with(this).load(storageRef).placeholder(R.drawable.bg_splash).into(imgMonAn);
                            }
                        }
                    }

                    StringBuilder sb = new StringBuilder();
                    if (currentMonAn.getDanh_sach_nguyen_lieu() != null) {
                        for (MonAn.ChiTietNguyenLieu nl : currentMonAn.getDanh_sach_nguyen_lieu()) {
                            sb.append("• ").append(nl.so_luong).append(nl.don_vi).append(" ").append(nl.ten_nguyen_lieu).append("\n");
                        }
                    }
                    tvNguyenLieu.setText(sb.toString());

                    layoutBuocNau.removeAllViews();
                    if (currentMonAn.getDanh_sach_buoc_nau() != null) {
                        for (MonAn.BuocNau buoc : currentMonAn.getDanh_sach_buoc_nau()) {
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
                        allComments.add(dg);
                        totalStars += dg.getSo_sao();
                    }
                    int count = allComments.size();
                    if (count > 0) {
                        float average = totalStars / count;
                        String info = String.format(Locale.getDefault(), "⭐ %.1f (%d đánh giá)", average, count);
                        tvDiemTrungBinh.setText(info);
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
            Toast.makeText(this, "Vui lòng đăng nhập để lưu món ăn!", Toast.LENGTH_SHORT).show();
            return;
        }
        String idLuu = currentUserId + "_" + currentDishId;
        if (isSaved) {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận")
                    .setMessage("Bạn có chắc chắn muốn bỏ yêu thích món ăn này không?")
                    .setPositiveButton("Có", (dialog, which) -> {
                        db.collection("mon_da_luu").document(idLuu).delete();
                        Toast.makeText(this, "Đã xóa khỏi danh sách yêu thích!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("id_nguoi_dung", currentUserId);
            data.put("id_mon_an", currentDishId);
            db.collection("mon_da_luu").document(idLuu).set(data);
            Toast.makeText(this, "Đã thêm vào yêu thích!", Toast.LENGTH_SHORT).show();
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
