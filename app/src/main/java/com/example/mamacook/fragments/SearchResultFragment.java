package com.example.mamacook.fragments;

import android.content.Context;
import android.os.Bundle;
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
        String[] words = noTone.split("\\s+");

        db.collection("mon_an")
                .whereArrayContains("tu_khoa_tim_kiem", words[0])
                .get()
                .addOnSuccessListener(snap -> {
                    resultList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        MonAn m = doc.toObject(MonAn.class);
                        if (m == null) continue;
                        if (m.getId_mon_an() == null || m.getId_mon_an().isEmpty()) {
                            m.setId_mon_an(doc.getId());
                        }
                        
                        List<String> kw = m.getTu_khoa_tim_kiem();
                        if (kw == null) continue;
                        
                        boolean ok = true;
                        for (String w : words) {
                            if (!kw.contains(w)) {
                                ok = false;
                                break;
                            }
                        }
                        if (ok) resultList.add(m);
                    }
                    
                    resultList.sort((a, b) -> Integer.compare(
                            calcRelevance(b, text, noTone),
                            calcRelevance(a, text, noTone)));

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
                });
    }

    private int calcRelevance(MonAn m, String q, String qnt) {
        if (m.getTen_mon() == null) return 0;
        int s = 0;
        String n = m.getTen_mon().toLowerCase();
        String nt = VNCharacterUtils.removeAccents(n);
        if (n.equals(q)) s += 1000; else if (nt.equals(qnt)) s += 900;
        if (n.startsWith(q)) s += 500; else if (nt.startsWith(qnt)) s += 450;
        if (n.contains(q)) s += 200; else if (nt.contains(qnt)) s += 180;
        return s;
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }
}