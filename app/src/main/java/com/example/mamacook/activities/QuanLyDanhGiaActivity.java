package com.example.mamacook.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.AdapterView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.adapters.DanhGiaAdminAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class QuanLyDanhGiaActivity extends AppCompatActivity {

    private RecyclerView rvDanhGia;
    private EditText edtTimTenNguoiDung, edtTimBinhLuan;
    private Spinner spinnerTrangThai;
    private TextView tvSoLuongDanhGia;

    private DanhGiaAdminAdapter adapter;
    private FirebaseFirestore db;

    private final List<DocumentSnapshot> danhGiaGocList = new ArrayList<>();
    private final List<DocumentSnapshot> danhGiaLocList = new ArrayList<>();

    private ListenerRegistration danhGiaListener;

    private String keywordTen = "";
    private String keywordBinhLuan = "";
    private String trangThaiDangChon = "tat_ca";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_danh_gia);

        db = FirebaseFirestore.getInstance();

        anhXa();
        setupRecyclerView();
        setupSpinner();
        setupTimKiem();
        loadDanhGiaRealtime();
    }

    private void anhXa() {
        rvDanhGia = findViewById(R.id.rv_danh_gia_admin);
        edtTimTenNguoiDung = findViewById(R.id.edt_tim_ten_nguoi_dung);
        edtTimBinhLuan = findViewById(R.id.edt_tim_binh_luan);
        spinnerTrangThai = findViewById(R.id.spinner_trang_thai_danh_gia);
        tvSoLuongDanhGia = findViewById(R.id.tv_so_luong_danh_gia);
    }

    private void setupRecyclerView() {
        adapter = new DanhGiaAdminAdapter(this, danhGiaLocList);
        rvDanhGia.setLayoutManager(new LinearLayoutManager(this));
        rvDanhGia.setAdapter(adapter);
    }

    private void setupSpinner() {
        String[] trangThaiList = {
                "Tất cả",
                "Đang hiển thị",
                "Đã ẩn",
                "Vi phạm"
        };

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                trangThaiList
        );

        spinnerTrangThai.setAdapter(spinnerAdapter);

        spinnerTrangThai.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) trangThaiDangChon = "tat_ca";
                else if (position == 1) trangThaiDangChon = "hien_thi";
                else if (position == 2) trangThaiDangChon = "an";
                else trangThaiDangChon = "vi_pham";

                locDanhGia();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupTimKiem() {
        edtTimTenNguoiDung.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                keywordTen = s.toString().trim().toLowerCase();
                locDanhGia();
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        edtTimBinhLuan.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                keywordBinhLuan = s.toString().trim().toLowerCase();
                locDanhGia();
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadDanhGiaRealtime() {
        danhGiaListener = db.collection("danh_gia")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải đánh giá: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (querySnapshot == null) return;

                    danhGiaGocList.clear();
                    danhGiaGocList.addAll(querySnapshot.getDocuments());

                    locDanhGia();
                });
    }

    private void locDanhGia() {
        danhGiaLocList.clear();

        for (DocumentSnapshot doc : danhGiaGocList) {
            String tenNguoiDung = doc.getString("ten_nguoi_dung");
            String noiDung = doc.getString("noi_dung");
            String trangThai = doc.getString("trang_thai");

            if (tenNguoiDung == null) tenNguoiDung = "";
            if (noiDung == null) noiDung = "";
            if (trangThai == null || trangThai.isEmpty()) trangThai = "hien_thi";

            boolean khopTen = tenNguoiDung.toLowerCase().contains(keywordTen);
            boolean khopBinhLuan = noiDung.toLowerCase().contains(keywordBinhLuan);
            boolean khopTrangThai = trangThaiDangChon.equals("tat_ca") || trangThai.equals(trangThaiDangChon);

            if (khopTen && khopBinhLuan && khopTrangThai) {
                danhGiaLocList.add(doc);
            }
        }
        adapter.notifyDataSetChanged();
        tvSoLuongDanhGia.setText("Tổng: " + danhGiaLocList.size() + " đánh giá");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (danhGiaListener != null) {
            danhGiaListener.remove();
        }
    }
}