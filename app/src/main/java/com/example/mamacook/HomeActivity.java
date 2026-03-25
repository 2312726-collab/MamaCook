package com.example.mamacook;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView tvGreeting, tvCategoryTitle;
    
    private RecyclerView rvCategory, rvFeatured, rvNew, rvHistory;
    private MonAnAdapter adapterCategory, adapterFeatured, adapterNew, adapterHistory;
    
    private List<MonAn> listCategory = new ArrayList<>();
    private List<MonAn> listFeatured = new ArrayList<>();
    private List<MonAn> listNew = new ArrayList<>();
    private List<MonAn> listHistory = new ArrayList<>();

    // Lưu trữ nút danh mục đang được chọn để đổi style
    private TextView currentSelectedCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        tvGreeting = findViewById(R.id.tv_greeting);
        tvCategoryTitle = findViewById(R.id.tv_category_display_title);

        setupRecyclerViews();
        setupCategoryButtons();
        
        loadFeaturedRecipes();
        loadNewRecipes();
        loadHistoryRecipes();
        
        // Mặc định chọn "Tất cả"
        currentSelectedCategory = findViewById(R.id.btn_cat_all);
        loadRecipesByCategory("all", "Gợi ý cho bạn");
    }

    private void setupRecyclerViews() {
        rvCategory = findViewById(R.id.rv_category_dishes);
        rvFeatured = findViewById(R.id.rv_featured);
        rvNew = findViewById(R.id.rv_new_recipes);
        rvHistory = findViewById(R.id.rv_history);

        rvCategory.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFeatured.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvNew.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvHistory.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        adapterCategory = new MonAnAdapter(listCategory);
        adapterFeatured = new MonAnAdapter(listFeatured);
        adapterNew = new MonAnAdapter(listNew);
        adapterHistory = new MonAnAdapter(listHistory);

        rvCategory.setAdapter(adapterCategory);
        rvFeatured.setAdapter(adapterFeatured);
        rvNew.setAdapter(adapterNew);
        rvHistory.setAdapter(adapterHistory);
    }

    private void loadFeaturedRecipes() {
        db.collection("mon_an")
                .orderBy("luot_xem", Query.Direction.DESCENDING)
                .limit(10)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    listFeatured.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        listFeatured.add(doc.toObject(MonAn.class));
                    }
                    adapterFeatured.notifyDataSetChanged();
                });
    }

    private void loadNewRecipes() {
        db.collection("mon_an")
                .orderBy("ngay_tao", Query.Direction.DESCENDING)
                .limit(5)
                .get().addOnSuccessListener(queryDocumentSnapshots -> {
                    listNew.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        listNew.add(doc.toObject(MonAn.class));
                    }
                    adapterNew.notifyDataSetChanged();
                });
    }

    private void loadRecipesByCategory(String categoryId, String title) {
        if (tvCategoryTitle != null) tvCategoryTitle.setText(title);
        
        Query query = (categoryId.equals("all")) ? db.collection("mon_an") : db.collection("mon_an").whereEqualTo("id_danh_muc", categoryId);
        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            listCategory.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                listCategory.add(doc.toObject(MonAn.class));
            }
            adapterCategory.notifyDataSetChanged();
            
            if (listCategory.isEmpty()) {
                // Toast.makeText(this, "Chưa có món ăn nào trong mục này", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadHistoryRecipes() {
        // Placeholder
    }

    private void setupCategoryButtons() {
        int[] btnIds = {R.id.btn_cat_all, R.id.btn_cat_man, R.id.btn_cat_canh, R.id.btn_cat_chay, R.id.btn_cat_vat, R.id.btn_cat_lau};
        String[] categoryIds = {"all", "mon_man", "mon_canh", "mon_chay", "an_vat", "mon_lau"};
        String[] titles = {"Gợi ý cho bạn", "Món mặn", "Món canh", "Món chay", "Ăn vặt", "Món lẩu"};

        for (int i = 0; i < btnIds.length; i++) {
            final int index = i;
            TextView btn = findViewById(btnIds[i]);
            if (btn != null) {
                btn.setOnClickListener(v -> {
                    // Đổi style cho nút cũ về bình thường
                    if (currentSelectedCategory != null) {
                        currentSelectedCategory.setBackgroundResource(R.drawable.bg_input_field);
                        currentSelectedCategory.setTextColor(android.graphics.Color.parseColor("#555555"));
                    }
                    
                    // Đổi style cho nút mới được chọn (Màu cam/đỏ)
                    btn.setBackgroundResource(R.drawable.bg_register_button);
                    btn.setTextColor(android.graphics.Color.WHITE);
                    currentSelectedCategory = btn;

                    // Load dữ liệu
                    loadRecipesByCategory(categoryIds[index], titles[index]);
                });
            }
        }
    }
}
