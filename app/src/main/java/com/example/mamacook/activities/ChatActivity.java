package com.example.mamacook.activities;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.adapters.TinNhanAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private TextView tvTieuDeChat;
    private RecyclerView rvTinNhan;
    private EditText edtTinNhan;
    private ImageButton btnGuiTinNhan, btnBack;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ListenerRegistration listener;

    private TinNhanAdapter adapter;
    private final List<DocumentSnapshot> tinNhanList = new ArrayList<>();

    private String idCuocTroChuyen;
    private String cheDo;
    private String tenUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvTieuDeChat = findViewById(R.id.tv_tieu_de_chat);
        rvTinNhan = findViewById(R.id.rv_tin_nhan);
        edtTinNhan = findViewById(R.id.edt_tin_nhan);
        btnGuiTinNhan = findViewById(R.id.btn_gui_tin_nhan);
        btnBack = findViewById(R.id.btn_back_chat);

        idCuocTroChuyen = getIntent().getStringExtra("id_cuoc_tro_chuyen");
        cheDo = getIntent().getStringExtra("che_do");
        tenUser = getIntent().getStringExtra("ten_user");

        if (cheDo == null) cheDo = "user";

        if (idCuocTroChuyen == null || idCuocTroChuyen.isEmpty()) {
            if (mAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Bạn cần đăng nhập", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            idCuocTroChuyen = mAuth.getCurrentUser().getUid();
        }

        tvTieuDeChat.setText("admin".equals(cheDo)
                ? (tenUser == null ? "Chat với người dùng" : tenUser)
                : "Chat với Admin");

        adapter = new TinNhanAdapter(this, tinNhanList, cheDo);

        // --- CẤU HÌNH RECYCLERVIEW BẮT ĐẦU TỪ ĐÁY LÊN ---
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Đẩy danh sách bắt đầu từ cuối lên
        rvTinNhan.setLayoutManager(layoutManager);
        rvTinNhan.setAdapter(adapter);

        // --- TỰ ĐỘNG CUỘN XUỐNG KHI NGƯỜI DÙNG CLICK VÀO Ô NHẬP ---
        edtTinNhan.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && adapter.getItemCount() > 0) {
                rvTinNhan.postDelayed(() -> rvTinNhan.scrollToPosition(adapter.getItemCount() - 1), 200);
            }
        });

        btnBack.setOnClickListener(v -> finish());
        btnGuiTinNhan.setOnClickListener(v -> guiTinNhan());

        taoCuocTroChuyenNeuChuaCo();
        loadTinNhanRealtime();

        // --- ĐOẠN CODE ÉP CO GIÃN THEO CHIỀU CAO BÀN PHÍM THỰC TẾ ---
        View rootLayout = findViewById(android.R.id.content);
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootLayout.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootLayout.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            // Nếu bàn phím mở ra (lớn hơn 15% chiều cao màn hình)
            if (keypadHeight > screenHeight * 0.15) {
                rootLayout.setPadding(0, 0, 0, keypadHeight);
            } else {
                rootLayout.setPadding(0, 0, 0, 0);
            }
        });
    }

    private void taoCuocTroChuyenNeuChuaCo() {
        db.collection("cuoc_tro_chuyen")
                .document(idCuocTroChuyen)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id_user", idCuocTroChuyen);
                        data.put("ten_user", tenUser == null ? "Người dùng" : tenUser);
                        data.put("tin_nhan_cuoi", "");
                        data.put("thoi_gian_cap_nhat", Timestamp.now());

                        db.collection("cuoc_tro_chuyen")
                                .document(idCuocTroChuyen)
                                .set(data);
                    }
                });
    }

    private void loadTinNhanRealtime() {
        listener = db.collection("tin_nhan")
                .whereEqualTo("id_cuoc_tro_chuyen", idCuocTroChuyen)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải tin nhắn: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (querySnapshot == null) return;

                    tinNhanList.clear();
                    tinNhanList.addAll(querySnapshot.getDocuments());
                    Collections.sort(tinNhanList, (d1, d2) -> {
                        Timestamp t1 = d1.getTimestamp("thoi_gian");
                        Timestamp t2 = d2.getTimestamp("thoi_gian");

                        if (t1 == null && t2 == null) return 0;
                        if (t1 == null) return -1;
                        if (t2 == null) return 1;

                        return t1.compareTo(t2);
                    });

                    adapter.notifyDataSetChanged();

                    if (!tinNhanList.isEmpty()) {
                        rvTinNhan.scrollToPosition(tinNhanList.size() - 1);
                    }
                });
    }

    private void guiTinNhan() {
        String noiDung = edtTinNhan.getText().toString().trim();
        if (noiDung.isEmpty()) return;

        String nguoiGui = "admin".equals(cheDo) ? "admin" : "user";

        Map<String, Object> tinNhan = new HashMap<>();
        tinNhan.put("id_cuoc_tro_chuyen", idCuocTroChuyen);
        tinNhan.put("nguoi_gui", nguoiGui);
        tinNhan.put("noi_dung", noiDung);
        tinNhan.put("thoi_gian", Timestamp.now());

        db.collection("tin_nhan")
                .add(tinNhan)
                .addOnSuccessListener(doc -> {
                    edtTinNhan.setText("");

                    Map<String, Object> update = new HashMap<>();
                    update.put("tin_nhan_cuoi", noiDung);
                    update.put("thoi_gian_cap_nhat", Timestamp.now());

                    db.collection("cuoc_tro_chuyen")
                            .document(idCuocTroChuyen)
                            .update(update);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi gửi tin nhắn: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}