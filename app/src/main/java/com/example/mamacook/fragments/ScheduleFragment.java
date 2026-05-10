package com.example.mamacook.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.activities.ChuanBiNauActivity;
import com.example.mamacook.activities.GioHangActivity;
import com.example.mamacook.adapters.CookingPlanHorizontalAdapter;
import com.example.mamacook.models.CookingPlan;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ScheduleFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView tvTotalDishes, tvShoppingProgress;
    private ImageButton btnAiSuggest;
    private RecyclerView rvSang, rvTrua, rvToi;
    private CookingPlanHorizontalAdapter adapterSang, adapterTrua, adapterToi;
    private List<CookingPlan> listSang = new ArrayList<>();
    private List<CookingPlan> listTrua = new ArrayList<>();
    private List<CookingPlan> listToi = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvTotalDishes = view.findViewById(R.id.tv_total_dishes);
        tvShoppingProgress = view.findViewById(R.id.tv_shopping_progress);
        btnAiSuggest = view.findViewById(R.id.btn_ai_suggest);

        setupRecyclerViews(view);
        cleanupOldCookingPlans();
        loadCookingPlans();

        view.findViewById(R.id.btn_shopping_list_schedule).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), GioHangActivity.class));
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        view.findViewById(R.id.btn_ready_to_cook_schedule).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ChuanBiNauActivity.class));
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        btnAiSuggest.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Tính năng AI đang được phát triển!", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void setupRecyclerViews(View view) {
        rvSang = view.findViewById(R.id.rv_meal_sang);
        rvTrua = view.findViewById(R.id.rv_meal_trua);
        rvToi = view.findViewById(R.id.rv_meal_toi);

        rvSang.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvTrua.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvToi.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        adapterSang = new CookingPlanHorizontalAdapter(listSang);
        adapterTrua = new CookingPlanHorizontalAdapter(listTrua);
        adapterToi = new CookingPlanHorizontalAdapter(listToi);

        rvSang.setAdapter(adapterSang);
        rvTrua.setAdapter(adapterTrua);
        rvToi.setAdapter(adapterToi);
    }

    private void loadCookingPlans() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("ke_hoach_nau_an")
                .whereEqualTo("id_nguoi_dung", user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    listSang.clear();
                    listTrua.clear();
                    listToi.clear();
                    
                    int totalBought = 0;
                    int totalIngredients = 0;

                    for (DocumentSnapshot doc : value) {
                        CookingPlan plan = doc.toObject(CookingPlan.class);
                        if (plan == null) continue;
                        plan.setId_plan(doc.getId()); // Gán ID document để xóa

                        String buoi = plan.getBuoi();
                        if ("Sang".equals(buoi)) listSang.add(plan);
                        else if ("Trua".equals(buoi)) listTrua.add(plan);
                        else if ("Toi".equals(buoi)) listToi.add(plan);

                        if (plan.getDanh_sach_nguyen_lieu() != null) {
                            for (CookingPlan.IngredientItem item : plan.getDanh_sach_nguyen_lieu()) {
                                totalIngredients++;
                                if (item.isDa_mua()) totalBought++;
                            }
                        }
                    }

                    tvTotalDishes.setText(String.valueOf(value.size()));
                    tvShoppingProgress.setText(totalBought + "/" + totalIngredients);

                    adapterSang.notifyDataSetChanged();
                    adapterTrua.notifyDataSetChanged();
                    adapterToi.notifyDataSetChanged();
                });
    }

    private void cleanupOldCookingPlans() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        long oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        Timestamp threshold = new Timestamp(new Date(oneDayAgo));

        db.collection("ke_hoach_nau_an")
                .whereEqualTo("id_nguoi_dung", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    boolean hasSomethingToDelete = false;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Timestamp time = doc.getTimestamp("ngay_lap_ke_hoach");
                        if (time != null && time.compareTo(threshold) < 0) {
                            batch.delete(doc.getReference());
                            hasSomethingToDelete = true;
                        }
                    }
                    if (hasSomethingToDelete) {
                        batch.commit();
                        Log.d("Cleanup", "Đã xóa các kế hoạch cũ hơn 24 giờ");
                    }
                });
    }
}
