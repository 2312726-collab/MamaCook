package com.example.mamacook.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.adapters.CookingPlanAdapter;
import com.example.mamacook.models.CookingPlan;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChuanBiNauActivity extends AppCompatActivity {

    private RecyclerView rvChuanBi;
    private CookingPlanAdapter adapter;
    private List<CookingPlan> planList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;
    private View layoutEmpty;
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        // Cố định thanh trạng thái màu trắng, chữ đen và không cho layout tràn lên
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.view.Window window = getWindow();
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            
            // Bật chế độ icon tối
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                View decorView = window.getDecorView();
                int flags = decorView.getSystemUiVisibility();
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                decorView.setSystemUiVisibility(flags);
            }
            
            window.setStatusBarColor(android.graphics.Color.parseColor("#F2E7D5"));
        }

        setContentView(R.layout.activity_chuan_bi_nau);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        selectedDate = getIntent().getStringExtra("SELECTED_DATE");

        Toolbar toolbar = findViewById(R.id.toolbar_chuan_bi_nau);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvChuanBi = findViewById(R.id.rv_chuan_bi_nau);
        layoutEmpty = findViewById(R.id.layout_empty_ready);

        adapter = new CookingPlanAdapter(planList, false); // Mode false for Ready to Cook
        rvChuanBi.setLayoutManager(new LinearLayoutManager(this));
        rvChuanBi.setAdapter(adapter);

        loadReadyToCookList();
    }

    private void loadReadyToCookList() {
        if (currentUserId == null) return;

        com.google.firebase.firestore.Query query = db.collection("ke_hoach_nau_an")
                .whereEqualTo("id_nguoi_dung", currentUserId)
                .whereEqualTo("trang_thai", "cho_nau");

        if (selectedDate != null) {
            query = query.whereEqualTo("ngay_chi_tiet", selectedDate);
        }

        query.addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    planList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            CookingPlan plan = doc.toObject(CookingPlan.class);
                            if (plan != null) {
                                plan.setId_plan(doc.getId()); // Gán ID document
                                planList.add(plan);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    layoutEmpty.setVisibility(planList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }
}
