            package com.example.mamacook.fragments;

            import android.Manifest;
            import android.content.Context;
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
            import android.widget.ArrayAdapter;
            import android.widget.EditText;
            import android.widget.ImageButton;
            import android.widget.LinearLayout;
            import android.widget.ProgressBar;
            import android.widget.Spinner;
            import android.widget.TextView;

            import androidx.annotation.NonNull;
            import androidx.annotation.Nullable;
            import androidx.core.app.ActivityCompat;
            import androidx.core.content.ContextCompat;
            import androidx.fragment.app.Fragment;
            import androidx.recyclerview.widget.LinearLayoutManager;
            import androidx.recyclerview.widget.RecyclerView;

            import com.example.mamacook.R;
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
            import java.util.List;
            import java.util.Locale;
            import java.util.concurrent.Executor;
            import java.util.concurrent.Executors;

            public class HomeFragment extends Fragment {

                private static final String TAG = "HomeFragment";
                private static final long AI_TIMEOUT_MS = 15_000L;

                // =========================================================================
                // AI STATE MACHINE
                //  IDLE    → chưa có gì, sẵn sàng nhận trigger
                //  WAITING → đang chờ data (weather/prefs/recipes chưa đủ)
                //  RUNNING → đang gọi Gemini
                //  DONE    → đã hiển thị kết quả AI
                // =========================================================================
                private enum AiState { IDLE, WAITING, RUNNING, DONE }
                private AiState aiState = AiState.IDLE;

                private String aiWeather  = null;
                private String aiLocation = null;

                private boolean weatherReady = false;
                private boolean prefsReady   = false;
                private boolean recipesReady = false;

                // Firebase & Location
                private FirebaseFirestore db;
                private FirebaseAuth mAuth;
                private FusedLocationProviderClient fusedLocationClient;

                // Views
                private TextView    tvGreeting, tvAiInsights;
                private EditText    etSearch;
                private ImageButton btnFilter;
                private LinearLayout layoutFilters;
                private Spinner     spnDifficulty, spnTime, spnRating;
                private ProgressBar pbAiLoading;
                private RecyclerView rvCategory, rvFeatured, rvNew, rvHistory;

                // Adapters
                private MonAnAdapter adapterCategory, adapterFeatured, adapterNew, adapterHistory;

                // Realtime listeners
                private ListenerRegistration categoryListener, featuredListener,
                        newRecipesListener, historyListener;

                // Data lists
                private final List<MonAn>  listCategory            = new ArrayList<>();
                private final List<MonAn>  listFeatured            = new ArrayList<>();
                private final List<MonAn>  listNew                 = new ArrayList<>();
                private final List<MonAn>  listHistory             = new ArrayList<>();
                private final List<MonAn>  listFullCurrentCategory = new ArrayList<>();
                private final List<String> userPrefs               = new ArrayList<>();

                // Category state
                private TextView currentSelectedCategory;
                private String lastCategoryId    = "all";
                private String lastCategoryTitle = "Gợi ý cho bạn";

                // Threading
                private final Handler  mainHandler     = new Handler(Looper.getMainLooper());
                private       Runnable aiTimeoutAction = null;
                private final Executor bgExecutor      = Executors.newCachedThreadPool();

                // =========================================================================
                // LIFECYCLE
                // =========================================================================

                @Nullable
                @Override
                public View onCreateView(@NonNull LayoutInflater inflater,
                                         @Nullable ViewGroup container,
                                         @Nullable Bundle savedInstanceState) {
                    View view = inflater.inflate(R.layout.fragment_home, container, false);

                    db                  = FirebaseFirestore.getInstance();
                    mAuth               = FirebaseAuth.getInstance();
                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

                    bindViews(view);
                    setupRecyclerViews(view);
                    setupCategoryButtons(view);
                    setupSearchAction();
                    setupSpinners();
                    btnFilter.setOnClickListener(v -> toggleFilterLayout());

                    loadUserProfile();
                    loadRecipesByCategory("all", lastCategoryTitle);
                    fetchLocationAndWeather();

                    loadFeaturedRecipes();
                    loadNewRecipes();
                    loadHistoryRecipes();

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
                    layoutFilters = v.findViewById(R.id.layout_filters);
                    pbAiLoading   = v.findViewById(R.id.pb_ai_loading);
                    spnDifficulty = v.findViewById(R.id.spn_difficulty_main);
                    spnTime       = v.findViewById(R.id.spn_time_main);
                    spnRating     = v.findViewById(R.id.spn_rating_main);
                    currentSelectedCategory = v.findViewById(R.id.btn_cat_all);
                }

                // =========================================================================
                // HELPER: Parse MonAn — ưu tiên field id_mon_an, fallback doc.getId()
                // =========================================================================
                private MonAn parseMonAn(DocumentSnapshot doc) {
                    MonAn m = doc.toObject(MonAn.class);
                    if (m == null) return null;
                    if (m.getId_mon_an() == null || m.getId_mon_an().isEmpty()) {
                        m.setId_mon_an(doc.getId());
                    }
                    return m;
                }

                // =========================================================================
                // AI GATE — điểm hội tụ DUY NHẤT để quyết định có gọi AI không
                // =========================================================================
                private void checkAndTriggerAi() {
                    if (Looper.myLooper() != Looper.getMainLooper()) {
                        mainHandler.post(this::checkAndTriggerAi);
                        return;
                    }
                    if (!isAdded()) return;
                    if (!lastCategoryId.equals("all")) return;

                    if (aiState == AiState.RUNNING || aiState == AiState.DONE) {
                        Log.d(TAG, "[AI-GATE] Skip — state=" + aiState);
                        return;
                    }

                    if (!weatherReady || !prefsReady || !recipesReady) {
                        aiState = AiState.WAITING;
                        Log.d(TAG, "[AI-GATE] WAITING — weather=" + weatherReady
                                + " prefs=" + prefsReady + " recipes=" + recipesReady);
                        return;
                    }

                    Log.d(TAG, "[AI-GATE] RUNNING — loc=" + aiLocation + " weather=" + aiWeather);
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
                    if (aiState == AiState.RUNNING) {
                        Log.d(TAG, "[AI-RESET] Skip — RUNNING");
                        return;
                    }
                    cancelAiTimeout();
                    aiState = AiState.IDLE;
                    Log.d(TAG, "[AI-RESET] Back to IDLE");
                }

                // =========================================================================
                // LOCATION + WEATHER
                // =========================================================================
                private void fetchLocationAndWeather() {
                    if (ActivityCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(requireActivity(),
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
                        aiWeather    = getWeatherByHour();
                        aiLocation   = "Viet Nam";
                        weatherReady = true;
                        updateAiInsightsTitle(null);
                        checkAndTriggerAi();
                        return;
                    }

                    fusedLocationClient.getLastLocation()
                            .addOnSuccessListener(loc -> {
                                if (loc == null) {
                                    aiWeather    = getWeatherByHour();
                                    aiLocation   = "Viet Nam";
                                    weatherReady = true;
                                    updateAiInsightsTitle(null);
                                    checkAndTriggerAi();
                                    return;
                                }
                                bgExecutor.execute(() ->
                                        runGeocodeAndWeather(loc.getLatitude(), loc.getLongitude()));
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "FusedLocation err: " + e.getMessage());
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
                    } catch (Exception e) {
                        Log.e(TAG, "Geocoder err: " + e.getMessage());
                    }

                    String weather = null;
                    try {
                        String url = "https://api.open-meteo.com/v1/forecast"
                                + "?latitude=" + lat + "&longitude=" + lon
                                + "&current_weather=true"
                                + "&hourly=precipitation_probability&forecast_days=1";
                        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                        conn.setConnectTimeout(6000);
                        conn.setReadTimeout(6000);
                        StringBuilder sb = new StringBuilder();
                        try (BufferedReader br =
                                     new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                            String ln;
                            while ((ln = br.readLine()) != null) sb.append(ln);
                        }
                        JSONObject root = new JSONObject(sb.toString());
                        JSONObject cw   = root.getJSONObject("current_weather");
                        double temp = cw.getDouble("temperature");
                        int    code = cw.getInt("weathercode");
                        double wind = cw.getDouble("windspeed");
                        int    rain = 0;
                        try {
                            rain = root.getJSONObject("hourly")
                                    .getJSONArray("precipitation_probability")
                                    .getInt(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
                        } catch (Exception ignored) {}
                        weather = buildWeatherDesc(temp, code, wind, rain);
                    } catch (Exception e) {
                        Log.e(TAG, "Weather API err: " + e.getMessage());
                    }

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
                    if      (code == 0)  cond = "troi quang, nang dep";
                    else if (code <= 2)  cond = "it may, nang nhe";
                    else if (code == 3)  cond = "nhieu may, troi u am";
                    else if (code <= 49) cond = "co suong mu";
                    else if (code <= 55) cond = "mua phun nhe";
                    else if (code <= 65) cond = "dang mua" + (rain > 70 ? " to" : "");
                    else if (code <= 75) cond = "co tuyet";
                    else if (code <= 82) cond = "mua rao";
                    else                 cond = "co giong sam set";

                    String tdesc;
                    if      (temp < 20) tdesc = "lanh ("     + (int) temp + "°C)";
                    else if (temp < 28) tdesc = "mat me ("   + (int) temp + "°C)";
                    else if (temp < 33) tdesc = "am ap ("    + (int) temp + "°C)";
                    else                tdesc = "nong buc (" + (int) temp + "°C)";

                    String wdesc = wind > 30 ? ", gio manh" : "";
                    String rdesc = rain > 60 ? ", kha nang mua " + rain + "%" : "";
                    return cond + ", " + tdesc + wdesc + rdesc;
                }

                private String getWeatherByHour() {
                    int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    if (h >= 5  && h < 9)  return "buoi sang som, troi mat de chiu";
                    if (h >= 9  && h < 12) return "buoi sang, nang len dan";
                    if (h >= 12 && h < 14) return "buoi trua, nang nong";
                    if (h >= 14 && h < 17) return "buoi chieu, hay co mua rao";
                    if (h >= 17 && h < 20) return "buoi chieu toi, mat dan";
                    return "buoi toi muon, troi se lanh";
                }

                // =========================================================================
                // USER PROFILE
                // =========================================================================
                private void loadUserProfile() {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null) {
                        prefsReady = true;
                        checkAndTriggerAi();
                        return;
                    }
                    db.collection("nguoi_dung").document(user.getUid()).get()
                            .addOnSuccessListener(doc -> {
                                if (!isAdded()) return;
                                if (doc.exists()) {
                                    String name = doc.getString("ho_ten");
                                    if (name != null && !name.isEmpty())
                                        tvGreeting.setText("Xin chao " + name + "!");
                                    @SuppressWarnings("unchecked")
                                    List<String> prefs = (List<String>) doc.get("so_thich");
                                    userPrefs.clear();
                                    if (prefs != null) userPrefs.addAll(prefs);
                                }
                                prefsReady = true;
                                checkAndTriggerAi();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Load profile err: " + e.getMessage());
                                prefsReady = true;
                                checkAndTriggerAi();
                            });
                }

                // =========================================================================
                // GEMINI CALL
                // =========================================================================
                private void callGemini(List<MonAn> candidates, String weather, String location) {
                    if (candidates.isEmpty()) {
                        onGeminiFailed(candidates, "empty candidates");
                        return;
                    }

                    List<MonAn> preferred = new ArrayList<>();
                    List<MonAn> others    = new ArrayList<>();
                    for (MonAn m : candidates) {
                        if (matchesPrefs(m)) preferred.add(m);
                        else                 others.add(m);
                    }
                    Collections.shuffle(preferred);
                    Collections.shuffle(others);

                    List<MonAn> pool = new ArrayList<>();
                    int pt = Math.min(preferred.size(), 15);
                    int ot = Math.min(others.size(), 25 - pt);
                    pool.addAll(preferred.subList(0, pt));
                    pool.addAll(others.subList(0, ot));

                    StringBuilder menu = new StringBuilder();
                    for (MonAn m : pool) {
                        if (m.getId_mon_an() == null || m.getTen_mon() == null) continue;
                        menu.append("- ID:\"").append(m.getId_mon_an())
                                .append("\" Ten:\"").append(m.getTen_mon()).append("\"\n");
                    }
                    if (menu.length() == 0) { onGeminiFailed(candidates, "menu empty"); return; }

                    int    hour     = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    String mealTime = getMealTime(hour);

                    String prompt =
                            "Ban la chuyen gia am thuc Viet Nam. Chon DUNG 5 mon phu hop nhat.\n\n"
                                    + "THONG TIN THUC TE:\n"
                                    + "- Dia diem: " + location + "\n"
                                    + "- Thoi tiet: " + weather + "\n"
                                    + "- Thoi diem: " + mealTime + " (" + hour + "h)\n"
                                    + "- So thich: " + (userPrefs.isEmpty() ? "chua ro"
                                    : String.join(", ", userPrefs)) + "\n\n"
                                    + "NGUYEN TAC:\n"
                                    + "- Lanh/mua → lau, sup, chao, mon nuoc nong\n"
                                    + "- Nong     → goi, mon nhe, do mat\n"
                                    + "- Sang     → pho, bun, chao, xoi, banh mi\n"
                                    + "- Trua/toi → com, mon man, day du dinh duong\n"
                                    + "- Khuya    → mi, chao, an vat\n"
                                    + "- Uu tien mon khop so thich (neu co)\n\n"
                                    + "DANH SACH MON HOP LE (chi duoc chon trong danh sach nay):\n"
                                    + menu
                                    + "\nQUY TAC BAT BUOC:\n"
                                    + "1. Chi dung ID co trong danh sach. KHONG tu tao ID.\n"
                                    + "2. Tra ve JSON THUAN TUY. KHONG markdown. KHONG ```json.\n"
                                    + "3. Greeting: 1 cau ~15 tu, de cap thoi tiet/dia diem that su.\n\n"
                                    + "{\"greeting\":\"...\",\"ids\":[\"id1\",\"id2\",\"id3\",\"id4\",\"id5\"]}";

                    String apiKey = "AIzaSyB98STC_84bt0XdZ6FPOdyUw5bj95lsGrQ";
                    GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", apiKey);
                    GenerativeModelFutures mdl = GenerativeModelFutures.from(gm);
                    Content                cnt = new Content.Builder().addText(prompt).build();
                    ListenableFuture<GenerateContentResponse> fut = mdl.generateContent(cnt);

                    final List<MonAn> fCandidates = candidates;
                    final List<MonAn> fPreferred  = preferred;

                    Futures.addCallback(fut, new FutureCallback<GenerateContentResponse>() {
                        @Override
                        public void onSuccess(GenerateContentResponse resp) {
                            cancelAiTimeout();
                            try {
                                String raw = resp.getText();
                                if (raw == null || raw.trim().isEmpty())
                                    throw new Exception("empty response");

                                String json = raw.replaceAll("(?s)```json\\s*", "")
                                        .replaceAll("(?s)```\\s*", "").trim();
                                int s = json.indexOf('{'), e = json.lastIndexOf('}');
                                if (s < 0 || e <= s) throw new Exception("no JSON found");
                                json = json.substring(s, e + 1);

                                JSONObject obj      = new JSONObject(json);
                                String     greeting = obj.optString("greeting", "");
                                JSONArray  idsArr   = obj.optJSONArray("ids");

                                List<MonAn> selected = new ArrayList<>();
                                if (idsArr != null) {
                                    for (int i = 0; i < idsArr.length(); i++) {
                                        String aiId = idsArr.getString(i).trim();
                                        for (MonAn m : fCandidates) {
                                            if (m.getId_mon_an() != null
                                                    && m.getId_mon_an().trim().equalsIgnoreCase(aiId)) {
                                                selected.add(m);
                                                break;
                                            }
                                        }
                                    }
                                }

                                if (selected.isEmpty()) {
                                    List<MonAn> fb = fPreferred.isEmpty()
                                            ? new ArrayList<>(fCandidates) : new ArrayList<>(fPreferred);
                                    Collections.shuffle(fb);
                                    selected.addAll(fb.subList(0, Math.min(5, fb.size())));
                                }

                                final List<MonAn> result   = selected;
                                final String      greetTxt = greeting;
                                if (!isAdded()) return;
                                mainHandler.post(() -> {
                                    if (!isAdded() || !lastCategoryId.equals("all")) return;
                                    onGeminiSuccess(result, greetTxt);
                                });

                            } catch (Exception ex) {
                                Log.e(TAG, "Parse err: " + ex.getMessage());
                                onGeminiFailed(fCandidates, ex.getMessage());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Throwable t) {
                            cancelAiTimeout();
                            Log.e(TAG, "Gemini failure: " + t.getMessage());
                            onGeminiFailed(fCandidates, t.getMessage());
                        }
                    }, ContextCompat.getMainExecutor(requireContext()));
                }

                private void onGeminiSuccess(List<MonAn> selected, String greeting) {
                    aiState = AiState.DONE;
                    if (!greeting.isEmpty() && tvAiInsights != null)
                        tvAiInsights.setText("✨ " + greeting);
                    listCategory.clear();
                    listCategory.addAll(selected);
                    if (adapterCategory != null) adapterCategory.notifyDataSetChanged();
                    showAiLoading(false);
                    Log.d(TAG, "[AI-DONE] " + selected.size() + " mon");
                }

                private void onGeminiFailed(List<MonAn> candidates, String reason) {
                    if (!isAdded()) return;
                    mainHandler.post(() -> {
                        if (!isAdded()) return;
                        Log.w(TAG, "[AI-FAIL] " + reason + " → fallback");
                        aiState = AiState.DONE;

                        List<MonAn> fb = new ArrayList<>();
                        for (MonAn m : candidates) { if (matchesPrefs(m)) fb.add(m); }
                        if (fb.size() < 5) {
                            for (MonAn m : candidates) { if (!fb.contains(m)) fb.add(m); }
                        }
                        Collections.shuffle(fb);
                        List<MonAn> result = new ArrayList<>(fb.subList(0, Math.min(5, fb.size())));

                        if (tvAiInsights != null)
                            tvAiInsights.setText("Một số món bạn có thể thích hôm nay");
                        listCategory.clear();
                        listCategory.addAll(result);
                        if (adapterCategory != null) adapterCategory.notifyDataSetChanged();
                        showAiLoading(false);
                    });
                }

                // =========================================================================
                // MATCHING
                // =========================================================================
                private boolean matchesPrefs(MonAn m) {
                    if (userPrefs.isEmpty()) return false;
                    String ten = m.getTen_mon()     != null
                            ? VNCharacterUtils.removeAccents(m.getTen_mon().toLowerCase())     : "";
                    String cat = m.getId_danh_muc() != null
                            ? VNCharacterUtils.removeAccents(m.getId_danh_muc().toLowerCase()) : "";
                    for (String p : userPrefs) {
                        if (p == null || p.isEmpty()) continue;
                        String np = VNCharacterUtils.removeAccents(p.trim().toLowerCase());
                        if (ten.contains(np) || cat.contains(np)) return true;
                    }
                    return false;
                }

                // =========================================================================
                // TIMEOUT
                // =========================================================================
                private void scheduleAiTimeout() {
                    cancelAiTimeout();
                    aiTimeoutAction = () -> {
                        if (!isAdded()) return;
                        Log.w(TAG, "AI timeout → fallback");
                        onGeminiFailed(new ArrayList<>(listFullCurrentCategory), "timeout");
                    };
                    mainHandler.postDelayed(aiTimeoutAction, AI_TIMEOUT_MS);
                }

                private void cancelAiTimeout() {
                    if (aiTimeoutAction != null) {
                        mainHandler.removeCallbacks(aiTimeoutAction);
                        aiTimeoutAction = null;
                    }
                }

                // =========================================================================
                // CATEGORY LOADING
                // =========================================================================
                private void loadRecipesByCategory(String categoryId, String title) {
                    lastCategoryId    = categoryId;
                    lastCategoryTitle = title;

                    if (categoryListener != null) categoryListener.remove();

                    Query q = categoryId.equals("all")
                            ? db.collection("mon_an")
                            : db.collection("mon_an").whereEqualTo("id_danh_muc", categoryId);

                    categoryListener = q.limit(100).addSnapshotListener((snap, error) -> {
                        if (!isAdded() || snap == null) {
                            if (error != null) Log.e(TAG, "Category listener err: " + error.getMessage());
                            return;
                        }

                        listFullCurrentCategory.clear();
                        for (QueryDocumentSnapshot doc : snap) {
                            MonAn m = parseMonAn(doc);
                            if (m != null) listFullCurrentCategory.add(m);
                        }
                        Log.d(TAG, "Loaded " + listFullCurrentCategory.size()
                                + " recipes, cat=" + categoryId);

                        if (categoryId.equals("all")) {
                            recipesReady = true;
                            checkAndTriggerAi();
                        } else {
                            applyLocalFilters();
                            showAiLoading(false);
                        }
                    });
                }

                private void setupCategoryButtons(View view) {
                    int[]    ids    = {R.id.btn_cat_all, R.id.btn_cat_man, R.id.btn_cat_canh,
                            R.id.btn_cat_chay, R.id.btn_cat_vat, R.id.btn_cat_lau};
                    String[] catIds = {"all", "mon_man", "mon_canh", "mon_chay", "an_vat", "mon_lau"};
                    String[] titles = {"Gợi ý cho bạn", "Món mặn", "Món canh",
                            "Món chay", "Ăn vặt", "Món lẩu"};

                    for (int i = 0; i < ids.length; i++) {
                        final int idx = i;
                        TextView btn = view.findViewById(ids[i]);
                        if (btn == null) continue;
                        btn.setOnClickListener(v -> {
                            if (currentSelectedCategory != null) {
                                currentSelectedCategory.setBackgroundResource(R.drawable.bg_input_field);
                                currentSelectedCategory.setTextColor(
                                        android.graphics.Color.parseColor("#555555"));
                            }
                            btn.setBackgroundResource(R.drawable.bg_register_button);
                            btn.setTextColor(android.graphics.Color.WHITE);
                            currentSelectedCategory = btn;
                            etSearch.setText("");

                            resetAi();
                            recipesReady = false;
                            loadRecipesByCategory(catIds[idx], titles[idx]);
                        });
                    }
                }

                // =========================================================================
                // SEARCH — FIX: dùng parseMonAn thay vì setId_mon_an(doc.getId())
                // =========================================================================
                private void setupSearchAction() {
                    etSearch.setOnEditorActionListener((v, actionId, event) -> {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                            String q = etSearch.getText().toString().trim()
                                    .toLowerCase(Locale.getDefault());
                            hideKeyboard();
                            if (!q.isEmpty()) performSearch(q);
                            else              loadRecipesByCategory(lastCategoryId, lastCategoryTitle);
                            return true;
                        }
                        return false;
                    });
                    etSearch.addTextChangedListener(new TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                        @Override public void onTextChanged    (CharSequence s, int a, int b, int c) {}
                        @Override public void afterTextChanged(Editable s) {
                            if (s.toString().trim().isEmpty())
                                loadRecipesByCategory(lastCategoryId, lastCategoryTitle);
                        }
                    });
                }

                private void performSearch(String text) {
                    String   noTone = VNCharacterUtils.removeAccents(text.toLowerCase());
                    String[] words  = noTone.split("\\s+");

                    showAiLoading(true);
                    db.collection("mon_an")
                            .whereArrayContains("tu_khoa_tim_kiem", words[0]).get()
                            .addOnSuccessListener(snap -> {
                                List<MonAn> res = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : snap) {
                                    MonAn m = parseMonAn(doc);
                                    if (m == null) continue;
                                    List<String> kw = m.getTu_khoa_tim_kiem();
                                    if (kw == null) continue;
                                    boolean ok = true;
                                    for (String w : words) {
                                        if (!kw.contains(w)) { ok = false; break; }
                                    }
                                    if (ok) res.add(m);
                                }
                                listFullCurrentCategory.clear();
                                listFullCurrentCategory.addAll(res);
                                applyLocalFilters();
                                showAiLoading(false);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Search err: " + e.getMessage());
                                showAiLoading(false);
                            });
                }

                private void hideKeyboard() {
                    if (getActivity() == null) return;
                    InputMethodManager imm = (InputMethodManager)
                            getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
                }

                // =========================================================================
                // FILTERS
                // =========================================================================
                private void toggleFilterLayout() {
                    if (layoutFilters.getVisibility() == View.GONE) {
                        layoutFilters.setVisibility(View.VISIBLE);
                        layoutFilters.setAlpha(0f);
                        layoutFilters.animate().alpha(1f).setDuration(300).start();
                    } else {
                        layoutFilters.setVisibility(View.GONE);
                        spnDifficulty.setSelection(0);
                        spnTime.setSelection(0);
                        spnRating.setSelection(0);
                    }
                }

                private void setupSpinners() {
                    String[] diff  = {"Tất cả", "Dễ", "Trung bình", "Khó"};
                    String[] times = {"Tất cả", "Dưới 15'", "15-30'", "30-60'", "Trên 60'"};
                    String[] rats  = {"Tất cả", "4 sao trở lên", "3 sao trở lên", "2 sao trở lên"};
                    if (getContext() == null) return;
                    spnDifficulty.setAdapter(new LabelSpinnerAdapter(getContext(), "Độ khó",    diff));
                    spnTime      .setAdapter(new LabelSpinnerAdapter(getContext(), "Thời gian", times));
                    spnRating    .setAdapter(new LabelSpinnerAdapter(getContext(), "Đánh giá",  rats));

                    android.widget.AdapterView.OnItemSelectedListener filterListener = new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { applyLocalFilters(); }
                        @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
                    };
                    spnDifficulty.setOnItemSelectedListener(filterListener);
                    spnTime.setOnItemSelectedListener(filterListener);
                    spnRating.setOnItemSelectedListener(filterListener);
                }

                private void applyLocalFilters() {
                    if (!isAdded()) return;
                    String diff  = spnDifficulty.getSelectedItem() != null ? spnDifficulty.getSelectedItem().toString() : "Tất cả";
                    String time  = spnTime.getSelectedItem()       != null ? spnTime.getSelectedItem().toString()       : "Tất cả";
                    String rat   = spnRating.getSelectedItem()     != null ? spnRating.getSelectedItem().toString()     : "Tất cả";
                    String query = etSearch.getText().toString().trim().toLowerCase();

                    List<MonAn> filtered = new ArrayList<>();
                    for (MonAn m : listFullCurrentCategory) {
                        if (!query.isEmpty()) {
                            String ten = m.getTen_mon() != null ? VNCharacterUtils.removeAccents(m.getTen_mon().toLowerCase()) : "";
                            String qnt = VNCharacterUtils.removeAccents(query);
                            if (!ten.contains(qnt)) continue;
                        }
                        if (!diff.equals("Tất cả") && m.getDo_kho() != null) {
                            if (!m.getDo_kho().equalsIgnoreCase(diff)) continue;
                        }
                        if (!time.equals("Tất cả")) {
                            int t = m.getThoi_gian_nau();
                            if      (time.equals("Dưới 15'")) { if (t >= 15) continue; }
                            else if (time.equals("15-30'"))   { if (t < 15 || t > 30) continue; }
                            else if (time.equals("30-60'"))   { if (t < 30 || t > 60) continue; }
                            else if (time.equals("Trên 60'")) { if (t <= 60) continue; }
                        }
                        if (!rat.equals("Tất cả")) {
                            double r = m.getRating();
                            if      (rat.contains("4 sao")) { if (r < 4) continue; }
                            else if (rat.contains("3 sao")) { if (r < 3) continue; }
                            else if (rat.contains("2 sao")) { if (r < 2) continue; }
                        }
                        filtered.add(m);
                    }
                    if (!query.isEmpty()) {
                        String qnt = VNCharacterUtils.removeAccents(query);
                        filtered.sort((a, b) -> Integer.compare(calcRelevance(b, query, qnt), calcRelevance(a, query, qnt)));
                    }
                    listCategory.clear();
                    listCategory.addAll(filtered);
                    if (adapterCategory != null) adapterCategory.notifyDataSetChanged();

                    if (tvAiInsights != null) {
                        if (!query.isEmpty()) tvAiInsights.setText("Tìm thấy " + filtered.size() + " kết quả cho '" + query + "'");
                        else if (!diff.equals("Tất cả") || !time.equals("Tất cả") || !rat.equals("Tất cả"))
                            tvAiInsights.setText("Đã lọc: " + filtered.size() + " món phù hợp");
                    }
                }

                private static class LabelSpinnerAdapter extends ArrayAdapter<String> {
                    private final String label;
                    LabelSpinnerAdapter(Context ctx, String lbl, String[] items) {
                        super(ctx, R.layout.spinner_item_selected, items);
                        label = lbl;
                        setDropDownViewResource(R.layout.spinner_dropdown_item);
                    }
                    @NonNull @Override
                    public View getView(int pos, @Nullable View cv, @NonNull ViewGroup parent) {
                        TextView tv = (TextView) super.getView(pos, cv, parent);
                        String item = getItem(pos);
                        tv.setText("Tất cả".equals(item) ? label : item);
                        return tv;
                    }
                }

                // =========================================================================
                // RECYCLER + FIREBASE LOADERS
                // =========================================================================
                private void setupRecyclerViews(View view) {
                    rvCategory = view.findViewById(R.id.rv_category_dishes);
                    rvFeatured = view.findViewById(R.id.rv_featured);
                    rvNew      = view.findViewById(R.id.rv_new_recipes);
                    rvHistory  = view.findViewById(R.id.rv_history);

                    int H = LinearLayoutManager.HORIZONTAL;
                    rvCategory.setLayoutManager(new LinearLayoutManager(getContext(), H, false));
                    rvFeatured.setLayoutManager(new LinearLayoutManager(getContext(), H, false));
                    rvNew     .setLayoutManager(new LinearLayoutManager(getContext(), H, false));
                    rvHistory .setLayoutManager(new LinearLayoutManager(getContext(), H, false));

                    adapterCategory = new MonAnAdapter(listCategory);
                    adapterFeatured = new MonAnAdapter(listFeatured);
                    adapterFeatured.setSectionInfo("NOI_BAT", "");
                    adapterNew      = new MonAnAdapter(listNew);
                    adapterNew.setSectionInfo("MOI", "");
                    adapterHistory  = new MonAnAdapter(listHistory);
                    adapterHistory.setSectionInfo("LICH_SU", "");

                    rvCategory.setAdapter(adapterCategory);
                    rvFeatured.setAdapter(adapterFeatured);
                    rvNew     .setAdapter(adapterNew);
                    rvHistory .setAdapter(adapterHistory);
                }

                private void loadFeaturedRecipes() {
                    if (featuredListener != null) featuredListener.remove();
                    featuredListener = db.collection("mon_an")
                            .orderBy("luot_xem", Query.Direction.DESCENDING)
                            .limit(11)
                            .addSnapshotListener((snap, error) -> {
                                if (!isAdded() || snap == null) {
                                    if (error != null) Log.e(TAG, "Featured err: " + error.getMessage());
                                    return;
                                }
                                listFeatured.clear();
                                for (QueryDocumentSnapshot doc : snap) {
                                    MonAn m = parseMonAn(doc);
                                    if (m != null) listFeatured.add(m);
                                }
                                adapterFeatured.notifyDataSetChanged();
                            });
                }

                // ✅ FIX: Bỏ orderBy("ngay_tao") vì document mới không có field này
                // → Dùng orderBy("rating") để sắp xếp có ý nghĩa
                private void loadNewRecipes() {
                    if (newRecipesListener != null) newRecipesListener.remove();
                    newRecipesListener = db.collection("mon_an")
                            .orderBy("rating", Query.Direction.DESCENDING)
                            .limit(11)
                            .addSnapshotListener((snap, error) -> {
                                if (!isAdded() || snap == null) {
                                    if (error != null) Log.e(TAG, "NewRecipes err: " + error.getMessage());
                                    return;
                                }
                                listNew.clear();
                                for (QueryDocumentSnapshot doc : snap) {
                                    MonAn m = parseMonAn(doc);
                                    if (m != null) listNew.add(m);
                                }
                                adapterNew.notifyDataSetChanged();
                            });
                }

                private void loadHistoryRecipes() {
                    String uid = mAuth.getUid();
                    if (uid == null) return;

                    if (historyListener != null) historyListener.remove();

                    historyListener = db.collection("lich_su_xem")
                            .whereEqualTo("id_nguoi_dung", uid)
                            .orderBy("thoi_gian_xem", Query.Direction.DESCENDING)
                            .limit(11)
                            .addSnapshotListener((snap, error) -> {
                                if (!isAdded() || snap == null) {
                                    if (error != null) Log.e(TAG, "History err: " + error.getMessage());
                                    return;
                                }

                                List<String> ids = new ArrayList<>();
                                for (DocumentSnapshot d : snap) {
                                    String id = d.getString("id_mon_an");
                                    if (id != null) ids.add(id);
                                }

                                if (ids.isEmpty()) {
                                    listHistory.clear();
                                    adapterHistory.notifyDataSetChanged();
                                    return;
                                }

                                List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                                for (String id : ids)
                                    tasks.add(db.collection("mon_an").document(id).get());

                                Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                                    if (!isAdded()) return;
                                    listHistory.clear();
                                    for (Object r : results) {
                                        DocumentSnapshot d = (DocumentSnapshot) r;
                                        if (d.exists()) {
                                            MonAn m = parseMonAn(d);
                                            if (m != null) listHistory.add(m);
                                        }
                                    }
                                    // Giữ đúng thứ tự lịch sử xem
                                    List<MonAn> sorted = new ArrayList<>();
                                    for (String id : ids) {
                                        for (MonAn m : listHistory) {
                                            if (id.equals(m.getId_mon_an())) {
                                                sorted.add(m);
                                                break;
                                            }
                                        }
                                    }
                                    listHistory.clear();
                                    listHistory.addAll(sorted);
                                    adapterHistory.notifyDataSetChanged();
                                });
                            });
                }

                // =========================================================================
                // HELPERS
                // =========================================================================
                private void showAiLoading(boolean show) {
                    if (!isAdded()) return;
                    if (pbAiLoading != null) pbAiLoading.setVisibility(show ? View.VISIBLE : View.GONE);
                    if (rvCategory  != null) rvCategory.setVisibility(show ? View.GONE    : View.VISIBLE);
                }

                private void updateAiInsightsTitle(@Nullable String city) {
                    if (!isAdded() || tvAiInsights == null) return;
                    if (city != null && !city.isEmpty()) {
                        String c = VNCharacterUtils.removeAccents(city.toLowerCase());
                        if      (c.contains("da lat") || c.contains("lam dong"))
                            tvAiInsights.setText("📍 Đà Lạt mát mẻ - AI đang chọn món...");
                        else if (c.contains("ho chi minh"))
                            tvAiInsights.setText("📍 Sài Gòn - AI đang chọn món cho bạn...");
                        else if (c.contains("ha noi"))
                            tvAiInsights.setText("📍 Hà Nội - AI đang chọn món phù hợp...");
                        else
                            tvAiInsights.setText("📍 " + city + " - AI đang chọn món...");
                    } else {
                        tvAiInsights.setText("✨ AI đang phân tích để gợi ý món ăn...");
                    }
                }

                private String getMealTime(int h) {
                    if (h >= 5  && h < 10) return "bua sang";
                    if (h >= 10 && h < 14) return "bua trua";
                    if (h >= 14 && h < 17) return "bua xe/an vat";
                    if (h >= 17 && h < 21) return "bua toi";
                    return "an khuya";
                }

                private int calcRelevance(MonAn m, String q, String qnt) {
                    if (m.getTen_mon() == null) return 0;
                    int    s  = 0;
                    String n  = m.getTen_mon().toLowerCase();
                    String nt = VNCharacterUtils.removeAccents(n);
                    if (n.equals(q))         s += 1000; else if (nt.equals(qnt))     s += 900;
                    if (n.startsWith(q))     s += 500;  else if (nt.startsWith(qnt)) s += 450;
                    if (n.contains(q))       s += 200;  else if (nt.contains(qnt))   s += 180;
                    return s;
                }
            }
