package com.example.mamacook.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.activities.ChuanBiNauActivity;
import com.example.mamacook.activities.GioHangActivity;
import com.example.mamacook.adapters.CookingPlanHorizontalAdapter;
import com.example.mamacook.models.CookingPlan;
import com.example.mamacook.models.MonAn;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.functions.FirebaseFunctions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScheduleFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView tvTotalDishes, tvShoppingProgress, tvWeekTitle;
    private ImageButton btnAiSuggest;
    private RecyclerView rvSang, rvTrua, rvToi, rvDays;
    private CookingPlanHorizontalAdapter adapterSang, adapterTrua, adapterToi;
    private DayAdapter dayAdapter;
    private List<CookingPlan> listSang = new ArrayList<>();
    private List<CookingPlan> listTrua = new ArrayList<>();
    private List<CookingPlan> listToi = new ArrayList<>();
    private List<DayItem> dayList = new ArrayList<>();
    private String selectedDate;
    private int currentWeekOffset = 0;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private ListenerRegistration planListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvTotalDishes = view.findViewById(R.id.tv_total_dishes);
        tvShoppingProgress = view.findViewById(R.id.tv_shopping_progress);
        tvWeekTitle = view.findViewById(R.id.tv_week_title);
        btnAiSuggest = view.findViewById(R.id.btn_ai_suggest);
        rvDays = view.findViewById(R.id.rv_days_of_week);

        selectedDate = sdf.format(new Date());

        setupRecyclerViews(view);
        setupDaySelector();
        cleanupOldCookingPlans();
        loadCookingPlans();

        view.findViewById(R.id.btn_shopping_list_schedule).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GioHangActivity.class);
            intent.putExtra("SELECTED_DATE", selectedDate);
            startActivity(intent);
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        view.findViewById(R.id.btn_ready_to_cook_schedule).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChuanBiNauActivity.class);
            intent.putExtra("SELECTED_DATE", selectedDate);
            startActivity(intent);
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        btnAiSuggest.setOnClickListener(v -> {
            showAiOptionDialog();
        });

        // Xử lý nút thêm món (+) cho từng buổi
        view.findViewById(R.id.btn_add_sang).setOnClickListener(v -> openFilterFragment("Sang"));
        view.findViewById(R.id.btn_add_trua).setOnClickListener(v -> openFilterFragment("Trua"));
        view.findViewById(R.id.btn_add_toi).setOnClickListener(v -> openFilterFragment("Toi"));

        return view;
    }

    private void openFilterFragment(String mealType) {
        FilterFragment filterFragment = new FilterFragment();
        Bundle args = new Bundle();
        args.putString("PRE_SELECTED_DATE", selectedDate);
        args.putString("PRE_SELECTED_MEAL", mealType);
        filterFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_up, R.anim.stay_still, R.anim.stay_still, R.anim.slide_down)
                .add(R.id.fragment_container, filterFragment)
                .hide(this)
                .addToBackStack(null)
                .commit();
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

    private void setupDaySelector() {
        rvDays.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        loadDaysOfWeek();
        dayAdapter = new DayAdapter(dayList, day -> {
            selectedDate = day.date;
            for (DayItem item : dayList) {
                item.isSelected = !item.isNav && item.date.equals(selectedDate);
            }
            dayAdapter.notifyDataSetChanged();
            loadCookingPlans(); // Tải lại dữ liệu cho ngày mới
        }, this);
        rvDays.setAdapter(dayAdapter);
    }

    private void changeWeek(int offset) {
        currentWeekOffset += offset;
        if (currentWeekOffset < 0) currentWeekOffset = 0;
        if (currentWeekOffset > 1) currentWeekOffset = 1;
        
        // Cập nhật tiêu đề tuần
        if (tvWeekTitle != null) {
            tvWeekTitle.setText(currentWeekOffset == 0 ? "Tuần này" : "Tuần kế tiếp");
        }

        loadDaysOfWeek();
        
        if (!dayList.isEmpty()) {
            // Khi chuyển tuần, chọn ngày đầu tiên hợp lệ (không phải nút điều hướng)
            for (DayItem item : dayList) {
                if (!item.isNav) {
                    selectedDate = item.date;
                    item.isSelected = true;
                    break;
                }
            }
        }
        
        dayAdapter.notifyDataSetChanged();
        loadCookingPlans();
    }

    private void loadDaysOfWeek() {
        dayList.clear();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek()); 
        
        // Điều chỉnh để bắt đầu từ Thứ 2 nếu cần
        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        } else if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        }

        // Áp dụng offset tuần
        cal.add(Calendar.WEEK_OF_YEAR, currentWeekOffset);

        // Nếu ở tuần kế tiếp, thêm nút "Về tuần này" ở đầu
        if (currentWeekOffset == 1) {
            DayItem backNav = new DayItem();
            backNav.isNav = true;
            backNav.navType = -1;
            dayList.add(backNav);
        }

        SimpleDateFormat dayNameSdf = new SimpleDateFormat("EEE", new Locale("vi", "VN"));
        SimpleDateFormat dayNumberSdf = new SimpleDateFormat("dd", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            DayItem item = new DayItem();
            item.date = sdf.format(cal.getTime());
            item.dayName = dayNameSdf.format(cal.getTime());
            item.dayNumber = dayNumberSdf.format(cal.getTime());
            item.isSelected = item.date.equals(selectedDate);
            dayList.add(item);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Nếu ở tuần hiện tại, thêm nút "Tuần sau" ở cuối
        if (currentWeekOffset == 0) {
            DayItem nextNav = new DayItem();
            nextNav.isNav = true;
            nextNav.navType = 1;
            dayList.add(nextNav);
        }
    }

    private void loadCookingPlans() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        if (planListener != null) planListener.remove();

        planListener = db.collection("ke_hoach_nau_an")
                .whereEqualTo("id_nguoi_dung", user.getUid())
                .whereEqualTo("ngay_chi_tiet", selectedDate)
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
                        plan.setId_plan(doc.getId());

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

    private void showAiOptionDialog() {
        String[] options = {
                "Cân bằng dinh dưỡng",
                "Món ăn thanh đạm (ít dầu mỡ)",
                "Tiết kiệm thời gian (dưới 30 phút)",
                "Giàu Protein cho sức khỏe",
                "Thực đơn món Việt truyền thống",
                "Yêu cầu khác..."
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Hôm nay bạn muốn ăn gì?")
                .setItems(options, (dialog, which) -> {
                    if (which == options.length - 1) {
                        showCustomNoteDialog();
                    } else {
                        askAiForSchedule("Gợi ý thực đơn: " + options[which]);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showCustomNoteDialog() {
        EditText input = new EditText(getContext());
        input.setHint("Ví dụ: Món cay, nhiều rau, ít calo...");
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(requireContext())
                .setTitle("Nhập yêu cầu của bạn")
                .setView(input)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String note = input.getText().toString().trim();
                    if (!note.isEmpty()) {
                        askAiForSchedule(note);
                    }
                })
                .setNegativeButton("Quay lại", (dialog, which) -> showAiOptionDialog())
                .show();
    }

    private void askAiForSchedule(String userNote) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        btnAiSuggest.setEnabled(false); // Khóa nút tránh spam
        Toast.makeText(getContext(), "AI đang nghiên cứu thực đơn cho bạn...", Toast.LENGTH_SHORT).show();

        // 1. Lấy danh sách món ăn từ Firestore để AI có dữ liệu chọn
        db.collection("mon_an").limit(40).get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Map<String, String>> dishBrief = new ArrayList<>();
            List<MonAn> fullDishes = new ArrayList<>();

            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                MonAn m = doc.toObject(MonAn.class);
                m.setId_mon_an(doc.getId());
                fullDishes.add(m);

                Map<String, String> item = new HashMap<>();
                item.put("id", m.getId_mon_an());
                item.put("name", m.getTen_mon());
                dishBrief.add(item);
            }

            // 2. Gọi Cloud Function
            callGenerateScheduleFunction(dishBrief, fullDishes, userNote);
        });
    }

    private void callGenerateScheduleFunction(List<Map<String, String>> dishBrief, List<MonAn> fullDishes, String userNote) {
        FirebaseFunctions mFunctions = FirebaseFunctions.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("availableDishes", dishBrief);
        data.put("userNote", userNote);

        mFunctions.getHttpsCallable("generateDailySchedule")
                .call(data)
                .addOnSuccessListener(result -> {
                    btnAiSuggest.setEnabled(true); // Mở lại nút
                    Map<String, Object> res = (Map<String, Object>) result.getData();
                    if (res == null) return;
                    
                    String reason = (String) res.get("reason");
                    Map<String, String> schedule = (Map<String, String>) res.get("schedule");

                    if (schedule != null && reason != null) {
                        showAiPreviewDialog(reason, schedule, fullDishes);
                    } else {
                        Toast.makeText(getContext(), "AI không tìm thấy món phù hợp, hãy thử lại!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnAiSuggest.setEnabled(true); // Mở lại nút khi gặp lỗi
                    Log.e("AI_ERROR", e.getMessage());
                    Toast.makeText(getContext(), "Lỗi kết nối AI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAiPreviewDialog(String reason, Map<String, String> schedule, List<MonAn> fullDishes) {
        String sangId = schedule.get("Sang");
        String truaId = schedule.get("Trua");
        String toiId = schedule.get("Toi");

        String monSang = getDishNameById(sangId, fullDishes);
        String monTrua = getDishNameById(truaId, fullDishes);
        String monToi = getDishNameById(toiId, fullDishes);

        String message = "💡 " + reason + "\n\n" +
                "Sáng: " + monSang + "\n" +
                "Trưa: " + monTrua + "\n" +
                "Tối: " + monToi;

        new AlertDialog.Builder(requireContext())
                .setTitle("Thực đơn AI đề xuất")
                .setMessage(message)
                .setPositiveButton("Đồng ý & Lưu", (dialog, which) -> {
                    saveAiScheduleToFirestore(schedule, fullDishes);
                    Toast.makeText(getContext(), "Đã cập nhật lịch trình của bạn!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Thử lại", (dialog, which) -> showAiOptionDialog())
                .setNeutralButton("Hủy", null)
                .show();
    }

    private String getDishNameById(String id, List<MonAn> fullDishes) {
        if (id == null) return "Không tìm thấy món";
        for (MonAn m : fullDishes) {
            if (m.getId_mon_an().equals(id)) return m.getTen_mon();
        }
        return "Món mới";
    }

    private void saveAiScheduleToFirestore(Map<String, String> schedule, List<MonAn> fullDishes) {
        String uid = mAuth.getUid();
        if (uid == null) return;

        WriteBatch batch = db.batch();
        Timestamp now = Timestamp.now();
        String dateStr = selectedDate; // Lưu cho ngày đang được chọn

        // Duyệt qua 3 buổi
        String[] buois = {"Sang", "Trua", "Toi"};
        for (String buoi : buois) {
            String targetId = schedule.get(buoi);
            if (targetId == null) continue;

            // Tìm thông tin chi tiết món ăn trong list đã load
            for (MonAn m : fullDishes) {
                if (m.getId_mon_an().equals(targetId)) {
                    CookingPlan plan = new CookingPlan();
                    plan.setId_nguoi_dung(uid);
                    plan.setId_mon_an(m.getId_mon_an());
                    plan.setTen_mon(m.getTen_mon());
                    plan.setHinh_anh(m.getHinh_anh());
                    plan.setNgay_lap_ke_hoach(now);
                    plan.setNgay_chi_tiet(dateStr);
                    plan.setTrang_thai("dang_di_cho");
                    plan.setBuoi(buoi);

                    // Chuyển đổi nguyên liệu
                    List<CookingPlan.IngredientItem> items = new ArrayList<>();
                    if (m.getDanh_sach_nguyen_lieu() != null) {
                        for (MonAn.ChiTietNguyenLieu nl : m.getDanh_sach_nguyen_lieu()) {
                            CookingPlan.IngredientItem item = new CookingPlan.IngredientItem();
                            item.setTen_nguyen_lieu(nl.ten_nguyen_lieu);
                            item.setSo_luong((int) nl.so_luong); // Ép kiểu từ double sang int
                            item.setDon_vi(nl.don_vi);
                            item.setDa_mua(false);
                            items.add(item);
                        }
                    }
                    plan.setDanh_sach_nguyen_lieu(items);

                    batch.set(db.collection("ke_hoach_nau_an").document(), plan);
                    break;
                }
            }
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            Log.d("AI_SAVE", "Đã lưu thực đơn AI vào lịch trình");
        });
    }

    private void cleanupOldCookingPlans() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Xóa các kế hoạch của tuần trước
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        Timestamp threshold = new Timestamp(cal.getTime());

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
                        Log.d("Cleanup", "Đã xóa các kế hoạch cũ hơn 7 ngày");
                    }
                });
    }

    // --- Inner Classes for Day Selector ---
    private static class DayItem {
        String date;
        String dayName;
        String dayNumber;
        boolean isSelected;
        boolean isNav = false;
        int navType = 0; // -1: Back, 1: Next
    }

    private interface OnDayClickListener {
        void onDayClick(DayItem day);
    }

    private static class DayAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_DAY = 1;
        private static final int TYPE_NAV = 2;

        private List<DayItem> list;
        private OnDayClickListener listener;
        private ScheduleFragment fragment; // Thêm reference để gọi changeWeek

        public DayAdapter(List<DayItem> list, OnDayClickListener listener, ScheduleFragment fragment) {
            this.list = list;
            this.listener = listener;
            this.fragment = fragment;
        }

        @Override
        public int getItemViewType(int position) {
            return list.get(position).isNav ? TYPE_NAV : TYPE_DAY;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_NAV) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_week_nav, parent, false);
                return new NavViewHolder(v);
            }
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_selector, parent, false);
            return new DayViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            DayItem item = list.get(position);

            if (holder instanceof DayViewHolder) {
                DayViewHolder dHolder = (DayViewHolder) holder;
                dHolder.tvName.setText(item.dayName);
                dHolder.tvNumber.setText(item.dayNumber);

                if (item.isSelected) {
                    dHolder.card.setCardBackgroundColor(Color.parseColor("#9B4F4F"));
                    dHolder.tvName.setTextColor(Color.WHITE);
                    dHolder.tvNumber.setTextColor(Color.WHITE);
                } else {
                    dHolder.card.setCardBackgroundColor(Color.WHITE);
                    dHolder.tvName.setTextColor(Color.GRAY);
                    dHolder.tvNumber.setTextColor(Color.BLACK);
                }

                dHolder.itemView.setOnClickListener(v -> listener.onDayClick(item));
            } else if (holder instanceof NavViewHolder) {
                NavViewHolder nHolder = (NavViewHolder) holder;
                ImageView ivNav = nHolder.itemView.findViewById(R.id.iv_nav_icon);
                
                if (item.navType == 1) {
                    if (ivNav != null) {
                        ivNav.setImageResource(R.drawable.ic_arrow_right);
                        ivNav.setRotation(0);
                    }
                } else {
                    if (ivNav != null) {
                        ivNav.setImageResource(R.drawable.ic_arrow_right);
                        ivNav.setRotation(180);
                    }
                }

                nHolder.itemView.setOnClickListener(v -> {
                    if (fragment != null) fragment.changeWeek(item.navType);
                });
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class DayViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvNumber;
            androidx.cardview.widget.CardView card;
            public DayViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_day_name);
                tvNumber = itemView.findViewById(R.id.tv_day_number);
                card = itemView.findViewById(R.id.card_day);
            }
        }

        static class NavViewHolder extends RecyclerView.ViewHolder {
            public NavViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (planListener != null) planListener.remove();
    }
}
