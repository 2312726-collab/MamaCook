package com.example.mamacook.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import com.example.mamacook.utils.VNCharacterUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchResultFragment extends Fragment {

    private static final String ARG_QUERY = "search_query";

    private String initialQuery;
    private EditText etSearch;
    private ImageButton btnBack;
    private TextView tvResultTitle;
    private RecyclerView rvResults;
    private ProgressBar pbLoading;
    private LinearLayout layoutEmpty;

    private MonAnVerticalAdapter adapter;
    private final List<MonAn> resultList = new ArrayList<>();
    private FirebaseFirestore db;

    public static SearchResultFragment newInstance(String query) {
        SearchResultFragment fragment = new SearchResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            initialQuery = getArguments().getString(ARG_QUERY);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_result, container, false);

        bindViews(view);
        setupRecyclerView();
        setupListeners();

        if (initialQuery != null && !initialQuery.isEmpty()) {
            etSearch.setText(initialQuery);
            performSearch(initialQuery);
        }

        return view;
    }

    private void bindViews(View v) {
        etSearch = v.findViewById(R.id.et_search_result);
        btnBack = v.findViewById(R.id.btn_back);
        tvResultTitle = v.findViewById(R.id.tv_result_title);
        rvResults = v.findViewById(R.id.rv_search_results);
        pbLoading = v.findViewById(R.id.pb_loading);
        layoutEmpty = v.findViewById(R.id.layout_empty_state);
    }

    private void setupRecyclerView() {
        adapter = new MonAnVerticalAdapter(resultList);
        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvResults.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                requireActivity().onBackPressed();
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String q = etSearch.getText().toString().trim();
                if (!q.isEmpty()) {
                    hideKeyboard();
                    performSearch(q);
                }
                return true;
            }
            return false;
        });
    }

    private void performSearch(String text) {
        pbLoading.setVisibility(View.VISIBLE);
        rvResults.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        tvResultTitle.setText("Đang tìm: '" + text + "'");

        String noTone = VNCharacterUtils.removeAccents(text.toLowerCase());
        String[] wordsArray = noTone.split("\\s+");
        List<String> queryWords = new ArrayList<>();
        for (String w : wordsArray) {
            if (!w.trim().isEmpty()) {
                queryWords.add(w);
            }
        }

        if (queryWords.isEmpty()) {
            pbLoading.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            return;
        }

        // Chỉ lấy tối đa 10 từ đầu tiên vì giới hạn của whereArrayContainsAny
        List<String> limitedWords = queryWords.subList(0, Math.min(10, queryWords.size()));

        db.collection("mon_an")
                .whereArrayContainsAny("tu_khoa_tim_kiem", limitedWords)
                .get()
                .addOnSuccessListener(snap -> {
                    resultList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        MonAn m = doc.toObject(MonAn.class);
                        if (m == null || m.getTen_mon() == null) continue;
                        
                        List<String> keywords = m.getTu_khoa_tim_kiem();
                        if (keywords == null) continue;

                        String currentTitleNoTone = VNCharacterUtils.removeAccents(m.getTen_mon().toLowerCase());

                        // LOGIC THÔNG MINH CẢI TIẾN:
                        // 1. Kiểm tra xem toàn bộ cụm từ tìm kiếm có xuất hiện trong Tên món không
                        boolean matchInTitle = currentTitleNoTone.contains(noTone);
                        
                        // 2. Kiểm tra trong danh sách nguyên liệu (Tìm theo cụm từ)
                        boolean matchInIngredients = false;
                        if (m.getDanh_sach_nguyen_lieu() != null) {
                            for (MonAn.ChiTietNguyenLieu nl : m.getDanh_sach_nguyen_lieu()) {
                                if (nl.ten_nguyen_lieu == null) continue;
                                String nlName = VNCharacterUtils.removeAccents(nl.ten_nguyen_lieu.toLowerCase());
                                if (nlName.contains(noTone)) {
                                    matchInIngredients = true;
                                    break;
                                }
                            }
                        }

                        // 3. Nếu không khớp cụm từ, mới kiểm tra khớp từng từ đơn trong mảng keywords
                        boolean isMatchAllWords = true;
                        for (String word : queryWords) {
                            if (!keywords.contains(word)) {
                                isMatchAllWords = false;
                                break;
                            }
                        }

                        // Chấp nhận nếu khớp cụm từ trong Tên/Nguyên liệu HOẶC khớp toàn bộ từ đơn
                        if (matchInTitle || matchInIngredients || isMatchAllWords) {
                            if (m.getId_mon_an() == null || m.getId_mon_an().isEmpty()) {
                                m.setId_mon_an(doc.getId());
                            }
                            resultList.add(m);
                        }
                    }
                    
                    // Sắp xếp theo độ liên quan cải tiến
                    resultList.sort((a, b) -> Integer.compare(
                            calcRelevance(b, text, noTone, wordsArray),
                            calcRelevance(a, text, noTone, wordsArray)));

                    pbLoading.setVisibility(View.GONE);
                    if (resultList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        tvResultTitle.setText("Không tìm thấy kết quả cho: '" + text + "'");
                    } else {
                        rvResults.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                        tvResultTitle.setText("Đã tìm thấy " + resultList.size() + " món ăn");
                    }
                })
                .addOnFailureListener(e -> {
                    pbLoading.setVisibility(View.GONE);
                    layoutEmpty.setVisibility(View.VISIBLE);
                    Log.e("SEARCH_ERROR", e.getMessage());
                });
    }

    private int calcRelevance(MonAn m, String query, String queryNoTone, String[] words) {
        if (m.getTen_mon() == null) return 0;
        int score = 0;
        String title = m.getTen_mon().toLowerCase();
        String titleNoTone = VNCharacterUtils.removeAccents(title);

        // 1. Ưu tiên khớp chính xác tuyệt đối (Cả dấu và không dấu)
        if (title.equals(query.toLowerCase())) score += 10000;
        else if (titleNoTone.equals(queryNoTone)) score += 8000;

        // 2. Ưu tiên khớp cụm từ dài hơn (Chuỗi con liên tục)
        if (title.contains(query.toLowerCase())) {
            score += (2000 + query.length() * 10);
        } else if (titleNoTone.contains(queryNoTone)) {
            score += (1500 + queryNoTone.length() * 10);
        }

        // 3. Ưu tiên theo số lượng từ khóa khớp (Càng nhiều từ trong query xuất hiện càng tốt)
        List<String> keywords = m.getTu_khoa_tim_kiem();
        if (keywords != null) {
            int matchCount = 0;
            for (String w : words) {
                if (keywords.contains(w)) {
                    matchCount++;
                    score += 500; // Cộng điểm cho mỗi từ khớp
                }
            }
            // Thưởng lớn nếu khớp toàn bộ các từ trong câu tìm kiếm
            if (matchCount >= words.length) {
                score += 3000;
            }
        }

        // 4. Ưu tiên món ăn có vị trí từ khóa ở đầu tên
        if (title.startsWith(query.toLowerCase()) || titleNoTone.startsWith(queryNoTone)) {
            score += 1000;
        }

        return score;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }
}