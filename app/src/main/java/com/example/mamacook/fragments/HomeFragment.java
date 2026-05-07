package com.example.mamacook.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.activities.AdminActivity;
import com.example.mamacook.activities.QuanLyDanhGiaActivity;
import com.example.mamacook.activities.QuanLyTaiKhoanActivity;
import com.example.mamacook.activities.ThongKeAdminActivity;
import com.example.mamacook.adapters.MonAnAdapter;
import com.example.mamacook.models.MonAn;
import com.example.mamacook.utils.VNCharacterUtils;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final long AI_TIMEOUT_MS = 15_000L;

    private enum AiState { IDLE, WAITING, RUNNING, DONE }
    private AiState aiState = AiState.IDLE;

    private String aiWeather  = null;
    private String aiLocation = null;

    private boolean weatherReady = false;
    private boolean prefsReady   = false;
    private boolean recipesReady = false;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;

    private TextView    tvGreeting, tvAiInsights;
    private EditText    etSearch;
    private ImageButton btnFilter, btnAdminMenu;
    private FrameLayout layoutThongBao;
    private ProgressBar pbAiLoading;
    private RecyclerView rvCategory, rvFeatured, rvNew, rvHistory, rvWeeklyAttention;

    private MonAnAdapter adapterCategory, adapterFeatured, adapterNew, adapterHistory, adapterWeeklyAttention;
    private final List<MonAn> listWeeklyAttention = new ArrayList<>();

    private ListenerRegistration categoryListener, featuredListener, newRecipesListener, historyListener;

    private final List<MonAn>  listCategory            = new ArrayList<>();
    private final List<MonAn>  listFeatured            = new ArrayList<>();
    private final List<MonAn>  listNew                 = new ArrayList<>();
    private final List<MonAn>  listHistory             = new ArrayList<>();
    private final List<MonAn>  listFullCurrentCategory = new ArrayList<>();
    private final List<String> userPrefs               = new ArrayList<>();

    private TextView currentSelectedCategory;
    private String lastCategoryId    = "all";
    private String lastCategoryTitle = "Gợi ý cho bạn";

    private final Handler  mainHandler     = new Handler(Looper.getMainLooper());
    private       Runnable aiTimeoutAction = null;
    private final Executor bgExecutor      = Executors.newCachedThreadPool();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db                  = FirebaseFirestore.getInstance();
        mAuth               = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        bindViews(view);
        setupRecyclerViews(view);
        setupCategoryButtons(view);
        setupSearchAction();
        
        // SỬA: Khi nhấn nút Lọc, mở màn hình kết quả chuyên biệt như hình mẫu cam đào
        btnFilter.setOnClickListener(v -> openFilterFragment());

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded()) return;
            loadUserProfile();
            loadRecipesByCategory("all", lastCategoryTitle);
            fetchLocationAndWeather();

            loadFeaturedRecipes();
            loadNewRecipes();
            loadHistoryRecipes();
            loadWeeklyAttentionRecipes();
            checkAdminRole();
        }, 300);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadHistoryRecipes();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelAiTimeout();
        if (categoryListener    != null) categoryListener.remove();
        if (featuredListener    != null) featuredListener.remove();
        if (newRecipesListener  != null) newRecipesListener.remove();
        if (historyListener     != null) historyListener.remove();
    }

    private void bindViews(View v) {
        tvGreeting    = v.findViewById(R.id.tv_greeting);
        tvAiInsights  = v.findViewById(R.id.tvAiInsights);
        etSearch      = v.findViewById(R.id.et_search);
        btnFilter     = v.findViewById(R.id.btn_filter);
        btnAdminMenu  = v.findViewById(R.id.btn_admin_menu);
        layoutThongBao = v.findViewById(R.id.layout_thong_bao);
        pbAiLoading   = v.findViewById(R.id.pb_ai_loading);
        currentSelectedCategory = v.findViewById(R.id.btn_cat_all);

        if (layoutThongBao != null) {
            layoutThongBao.setOnClickListener(view -> {
                android.content.Intent intent = new android.content.Intent(getActivity(), com.example.mamacook.activities.ThongBaoActivity.class);
                startActivity(intent);
            });
        }
    }

    private MonAn parseMonAn(DocumentSnapshot doc) {
        MonAn m = doc.toObject(MonAn.class);
        if (m == null) return null;
        if (m.getId_mon_an() == null || m.getId_mon_an().isEmpty()) {
            m.setId_mon_an(doc.getId());
        }
        return m;
    }

    private void checkAndTriggerAi() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(this::checkAndTriggerAi);
            return;
        }
        if (!isAdded()) return;
        if (!lastCategoryId.equals("all")) return;
        if (aiState == AiState.RUNNING || aiState == AiState.DONE) return;
        if (!weatherReady || !prefsReady || !recipesReady) {
            aiState = AiState.WAITING;
            return;
        }

        aiState = AiState.RUNNING;
        showAiLoading(true);
        scheduleAiTimeout();

        String weather  = aiWeather  != null ? aiWeather  : getWeatherByHour();
        String location = aiLocation != null ? aiLocation : "Viet Nam";
        callGemini(new ArrayList<>(listFullCurrentCategory), weather, location);
    }

    private void resetAi() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(this::resetAi);
            return;
        }
        if (aiState == AiState.RUNNING) return;
        cancelAiTimeout();
        aiState = AiState.IDLE;
    }

    private void fetchLocationAndWeather() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            aiWeather    = getWeatherByHour();
            aiLocation   = "Viet Nam";
            weatherReady = true;
            updateAiInsightsTitle(null);
            checkAndTriggerAi();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc == null) {
                aiWeather    = getWeatherByHour();
                aiLocation   = "Viet Nam";
                weatherReady = true;
                updateAiInsightsTitle(null);
                checkAndTriggerAi();
                return;
            }
            bgExecutor.execute(() -> runGeocodeAndWeather(loc.getLatitude(), loc.getLongitude()));
        }).addOnFailureListener(e -> {
            aiWeather    = getWeatherByHour();
            aiLocation   = "Viet Nam";
            weatherReady = true;
            checkAndTriggerAi();
        });
    }

    private void runGeocodeAndWeather(double lat, double lon) {
        String city = null;
        try {
            Geocoder gc = new Geocoder(requireContext(), new Locale("vi", "VN"));
            List<Address> addrs = gc.getFromLocation(lat, lon, 1);
            if (addrs != null && !addrs.isEmpty()) city = addrs.get(0).getAdminArea();
        } catch (Exception ignored) {}

        String weather = null;
        try {
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current_weather=true&hourly=precipitation_probability&forecast_days=1";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String ln;
                while ((ln = br.readLine()) != null) sb.append(ln);
            }
            JSONObject root = new JSONObject(sb.toString());
            JSONObject cw   = root.getJSONObject("current_weather");
            weather = buildWeatherDesc(cw.getDouble("temperature"), cw.getInt("weathercode"), cw.getDouble("windspeed"), 0);
        } catch (Exception ignored) {}

        final String fw = weather != null ? weather : getWeatherByHour();
        final String fc = city    != null ? city    : "Viet Nam";

        if (!isAdded()) return;
        mainHandler.post(() -> {
            if (!isAdded()) return;
            boolean cityChanged = !fc.equals(aiLocation);
            aiWeather    = fw;
            aiLocation   = fc;
            weatherReady = true;
            updateAiInsightsTitle(aiLocation);
            if (cityChanged && aiState == AiState.DONE) resetAi();
            checkAndTriggerAi();
        });
    }

    private String buildWeatherDesc(double temp, int code, double wind, int rain) {
        String cond;
        if      (code == 0)  cond = "trời quang, nắng đẹp";
        else if (code <= 2)  cond = "ít mây, nắng nhẹ";
        else if (code == 3)  cond = "nhiều mây, trời u ám";
        else if (code <= 65) cond = "đang mưa";
        else                 cond = "có giông sấm sét";
        return cond + ", " + (int)temp + "°C";
    }

    private String getWeatherByHour() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h >= 5  && h < 12) return "buổi sáng, trời mát";
        if (h >= 12 && h < 17) return "buổi chiều, nắng nóng";
        return "buổi tối, trời sẽ lạnh";
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) { prefsReady = true; checkAndTriggerAi(); return; }
        db.collection("nguoi_dung").document(user.getUid()).get().addOnSuccessListener(doc -> {
            if (!isAdded()) return;
            if (doc.exists()) {
                String name = doc.getString("ho_ten");
                if (name != null) tvGreeting.setText("Xin chào " + name + "!");
                List<String> prefs = (List<String>) doc.get("so_thich");
                userPrefs.clear(); if (prefs != null) userPrefs.addAll(prefs);
            }
            prefsReady = true; checkAndTriggerAi();
        }).addOnFailureListener(e -> { prefsReady = true; checkAndTriggerAi(); });
    }

    private void callGemini(List<MonAn> candidates, String weather, String location) {
        if (candidates.isEmpty()) { onGeminiFailed(candidates, "empty"); return; }

        String apiKey = getString(R.string.gemini_api_key);
        GenerativeModel gm = new GenerativeModel("gemini-2.0-flash", apiKey);
        GenerativeModelFutures mdl = GenerativeModelFutures.from(gm);
        
        StringBuilder menuStr = new StringBuilder();
        for (int i=0; i<Math.min(20, candidates.size()); i++) {
            menuStr.append("- ID:\"").append(candidates.get(i).getId_mon_an()).append("\" Tên:\"").append(candidates.get(i).getTen_mon()).append("\"\n");
        }

        String prompt = "Bạn là chuyên gia ẩm thực. Chọn 5 món phù hợp nhất với thời tiết: " + weather 
                + ", địa điểm: " + location + ", sở thích: " + String.join(", ", userPrefs)
                + ". Danh sách món:\n" + menuStr
                + "\nTrả về JSON thuần túy (không markdown): {\"greeting\":\"...\",\"ids\":[\"id1\",...]}";
        
        Content cnt = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> fut = mdl.generateContent(cnt);
        Futures.addCallback(fut, new FutureCallback<GenerateContentResponse>() {
            @Override public void onSuccess(GenerateContentResponse resp) {
                try {
                    String text = resp.getText();
                    if (text == null) throw new Exception("Null response");
                    JSONObject obj = new JSONObject(text.replaceAll("(?s)```json|```", "").trim());
                    JSONArray ids = obj.getJSONArray("ids");
                    List<MonAn> res = new ArrayList<>();
                    for (int i=0; i<ids.length(); i++) {
                        String aiId = ids.getString(i);
                        for (MonAn m : candidates) if (m.getId_mon_an().equals(aiId)) { res.add(m); break; }
                    }
                    if (res.isEmpty()) throw new Exception("Match empty");
                    mainHandler.post(() -> onGeminiSuccess(res, obj.optString("greeting", "Gợi ý cho bạn")));
                } catch (Exception e) { onGeminiFailed(candidates, e.getMessage()); }
            }
            @Override public void onFailure(@NonNull Throwable t) { onGeminiFailed(candidates, t.getMessage()); }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private boolean matchesPrefs(MonAn m) {
        if (userPrefs.isEmpty()) return false;
        String ten = VNCharacterUtils.removeAccents(m.getTen_mon() != null ? m.getTen_mon().toLowerCase() : "");
        for (String p : userPrefs) {
            if (p == null || p.isEmpty()) continue;
            if (ten.contains(VNCharacterUtils.removeAccents(p.toLowerCase()))) return true;
        }
        return false;
    }

    private void onGeminiSuccess(List<MonAn> selected, String greeting) {
        aiState = AiState.DONE;
        if (tvAiInsights != null) tvAiInsights.setText("✨ " + greeting);
        listCategory.clear(); listCategory.addAll(selected);
        if (adapterCategory != null) adapterCategory.notifyDataSetChanged();
        showAiLoading(false);
    }

    private void onGeminiFailed(List<MonAn> candidates, String reason) {
        aiState = AiState.DONE;
        List<MonAn> fb = new ArrayList<>();
        for (MonAn m : candidates) if (matchesPrefs(m)) fb.add(m);
        if (fb.size() < 5) { for (MonAn m : candidates) if (!fb.contains(m)) fb.add(m); }
        Collections.shuffle(fb);
        listCategory.clear();
        for (int i=0; i<Math.min(5, fb.size()); i++) listCategory.add(fb.get(i));
        if (adapterCategory != null) adapterCategory.notifyDataSetChanged();
        showAiLoading(false);
    }

    private void scheduleAiTimeout() {
        cancelAiTimeout();
        aiTimeoutAction = () -> onGeminiFailed(new ArrayList<>(listFullCurrentCategory), "timeout");
        mainHandler.postDelayed(aiTimeoutAction, AI_TIMEOUT_MS);
    }

    private void cancelAiTimeout() { if (aiTimeoutAction != null) mainHandler.removeCallbacks(aiTimeoutAction); }

    private void loadRecipesByCategory(String categoryId, String title) {
        lastCategoryId = categoryId; lastCategoryTitle = title;
        if (categoryListener != null) categoryListener.remove();
        Query q = categoryId.equals("all") ? db.collection("mon_an") : db.collection("mon_an").whereEqualTo("id_danh_muc", categoryId);
        categoryListener = q.limit(100).addSnapshotListener((snap, error) -> {
            if (!isAdded() || snap == null) return;
            listFullCurrentCategory.clear();
            for (QueryDocumentSnapshot doc : snap) {
                MonAn m = parseMonAn(doc); if (m != null) listFullCurrentCategory.add(m);
            }
            if (categoryId.equals("all")) { recipesReady = true; checkAndTriggerAi(); }
            else { 
                listCategory.clear(); 
                listCategory.addAll(listFullCurrentCategory);
                if (adapterCategory != null) adapterCategory.notifyDataSetChanged();
                showAiLoading(false); 
            }
        });
    }

    private void setupCategoryButtons(View view) {
        int[] ids = {R.id.btn_cat_all, R.id.btn_cat_man, R.id.btn_cat_canh, R.id.btn_cat_chay, R.id.btn_cat_vat, R.id.btn_cat_lau};
        String[] catIds = {"all", "mon_man", "mon_canh", "mon_chay", "an_vat", "mon_lau"};
        String[] titles = {"Gợi ý cho bạn", "Món mặn", "Món canh", "Món chay", "Ăn vặt", "Món lẩu"};
        for (int i = 0; i < ids.length; i++) {
            final int idx = i; TextView btn = view.findViewById(ids[i]);
            if (btn == null) continue;
            btn.setOnClickListener(v -> {
                if (currentSelectedCategory != null) {
                    currentSelectedCategory.setBackgroundResource(R.drawable.bg_input_field);
                    currentSelectedCategory.setTextColor(android.graphics.Color.parseColor("#555555"));
                }
                btn.setBackgroundResource(R.drawable.bg_register_button);
                btn.setTextColor(android.graphics.Color.WHITE);
                currentSelectedCategory = btn; etSearch.setText("");
                resetAi(); recipesReady = false; loadRecipesByCategory(catIds[idx], titles[idx]);
            });
        }
    }

    private void setupSearchAction() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String q = etSearch.getText().toString().trim();
                hideKeyboard();
                if (!q.isEmpty()) openSearchResult(q);
                return true;
            }
            return false;
        });
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { 
                if (s.toString().isEmpty()) loadRecipesByCategory(lastCategoryId, lastCategoryTitle); 
            }
        });
    }

    private void openSearchResult(String query) {
        // SỬA: Chuyển sang SearchResultFragment (Giao diện cam đào)
        SearchResultFragment fragment = SearchResultFragment.newInstance(query);
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .add(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openFilterFragment() {
        // SỬA: Chuyển sang FilterFragment (Giao diện cam đào)
        FilterFragment fragment = new FilterFragment();
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .add(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void hideKeyboard() {
        if (getActivity() == null) return;
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }

    private void setupRecyclerViews(View view) {
        rvCategory = view.findViewById(R.id.rv_category_dishes);
        rvFeatured = view.findViewById(R.id.rv_featured);
        rvNew = view.findViewById(R.id.rv_new_recipes);
        rvHistory = view.findViewById(R.id.rv_history);
        rvWeeklyAttention = view.findViewById(R.id.rv_weekly_attention);
        
        int H = LinearLayoutManager.HORIZONTAL;
        rvCategory.setLayoutManager(new LinearLayoutManager(getContext(), H, false));
        rvFeatured.setLayoutManager(new LinearLayoutManager(getContext(), H, false));
        rvNew.setLayoutManager(new LinearLayoutManager(getContext(), H, false));
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext(), H, false));
        rvWeeklyAttention.setLayoutManager(new LinearLayoutManager(getContext(), H, false));
        
        adapterCategory = new MonAnAdapter(listCategory);
        adapterFeatured = new MonAnAdapter(listFeatured); adapterFeatured.setSectionInfo("NOI_BAT", "");
        adapterNew = new MonAnAdapter(listNew); adapterNew.setSectionInfo("MOI", "");
        adapterHistory = new MonAnAdapter(listHistory); adapterHistory.setSectionInfo("LICH_SU", "");
        adapterWeeklyAttention = new MonAnAdapter(listWeeklyAttention); adapterWeeklyAttention.setSectionInfo("DUOC_DE_Y_TUAN", "");
        
        rvCategory.setAdapter(adapterCategory); rvFeatured.setAdapter(adapterFeatured);
        rvNew.setAdapter(adapterNew); rvHistory.setAdapter(adapterHistory); rvWeeklyAttention.setAdapter(adapterWeeklyAttention);
    }

    private void loadFeaturedRecipes() {
        featuredListener = db.collection("mon_an").orderBy("luot_xem", Query.Direction.DESCENDING).limit(11).addSnapshotListener((snap, e) -> {
            if (snap == null) return; listFeatured.clear();
            for (QueryDocumentSnapshot d : snap) listFeatured.add(parseMonAn(d));
            adapterFeatured.notifyDataSetChanged();
        });
    }

    private void loadNewRecipes() {
        newRecipesListener = db.collection("mon_an").orderBy("rating", Query.Direction.DESCENDING).limit(11).addSnapshotListener((snap, e) -> {
            if (snap == null) return; listNew.clear();
            for (QueryDocumentSnapshot d : snap) listNew.add(parseMonAn(d));
            adapterNew.notifyDataSetChanged();
        });
    }

    private void loadHistoryRecipes() {
        String uid = mAuth.getUid(); if (uid == null) return;
        historyListener = db.collection("lich_su_xem").whereEqualTo("id_nguoi_dung", uid).orderBy("thoi_gian_xem", Query.Direction.DESCENDING).limit(11).addSnapshotListener((snap, e) -> {
            if (snap == null) return; List<String> ids = new ArrayList<>();
            for (DocumentSnapshot d : snap) ids.add(d.getString("id_mon_an"));
            if (ids.isEmpty()) { listHistory.clear(); adapterHistory.notifyDataSetChanged(); return; }
            List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
            for (String id : ids) tasks.add(db.collection("mon_an").document(id).get());
            Tasks.whenAllSuccess(tasks).addOnSuccessListener(res -> {
                if (!isAdded()) return;
                listHistory.clear(); 
                for (Object r : res) { MonAn m = parseMonAn((DocumentSnapshot)r); if(m!=null) listHistory.add(m); }
                List<MonAn> sorted = new ArrayList<>();
                for (String id : ids) { for (MonAn m : listHistory) if (m.getId_mon_an().equals(id)) { sorted.add(m); break; } }
                listHistory.clear(); listHistory.addAll(sorted);
                adapterHistory.notifyDataSetChanged();
            });
        });
    }

    private void checkAdminRole() {
        FirebaseUser user = mAuth.getCurrentUser(); if (user == null) return;
        db.collection("nguoi_dung").document(user.getUid()).get().addOnSuccessListener(doc -> {
            if (!isAdded() || !doc.exists()) return;
            String v = doc.getString("vai_tro"), r = doc.getString("role");
            if ("admin".equals(v) || "admin".equals(r)) { btnAdminMenu.setVisibility(View.VISIBLE); setupAdminMenu(); }
        });
    }

    private void setupAdminMenu() {
        btnAdminMenu.setOnClickListener(v -> {
            android.widget.PopupMenu p = new android.widget.PopupMenu(getContext(), v);
            p.getMenuInflater().inflate(R.menu.menu_admin_popup, p.getMenu());
            p.setOnMenuItemClickListener(item -> {
                if (getActivity() == null) return false;
                if (item.getItemId() == R.id.menu_admin_dashboard) startActivity(new Intent(getActivity(), AdminActivity.class));
                else if (item.getItemId() == R.id.menu_admin_stats) startActivity(new Intent(getActivity(), ThongKeAdminActivity.class));
                else if (item.getItemId() == R.id.menu_admin_users) startActivity(new Intent(getActivity(), QuanLyTaiKhoanActivity.class));
                else if (item.getItemId() == R.id.menu_admin_reviews) startActivity(new Intent(getActivity(), QuanLyDanhGiaActivity.class));
                return true;
            });
            p.show();
        });
    }

    private void loadWeeklyAttentionRecipes() {
        long moc = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000;
        db.collection("lich_su_xem").whereGreaterThanOrEqualTo("thoi_gian_xem", new com.google.firebase.Timestamp(new java.util.Date(moc))).get().addOnSuccessListener(snap -> {
            Map<String, Integer> score = new HashMap<>();
            for (DocumentSnapshot d : snap) { String id = d.getString("id_mon_an"); if(id!=null) score.put(id, score.getOrDefault(id, 0) + 1); }
            if (score.isEmpty()) { loadFeaturedRecipesFallbackForWeekly(); return; }
            List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(score.entrySet());
            sortedList.sort((a,b) -> b.getValue() - a.getValue());
            List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
            for (int i=0; i<Math.min(10, sortedList.size()); i++) tasks.add(db.collection("mon_an").document(sortedList.get(i).getKey()).get());
            Tasks.whenAllSuccess(tasks).addOnSuccessListener(res -> {
                if (!isAdded()) return;
                listWeeklyAttention.clear();
                for (Object r : res) { MonAn m = parseMonAn((DocumentSnapshot)r); if (m!=null) listWeeklyAttention.add(m); }
                adapterWeeklyAttention.notifyDataSetChanged();
            });
        });
    }

    private void loadFeaturedRecipesFallbackForWeekly() {
        db.collection("mon_an").orderBy("luot_xem", Query.Direction.DESCENDING).limit(10).get().addOnSuccessListener(snap -> {
            if (!isAdded()) return;
            listWeeklyAttention.clear();
            for (QueryDocumentSnapshot d : snap) listWeeklyAttention.add(parseMonAn(d));
            adapterWeeklyAttention.notifyDataSetChanged();
        });
    }

    private void updateAiInsightsTitle(@Nullable String city) {
        if (!isAdded() || tvAiInsights == null || !lastCategoryId.equals("all")) return;
        tvAiInsights.setText(city != null ? "📍 " + city + " - AI đang chọn món..." : "✨ AI đang phân tích...");
    }

    private void showAiLoading(boolean show) {
        if (!isAdded()) return;
        if (pbAiLoading != null) pbAiLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (rvCategory != null) rvCategory.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private static class LabelSpinnerAdapter extends android.widget.ArrayAdapter<String> {
        private final String label;
        LabelSpinnerAdapter(Context ctx, String lbl, String[] items) { super(ctx, R.layout.spinner_item_selected, items); label = lbl; setDropDownViewResource(R.layout.spinner_dropdown_item); }
        @Override public View getView(int pos, View cv, ViewGroup p) { TextView tv = (TextView) super.getView(pos, cv, p); tv.setText("Tất cả".equals(getItem(pos)) ? label : getItem(pos)); return tv; }
    }
}
