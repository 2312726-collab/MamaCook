package com.example.mamacook.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.adapters.MonAnVerticalAdapter;
import com.example.mamacook.models.MonAn;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class QuanLyMonAnActivity extends AppCompatActivity {

    private RecyclerView rvMonAnAdmin;
    private Button btnTatCaMon, btnMonDanhGiaThap;

    private FirebaseFirestore db;
    private MonAnVerticalAdapter adapter;

    private final List<MonAn> fullList = new ArrayList<>();
    private final List<MonAn> filteredList = new ArrayList<>();

    private boolean dangLocDanhGiaThap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_mon_an);

        db = FirebaseFirestore.getInstance();

        rvMonAnAdmin = findViewById(R.id.rv_mon_an_admin);
        btnTatCaMon = findViewById(R.id.btn_tat_ca_mon);
        btnMonDanhGiaThap = findViewById(R.id.btn_mon_danh_gia_thap);

        adapter = new MonAnVerticalAdapter(filteredList);
        rvMonAnAdmin.setLayoutManager(new LinearLayoutManager(this));
        rvMonAnAdmin.setAdapter(adapter);

        btnTatCaMon.setOnClickListener(v -> {
            dangLocDanhGiaThap = false;
            locMonAn();
        });

        btnMonDanhGiaThap.setOnClickListener(v -> {
            dangLocDanhGiaThap = true;
            locMonAn();
        });

        loadMonAn();
    }

    private void loadMonAn() {
        db.collection("mon_an")
                .get()
                .addOnSuccessListener(query -> {
                    fullList.clear();
                    filteredList.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        MonAn monAn = doc.toObject(MonAn.class);

                        if (monAn.getId_mon_an() == null || monAn.getId_mon_an().isEmpty()) {
                            monAn.setId_mon_an(doc.getId());
                        }

                        fullList.add(monAn);
                    }

                    locMonAn();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải món ăn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void locMonAn() {
        filteredList.clear();

        for (MonAn monAn : fullList) {
            if (!dangLocDanhGiaThap) {
                filteredList.add(monAn);
            } else {
                if (monAn.getTong_luot_danh_gia() > 0 && monAn.getRating() <= 3.5) {
                    filteredList.add(monAn);
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (dangLocDanhGiaThap && filteredList.isEmpty()) {
            Toast.makeText(this, "Chưa có món nào bị đánh giá thấp", Toast.LENGTH_SHORT).show();
        }
    }
}