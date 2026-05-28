package com.example.mamacook.activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mamacook.R;
import com.example.mamacook.adapters.MonAnVerticalAdapter;
import com.example.mamacook.models.MonAn;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class SeeAllActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView rvSeeAll;
    private MonAnVerticalAdapter adapter;
    private List<MonAn> listMonAn = new ArrayList<>();
    private TextView tvTitle;
    private String sectionType, categoryId;
    
    // Thêm biến nhận bộ lọc
    private String filterDifficulty, filterTime, filterRating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_all);

        db = FirebaseFirestore.getInstance();
        rvSeeAll = findViewById(R.id.rv_see_all);
        tvTitle = findViewById(R.id.tv_see_all_title);
        Toolbar toolbar = findViewById(R.id.toolbar_see_all);
        
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        sectionType = getIntent().getStringExtra("SECTION_TYPE");
        categoryId = getIntent().getStringExtra("CATEGORY_ID");
        
        // Nhận bộ lọc từ Intent
        filterDifficulty = getIntent().getStringExtra("FILTER_DIFFICULTY");
        filterTime = getIntent().getStringExtra("FILTER_TIME");
        filterRating = getIntent().getStringExtra("FILTER_RATING");

        adapter = new MonAnVerticalAdapter(listMonAn);
        rvSeeAll.setLayoutManager(new LinearLayoutManager(this));
        rvSeeAll.setAdapter(adapter);

        loadData();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void loadData() {
        if ("DANH_MUC".equals(sectionType)) {
            loadByCategory();
        } else if ("MOI".equals(sectionType)) {
            loadNewRecipes();
        } else if ("LICH_SU".equals(sectionType)) {
            loadHistory();
        } else if ("NOI_BAT".equals(sectionType)) {
            loadFeatured();
        }
    }

    private void loadByCategory() {
        tvTitle.setText("Danh mục món ăn");
        Query query = categoryId.equals("all") ? db.collection("mon_an") : db.collection("mon_an").whereEqualTo("id_danh_muc", categoryId);
        
        // Gemini: Dùng SnapshotListener để cập nhật Rating realtime
        query.addSnapshotListener(this, (querySnapshot, error) -> {
            if (error != null || querySnapshot == null) return;
            
            listMonAn.clear();
            for (DocumentSnapshot doc : querySnapshot) {
                MonAn mon = doc.toObject(MonAn.class);
                if (mon != null) {
                    mon.setId_mon_an(doc.getId());
                    if (applyFilters(mon)) {
                        listMonAn.add(mon);
                    }
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    private boolean applyFilters(MonAn mon) {
        // Nếu không có thông tin bộ lọc (từ các mục khác), mặc định cho qua
        if (filterDifficulty == null) return true;

        // 1. Lọc Độ khó
        boolean matchDiff = filterDifficulty.equals("Tất cả") || (mon.getDo_kho() != null && mon.getDo_kho().equals(filterDifficulty));
        
        // 2. Lọc Thời gian
        boolean matchTime = false;
        int time = mon.getThoi_gian_nau();
        if (filterTime == null || filterTime.equals("Tất cả")) matchTime = true;
        else if (filterTime.equals("Dưới 15'") && time < 15) matchTime = true;
        else if (filterTime.equals("15-30'") && time >= 15 && time <= 30) matchTime = true;
        else if (filterTime.equals("30-60'") && time > 30 && time <= 60) matchTime = true;
        else if (filterTime.equals("Trên 60'") && time > 60) matchTime = true;

        // 3. Lọc Đánh giá
        boolean matchRate = false;
        double rate = mon.getRating();
        if (filterRating == null || filterRating.equals("Tất cả")) matchRate = true;
        else if (filterRating.equals("4★ trở lên") && rate >= 4.0) matchRate = true;
        else if (filterRating.equals("3★ trở lên") && rate >= 3.0) matchRate = true;
        else if (filterRating.equals("2★ trở lên") && rate >= 2.0) matchRate = true;

        return matchDiff && matchTime && matchRate;
    }

    private void loadNewRecipes() {
        tvTitle.setText("Công thức mới");
        db.collection("mon_an").orderBy("ngay_tao", Query.Direction.DESCENDING).limit(30)
                .addSnapshotListener(this, (querySnapshot, error) -> {
                    if (error != null || querySnapshot == null) return;
                    listMonAn.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        MonAn mon = doc.toObject(MonAn.class);
                        if (mon != null) {
                            mon.setId_mon_an(doc.getId());
                            listMonAn.add(mon);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void loadFeatured() {
        tvTitle.setText("Món ăn nổi bật");
        db.collection("mon_an").orderBy("luot_xem", Query.Direction.DESCENDING).limit(30)
                .addSnapshotListener(this, (querySnapshot, error) -> {
                    if (error != null || querySnapshot == null) return;
                    listMonAn.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        MonAn mon = doc.toObject(MonAn.class);
                        if (mon != null) {
                            mon.setId_mon_an(doc.getId());
                            listMonAn.add(mon);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void loadHistory() {
        tvTitle.setText("Lịch sử xem");
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("lich_su_xem")
                .whereEqualTo("id_nguoi_dung", uid)
                .orderBy("thoi_gian_xem", Query.Direction.DESCENDING)
                .limit(15)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> dishIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        String id = doc.getString("id_mon_an");
                        if (id != null && !dishIds.contains(id)) {
                            dishIds.add(id);
                        }
                    }
                    if (dishIds.isEmpty()) return;

                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                    for (String id : dishIds) {
                        tasks.add(db.collection("mon_an").document(id).get());
                    }

                    Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                        listMonAn.clear();
                        for (Object result : results) {
                            DocumentSnapshot monDoc = (DocumentSnapshot) result;
                            if (monDoc.exists()) {
                                MonAn mon = monDoc.toObject(MonAn.class);
                                mon.setId_mon_an(monDoc.getId());
                                listMonAn.add(mon);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    });
                });
    }
}
