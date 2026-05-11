package com.example.mamacook.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.adapters.ThongBaoAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThongBaoActivity extends AppCompatActivity {

    private RecyclerView rvThongBao;
    private ImageButton btnBack;

    private ThongBaoAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration thongBaoListener;

    private final List<DocumentSnapshot> thongBaoList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thong_bao);

        db = FirebaseFirestore.getInstance();

        rvThongBao = findViewById(R.id.rv_thong_bao);
        btnBack = findViewById(R.id.btn_back_thong_bao);

        adapter = new ThongBaoAdapter(this, thongBaoList);

        rvThongBao.setLayoutManager(new LinearLayoutManager(this));
        rvThongBao.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        loadThongBaoRealtime();
    }

    private void loadThongBaoRealtime() {
        thongBaoListener = db.collection("thong_bao")
                .whereEqualTo("id_nguoi_nhan", "all")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải thông báo: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (querySnapshot == null) return;

                    thongBaoList.clear();
                    thongBaoList.addAll(querySnapshot.getDocuments());

                    Collections.sort(thongBaoList, (doc1, doc2) -> {
                        Timestamp time1 = doc1.getTimestamp("ngay_tao");
                        Timestamp time2 = doc2.getTimestamp("ngay_tao");

                        if (time1 == null && time2 == null) return 0;
                        if (time1 == null) return 1;
                        if (time2 == null) return -1;

                        return time2.compareTo(time1);
                    });

                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (thongBaoListener != null) {
            thongBaoListener.remove();
        }
    }
}