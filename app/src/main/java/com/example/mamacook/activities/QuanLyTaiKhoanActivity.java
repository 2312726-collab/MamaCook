package com.example.mamacook.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.adapters.UserAdminAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class QuanLyTaiKhoanActivity extends AppCompatActivity {

    private EditText edtSearchUser;
    private RecyclerView rvUsers;

    private Button btnTatCa;
    private Button btnViPham;

    private UserAdminAdapter adapter;
    private FirebaseFirestore db;

    private final List<DocumentSnapshot> fullList = new ArrayList<>();
    private final List<DocumentSnapshot> filteredList = new ArrayList<>();

    private boolean dangLocViPham = false;
    private String keywordHienTai = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quan_ly_tai_khoan);

        db = FirebaseFirestore.getInstance();

        edtSearchUser = findViewById(R.id.edt_search_user);
        rvUsers = findViewById(R.id.rv_users_admin);

        btnTatCa = findViewById(R.id.btn_tat_ca_tai_khoan);
        btnViPham = findViewById(R.id.btn_tai_khoan_vi_pham);

        adapter = new UserAdminAdapter(this, filteredList);

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        setupSearch();
        setupButtons();
        loadUsers();
    }

    private void loadUsers() {
        db.collection("nguoi_dung")
                .addSnapshotListener((query, error) -> {

                    if (error != null || query == null) {
                        return;
                    }

                    fullList.clear();
                    fullList.addAll(query.getDocuments());

                    if (dangLocViPham) {
                        hienThiTaiKhoanViPham(keywordHienTai);
                    } else {
                        hienThiTatCa(keywordHienTai);
                    }
                });
    }

    private void setupButtons() {

        btnTatCa.setOnClickListener(v -> {
            dangLocViPham = false;
            hienThiTatCa(keywordHienTai);
        });

        btnViPham.setOnClickListener(v -> {
            dangLocViPham = true;
            hienThiTaiKhoanViPham(keywordHienTai);
        });
    }

    private void setupSearch() {

        edtSearchUser.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s,
                                          int start,
                                          int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s,
                                      int start,
                                      int before,
                                      int count) {

                keywordHienTai = s.toString().trim().toLowerCase();

                if (dangLocViPham) {
                    hienThiTaiKhoanViPham(keywordHienTai);
                } else {
                    hienThiTatCa(keywordHienTai);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void hienThiTatCa(String keyword) {

        filteredList.clear();

        for (DocumentSnapshot doc : fullList) {

            if (kiemTraTimKiem(doc, keyword)) {
                filteredList.add(doc);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void hienThiTaiKhoanViPham(String keyword) {

        filteredList.clear();
        adapter.notifyDataSetChanged();

        for (DocumentSnapshot doc : fullList) {

            if (!kiemTraTimKiem(doc, keyword)) {
                continue;
            }

            String userId = doc.getId();

            db.collection("danh_gia")
                    .whereEqualTo("id_nguoi_dung", userId)
                    .whereEqualTo("trang_thai", "vi_pham")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {

                        if (!queryDocumentSnapshots.isEmpty()) {

                            if (!filteredList.contains(doc)) {
                                filteredList.add(doc);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
        }
    }

    private boolean kiemTraTimKiem(DocumentSnapshot doc, String keyword) {

        if (keyword == null || keyword.isEmpty()) {
            return true;
        }

        String ten = doc.getString("ho_ten");
        String email = doc.getString("email");

        boolean matchTen =
                ten != null &&
                        ten.toLowerCase().contains(keyword);

        boolean matchEmail =
                email != null &&
                        email.toLowerCase().contains(keyword);

        return matchTen || matchEmail;
    }
}