package com.example.mamacook.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import java.util.List;
import java.util.Locale;
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
    private ImageButton btnFilter;
    private ProgressBar pbAiLoading;
    private RecyclerView rvCategory, rvFeatured, rvNew, rvHistory;

    private MonAnAdapter adapterCategory, adapterFeatured, adapterNew, adapterHistory;

    private ListenerRegistration categoryListener, featuredListener,
            newRecipesListener, historyListener;

    private final List<MonAn>  listCategory            = new ArrayList<>();
    private final List<MonAn>  listFeatured            = new ArrayList<>();
    private final List<MonAn>  listNew                 = new ArrayList<>();
    private final List<MonAn>  listHistory             = new ArrayList<>();
    private final List<MonAn>  listFullCurrentCategory = new ArrayList<>();
    private final List<String> userPrefs               = new ArrayList<>();

    private TextView currentSelectedCategory;
    private String lastCategoryId    = "all";
    private String lastCategoryTitle = "Các món gợi ý";

    private final Handler  mainHandler     = new Handler(Looper.getMainLooper());
    private       Runnable aiTimeoutAction = null;
    private final Executor bgExecutor      = Executors.newCachedThreadPool();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db                  = FirebaseFirestore.getInstance();
        mAuth               = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        bindViews(view);
        setupRecyclerViews(view);
        setupCategoryButtons(view);
        setupSearchAction();
        btnFilter.setOnClickListener(v -> openFilterFragment());

        loadUserProfile();
        loadRecipesByCategory("all", lastCategoryTitle);
        fetchLocationAndWeather();

        loadFeaturedRecipes();
        loadNewRecipes();
        loadHistoryRecipes();

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
        pbAiLoading   = v.findViewById(R.id.pb_ai_loading);
        currentSelectedCategory = v.findViewById(R.id.btn_cat_all);
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

        if (aiState == AiState.RUNNING || aiState == AiState.DONE) {
            return;
        }

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
        if (aiState == AiState.RUNNING) {
            return;
        }
        cancelAiTimeout();
        aiState = AiState.IDLE;
    }

    private void fetchLocationAndWeather() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            aiWeather    = getWeatherByHour();
            aiLocation   = "Viet Nam";
            weatherReady = true;
            updateAiInsightsTitle(null);
            checkAndTriggerAi();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc == null) {
                        aiWeather    = getWeatherByHour();
                        aiLocation   = "Viet Nam";
                        weatherReady = true;
                        updateAiInsightsTitle(null);
                        checkAndTriggerAi();
                        return;
                    }
                    bgExecutor.execute(() ->
                            runGeocodeAndWeather(loc.getLatitude(), loc.getLongitude()));
                })
                .addOnFailureListener(e -> {
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
        } catch (Exception e) {
            Log.e(TAG, "Geocoder err: " + e.getMessage());
        }

        String weather = null;
        try {
            String url = "https://api.open-meteo.com/v1/forecast"
                    + "?latitude=" + lat + "&longitude=" + lon
                    + "&current_weather=true"
                    + "&hourly=precipitation_probability&forecast_days=1";
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br =
                         new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String ln;
                while ((ln = br.readLine()) != null) sb.append(ln);
            }
            JSONObject root = new JSONObject(sb.toString());
            JSONObject cw   = root.getJSONObject("current_weather");
            double temp = cw.getDouble("temperature");
            int    code = cw.getInt("weathercode");
            double wind = cw.getDouble("windspeed");
            int    rain = 0;
            try {
                rain = root.getJSONObject("hourly")
                        .getJSONArray("precipitation_probability")
                        .getInt(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
            } catch (Exception ignored) {}
            weather = buildWeatherDesc(temp, code, wind, rain);
        } catch (Exception e) {
            Log.e(TAG, "Weather API err: " + e.getMessage());
        }

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
        if      (code == 0)  cond = "troi quang, nang dep";
        else if (code <= 2)  cond = "it may, nang nhe";
        else if (code == 3)  cond = "nhieu may, troi u am";
        else if (code <= 49) cond = "co suong mu";
        else if (code <= 55) cond = "mua phun nhe";
        else if (code <= 65) cond = "dang mua" + (rain > 70 ? " to" : "");
        else if (code <= 75) cond = "co tuyet";
        else if (code <= 82) cond = "mua rao";
        else                 cond = "co giong sam set";

        String tdesc;
        if      (temp < 20) tdesc = "lanh ("     + (int) temp + "°C)";
        else if (temp < 28) tdesc = "mat me ("   + (int) temp + "°C)";
        else if (temp < 33) tdesc = "am ap ("    + (int) temp + "°C)";
        else                tdesc = "nong buc (" + (int) temp + "°C)";

        String wdesc = wind > 30 ? ", gio manh" : "";
        String rdesc = rain > 60 ? ", kha nang mua " + rain + "%" : "";
        return cond + ", " + tdesc + wdesc + rdesc;
    }

    private String getWeatherByHour() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h >= 5  && h < 9)  return "buoi sang som, troi mat de chiu";
        if (h >= 9  && h < 12) return "buoi sang, nang len dan";
        if (h >= 12 && h < 14) return "buoi trua, nang nong";
        if (h >= 14 && h < 17) return "buoi chieu, hay co mua rao";
        if (h >= 17 && h < 20) return "buoi chieu toi, mat dan";
        return "buoi toi muon, troi se lanh";
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            prefsReady = true;
            checkAndTriggerAi();
            return;
        }
        db.collection("nguoi_dung").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    if (doc.exists()) {
                        String name = doc.getString("ho_ten");
                        if (name != null && !name.isEmpty())
                            tvGreeting.setText("Xin chao " + name + "!");
                        @SuppressWarnings("unchecked")
                        List<String> prefs = (List<String>) doc.get("so_thich");
                        userPrefs.clear();
                        if (prefs != null) userPrefs.addAll(prefs);
                    }
                    prefsReady = true;
                    checkAndTriggerAi();
                })
                .addOnFailureListener(e -> {
                    prefsReady = true;
                    checkAndTriggerAi();
                });
    }

    private void callGemini(List<MonAn> candidates, String weather, String location) {
        if (candidates.isEmpty()) {
            onGeminiFailed(candidates, "empty candidates");
            return;
        }

        List<MonAn> preferred = new ArrayList<>();
        List<MonAn> others    = new ArrayList<>();
        for (MonAn m : candidates) {
            if (matchesPrefs(m)) preferred.add(m);
            else                 others.add(m);
        }
        Collections.shuffle(preferred);
        Collections.shuffle(others);

        List<MonAn> pool = new ArrayList<>();
        int pt = Math.min(preferred.size(), 15);
        int ot = Math.min(others.size(), 25 - pt);
        pool.addAll(preferred.subList(0, pt));
        pool.addAll(others.subList(0, ot));

        StringBuilder menu = new StringBuilder();
        for (MonAn m : pool) {
            if (m.getId_mon_an() == null || m.getTen_mon() == null) continue;
            menu.append("- ID:\"").append(m.getId_mon_an())
                    .append("\" Ten:\"").append(m.getTen_mon()).append("\"\n");
        }
        if (menu.length() == 0) { onGeminiFailed(candidates, "menu empty"); return; }

        int    hour     = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String mealTime = getMealTime(hour);

        String prompt =
                "Ban la chuyen gia am thuc Viet Nam. Chon DUNG 5 mon phu hop nhat.\n\n"
                        + "THONG TIN THUC TE:\n"
                        + "- Dia diem: " + location + "\n"
                        + "- Thoi tiet: " + weather + "\n"
                        + "- Thoi diem: " + mealTime + " (" + hour + "h)\n"
                        + "- So thich: " + (userPrefs.isEmpty() ? "chua ro"
                        : String.join(", ", userPrefs)) + "\n\n"
                        + "NGUYEN TAC:\n"
                        + "- Lanh/mua → lau, sup, chao, mon nuoc nong\n"
                        + "- Nong     → goi, mon nhe, do mat\n"
                        + "- Sang     → pho, bun, chao, xoi, banh mi\n"
                        + "- Trua/toi → com, mon man, day du dinh duong\n"
                        + "- Khuya    → mi, chao, an vat\n"
                        + "- Uu tien mon khop so thich (neu co)\n\n"
                        + "DANH SACH MON HOP LE (chi duoc chon trong danh sach nay):\n"
                        + menu
                        + "\nQUY TAC BAT BUOC:\n"
                        + "1. Chi dung ID co trong danh sach. KHONG tu tao ID.\n"
                        + "2. Tra ve JSON THUAN TUY. KHONG markdown. KHONG ```json.\n"
                        + "3. Greeting: 1 cau ~15 tu, de cap thoi tiet/dia diem that su.\n\n"
                        + "{\"greeting\":\"...\",\"ids\":[\"id1\",\"id2\",\"id3\",\"id4\",\"id5\"]}";

        GenerativeModel        gm  = new GenerativeModel("gemini-1.5-flash",
                getString(R.string.gemini_api_key));
        GenerativeModelFutures mdl = GenerativeModelFutures.from(gm);
        Content                cnt = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> fut = mdl.generateContent(cnt);

        final List<MonAn> fCandidates = candidates;
        final List<MonAn> fPreferred  = preferred;

        Futures.addCallback(fut, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse resp) {
                cancelAiTimeout();
                try {
                    String raw = resp.getText();
                    if (raw == null || raw.trim().isEmpty())
                        throw new Exception("empty response");

                    String json = raw.replaceAll("(?s)```json\\s*", "")
                            .replaceAll("(?s)```\\s*", "").trim();
                    int s = json.indexOf('{'), e = json.lastIndexOf('}');
                    if (s < 0 || e <= s) throw new Exception("no JSON found");
                    json = json.substring(s, e + 1);

                    JSONObject obj      = new JSONObject(json);
                    String     greeting = obj.optString("greeting", "");
                    JSONArray  idsArr   = obj.optJSONArray("ids");

                    List<MonAn> selected = new ArrayList<>();
                    if (idsArr != null) {
                        for (int i = 0; i < idsArr.length(); i++) {
                            String aiId = idsArr.getString(i).trim();
                            for (MonAn m : fCandidates) {
                                if (m.getId_mon_an() != null
                                        && m.getId_mon_an().trim().equalsIgnoreCase(aiId)) {
                                    selected.add(m);
                                    break;
                                }
                            }
                        }
                    }

                    if (selected.isEmpty()) {
                        List<MonAn> fb = fPreferred.isEmpty()
                                ? new ArrayList<>(fCandidates) : new ArrayList<>(fPreferred);
                        Collections.shuffle(fb);
                        selected.addAll(fb.subList(0, Math.min(5, fb.size())));
                    }

                    final List<MonAn> result   = selected;
                    final String      greetTxt = greeting;
                    if (!isAdded()) return;
                    mainHandler.post(() -> {
                        if (!isAdded() || !lastCategoryId.equals("all")) return;
                        onGeminiSuccess(result, greetTxt);
                    });

                } catch (Exception ex) {
                    onGeminiFailed(fCandidates, ex.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                cancelAiTimeout();
                onGeminiFailed(fCandidates, t.getMessage());
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void onGeminiSuccess(List<MonAn> selected, String greeting) {
        aiState = AiState.DONE;
        if (!greeting.isEmpty() && tvAiInsights != null)
            tvAiInsights.setText("✨ " + greeting);
        listCategory.clear();
        listCategory.addAll(selected);
        if (adapterCategory != null) adapterCategory.notifyDataSetChanged();
        showAiLoading(false);
    }

    private void onGeminiFailed(List<MonAn> candidates, String reason) {
        if (!isAdded()) return;
        mainHandler.post(() -> {
            if (!isAdded() || !lastCategoryId.equals("all")) return;
            aiState = AiState.DONE;

            List<MonAn> fb = new ArrayList<>();
            for (MonAn m : candidates) { if (matchesPrefs(m)) fb.add(m); }
            if (fb.size() < 5) {
                for (MonAn m : candidates) { if (!fb.contains(m)) fb.add(m); }
            }
            Collections.shuffle(fb);
            List<MonAn> result = new ArrayList<>(fb.subList(0, Math.min(5, fb.size())));

            if (tvAiInsights != null)
                tvAiInsights.setText("Một số món bạn có thể thích hôm nay");
            listCategory.clear();
            listCategory.addAll(result);
            if (adapterCategory != null) adapterCategory.notifyDataSetChanged();
            showAiLoading(false);
        });
    }

    private boolean matchesPrefs(MonAn m) {
        if (userPrefs.isEmpty()) return false;
        String ten = m.getTen_mon()     != null
                ? VNCharacterUtils.removeAccents(m.getTen_mon().toLowerCase())     : "";
        String cat = m.getId_danh_muc() != null
                ? VNCharacterUtils.removeAccents(m.getId_danh_muc().toLowerCase()) : "";
        for (String p : userPrefs) {
            if (p == null || p.isEmpty()) continue;
            String np = VNCharacterUtils.removeAccents(p.trim().toLowerCase());
            if (ten.contains(np) || cat.contains(np)) return true;
        }
        return false;
    }

    private void scheduleAiTimeout() {
        cancelAiTimeout();
        aiTimeoutAction = () -> {
            if (!isAdded()) return;
            onGeminiFailed(new ArrayList<>(listFullCurrentCategory), "timeout");
        };
        mainHandler.postDelayed(aiTimeoutAction, AI_TIMEOUT_MS);
    }

    private void cancelAiTimeout() {
        if (aiTimeoutAction != null) {
            mainHandler.removeCallbacks(aiTimeoutAction);
            aiTimeoutAction = null;
        }
    }

    private void loadRecipesByCategory(String categoryId, String title) {
        lastCategoryId    = categoryId;
        lastCategoryTitle = title;

        if (categoryListener != null) categoryListener.remove();

        Query q = categoryId.equals("all")
                ? db.collection("mon_an")
                : db.collection("mon_an").whereEqualTo("id_danh_muc", categoryId);

        categoryListener = q.limit(100).addSnapshotListener((snap, error) -> {
            if (!isAdded() || snap == null) return;

            listFullCurrentCategory.clear();
            for (QueryDocumentSnapshot doc : snap) {
                MonAn m = parseMonAn(doc);
                if (m != null) listFullCurrentCategory.add(m);
            }

            if (categoryId.equals("all")) {
                recipesReady = true;
                checkAndTriggerAi();
            } else {
                if (tvAiInsights != null) {
                    tvAiInsights.setText("Danh mục món " + title);
                }
                listCategory.clear();
                listCategory.addAll(listFullCurrentCategory);
                if (adapterCategory != null) adapterCategory.notifyDataSetChanged();
                showAiLoading(false);
            }
        });
    }

    private void setupCategoryButtons(View view) {
        int[]    ids    = {R.id.btn_cat_all, R.id.btn_cat_man, R.id.btn_cat_canh,
                R.id.btn_cat_chay, R.id.btn_cat_vat, R.id.btn_cat_lau};
        String[] catIds = {"all", "mon_man", "mon_canh", "mon_chay", "an_vat", "mon_lau"};
        String[] titles = {"Các món gợi ý", "Món mặn", "Món canh",
                "Món chay", "Ăn vặt", "Món lẩu"};

        for (int i = 0; i < ids.length; i++) {
            final int idx = i;
            TextView btn = view.findViewById(ids[i]);
            if (btn == null) continue;
            btn.setOnClickListener(v -> {
                if (currentSelectedCategory != null) {
                    currentSelectedCategory.setBackgroundResource(R.drawable.bg_input_field);
                    currentSelectedCategory.setTextColor(
                            android.graphics.Color.parseColor("#555555"));
                }
                btn.setBackgroundResource(R.drawable.bg_register_button);
                btn.setTextColor(android.graphics.Color.WHITE);
                currentSelectedCategory = btn;
                etSearch.setText("");

                resetAi();
                recipesReady = false;
                loadRecipesByCategory(catIds[idx], titles[idx]);
            });
        }
    }

    private void setupSearchAction() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String q = etSearch.getText().toString().trim();
                hideKeyboard();
                if (!q.isEmpty()) {
                    openSearchResult(q);
                }
                return true;
            }
            return false;
        });
    }

    private void openSearchResult(String query) {
        SearchResultFragment fragment = SearchResultFragment.newInstance(query);
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .add(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openFilterFragment() {
        FilterFragment fragment = new FilterFragment();
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .add(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void hideKeyboard() {
        if (getActivity() == null) return;
        InputMethodManager imm = (InputMethodManager)
                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }

    private void setupRecyclerViews(View view) {
        rvCategory = view.findViewById(R.id.rv_category_dishes);
        rvFeatured = view.findViewById(R.id.rv_featured);
        rvNew      = view.findViewById(R.id.rv_new_recipes);
        rvHistory  = view.findViewById(R.id.rv_history);

        int H = LinearLayoutManager.HORIZONTAL;
        rvCategory.setLayoutManager(new LinearLayoutManager(getContext(), H, false));
        rvFeatured.setLayoutManager(new LinearLayoutManager(getContext(), H, false));
        rvNew     .setLayoutManager(new LinearLayoutManager(getContext(), H, false));
        rvHistory .setLayoutManager(new LinearLayoutManager(getContext(), H, false));

        adapterCategory = new MonAnAdapter(listCategory);
        adapterFeatured = new MonAnAdapter(listFeatured);
        adapterFeatured.setSectionInfo("NOI_BAT", "");
        adapterNew      = new MonAnAdapter(listNew);
        adapterNew.setSectionInfo("MOI", "");
        adapterHistory  = new MonAnAdapter(listHistory);
        adapterHistory.setSectionInfo("LICH_SU", "");

        rvCategory.setAdapter(adapterCategory);
        rvFeatured.setAdapter(adapterFeatured);
        rvNew     .setAdapter(adapterNew);
        rvHistory .setAdapter(adapterHistory);
    }

    private void loadFeaturedRecipes() {
        if (featuredListener != null) featuredListener.remove();
        featuredListener = db.collection("mon_an")
                .orderBy("luot_xem", Query.Direction.DESCENDING)
                .limit(11)
                .addSnapshotListener((snap, error) -> {
                    if (!isAdded() || snap == null) return;
                    listFeatured.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        MonAn m = parseMonAn(doc);
                        if (m != null) listFeatured.add(m);
                    }
                    adapterFeatured.notifyDataSetChanged();
                });
    }

    private void loadNewRecipes() {
        if (newRecipesListener != null) newRecipesListener.remove();
        newRecipesListener = db.collection("mon_an")
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(11)
                .addSnapshotListener((snap, error) -> {
                    if (!isAdded() || snap == null) return;
                    listNew.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        MonAn m = parseMonAn(doc);
                        if (m != null) listNew.add(m);
                    }
                    adapterNew.notifyDataSetChanged();
                });
    }

    private void loadHistoryRecipes() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        if (historyListener != null) historyListener.remove();

        historyListener = db.collection("lich_su_xem")
                .whereEqualTo("id_nguoi_dung", uid)
                .orderBy("thoi_gian_xem", Query.Direction.DESCENDING)
                .limit(11)
                .addSnapshotListener((snap, error) -> {
                    if (!isAdded() || snap == null) return;

                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot d : snap) {
                        String id = d.getString("id_mon_an");
                        if (id != null) ids.add(id);
                    }

                    if (ids.isEmpty()) {
                        listHistory.clear();
                        adapterHistory.notifyDataSetChanged();
                        return;
                    }

                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                    for (String id : ids)
                        tasks.add(db.collection("mon_an").document(id).get());

                    Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                        if (!isAdded()) return;
                        listHistory.clear();
                        for (Object r : results) {
                            DocumentSnapshot d = (DocumentSnapshot) r;
                            if (d.exists()) {
                                MonAn m = parseMonAn(d);
                                if (m != null) listHistory.add(m);
                            }
                        }
                        List<MonAn> sorted = new ArrayList<>();
                        for (String id : ids) {
                            for (MonAn m : listHistory) {
                                if (id.equals(m.getId_mon_an())) {
                                    sorted.add(m);
                                    break;
                                }
                            }
                        }
                        listHistory.clear();
                        listHistory.addAll(sorted);
                        adapterHistory.notifyDataSetChanged();
                    });
                });
    }

    private void showAiLoading(boolean show) {
        if (!isAdded()) return;
        if (pbAiLoading != null) pbAiLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        if (rvCategory  != null) rvCategory.setVisibility(show ? View.GONE    : View.VISIBLE);
    }

    private void updateAiInsightsTitle(@Nullable String city) {
        if (!isAdded() || tvAiInsights == null || !lastCategoryId.equals("all")) return;
        if (city != null && !city.isEmpty()) {
            String c = VNCharacterUtils.removeAccents(city.toLowerCase());
            if      (c.contains("da lat") || c.contains("lam dong"))
                tvAiInsights.setText("📍 Đà Lạt mát mẻ - AI đang chọn món...");
            else if (c.contains("ho chi minh"))
                tvAiInsights.setText("📍 Sài Gòn - AI đang chọn món cho bạn...");
            else if (c.contains("ha noi"))
                tvAiInsights.setText("📍 Hà Nội - AI đang chọn món phù hợp...");
            else
                tvAiInsights.setText("📍 " + city + " - AI đang chọn món...");
        } else {
            tvAiInsights.setText("✨ AI đang phân tích để gợi ý món ăn...");
        }
    }

    private String getMealTime(int h) {
        if (h >= 5  && h < 10) return "bua sang";
        if (h >= 10 && h < 14) return "bua trua";
        if (h >= 14 && h < 17) return "bua xe/an vat";
        if (h >= 17 && h < 21) return "bua toi";
        return "an khuya";
    }
}