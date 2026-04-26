package com.example.mamacook.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mamacook.R;
import com.example.mamacook.adapters.MonAnVerticalAdapter;
import com.example.mamacook.models.MonAn;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class FavoriteActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView rvFavorite;
    private MonAnVerticalAdapter adapterFavorite;
    private List<MonAn> listFavorite = new ArrayList<>();
    private TextView tvEmptyMessage;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
            window.setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_favorite);

        db = FirebaseFirestore.getInstance();
        rvFavorite = findViewById(R.id.rv_favorite);
        tvEmptyMessage = findViewById(R.id.tv_empty_favorite);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        rvFavorite.setLayoutManager(new LinearLayoutManager(this));
        adapterFavorite = new MonAnVerticalAdapter(listFavorite);
        rvFavorite.setAdapter(adapterFavorite);

        setupBottomNavigation();
        loadFavoriteRecipes();
    }

    private void loadFavoriteRecipes() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("mon_da_luu")
                .whereEqualTo("id_nguoi_dung", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value == null || value.isEmpty()) {
                        listFavorite.clear();
                        adapterFavorite.notifyDataSetChanged();
                        tvEmptyMessage.setVisibility(View.VISIBLE);
                        rvFavorite.setVisibility(View.GONE);
                        return;
                    }

                    List<String> listIdFavorite = new ArrayList<>();
                    for (DocumentSnapshot doc : value) {
                        String idMonAn = doc.getString("id_mon_an");
                        if (idMonAn != null) listIdFavorite.add(idMonAn);
                    }

                    tvEmptyMessage.setVisibility(View.GONE);
                    rvFavorite.setVisibility(View.VISIBLE);

                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                    for (String id : listIdFavorite) {
                        tasks.add(db.collection("mon_an").document(id).get());
                    }

                    Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                        listFavorite.clear();
                        for (Object result : results) {
                            DocumentSnapshot doc = (DocumentSnapshot) result;
                            if (doc.exists()) {
                                MonAn mon = doc.toObject(MonAn.class);
                                if (mon != null) {
                                    mon.setId_mon_an(doc.getId());
                                    listFavorite.add(mon);
                                }
                            }
                        }
                        adapterFavorite.notifyDataSetChanged();
                    });
                });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_favorites);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_favorites) {
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }
}
