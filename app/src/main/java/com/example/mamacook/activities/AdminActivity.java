package com.example.mamacook.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mamacook.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends AppCompatActivity {

    private TextView tvTongNguoiDung, tvTongMonAn, tvTongDanhGia;

    private TextView[] tvWeekValues;
    private View[] barWeekViews;

    private LinearLayout layoutThongKe, layoutQuanLyTaiKhoan, layoutQuanLyDanhGia;
    private LinearLayout layoutQuanLyMonAn, layoutGuiThongBao, layoutChatNguoiDung;

    private FirebaseFirestore db;

    private int[] weeklyData = new int[7];
    private int loadedWeeklySources = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();

        anhXaView();
        loadTongQuanDuLieu();
        loadWeeklyActivity();
        batSuKienChuyenManHinh();
    }

    private void anhXaView() {
        tvTongNguoiDung = findViewById(R.id.tv_tong_nguoi_dung);
        tvTongMonAn = findViewById(R.id.tv_tong_mon_an);
        tvTongDanhGia = findViewById(R.id.tv_tong_danh_gia);

        tvWeekValues = new TextView[]{
                findViewById(R.id.tv_value_mon),
                findViewById(R.id.tv_value_tue),
                findViewById(R.id.tv_value_wed),
                findViewById(R.id.tv_value_thu),
                findViewById(R.id.tv_value_fri),
                findViewById(R.id.tv_value_sat),
                findViewById(R.id.tv_value_sun)
        };

        barWeekViews = new View[]{
                findViewById(R.id.bar_mon),
                findViewById(R.id.bar_tue),
                findViewById(R.id.bar_wed),
                findViewById(R.id.bar_thu),
                findViewById(R.id.bar_fri),
                findViewById(R.id.bar_sat),
                findViewById(R.id.bar_sun)
        };

        layoutThongKe = findViewById(R.id.layout_thong_ke);
        layoutQuanLyTaiKhoan = findViewById(R.id.layout_quan_ly_tai_khoan);
        layoutQuanLyDanhGia = findViewById(R.id.layout_quan_ly_danh_gia);
        layoutQuanLyMonAn = findViewById(R.id.layout_quan_ly_mon_an);
        layoutGuiThongBao = findViewById(R.id.layout_gui_thong_bao);
        layoutChatNguoiDung = findViewById(R.id.layout_chat_nguoi_dung);
    }

    private void loadTongQuanDuLieu() {
        db.collection("nguoi_dung")
                .get()
                .addOnSuccessListener(query ->
                        tvTongNguoiDung.setText(String.valueOf(query.size())))
                .addOnFailureListener(e ->
                        tvTongNguoiDung.setText("0"));

        db.collection("mon_an")
                .get()
                .addOnSuccessListener(query ->
                        tvTongMonAn.setText(String.valueOf(query.size())))
                .addOnFailureListener(e ->
                        tvTongMonAn.setText("0"));

        db.collection("danh_gia")
                .get()
                .addOnSuccessListener(query ->
                        tvTongDanhGia.setText(String.valueOf(query.size())))
                .addOnFailureListener(e ->
                        tvTongDanhGia.setText("0"));
    }

    private void loadWeeklyActivity() {
        weeklyData = new int[7];
        loadedWeeklySources = 0;

        loadWeeklyFromCollection("nguoi_dung", "ngay_tao");
        loadWeeklyFromCollection("danh_gia", "ngay_danh_gia");
        loadWeeklyFromCollection("lich_su_xem", "thoi_gian_xem");
    }

    private void loadWeeklyFromCollection(String collectionName, String timeField) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();

        calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);

        Timestamp startOfWeek = new Timestamp(calendar.getTime());

        db.collection(collectionName)
                .whereGreaterThanOrEqualTo(timeField, startOfWeek)
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Timestamp timestamp = doc.getTimestamp(timeField);
                        addToWeeklyData(timestamp);
                    }

                    loadedWeeklySources++;
                    if (loadedWeeklySources == 3) {
                        updateWeeklyChart();
                    }
                })
                .addOnFailureListener(e -> {
                    loadedWeeklySources++;
                    if (loadedWeeklySources == 3) {
                        updateWeeklyChart();
                    }
                });
    }

    private void addToWeeklyData(Timestamp timestamp) {
        if (timestamp == null) return;

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTime(timestamp.toDate());

        int day = calendar.get(java.util.Calendar.DAY_OF_WEEK);
        int index;

        switch (day) {
            case java.util.Calendar.MONDAY:
                index = 0;
                break;
            case java.util.Calendar.TUESDAY:
                index = 1;
                break;
            case java.util.Calendar.WEDNESDAY:
                index = 2;
                break;
            case java.util.Calendar.THURSDAY:
                index = 3;
                break;
            case java.util.Calendar.FRIDAY:
                index = 4;
                break;
            case java.util.Calendar.SATURDAY:
                index = 5;
                break;
            default:
                index = 6;
                break;
        }

        weeklyData[index]++;
    }

    private void updateWeeklyChart() {
        int max = 0;

        for (int value : weeklyData) {
            if (value > max) {
                max = value;
            }
        }

        if (max == 0) {
            max = 1;
        }

        for (int i = 0; i < 7; i++) {
            tvWeekValues[i].setText(String.valueOf(weeklyData[i]));

            int height;

            if (weeklyData[i] == 0) {
                height = 12;
            } else {
                height = 25 + (int) (Math.sqrt(weeklyData[i]) * 150 / Math.sqrt(max));
            }

            android.view.ViewGroup.LayoutParams params = barWeekViews[i].getLayoutParams();
            params.height = height;
            barWeekViews[i].setLayoutParams(params);
        }
    }

    private void batSuKienChuyenManHinh() {
        layoutThongKe.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, ThongKeAdminActivity.class)));

        layoutQuanLyTaiKhoan.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, QuanLyTaiKhoanActivity.class)));

        layoutQuanLyDanhGia.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, QuanLyDanhGiaActivity.class)));

        layoutQuanLyMonAn.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, QuanLyMonAnActivity.class)));

        layoutGuiThongBao.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, AdminGuiThongBaoActivity.class)));

        layoutChatNguoiDung.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, DanhSachChatActivity.class)));
    }
}