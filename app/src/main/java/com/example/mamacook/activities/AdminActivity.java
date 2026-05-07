package com.example.mamacook.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mamacook.R;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminActivity extends AppCompatActivity {

    private TextView tvTongNguoiDung, tvTongMonAn, tvTongDanhGia;
    private LinearLayout layoutThongKe, layoutQuanLyTaiKhoan, layoutQuanLyDanhGia, layoutQuanLyMonAn;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();

        tvTongNguoiDung = findViewById(R.id.tv_tong_nguoi_dung);
        tvTongMonAn = findViewById(R.id.tv_tong_mon_an);
        tvTongDanhGia = findViewById(R.id.tv_tong_danh_gia);

        layoutThongKe = findViewById(R.id.layout_thong_ke);
        layoutQuanLyTaiKhoan = findViewById(R.id.layout_quan_ly_tai_khoan);
        layoutQuanLyDanhGia = findViewById(R.id.layout_quan_ly_danh_gia);
        layoutQuanLyMonAn = findViewById(R.id.layout_quan_ly_mon_an);

        loadThongKeNhanh();
        setupClick();
    }

    private void setupClick() {
        layoutThongKe.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, ThongKeAdminActivity.class)));

        layoutQuanLyTaiKhoan.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, QuanLyTaiKhoanActivity.class)));

        layoutQuanLyDanhGia.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, QuanLyDanhGiaActivity.class)));

        layoutQuanLyMonAn.setOnClickListener(v ->
                startActivity(new Intent(AdminActivity.this, QuanLyMonAnActivity.class)));
    }

    private void loadThongKeNhanh() {
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
                .addOnSuccessListener(query -> tvTongDanhGia.setText(String.valueOf(query.size())))
                .addOnFailureListener(e -> tvTongDanhGia.setText("0"));
    }
}