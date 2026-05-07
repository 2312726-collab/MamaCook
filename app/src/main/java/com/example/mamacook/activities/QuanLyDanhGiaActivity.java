package com.example.mamacook.activities;

import android.os.Bundle;
import android.widget.Toast;

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
    private DanhGiaAdminAdapter adapter;
    private FirebaseFirestore db;
    private final List<DocumentSnapshot> danhGiaList = new ArrayList<>();
    private ListenerRegistration danhGiaListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_danh_gia);

        db = FirebaseFirestore.getInstance();

        rvDanhGia = findViewById(R.id.rv_danh_gia_admin);
        adapter = new DanhGiaAdminAdapter(this, danhGiaList);

        rvDanhGia.setLayoutManager(new LinearLayoutManager(this));
        rvDanhGia.setAdapter(adapter);

        loadDanhGiaRealtime();
    }

    private void loadDanhGiaRealtime() {
        danhGiaListener = db.collection("danh_gia")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải đánh giá: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (querySnapshot == null) return;

                    danhGiaList.clear();
                    danhGiaList.addAll(querySnapshot.getDocuments());
                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (danhGiaListener != null) {
            danhGiaListener.remove();
        }
    }
}