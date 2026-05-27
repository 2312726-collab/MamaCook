package com.example.mamacook.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.adapters.UserAdminAdapter;
import com.example.mamacook.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class QuanLyTaiKhoanActivity extends AppCompatActivity implements UserAdminAdapter.OnUserActionListener {

    private EditText edtSearchUser;
    private RecyclerView rvUsers;
    private Button btnTatCa, btnViPham, btnUpdateViPham;
    private UserAdminAdapter adapter;
    private FirebaseFirestore db;
    private com.google.firebase.firestore.ListenerRegistration userListener;

    private final List<User> fullList = new ArrayList<>();
    private final List<User> filteredList = new ArrayList<>();
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
        btnUpdateViPham = findViewById(R.id.btn_update_vi_pham);

        adapter = new UserAdminAdapter(this, filteredList, this);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        setupSearch();
        setupButtons();
        loadUsers();

        btnUpdateViPham.setOnClickListener(v -> updateAllUsersViPham());
    }

    // ✅ THÊM onResume để reload khi quay lại màn hình
    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }

    private void updateAllUsersViPham() {
        new AlertDialog.Builder(this)
                .setTitle("Cập nhật vi phạm")
                .setMessage("Thêm field 'so_lan_vi_pham = 0' cho tất cả tài khoản?")
                .setPositiveButton("Có", (d, w) -> {
                    db.collection("nguoi_dung")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                int count = 0;
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    doc.getReference().update("so_lan_vi_pham", 0);
                                    count++;
                                }
                                Toast.makeText(QuanLyTaiKhoanActivity.this,
                                        "Đã cập nhật " + count + " tài khoản",
                                        Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(QuanLyTaiKhoanActivity.this,
                                            "Lỗi: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Không", null)
                .show();
    }

    private void loadUsers() {
        com.google.firebase.auth.FirebaseUser currentUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_LONG).show();
            return;
        }

        if (userListener != null) {
            userListener.remove();
        }

        userListener = db.collection("nguoi_dung")
                .addSnapshotListener((query, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    fullList.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        User u = doc.toObject(User.class);
                        u.setId_nguoi_dung(doc.getId());

                        // Đếm số lần vi phạm từ collection danh_gia
                        String userId = doc.getId();
                        db.collection("danh_gia")
                                .whereEqualTo("id_nguoi_dung", userId)
                                .whereEqualTo("trang_thai", "vi_pham")
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    int soLanViPham = querySnapshot.size();
                                    u.setSo_lan_vi_pham(soLanViPham);

                                    // Cập nhật vào Firestore
                                    db.collection("nguoi_dung").document(userId)
                                            .update("so_lan_vi_pham", soLanViPham);

                                    adapter.notifyDataSetChanged();
                                });

                        fullList.add(u);
                    }
                    applyFilters();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) {
            userListener.remove();
        }
    }

    private void setupButtons() {
        btnTatCa.setOnClickListener(v -> {
            dangLocViPham = false;
            applyFilters();
        });
        btnViPham.setOnClickListener(v -> {
            dangLocViPham = true;
            applyFilters();
        });
    }

    private void setupSearch() {
        edtSearchUser.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                keywordHienTai = s.toString().trim().toLowerCase();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilters() {
        List<User> newFilteredList = new ArrayList<>();
        for (User u : fullList) {
            boolean matchesSearch = (u.getHo_ten() != null && u.getHo_ten().toLowerCase().contains(keywordHienTai))
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(keywordHienTai));

            if (keywordHienTai.isEmpty()) matchesSearch = true;

            if (matchesSearch) {
                if (dangLocViPham) {
                    if (u.getSo_lan_vi_pham() > 0) newFilteredList.add(u);
                } else {
                    newFilteredList.add(u);
                }
            }
        }
        filteredList.clear();
        filteredList.addAll(newFilteredList);
        adapter.updateList(newFilteredList);
    }

    @Override
    public void onToggleStatus(User user) {
        String newStatus = "bi_khoa".equals(user.getTrang_thai_tai_khoan()) ? "dang_hoat_dong" : "bi_khoa";
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận")
                .setMessage("Bạn có chắc muốn cập nhật trạng thái?")
                .setPositiveButton("Có", (d, w) -> {
                    db.collection("nguoi_dung").document(user.getId_nguoi_dung())
                            .update("trang_thai_tai_khoan", newStatus)
                            .addOnSuccessListener(a ->
                                    Toast.makeText(this, "Đã cập nhật", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Không", null)
                .show();
    }

    @Override
    public void onChangeRole(User user) {
        String newRole = "admin".equals(user.getRole()) ? "user" : "admin";
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận")
                .setMessage("Đổi vai trò người dùng này?")
                .setPositiveButton("Có", (d, w) -> {
                    db.collection("nguoi_dung").document(user.getId_nguoi_dung())
                            .update("role", newRole)
                            .addOnSuccessListener(a ->
                                    Toast.makeText(this, "Đã đổi role", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Không", null)
                .show();
    }
}