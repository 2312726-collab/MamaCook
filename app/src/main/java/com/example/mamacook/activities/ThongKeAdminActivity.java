package com.example.mamacook.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mamacook.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ThongKeAdminActivity extends AppCompatActivity {

    private TextView tvTongNguoiDung, tvTongMonAn, tvTongDanhGia;
    private TextView tvTrungBinhSao, tvMonHotTuan;
    private TextView tvLowRatingDishes, tvHoatDongHeThong;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thong_ke_admin);

        db = FirebaseFirestore.getInstance();

        tvTongNguoiDung = findViewById(R.id.tv_stat_users);
        tvTongMonAn = findViewById(R.id.tv_stat_dishes);
        tvTongDanhGia = findViewById(R.id.tv_stat_reviews);
        tvTrungBinhSao = findViewById(R.id.tv_stat_rating_avg);
        tvMonHotTuan = findViewById(R.id.tv_stat_hot_week);
        tvLowRatingDishes = findViewById(R.id.tv_low_rating_dishes);
        tvHoatDongHeThong = findViewById(R.id.tv_hoat_dong_he_thong);

        loadThongKe();
        loadMonHotTuan();
        loadMonDanhGiaThap();
        loadHoatDongHeThong();
    }

    private void loadThongKe() {
        db.collection("nguoi_dung")
                .get()
                .addOnSuccessListener(query ->
                        tvTongNguoiDung.setText(String.valueOf(query.size())))
                .addOnFailureListener(e -> tvTongNguoiDung.setText("0"));

        db.collection("mon_an")
                .get()
                .addOnSuccessListener(query ->
                        tvTongMonAn.setText(String.valueOf(query.size())))
                .addOnFailureListener(e -> tvTongMonAn.setText("0"));

        db.collection("danh_gia")
                .get()
                .addOnSuccessListener(query -> {
                    tvTongDanhGia.setText(String.valueOf(query.size()));

                    double tongSao = 0;
                    int count = 0;

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Number sao = doc.getDouble("so_sao");
                        if (sao == null) sao = doc.getLong("so_sao");

                        if (sao != null) {
                            tongSao += sao.doubleValue();
                            count++;
                        }
                    }

                    double avg = count == 0 ? 0 : tongSao / count;
                    tvTrungBinhSao.setText(String.format("%.1f /5", avg));
                })
                .addOnFailureListener(e -> {
                    tvTongDanhGia.setText("0");
                    tvTrungBinhSao.setText("0.0 /5");
                });
    }

    private void loadMonDanhGiaThap() {
        db.collection("mon_an")
                .get()
                .addOnSuccessListener(query -> {
                    int countLow = 0;

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Number rating = doc.getDouble("rating");
                        if (rating == null) rating = doc.getLong("rating");

                        Number tongLuot = doc.getLong("tong_luot_danh_gia");

                        if (rating != null
                                && tongLuot != null
                                && tongLuot.intValue() > 0
                                && rating.doubleValue() <= 3.5) {
                            countLow++;
                        }
                    }

                    tvLowRatingDishes.setText(String.valueOf(countLow));
                })
                .addOnFailureListener(e -> tvLowRatingDishes.setText("0"));
    }

    private void loadMonHotTuan() {
        long now = System.currentTimeMillis();
        long sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000;

        Timestamp mocTuan = new Timestamp(new java.util.Date(sevenDaysAgo));

        db.collection("lich_su_xem")
                .whereGreaterThanOrEqualTo("thoi_gian_xem", mocTuan)
                .get()
                .addOnSuccessListener(query -> {
                    java.util.Map<String, Integer> demMon = new java.util.HashMap<>();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String idMon = doc.getString("id_mon_an");

                        if (idMon != null) {
                            int soLan = demMon.containsKey(idMon)
                                    ? demMon.get(idMon)
                                    : 0;

                            demMon.put(idMon, soLan + 1);
                        }
                    }

                    if (demMon.isEmpty()) {
                        tvMonHotTuan.setText("Chưa có dữ liệu");
                        return;
                    }

                    String idMonHot = null;
                    int max = -1;

                    for (java.util.Map.Entry<String, Integer> entry : demMon.entrySet()) {
                        if (entry.getValue() > max) {
                            max = entry.getValue();
                            idMonHot = entry.getKey();
                        }
                    }

                    if (idMonHot == null) {
                        tvMonHotTuan.setText("Chưa có dữ liệu");
                        return;
                    }

                    db.collection("mon_an")
                            .document(idMonHot)
                            .get()
                            .addOnSuccessListener(doc -> {
                                String tenMon = doc.getString("ten_mon");

                                if (tenMon == null || tenMon.trim().isEmpty()) {
                                    tvMonHotTuan.setText("Không rõ tên món");
                                } else {
                                    tvMonHotTuan.setText(tenMon);
                                }
                            })
                            .addOnFailureListener(e ->
                                    tvMonHotTuan.setText("Lỗi tải dữ liệu"));
                })
                .addOnFailureListener(e ->
                        tvMonHotTuan.setText("Lỗi tải dữ liệu"));
    }

    private void loadHoatDongHeThong() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();

        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);

        Timestamp mocHomNay = new Timestamp(calendar.getTime());

        db.collection("nguoi_dung")
                .whereGreaterThanOrEqualTo("ngay_tao", mocHomNay)
                .get()
                .addOnSuccessListener(userQuery -> {
                    int soUserMoi = userQuery.size();

                    db.collection("mon_an")
                            .whereGreaterThanOrEqualTo("ngay_tao", mocHomNay)
                            .get()
                            .addOnSuccessListener(monQuery -> {
                                int soMonMoi = monQuery.size();

                                db.collection("danh_gia")
                                        .whereGreaterThanOrEqualTo("ngay_danh_gia", mocHomNay)
                                        .get()
                                        .addOnSuccessListener(danhGiaQuery -> {
                                            int soDanhGiaMoi = danhGiaQuery.size();

                                            String text =
                                                    "• " + soUserMoi + " người dùng mới đăng ký hôm nay\n\n" +
                                                            "• " + soMonMoi + " món ăn vừa được thêm\n\n" +
                                                            "• " + soDanhGiaMoi + " lượt đánh giá mới";

                                            tvHoatDongHeThong.setText(text);
                                        })
                                        .addOnFailureListener(e ->
                                                tvHoatDongHeThong.setText("Không thể tải số lượt đánh giá hôm nay."));
                            })
                            .addOnFailureListener(e ->
                                    tvHoatDongHeThong.setText("Không thể tải số món ăn mới hôm nay."));
                })
                .addOnFailureListener(e ->
                        tvHoatDongHeThong.setText("Không thể tải hoạt động hệ thống hôm nay."));
    }
}