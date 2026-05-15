package com.example.mamacook.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.adapters.MonAnAdapter;
import com.example.mamacook.models.MonAn;
import com.example.mamacook.utils.VNCharacterUtils;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String tenNguoiDung = "";
    private boolean isRedirecting = false;

    private TextView tvGreeting, tvCategoryTitle;
    private EditText etSearch;
    private ImageButton btnFilter;
    private FrameLayout layoutThongBao;
    private LinearLayout layoutFilters;
    private Spinner spnDifficulty, spnTime, spnRating;
    private ImageButton btnChatFloat;
    private ImageButton btnAdminMenu;

    private RecyclerView rvCategory, rvFeatured, rvNew, rvHistory, rvWeeklyAttention;
    private MonAnAdapter adapterCategory, adapterFeatured, adapterNew, adapterHistory, adapterWeeklyAttention;

    private List<MonAn> listCategory = new ArrayList<>();
    private List<MonAn> listFeatured = new ArrayList<>();
    private List<MonAn> listNew = new ArrayList<>();
    private List<MonAn> listHistory = new ArrayList<>();
    private List<MonAn> listWeeklyAttention = new ArrayList<>();

    private TextView currentSelectedCategory;
    private List<MonAn> listFullCurrentCategory = new ArrayList<>();

    private String lastCategoryId = "all";
    private String lastCategoryTitle = "Gợi ý cho bạn";

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

        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        kiemTraTaiKhoanBiKhoa();

        tvGreeting = findViewById(R.id.tv_greeting);
        tvCategoryTitle = findViewById(R.id.tv_category_display_title);
        etSearch = findViewById(R.id.et_search);
        btnFilter = findViewById(R.id.btn_filter);
        layoutFilters = findViewById(R.id.layout_filters);

        spnDifficulty = findViewById(R.id.spn_difficulty_main);
        spnTime = findViewById(R.id.spn_time_main);
        spnRating = findViewById(R.id.spn_rating_main);
        btnAdminMenu = findViewById(R.id.btn_admin_menu);
        layoutThongBao = findViewById(R.id.layout_thong_bao);
        btnChatFloat = findViewById(R.id.btn_chat_float);

        layoutThongBao.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ThongBaoActivity.class);
            startActivity(intent);
        });

        btnChatFloat.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ChatActivity.class);
            intent.putExtra("che_do", "user");
            intent.putExtra("ten_user", tenNguoiDung);
            startActivity(intent);
        });

        checkAdminRole();
        displayUserProfile();
        setupRecyclerViews();
        setupCategoryButtons();
        setupSearchAction();
        setupSpinners();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_favorites) {
                startActivity(new Intent(HomeActivity.this, FavoriteActivity.class));
                return true;
            }

            return false;
        });

        btnFilter.setOnClickListener(v -> toggleFilterLayout());

        loadFeaturedRecipes();
        loadNewRecipes();
        loadHistoryRecipes(11);
        loadWeeklyAttentionRecipes();

        currentSelectedCategory = findViewById(R.id.btn_cat_all);
        loadRecipesByCategory("all", "Gợi ý cho bạn");
    }

    private void kiemTraTaiKhoanBiKhoa() {
        if (isRedirecting) return;

        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            chuyenVeDangNhap();
            return;
        }

        db.collection("nguoi_dung")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, "Không tìm thấy thông tin tài khoản", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        chuyenVeDangNhap();
                        return;
                    }

                    String trangThai = documentSnapshot.getString("trang_thai_tai_khoan");

                    if (trangThai != null && trangThai.trim().equalsIgnoreCase("bi_khoa")) {
                        Toast.makeText(this, "Tài khoản của bạn đã bị khóa", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        chuyenVeDangNhap();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi kiểm tra trạng thái tài khoản", Toast.LENGTH_SHORT).show();
                });
    }

    private void chuyenVeDangNhap() {
        if (isRedirecting) return;

        isRedirecting = true;

        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupSearchAction() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim().toLowerCase(Locale.getDefault());

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);

                if (!query.isEmpty()) {
                    performSearch(query);
                } else {
                    loadRecipesByCategory(lastCategoryId, lastCategoryTitle);
                }

                return true;
            }

            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    loadRecipesByCategory(lastCategoryId, lastCategoryTitle);
                }
            }
        });
    }

    private void checkAdminRole() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance()
                .collection("nguoi_dung")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");

                        if ("admin".equals(role)) {
                            btnAdminMenu.setVisibility(View.VISIBLE);
                            setupAdminMenu();
                        } else {
                            btnAdminMenu.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void setupAdminMenu() {
        btnAdminMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(HomeActivity.this, btnAdminMenu);
            popupMenu.getMenuInflater().inflate(R.menu.menu_admin_popup, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_admin_dashboard) {
                    startActivity(new Intent(this, AdminActivity.class));
                    return true;
                }

                if (item.getItemId() == R.id.menu_admin_stats) {
                    startActivity(new Intent(this, ThongKeAdminActivity.class));
                    return true;
                }

                if (item.getItemId() == R.id.menu_admin_users) {
                    startActivity(new Intent(this, QuanLyTaiKhoanActivity.class));
                    return true;
                }

                if (item.getItemId() == R.id.menu_admin_reviews) {
                    startActivity(new Intent(this, QuanLyDanhGiaActivity.class));
                    return true;
                }

                return false;
            });

            popupMenu.show();
        });
    }

    private void setupCategoryButtons() {
        int[] btnIds = {
                R.id.btn_cat_all,
                R.id.btn_cat_man,
                R.id.btn_cat_canh,
                R.id.btn_cat_chay,
                R.id.btn_cat_vat,
                R.id.btn_cat_lau
        };

        String[] categoryIds = {
                "all",
                "mon_man",
                "mon_canh",
                "mon_chay",
                "an_vat",
                "mon_lau"
        };

        String[] titles = {
                "Gợi ý cho bạn",
                "Món mặn",
                "Món canh",
                "Món chay",
                "Ăn vặt",
                "Món lẩu"
        };

        for (int i = 0; i < btnIds.length; i++) {
            final int index = i;
            TextView btn = findViewById(btnIds[i]);

            if (btn != null) {
                btn.setOnClickListener(v -> {
                    lastCategoryId = categoryIds[index];
                    lastCategoryTitle = titles[index];

                    if (currentSelectedCategory != null) {
                        currentSelectedCategory.setBackgroundResource(R.drawable.bg_input_field);
                        currentSelectedCategory.setTextColor(Color.parseColor("#555555"));
                    }

                    btn.setBackgroundResource(R.drawable.bg_register_button);
                    btn.setTextColor(Color.WHITE);
                    currentSelectedCategory = btn;

                    etSearch.setText("");
                    loadRecipesByCategory(lastCategoryId, lastCategoryTitle);
                });
            }
        }
    }

    private void toggleFilterLayout() {
        if (layoutFilters.getVisibility() == View.GONE) {
            layoutFilters.setVisibility(View.VISIBLE);
            layoutFilters.setAlpha(0f);
            layoutFilters.animate().alpha(1f).setDuration(300).start();
        } else {
            layoutFilters.setVisibility(View.GONE);
            resetFilters();
        }
    }

    private void resetFilters() {
        spnDifficulty.setSelection(0);
        spnTime.setSelection(0);
        spnRating.setSelection(0);
        applyFilters();
    }

    private void setupSpinners() {
        String[] difficulties = {"Tất cả", "Dễ", "Trung bình", "Khó"};
        String[] times = {"Tất cả", "Dưới 15'", "15-30'", "30-60'", "Trên 60'"};
        String[] ratings = {"Tất cả", "4★ trở lên", "3★ trở lên", "2★ trở lên"};

        spnDifficulty.setAdapter(new LabelSpinnerAdapter(this, "Độ khó", difficulties));
        spnTime.setAdapter(new LabelSpinnerAdapter(this, "Thời gian", times));
        spnRating.setAdapter(new LabelSpinnerAdapter(this, "Đánh giá", ratings));

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spnDifficulty.setOnItemSelectedListener(filterListener);
        spnTime.setOnItemSelectedListener(filterListener);
        spnRating.setOnItemSelectedListener(filterListener);
    }

    private static class LabelSpinnerAdapter extends ArrayAdapter<String> {
        private final String label;

        public LabelSpinnerAdapter(Context context, String label, String[] items) {
            super(context, R.layout.spinner_item_selected, items);
            this.label = label;
            setDropDownViewResource(R.layout.spinner_dropdown_item);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            String item = getItem(position);

            if (item != null && item.equals("Tất cả")) {
                view.setText(label);
            } else {
                view.setText(item);
            }

            return view;
        }
    }

    private void performSearch(String searchText) {
        tvCategoryTitle.setText("Kết quả cho: '" + searchText + "'");

        String searchTextNoTone = VNCharacterUtils.removeAccents(searchText);
        String[] searchWordsNoTone = searchTextNoTone.split("\\s+");

        db.collection("mon_an")
                .whereArrayContains("tu_khoa_tim_kiem", searchWordsNoTone[0])
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<MonAn> results = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MonAn mon = doc.toObject(MonAn.class);
                        mon.setId_mon_an(doc.getId());

                        List<String> dbKeywords = mon.getTu_khoa_tim_kiem();
                        if (dbKeywords == null) continue;

                        boolean matchAll = true;

                        for (String wordNoTone : searchWordsNoTone) {
                            if (!dbKeywords.contains(wordNoTone)) {
                                matchAll = false;
                                break;
                            }
                        }

                        if (matchAll) results.add(mon);
                    }

                    Collections.sort(results, (m1, m2) ->
                            Integer.compare(
                                    calculateRelevance(m2, searchText, searchTextNoTone),
                                    calculateRelevance(m1, searchText, searchTextNoTone)
                            )
                    );

                    listFullCurrentCategory = new ArrayList<>(results);
                    applyFilters();
                });
    }

    private void applyFilters() {
        if (spnDifficulty.getSelectedItem() == null) return;

        String difficulty = spnDifficulty.getSelectedItem().toString();
        String timeRange = spnTime.getSelectedItem().toString();
        String ratingRange = spnRating.getSelectedItem().toString();

        List<MonAn> filteredList = new ArrayList<>();

        for (MonAn mon : listFullCurrentCategory) {
            boolean matchDifficulty = difficulty.equals("Tất cả") ||
                    (mon.getDo_kho() != null && mon.getDo_kho().equals(difficulty));

            boolean matchTime = false;
            int time = mon.getThoi_gian_nau();

            if (timeRange.equals("Tất cả")) matchTime = true;
            else if (timeRange.equals("Dưới 15'") && time < 15) matchTime = true;
            else if (timeRange.equals("15-30'") && time >= 15 && time <= 30) matchTime = true;
            else if (timeRange.equals("30-60'") && time > 30 && time <= 60) matchTime = true;
            else if (timeRange.equals("Trên 60'") && time > 60) matchTime = true;

            boolean matchRating = false;
            double rating = mon.getRating();

            if (ratingRange.equals("Tất cả")) matchRating = true;
            else if (ratingRange.equals("4★ trở lên") && rating >= 4.0) matchRating = true;
            else if (ratingRange.equals("3★ trở lên") && rating >= 3.0) matchRating = true;
            else if (ratingRange.equals("2★ trở lên") && rating >= 2.0) matchRating = true;

            if (matchDifficulty && matchTime && matchRating) {
                filteredList.add(mon);
            }
        }

        listCategory.clear();
        listCategory.addAll(filteredList);

        adapterCategory.setSectionInfo("DANH_MUC", lastCategoryId, difficulty, timeRange, ratingRange);
        adapterCategory.notifyDataSetChanged();
    }

    private int calculateRelevance(MonAn mon, String query, String queryNoTone) {
        int score = 0;

        String name = mon.getTen_mon().toLowerCase();
        String nameNoTone = VNCharacterUtils.removeAccents(name);

        if (name.equals(query)) score += 1000;
        else if (nameNoTone.equals(queryNoTone)) score += 900;

        if (name.startsWith(query)) score += 500;
        else if (nameNoTone.startsWith(queryNoTone)) score += 450;

        if (name.contains(query)) score += 200;
        else if (nameNoTone.contains(queryNoTone)) score += 180;

        return score;
    }

    private void displayUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            db.collection("nguoi_dung")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("ho_ten");

                            if (name != null && !name.isEmpty()) {
                                tenNguoiDung = name;
                                tvGreeting.setText("Xin chào " + name + "!");
                            }
                        }
                    });
        }
    }

    private void setupRecyclerViews() {
        rvCategory = findViewById(R.id.rv_category_dishes);
        rvFeatured = findViewById(R.id.rv_featured);
        rvNew = findViewById(R.id.rv_new_recipes);
        rvHistory = findViewById(R.id.rv_history);
        rvWeeklyAttention = findViewById(R.id.rv_weekly_attention);

        rvCategory.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFeatured.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvNew.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvHistory.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvWeeklyAttention.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        adapterCategory = new MonAnAdapter(listCategory);

        adapterFeatured = new MonAnAdapter(listFeatured);
        adapterFeatured.setSectionInfo("NOI_BAT", "");

        adapterNew = new MonAnAdapter(listNew);
        adapterNew.setSectionInfo("MOI", "");

        adapterHistory = new MonAnAdapter(listHistory);
        adapterHistory.setSectionInfo("LICH_SU", "");

        adapterWeeklyAttention = new MonAnAdapter(listWeeklyAttention);
        adapterWeeklyAttention.setSectionInfo("DUOC_DE_Y_TUAN", "");

        rvCategory.setAdapter(adapterCategory);
        rvFeatured.setAdapter(adapterFeatured);
        rvNew.setAdapter(adapterNew);
        rvHistory.setAdapter(adapterHistory);
        rvWeeklyAttention.setAdapter(adapterWeeklyAttention);
    }

    private void loadFeaturedRecipes() {
        db.collection("mon_an")
                .orderBy("luot_xem", Query.Direction.DESCENDING)
                .limit(11)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listFeatured.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MonAn mon = doc.toObject(MonAn.class);
                        mon.setId_mon_an(doc.getId());
                        listFeatured.add(mon);
                    }

                    adapterFeatured.notifyDataSetChanged();
                });
    }

    private void loadNewRecipes() {
        db.collection("mon_an")
                .orderBy("ngay_tao", Query.Direction.DESCENDING)
                .limit(11)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listNew.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MonAn mon = doc.toObject(MonAn.class);
                        mon.setId_mon_an(doc.getId());
                        listNew.add(mon);
                    }

                    adapterNew.notifyDataSetChanged();
                });
    }

    private void loadRecipesByCategory(String categoryId, String title) {
        if (tvCategoryTitle != null) tvCategoryTitle.setText(title);

        Query query = categoryId.equals("all")
                ? db.collection("mon_an")
                : db.collection("mon_an").whereEqualTo("id_danh_muc", categoryId);

        query.limit(100)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listFullCurrentCategory.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MonAn mon = doc.toObject(MonAn.class);
                        mon.setId_mon_an(doc.getId());
                        listFullCurrentCategory.add(mon);
                    }

                    applyFilters();
                });
    }

    private void loadHistoryRecipes(int limit) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("lich_su_xem")
                .whereEqualTo("id_nguoi_dung", uid)
                .orderBy("thoi_gian_xem", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> dishIds = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        dishIds.add(doc.getString("id_mon_an"));
                    }

                    if (dishIds.isEmpty()) {
                        listHistory.clear();
                        adapterHistory.notifyDataSetChanged();
                        return;
                    }

                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

                    for (String id : dishIds) {
                        if (id != null) {
                            tasks.add(db.collection("mon_an").document(id).get());
                        }
                    }

                    Tasks.whenAllSuccess(tasks)
                            .addOnSuccessListener(results -> {
                                listHistory.clear();

                                for (Object result : results) {
                                    DocumentSnapshot monDoc = (DocumentSnapshot) result;

                                    if (monDoc.exists()) {
                                        MonAn mon = monDoc.toObject(MonAn.class);

                                        if (mon != null) {
                                            mon.setId_mon_an(monDoc.getId());
                                            listHistory.add(mon);
                                        }
                                    }
                                }

                                sortHistoryList(dishIds);
                                adapterHistory.notifyDataSetChanged();
                            });
                });
    }

    private void loadFeaturedRecipesFallbackForWeekly() {
        db.collection("mon_an")
                .orderBy("luot_xem", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(query -> {
                    listWeeklyAttention.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        MonAn mon = doc.toObject(MonAn.class);
                        mon.setId_mon_an(doc.getId());

                        if (mon.getTrang_thai() == null || "hien_thi".equals(mon.getTrang_thai())) {
                            listWeeklyAttention.add(mon);
                        }
                    }

                    adapterWeeklyAttention.notifyDataSetChanged();
                });
    }

    private void sortHistoryList(List<String> orderIds) {
        List<MonAn> sortedList = new ArrayList<>();

        for (String id : orderIds) {
            for (MonAn mon : listHistory) {
                if (mon.getId_mon_an() != null && mon.getId_mon_an().equals(id)) {
                    sortedList.add(mon);
                    break;
                }
            }
        }

        listHistory.clear();
        listHistory.addAll(sortedList);
    }

    private void sortWeekly(List<String> orderIds) {
        List<MonAn> sorted = new ArrayList<>();

        for (String id : orderIds) {
            for (MonAn mon : listWeeklyAttention) {
                if (mon.getId_mon_an() != null && mon.getId_mon_an().equals(id)) {
                    sorted.add(mon);
                    break;
                }
            }
        }

        listWeeklyAttention.clear();
        listWeeklyAttention.addAll(sorted);
    }

    private void loadWeeklyAttentionRecipes() {
        long now = System.currentTimeMillis();
        long sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000;

        Timestamp mocTuan = new Timestamp(new java.util.Date(sevenDaysAgo));

        db.collection("lich_su_xem")
                .whereGreaterThanOrEqualTo("thoi_gian_xem", mocTuan)
                .get()
                .addOnSuccessListener(lichSuSnapshot -> {
                    java.util.Map<String, Integer> diemMon = new java.util.HashMap<>();

                    for (DocumentSnapshot doc : lichSuSnapshot.getDocuments()) {
                        String idMon = doc.getString("id_mon_an");

                        if (idMon != null) {
                            int diem = diemMon.containsKey(idMon) ? diemMon.get(idMon) : 0;
                            diemMon.put(idMon, diem + 1);
                        }
                    }

                    db.collection("danh_gia")
                            .whereGreaterThanOrEqualTo("ngay_danh_gia", mocTuan)
                            .whereEqualTo("trang_thai", "hien_thi")
                            .get()
                            .addOnSuccessListener(danhGiaSnapshot -> {
                                for (DocumentSnapshot doc : danhGiaSnapshot.getDocuments()) {
                                    String idMon = doc.getString("id_mon_an");

                                    if (idMon != null) {
                                        int diem = diemMon.containsKey(idMon) ? diemMon.get(idMon) : 0;
                                        diemMon.put(idMon, diem + 3);
                                    }
                                }

                                if (diemMon.isEmpty()) {
                                    loadFeaturedRecipesFallbackForWeekly();
                                    return;
                                }

                                List<java.util.Map.Entry<String, Integer>> sorted =
                                        new ArrayList<>(diemMon.entrySet());

                                Collections.sort(sorted, (a, b) -> b.getValue() - a.getValue());

                                List<String> topIds = new ArrayList<>();
                                int limit = Math.min(10, sorted.size());

                                for (int i = 0; i < limit; i++) {
                                    topIds.add(sorted.get(i).getKey());
                                }

                                List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

                                for (String id : topIds) {
                                    tasks.add(db.collection("mon_an").document(id).get());
                                }

                                Tasks.whenAllSuccess(tasks)
                                        .addOnSuccessListener(results -> {
                                            listWeeklyAttention.clear();

                                            for (Object result : results) {
                                                DocumentSnapshot doc = (DocumentSnapshot) result;

                                                if (doc.exists()) {
                                                    MonAn mon = doc.toObject(MonAn.class);

                                                    if (mon != null) {
                                                        mon.setId_mon_an(doc.getId());

                                                        if (mon.getTrang_thai() == null || "hien_thi".equals(mon.getTrang_thai())) {
                                                            listWeeklyAttention.add(mon);
                                                        }
                                                    }
                                                }
                                            }

                                            sortWeekly(topIds);
                                            adapterWeeklyAttention.notifyDataSetChanged();
                                        });
                            });
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        kiemTraTaiKhoanBiKhoa();
        loadHistoryRecipes(11);
    }
}