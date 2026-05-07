package com.example.mamacook.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mamacook.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ThongKeAdminActivity extends AppCompatActivity {

    private TextView tvTongNguoiDung, tvTongMonAn, tvTongDanhGia, tvTrungBinhSao, tvMonHotTuan;
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

        loadThongKe();
        loadMonHotTuan();
    }

    private void loadThongKe() {
        db.collection("nguoi_dung")
                .get()
                .addOnSuccessListener(query -> tvTongNguoiDung.setText(String.valueOf(query.size())))
                .addOnFailureListener(e -> tvTongNguoiDung.setText("0"));

        db.collection("mon_an")
                .get()
                .addOnSuccessListener(query -> tvTongMonAn.setText(String.valueOf(query.size())))
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
                    tvTrungBinhSao.setText(String.format("%.1f", avg));
                })
                .addOnFailureListener(e -> {
                    tvTongDanhGia.setText("0");
                    tvTrungBinhSao.setText("0.0");
                });
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
                            int soLan = demMon.containsKey(idMon) ? demMon.get(idMon) : 0;
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
                            .addOnFailureListener(e -> tvMonHotTuan.setText("Lỗi tải dữ liệu"));
                })
                .addOnFailureListener(e -> tvMonHotTuan.setText("Lỗi tải dữ liệu"));
    }
}