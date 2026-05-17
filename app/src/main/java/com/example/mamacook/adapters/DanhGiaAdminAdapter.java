package com.example.mamacook.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class DanhGiaAdminAdapter extends RecyclerView.Adapter<DanhGiaAdminAdapter.DanhGiaViewHolder> {

    private final Context context;
    private final List<DocumentSnapshot> danhGiaList;
    private final FirebaseFirestore db;

    public DanhGiaAdminAdapter(Context context, List<DocumentSnapshot> danhGiaList) {
        this.context = context;
        this.danhGiaList = danhGiaList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public DanhGiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_danh_gia_admin, parent, false);
        return new DanhGiaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DanhGiaViewHolder holder, int position) {
        DocumentSnapshot doc = danhGiaList.get(position);

        String id = doc.getId();
        String tenNguoiDung = doc.getString("ten_nguoi_dung");
        String noiDung = doc.getString("noi_dung");
        String trangThai = doc.getString("trang_thai");

        if (trangThai == null || trangThai.isEmpty()) {
            trangThai = "hien_thi";
        }

        Number soSao = doc.getDouble("so_sao");
        if (soSao == null) {
            soSao = doc.getLong("so_sao");
        }

        holder.tvTenNguoiDung.setText(tenNguoiDung == null ? "Ẩn danh" : tenNguoiDung);
        holder.tvNoiDung.setText(noiDung == null ? "Không có nội dung" : noiDung);
        holder.tvSoSao.setText("Số sao: " + (soSao == null ? "0" : soSao.toString()));
        holder.tvTrangThai.setText("Trạng thái: " + trangThai);

        if ("vi_pham".equals(trangThai)) {
            holder.tvTrangThai.setTextColor(Color.RED);
            holder.btnAnHien.setText("Hiện");
        } else {
            holder.tvTrangThai.setTextColor(Color.parseColor("#2E7D32"));
            holder.btnAnHien.setText("Ẩn");
        }

        holder.btnAnHien.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION
                    || currentPosition >= danhGiaList.size()) {
                return;
            }

            DocumentSnapshot currentDoc = danhGiaList.get(currentPosition);
            String currentId = currentDoc.getId();
            String currentTrangThai = currentDoc.getString("trang_thai");

            if (currentTrangThai == null || currentTrangThai.isEmpty()) {
                currentTrangThai = "hien_thi";
            }

            String trangThaiMoi;

            if ("hien_thi".equals(currentTrangThai)) {
                trangThaiMoi = "vi_pham";
            } else {
                trangThaiMoi = "hien_thi";
            }

            db.collection("danh_gia")
                    .document(currentId)
                    .update("trang_thai", trangThaiMoi)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(context, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show();

                        db.collection("danh_gia")
                                .document(currentId)
                                .get()
                                .addOnSuccessListener(newDoc -> {
                                    capNhatItemTheoId(currentId, newDoc);
                                });
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });

        holder.btnXoa.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION
                    || currentPosition >= danhGiaList.size()) {
                return;
            }

            DocumentSnapshot currentDoc = danhGiaList.get(currentPosition);
            String currentId = currentDoc.getId();

            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận")
                    .setMessage("Bạn có chắc muốn xóa đánh giá này không?")
                    .setPositiveButton("Có", (dialog, which) -> {
                        db.collection("danh_gia")
                                .document(currentId)
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(context, "Đã xóa đánh giá", Toast.LENGTH_SHORT).show();
                                    xoaItemTheoId(currentId);
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );
                    })
                    .setNegativeButton("Không", null)
                    .show();
        });
    }

    private void capNhatItemTheoId(String id, DocumentSnapshot newDoc) {
        for (int i = 0; i < danhGiaList.size(); i++) {
            if (danhGiaList.get(i).getId().equals(id)) {
                danhGiaList.set(i, newDoc);
                notifyItemChanged(i);
                return;
            }
        }
    }

    private void xoaItemTheoId(String id) {
        for (int i = 0; i < danhGiaList.size(); i++) {
            if (danhGiaList.get(i).getId().equals(id)) {
                danhGiaList.remove(i);
                notifyDataSetChanged();
                return;
            }
        }
    }

    @Override
    public int getItemCount() {
        return danhGiaList == null ? 0 : danhGiaList.size();
    }

    static class DanhGiaViewHolder extends RecyclerView.ViewHolder {
        TextView tvTenNguoiDung, tvNoiDung, tvSoSao, tvTrangThai;
        Button btnAnHien, btnXoa;

        public DanhGiaViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTenNguoiDung = itemView.findViewById(R.id.tv_ten_nguoi_dung_dg);
            tvNoiDung = itemView.findViewById(R.id.tv_noi_dung_dg);
            tvSoSao = itemView.findViewById(R.id.tv_so_sao_dg);
            tvTrangThai = itemView.findViewById(R.id.tv_trang_thai_dg);
            btnAnHien = itemView.findViewById(R.id.btn_an_hien_dg);
            btnXoa = itemView.findViewById(R.id.btn_xoa_dg);
        }
    }
}