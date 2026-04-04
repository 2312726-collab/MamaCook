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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class DetailMonAnActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ImageView imgMonAn, btnCamera;
    private TextView tvTen, tvRatingInfo, tvThoiGian, tvNguyenLieu, tvSummaryRating;
    private LinearLayout layoutBuocNau, layoutDanhGia;
    private EditText etBinhLuan;
    private ImageView btnGuiBinhLuan;
    private Button btnSaveRecipe;
    private String currentDishId;
    private String currentUserId;
    private boolean isSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_mon_an);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        // Ánh xạ views
        imgMonAn = findViewById(R.id.img_detail_mon_an);
        tvTen = findViewById(R.id.tv_detail_ten);
        tvRatingInfo = findViewById(R.id.tv_detail_rating_info);
        tvThoiGian = findViewById(R.id.tv_detail_thoi_gian);
        tvNguyenLieu = findViewById(R.id.tv_detail_nguyen_lieu);
        tvSummaryRating = findViewById(R.id.tv_summary_rating);
        layoutBuocNau = findViewById(R.id.layout_buoc_nau);
        layoutDanhGia = findViewById(R.id.layout_danh_gia);
        etBinhLuan = findViewById(R.id.et_binh_luan);
        btnGuiBinhLuan = findViewById(R.id.btn_gui_binh_luan);
        btnSaveRecipe = findViewById(R.id.btn_save_recipe);
        btnCamera = findViewById(R.id.btn_camera);

        Toolbar toolbar = findViewById(R.id.toolbar_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> finish());

        currentDishId = getIntent().getStringExtra("ID_MON_AN");
        if (currentDishId != null) {
            fetchDishDetailsRealtime(currentDishId);
            fetchCommentsCleanRealtime(currentDishId);
            checkIfSaved();
            
            // 🔥 NẾU ÔNG MUỐN XÓA SẠCH ĐỂ TEST LẠI THÌ BỎ 2 DẤU // Ở DƯỚI RỒI CHẠY APP
            // clearAllComments(); 
        }

        // GÁN SỰ KIỆN CHO CÁC NÚT (Đã fix đủ)
        btnGuiBinhLuan.setOnClickListener(v -> postCommentWithAI());
        btnSaveRecipe.setOnClickListener(v -> toggleSaveRecipe());
        
        btnCamera.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng gửi ảnh đang được cập nhật!", Toast.LENGTH_SHORT).show();
        });

        tvSummaryRating.setOnClickListener(v -> {
            Toast.makeText(this, "Xem tất cả đánh giá của món này", Toast.LENGTH_SHORT).show();
        });
    }

    private void clearAllComments() {
        db.collection("danh_gia").get().addOnSuccessListener(querySnapshot -> {
            for (var doc : querySnapshot.getDocuments()) {
                doc.getReference().delete();
            }
            Toast.makeText(this, "🔥 Đã dọn dẹp sạch database!", Toast.LENGTH_SHORT).show();
        });
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
                            ImageView imgStep = stepView.findViewById(R.id.img_step);
                            if (buoc.hinh_anh_buoc != null && !buoc.hinh_anh_buoc.isEmpty()) {
                                Glide.with(this).load(buoc.hinh_anh_buoc).into(imgStep);
                            } else {
                                imgStep.setVisibility(View.GONE);
                            }
                            layoutBuocNau.addView(stepView);
                        }
                    }
                }
            }
        });
    }

    private void fetchCommentsCleanRealtime(String dishId) {
        db.collection("danh_gia")
                .whereEqualTo("id_mon_an", dishId)
                .whereEqualTo("trang_thai", "hien_thi")
                .orderBy("ngay_danh_gia", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    layoutDanhGia.removeAllViews();
                    for (var doc : value.getDocuments()) {
                        DanhGia dg = doc.toObject(DanhGia.class);
                        if (dg != null) addCommentView(dg);
                    }
                });
    }

    private void addCommentView(DanhGia dg) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_danh_gia, layoutDanhGia, false);
        ((TextView) view.findViewById(R.id.tv_user_name)).setText(dg.getHo_ten());
        ((TextView) view.findViewById(R.id.tv_comment_content)).setText(dg.getNoi_dung_danh_gia());
        TextView tvStatus = view.findViewById(R.id.tv_status_label);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("Đã đăng");
        tvStatus.setTextColor(Color.parseColor("#4CAF50"));
        layoutDanhGia.addView(view);
    }

    private void postCommentWithAI() {
        String contentText = etBinhLuan.getText().toString().trim();
        if (TextUtils.isEmpty(contentText)) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : (user != null ? user.getEmail() : "Người dùng");

        Map<String, Object> data = new HashMap<>();
        data.put("id_mon_an", currentDishId);
        data.put("id_nguoi_dung", currentUserId);
        data.put("ho_ten", name);
        data.put("noi_dung_danh_gia", contentText);
        data.put("trang_thai", "pending"); 
        data.put("ngay_danh_gia", Timestamp.now());

        db.collection("danh_gia").add(data).addOnSuccessListener(docRef -> {
            etBinhLuan.setText("");
            Toast.makeText(this, "Đang kiểm duyệt...", Toast.LENGTH_SHORT).show();

            // 🤖 PROMPT MỚI: CÔNG TÂM - KHÔNG CHẶN NHẦM LỜI KHEN
            String prompt = "Mày là trợ lý kiểm duyệt bình luận cho App nấu ăn MamaCook. " +
                            "Nhiệm vụ: Phân loại bình luận sau: '" + contentText + "'. " +
                            "QUY TẮC: " +
                            "- Trả về 'APPROVE' cho các lời khen, cảm nhận tích cực (VD: ngon, tuyệt, hay, cám ơn...). " +
                            "- CHỈ trả về 'REJECT' khi nội dung có chửi thề, viết tắt thô tục (VD: cc, cl, đm, vcl...). " +
                            "TRẢ VỀ ĐÚNG 1 TỪ: APPROVE HOẶC REJECT. KHÔNG GIẢI THÍCH.";

            GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", "YOUR_API_KEY"); 
            GenerativeModelFutures model = GenerativeModelFutures.from(gm);
            Content content = new Content.Builder().addText(prompt).build();

            Futures.addCallback(model.generateContent(content), new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String verdict = result.getText().trim().toUpperCase();
                    String finalStatus = verdict.contains("APPROVE") ? "hien_thi" : "vi_pham";
                    docRef.update("trang_thai", finalStatus);
                    if (finalStatus.equals("vi_pham")) {
                        Toast.makeText(DetailMonAnActivity.this, "Bình luận vi phạm đã bị ẩn!", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Throwable t) { 
                    docRef.update("trang_thai", "vi_pham"); 
                }
            }, ContextCompat.getMainExecutor(this));
        });
    }

    private void checkIfSaved() {
        if (currentUserId == null) return;
        db.collection("mon_da_luu").document(currentUserId + "_" + currentDishId).addSnapshotListener((doc, error) -> {
            if (doc != null) {
                isSaved = doc.exists();
                btnSaveRecipe.setText(isSaved ? "❤ Đã lưu" : "❤ Lưu công thức");
            }
        });
    }

    private void toggleSaveRecipe() {
        if (currentUserId == null) return;
        String idLuu = currentUserId + "_" + currentDishId; // Fix lỗi ID lưu
        if (isSaved) {
            db.collection("mon_da_luu").document(idLuu).delete();
        } else {
            Map<String, Object> data = new HashMap<>();
            data.put("id_nguoi_dung", currentUserId);
            data.put("id_mon_an", currentDishId);
            db.collection("mon_da_luu").document(idLuu).set(data);
        }
    }
}
