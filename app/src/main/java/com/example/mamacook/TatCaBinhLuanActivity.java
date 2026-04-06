package com.example.mamacook;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TatCaBinhLuanActivity extends AppCompatActivity {

    private RecyclerView rvTatCa;
    private BinhLuanDocAdapter adapter;
    private List<DanhGia> listDanhGia = new ArrayList<>();
    private FirebaseFirestore db;
    private String idMonAn;
    private MaterialButton btnMoBoLoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tat_ca_binh_luan);

        db = FirebaseFirestore.getInstance();
        idMonAn = getIntent().getStringExtra("ID_MON_AN");

        rvTatCa = findViewById(R.id.rvTatCaBinhLuan);
        rvTatCa.setLayoutManager(new LinearLayoutManager(this));

        adapter = new BinhLuanDocAdapter(listDanhGia); 
        rvTatCa.setAdapter(adapter);

        ImageView imgBack = findViewById(R.id.imgBack);
        if (imgBack != null) {
            imgBack.setOnClickListener(v -> finish());
        }

        btnMoBoLoc = findViewById(R.id.btnMoBoLoc);
        if (btnMoBoLoc != null) {
            btnMoBoLoc.setOnClickListener(v -> showBottomSheetLoc());
        }

        if (idMonAn != null) {
            loadDuLieuTheoBoLoc("moi_nhat");
        }
    }

    private void showBottomSheetLoc() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.layout_bottom_sheet_loc);

        RadioGroup radioGroupLoc = bottomSheetDialog.findViewById(R.id.radioGroupLoc);

        if (radioGroupLoc != null) {
            radioGroupLoc.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rbMoiNhat) {
                    btnMoBoLoc.setText("Lọc theo: Mới nhất ▾");
                    loadDuLieuTheoBoLoc("moi_nhat");
                } else if (checkedId == R.id.rbCuNhat) {
                    btnMoBoLoc.setText("Lọc theo: Cũ nhất ▾");
                    loadDuLieuTheoBoLoc("cu_nhat");
                } else if (checkedId == R.id.rb5Sao) {
                    btnMoBoLoc.setText("Lọc theo: 5 Sao ▾");
                    loadDuLieuTheoBoLoc("5_sao");
                } else if (checkedId == R.id.rbThapSao) {
                    btnMoBoLoc.setText("Lọc theo: Dưới 5 Sao ▾");
                    loadDuLieuTheoBoLoc("thap_sao");
                }
                bottomSheetDialog.dismiss();
            });
        }

        bottomSheetDialog.show();
    }

    private void loadDuLieuTheoBoLoc(String theLoai) {
        Query query = db.collection("danh_gia")
                .whereEqualTo("id_mon_an", idMonAn)
                .whereEqualTo("trang_thai", "hien_thi");

        if (theLoai.equals("moi_nhat")) {
            query = query.orderBy("ngay_danh_gia", Query.Direction.DESCENDING);
        } else if (theLoai.equals("cu_nhat")) {
            query = query.orderBy("ngay_danh_gia", Query.Direction.ASCENDING);
        } else if (theLoai.equals("5_sao")) {
            query = query.whereEqualTo("so_sao", 5.0).orderBy("ngay_danh_gia", Query.Direction.DESCENDING);
        } else if (theLoai.equals("thap_sao")) {
            query = query.whereLessThan("so_sao", 5.0).orderBy("so_sao", Query.Direction.ASCENDING);
        }

        query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Loi_Loc", error.getMessage());
                return;
            }
            if (value == null) return;

            listDanhGia.clear();
            for (QueryDocumentSnapshot doc : value) {
                DanhGia dg = doc.toObject(DanhGia.class);
                if (dg != null) listDanhGia.add(dg);
            }
            adapter.notifyDataSetChanged();
        });
    }
}
