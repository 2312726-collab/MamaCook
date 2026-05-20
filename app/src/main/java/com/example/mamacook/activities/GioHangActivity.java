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

public class GioHangActivity extends AppCompatActivity {

    private RecyclerView rvGioHang;
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

        // Làm trong suốt thanh trạng thái và chỉnh màu icon (Pin, Sóng...) cho dễ nhìn trên nền sáng
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.view.Window window = getWindow();
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            
            // Nếu Android 6.0 trở lên, đổi icon thanh trạng thái sang màu tối (vì nền của mình đang là màu sáng)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }

            window.getDecorView().setSystemUiVisibility(flags);
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_gio_hang);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();
        selectedDate = getIntent().getStringExtra("SELECTED_DATE");

        Toolbar toolbar = findViewById(R.id.toolbar_gio_hang);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvGioHang = findViewById(R.id.rv_gio_hang);
        layoutEmpty = findViewById(R.id.layout_empty_cart);

        adapter = new CookingPlanAdapter(planList, true);
        rvGioHang.setLayoutManager(new LinearLayoutManager(this));
        rvGioHang.setAdapter(adapter);

        loadShoppingList();
    }

    private void loadShoppingList() {
        if (currentUserId == null) return;

        com.google.firebase.firestore.Query query = db.collection("ke_hoach_nau_an")
                .whereEqualTo("id_nguoi_dung", currentUserId)
                .whereEqualTo("trang_thai", "dang_di_cho");

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
                                plan.setId_plan(doc.getId()); // Quan trọng: Gán ID để xóa/sửa chính xác
                                planList.add(plan);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    layoutEmpty.setVisibility(planList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }
}
