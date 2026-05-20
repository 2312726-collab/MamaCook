package com.example.mamacook.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.adapters.MonAnVerticalAdapter;
import com.example.mamacook.models.MonAn;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FilterFragment extends Fragment {

    private ImageButton btnBack, btnOpenFilter;
    private TextView tvFilterStatus, tvCount;
    private RecyclerView rvResults;
    private ProgressBar pbLoading;
    private LinearLayout layoutEmpty;

    private MonAnVerticalAdapter adapter;
    private final List<MonAn> allRecipes = new ArrayList<>();
    private final List<MonAn> displayList = new ArrayList<>();
    private FirebaseFirestore db;

    private int ratingRange = 0; // 0: All, 4: 4-5, 3: 3-4, 2: 2-3, 1: 1-2
    private int timeRange = 0; // 0: All, 1: <15, 2: 15-30, 3: 30-60, 4: >60
    private String currentDifficulty = "Tất cả";

    private String preSelectedDate, preSelectedMeal;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter_all, container, false);
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            preSelectedDate = getArguments().getString("PRE_SELECTED_DATE");
            preSelectedMeal = getArguments().getString("PRE_SELECTED_MEAL");
        }

        bindViews(view);
        setupRecyclerView();
        setupListeners();
        loadAllData();

        return view;
    }

    private void bindViews(View v) {
        btnBack = v.findViewById(R.id.btn_back_filter);
        btnOpenFilter = v.findViewById(R.id.btn_open_bottom_sheet);
        tvFilterStatus = v.findViewById(R.id.tv_filter_status);
        tvCount = v.findViewById(R.id.tv_count);
        rvResults = v.findViewById(R.id.rv_filter_results);
        pbLoading = v.findViewById(R.id.pb_filter_loading);
        layoutEmpty = v.findViewById(R.id.layout_filter_empty);
    }

    private void setupRecyclerView() {
        adapter = new MonAnVerticalAdapter(displayList);
        adapter.setPreSelectedData(preSelectedDate, preSelectedMeal);
        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvResults.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnOpenFilter.setOnClickListener(v -> showFilterBottomSheet());
    }

    private void loadAllData() {
        pbLoading.setVisibility(View.VISIBLE);
        rvResults.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        db.collection("mon_an").get().addOnSuccessListener(snap -> {
            allRecipes.clear();
            for (QueryDocumentSnapshot doc : snap) {
                MonAn m = doc.toObject(MonAn.class);
                if (m.getId_mon_an() == null) m.setId_mon_an(doc.getId());
                allRecipes.add(m);
            }
            applyFilter();
            pbLoading.setVisibility(View.GONE);
        });
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_filter_recipes, (ViewGroup) getView(), false);
        dialog.setContentView(view);

        ChipGroup cgRating = view.findViewById(R.id.cg_rating);
        ChipGroup cgTime = view.findViewById(R.id.cg_time);
        ChipGroup cgDifficulty = view.findViewById(R.id.cg_difficulty);
        Button btnApply = view.findViewById(R.id.btn_apply_filter);

        // Khởi tạo giá trị hiện tại
        if (ratingRange == 4) cgRating.check(R.id.chip_rate_4);
        else if (ratingRange == 3) cgRating.check(R.id.chip_rate_3);
        else if (ratingRange == 2) cgRating.check(R.id.chip_rate_2);
        else if (ratingRange == 1) cgRating.check(R.id.chip_rate_1);
        else cgRating.check(R.id.chip_rate_all);

        if (timeRange == 1) cgTime.check(R.id.chip_time_15);
        else if (timeRange == 2) cgTime.check(R.id.chip_time_30);
        else if (timeRange == 3) cgTime.check(R.id.chip_time_60);
        else if (timeRange == 4) cgTime.check(R.id.chip_time_above_60);
        else cgTime.check(R.id.chip_time_all);

        if (Objects.equals(currentDifficulty, "Dễ")) cgDifficulty.check(R.id.chip_diff_easy);
        else if (Objects.equals(currentDifficulty, "Trung bình")) cgDifficulty.check(R.id.chip_diff_medium);
        else if (Objects.equals(currentDifficulty, "Khó")) cgDifficulty.check(R.id.chip_diff_hard);
        else cgDifficulty.check(R.id.chip_diff_all);

        btnApply.setOnClickListener(v -> {
            // Lấy giá trị Rating
            int rateId = cgRating.getCheckedChipId();
            if (rateId == R.id.chip_rate_4) ratingRange = 4;
            else if (rateId == R.id.chip_rate_3) ratingRange = 3;
            else if (rateId == R.id.chip_rate_2) ratingRange = 2;
            else if (rateId == R.id.chip_rate_1) ratingRange = 1;
            else ratingRange = 0;

            // Lấy giá trị Thời gian
            int timeId = cgTime.getCheckedChipId();
            if (timeId == R.id.chip_time_15) timeRange = 1;
            else if (timeId == R.id.chip_time_30) timeRange = 2;
            else if (timeId == R.id.chip_time_60) timeRange = 3;
            else if (timeId == R.id.chip_time_above_60) timeRange = 4;
            else timeRange = 0;

            // Lấy giá trị Độ khó
            int diffId = cgDifficulty.getCheckedChipId();
            if (diffId == R.id.chip_diff_easy) currentDifficulty = "Dễ";
            else if (diffId == R.id.chip_diff_medium) currentDifficulty = "Trung bình";
            else if (diffId == R.id.chip_diff_hard) currentDifficulty = "Khó";
            else currentDifficulty = "Tất cả";

            applyFilter();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void applyFilter() {
        displayList.clear();
        displayList.addAll(allRecipes);

        // 1. Lọc theo Rating
        if (ratingRange == 4) { // 4-5 sao
            displayList.removeIf(m -> m.getRating() < 4.0);
        } else if (ratingRange == 3) { // 3-4 sao
            displayList.removeIf(m -> m.getRating() < 3.0 || m.getRating() >= 4.0);
        } else if (ratingRange == 2) { // 2-3 sao
            displayList.removeIf(m -> m.getRating() < 2.0 || m.getRating() >= 3.0);
        } else if (ratingRange == 1) { // 1-2 sao
            displayList.removeIf(m -> m.getRating() < 1.0 || m.getRating() >= 2.0);
        }

        // 2. Lọc theo Thời gian nấu
        if (timeRange == 1) { // < 15p
            displayList.removeIf(m -> m.getThoi_gian_nau() >= 15);
        } else if (timeRange == 2) { // 15-30p
            displayList.removeIf(m -> m.getThoi_gian_nau() < 15 || m.getThoi_gian_nau() > 30);
        } else if (timeRange == 3) { // 30-60p
            displayList.removeIf(m -> m.getThoi_gian_nau() < 30 || m.getThoi_gian_nau() > 60);
        } else if (timeRange == 4) { // > 60p
            displayList.removeIf(m -> m.getThoi_gian_nau() <= 60);
        }

        // 3. Lọc theo Độ khó
        if (!Objects.equals(currentDifficulty, "Tất cả")) {
            displayList.removeIf(m -> m.getDo_kho() == null || !Objects.equals(m.getDo_kho(), currentDifficulty));
        }

        // 4. Mặc định sắp xếp theo mới nhất
        displayList.sort((a, b) -> {
            if (a.getNgay_tao() == null && b.getNgay_tao() == null) return 0;
            if (a.getNgay_tao() == null) return 1;
            if (b.getNgay_tao() == null) return -1;
            return b.getNgay_tao().compareTo(a.getNgay_tao());
        });

        // Cập nhật UI
        updateUI();
    }

    private void updateUI() {
        tvFilterStatus.setText("Kết quả lọc");
        tvCount.setText(getString(R.string.recipe_unit, displayList.size()));
        
        if (displayList.isEmpty()) {
            rvResults.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvResults.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }
}
