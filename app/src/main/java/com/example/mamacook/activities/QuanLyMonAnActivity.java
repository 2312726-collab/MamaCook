package com.example.mamacook.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.adapters.MonAnVerticalAdapter;
import com.example.mamacook.models.MonAn;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class QuanLyMonAnActivity extends AppCompatActivity {

    private RecyclerView rvMonAnAdmin;
    private Button btnTatCaMon, btnMonDanhGiaThap;
    private SearchView searchViewMonAn;
    private FloatingActionButton fabAddMonAn;

    private FirebaseFirestore db;
    private MonAnVerticalAdapter adapter;
    private ListenerRegistration snapshotListener;

    private final List<MonAn> fullList = new ArrayList<>();
    private final List<MonAn> filteredList = new ArrayList<>();

    private boolean dangLocDanhGiaThap = false;
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_mon_an);

        initViews();
        setupRecyclerView();
        setupListeners();
        observeMonAnRealtime();
    }

    private void initViews() {
        db = FirebaseFirestore.getInstance();
        rvMonAnAdmin = findViewById(R.id.rv_mon_an_admin);
        btnTatCaMon = findViewById(R.id.btn_tat_ca_mon);
        btnMonDanhGiaThap = findViewById(R.id.btn_mon_danh_gia_thap);
        searchViewMonAn = findViewById(R.id.search_view_mon_an);
        fabAddMonAn = findViewById(R.id.fab_add_mon_an);
    }

    private void setupRecyclerView() {
        adapter = new MonAnVerticalAdapter(filteredList);
        adapter.setAdminMode(true);

        // ✅ THÊM: Xử lý khi bấm nút AI
        adapter.setOnItemLongClickListener(monAn -> {
            Toast.makeText(this, "AI xử lý: " + monAn.getTen_mon(), Toast.LENGTH_SHORT).show();
            // Thêm xử lý AI của bạn vào đây
        });

        rvMonAnAdmin.setLayoutManager(new LinearLayoutManager(this));
        rvMonAnAdmin.setAdapter(adapter);
    }
    private void setupListeners() {
        btnTatCaMon.setOnClickListener(v -> {
            dangLocDanhGiaThap = false;
            updateFilterAndUI();
        });

        btnMonDanhGiaThap.setOnClickListener(v -> {
            dangLocDanhGiaThap = true;
            updateFilterAndUI();
        });

        searchViewMonAn.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query.toLowerCase().trim();
                updateFilterAndUI();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText.toLowerCase().trim();
                updateFilterAndUI();
                return true;
            }
        });

        fabAddMonAn.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditMonAnActivity.class);
            startActivity(intent);
        });
    }

    private void observeMonAnRealtime() {
        if (snapshotListener != null) snapshotListener.remove();

        snapshotListener = db.collection("mon_an")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi cập nhật Real-time: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        fullList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            MonAn monAn = doc.toObject(MonAn.class);
                            if (monAn.getId_mon_an() == null) monAn.setId_mon_an(doc.getId());
                            fullList.add(monAn);
                        }
                        updateFilterAndUI();
                    }
                });
    }

    private void updateFilterAndUI() {
        filteredList.clear();

        for (MonAn monAn : fullList) {
            boolean matchesSearch = monAn.getTen_mon().toLowerCase().contains(currentQuery);
            boolean matchesFilter = !dangLocDanhGiaThap || (monAn.getTong_luot_danh_gia() > 0 && monAn.getRating() <= 3.5);

            if (matchesSearch && matchesFilter) {
                filteredList.add(monAn);
            }
        }

        adapter.notifyDataSetChanged();
        
        // Cập nhật UI nút bấm
        btnTatCaMon.setAlpha(dangLocDanhGiaThap ? 0.6f : 1.0f);
        btnMonDanhGiaThap.setAlpha(dangLocDanhGiaThap ? 1.0f : 0.6f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (snapshotListener != null) snapshotListener.remove();
    }
}