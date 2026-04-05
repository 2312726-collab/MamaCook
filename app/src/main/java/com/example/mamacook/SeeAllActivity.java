package com.example.mamacook;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class SeeAllActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView rvSeeAll;
    private MonAnVerticalAdapter adapter;
    private List<MonAn> listMonAn = new ArrayList<>();
    private TextView tvTitle;
    private String sectionType, categoryId;

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
        toolbar.setNavigationOnClickListener(v -> finish());

        sectionType = getIntent().getStringExtra("SECTION_TYPE");
        categoryId = getIntent().getStringExtra("CATEGORY_ID");

        adapter = new MonAnVerticalAdapter(listMonAn);
        rvSeeAll.setLayoutManager(new LinearLayoutManager(this));
        rvSeeAll.setAdapter(adapter);

        loadData();
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
        query.get().addOnSuccessListener(querySnapshot -> {
            listMonAn.clear();
            for (DocumentSnapshot doc : querySnapshot) {
                MonAn mon = doc.toObject(MonAn.class);
                mon.setId_mon_an(doc.getId());
                listMonAn.add(mon);
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void loadNewRecipes() {
        tvTitle.setText("Công thức mới");
        db.collection("mon_an").orderBy("ngay_tao", Query.Direction.DESCENDING).limit(20).get()
                .addOnSuccessListener(querySnapshot -> {
                    listMonAn.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        MonAn mon = doc.toObject(MonAn.class);
                        mon.setId_mon_an(doc.getId());
                        listMonAn.add(mon);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void loadFeatured() {
        tvTitle.setText("Món ăn nổi bật");
        db.collection("mon_an").orderBy("luot_xem", Query.Direction.DESCENDING).limit(20).get()
                .addOnSuccessListener(querySnapshot -> {
                    listMonAn.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        MonAn mon = doc.toObject(MonAn.class);
                        mon.setId_mon_an(doc.getId());
                        listMonAn.add(mon);
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
                        dishIds.add(doc.getString("id_mon_an"));
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
