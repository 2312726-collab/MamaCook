package com.example.mamacook.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.adapters.ThongBaoAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminGuiThongBaoActivity extends AppCompatActivity {

    private EditText edtTieuDe, edtNoiDung;
    private Button btnGuiThongBao;
    private RecyclerView rvThongBaoAdmin;

    private FirebaseFirestore db;
    private ThongBaoAdapter adapter;
    private ListenerRegistration thongBaoListener;

    private final List<DocumentSnapshot> thongBaoList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_gui_thong_bao);

        db = FirebaseFirestore.getInstance();

        edtTieuDe = findViewById(R.id.edt_tieu_de_thong_bao);
        edtNoiDung = findViewById(R.id.edt_noi_dung_thong_bao);
        btnGuiThongBao = findViewById(R.id.btn_gui_thong_bao);
        rvThongBaoAdmin = findViewById(R.id.rv_thong_bao_admin);

        adapter = new ThongBaoAdapter(this, thongBaoList, true);

        rvThongBaoAdmin.setLayoutManager(new LinearLayoutManager(this));
        rvThongBaoAdmin.setAdapter(adapter);

        btnGuiThongBao.setOnClickListener(v -> guiThongBao());

        loadThongBaoRealtime();
    }

    private void guiThongBao() {
        String tieuDe = edtTieuDe.getText().toString().trim();
        String noiDung = edtNoiDung.getText().toString().trim();

        if (tieuDe.isEmpty()) {
            edtTieuDe.setError("Vui lòng nhập tiêu đề");
            edtTieuDe.requestFocus();
            return;
        }

        if (noiDung.isEmpty()) {
            edtNoiDung.setError("Vui lòng nhập nội dung");
            edtNoiDung.requestFocus();
            return;
        }

        btnGuiThongBao.setEnabled(false);
        btnGuiThongBao.setText("Đang gửi...");

        Map<String, Object> thongBao = new HashMap<>();
        thongBao.put("tieu_de", tieuDe);
        thongBao.put("noi_dung", noiDung);
        thongBao.put("loai", "he_thong");
        thongBao.put("id_nguoi_nhan", "all");
        thongBao.put("nguoi_gui", "admin");
        thongBao.put("da_doc", false);
        thongBao.put("ngay_tao", Timestamp.now());

        db.collection("thong_bao")
                .add(thongBao)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Đã gửi thông báo", Toast.LENGTH_SHORT).show();

                    edtTieuDe.setText("");
                    edtNoiDung.setText("");

                    btnGuiThongBao.setEnabled(true);
                    btnGuiThongBao.setText("Gửi thông báo");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();

                    btnGuiThongBao.setEnabled(true);
                    btnGuiThongBao.setText("Gửi thông báo");
                });
    }

    private void loadThongBaoRealtime() {
        thongBaoListener = db.collection("thong_bao")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải thông báo: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (querySnapshot == null) return;

                    thongBaoList.clear();
                    thongBaoList.addAll(querySnapshot.getDocuments());

                    Collections.sort(thongBaoList, (doc1, doc2) -> {
                        Timestamp time1 = doc1.getTimestamp("ngay_tao");
                        Timestamp time2 = doc2.getTimestamp("ngay_tao");

                        if (time1 == null && time2 == null) return 0;
                        if (time1 == null) return 1;
                        if (time2 == null) return -1;

                        return time2.compareTo(time1);
                    });

                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (thongBaoListener != null) {
            thongBaoListener.remove();
        }
    }
}