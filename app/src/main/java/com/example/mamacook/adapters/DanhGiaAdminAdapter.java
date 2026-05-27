package com.example.mamacook.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.activities.QuanLyTaiKhoanActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
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

    @NonNull @Override
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

        String idMonAn = doc.getString("id_mon_an");
        String tenMon = doc.getString("ten_mon_an");

        if (tenMon == null || tenMon.isEmpty()) {
            if (idMonAn != null && !idMonAn.isEmpty()) {
                db.collection("mon_an").document(idMonAn).get()
                        .addOnSuccessListener(monAnDoc -> {
                            if (monAnDoc.exists()) {
                                String tenMonAn = monAnDoc.getString("ten_mon");
                                holder.tvMonAn.setText("Món: " + (tenMonAn != null ? tenMonAn : "N/A"));
                            } else {
                                holder.tvMonAn.setText("Món: N/A");
                            }
                        })
                        .addOnFailureListener(e -> holder.tvMonAn.setText("Món: N/A"));
            } else {
                holder.tvMonAn.setText("Món: N/A");
            }
        } else {
            holder.tvMonAn.setText("Món: " + tenMon);
        }

        com.google.firebase.Timestamp timestamp = doc.getTimestamp("ngay_danh_gia");
        if (timestamp != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            holder.tvNgay.setText(sdf.format(timestamp.toDate()));
        } else {
            holder.tvNgay.setText("");
        }

        float soSaoVal = (soSao == null) ? 0f : soSao.floatValue();
        holder.rbSoSao.setRating(soSaoVal);

        holder.tvTrangThai.setText("Trạng thái: " + trangThai);

        if ("vi_pham".equals(trangThai)) {
            holder.tvTrangThai.setTextColor(Color.RED);
            holder.btnAnHien.setText("Hiện");

            // ✅ TỰ ĐỘNG TĂNG VI PHẠM nếu chưa từng tăng
            Boolean daTang = doc.getBoolean("da_tang_vi_pham");
            if (daTang == null || !daTang) {
                String idNguoiDung = doc.getString("id_nguoi_dung");
                if (idNguoiDung != null && !idNguoiDung.isEmpty()) {
                    db.collection("nguoi_dung").document(idNguoiDung)
                            .update("so_lan_vi_pham", FieldValue.increment(1))
                            .addOnSuccessListener(aVoid -> {
                                doc.getReference().update("da_tang_vi_pham", true);
                            });
                }
            }
        } else {
            holder.tvTrangThai.setTextColor(Color.parseColor("#2E7D32"));
            holder.btnAnHien.setText("Ẩn");
        }

        holder.btnAnHien.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION || currentPosition >= danhGiaList.size()) return;

            DocumentSnapshot currentDoc = danhGiaList.get(currentPosition);
            String currentId = currentDoc.getId();

            String[] options = {"Hiển thị", "Ẩn", "Vi phạm"};
            new AlertDialog.Builder(context)
                    .setTitle("Chọn trạng thái")
                    .setItems(options, (dialog, which) -> {
                        String trangThaiMoi;
                        if (which == 0) trangThaiMoi = "hien_thi";
                        else if (which == 1) trangThaiMoi = "an";
                        else trangThaiMoi = "vi_pham";

                        db.collection("danh_gia")
                                .document(currentId)
                                .update("trang_thai", trangThaiMoi)
                                .addOnSuccessListener(unused -> {
                                    if ("vi_pham".equals(trangThaiMoi)) {
                                        String idNguoiDung = currentDoc.getString("id_nguoi_dung");
                                        if (idNguoiDung != null && !idNguoiDung.isEmpty()) {
                                            db.collection("nguoi_dung").document(idNguoiDung)
                                                    .update("so_lan_vi_pham", FieldValue.increment(1))
                                                    .addOnSuccessListener(aVoid -> {
                                                        Toast.makeText(context, "Đã tăng vi phạm!", Toast.LENGTH_SHORT).show();
                                                        Intent intent = new Intent(context, QuanLyTaiKhoanActivity.class);
                                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                                        context.startActivity(intent);
                                                    });
                                        }
                                    } else {
                                        Toast.makeText(context, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );
                    })
                    .show();
        });

        holder.btnViPham.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION || currentPosition >= danhGiaList.size()) return;

            DocumentSnapshot currentDoc = danhGiaList.get(currentPosition);
            String currentId = currentDoc.getId();

            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận")
                    .setMessage("Đánh dấu đánh giá này là vi phạm?")
                    .setPositiveButton("Có", (dialog, which) -> {
                        db.collection("danh_gia")
                                .document(currentId)
                                .update("trang_thai", "vi_pham")
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(context, "Đã đánh dấu vi phạm", Toast.LENGTH_SHORT).show();
                                    db.collection("danh_gia").document(currentId).get()
                                            .addOnSuccessListener(newDoc -> capNhatItemTheoId(currentId, newDoc));
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );
                    })
                    .setNegativeButton("Không", null)
                    .show();
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

    private void xoaItemTheoId(String id) {
        for (int i = 0; i < danhGiaList.size(); i++) {
            if (danhGiaList.get(i).getId().equals(id)) {
                danhGiaList.remove(i);
                notifyDataSetChanged();
                return;
            }
        }
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

    @Override
    public int getItemCount() {
        return danhGiaList == null ? 0 : danhGiaList.size();
    }

    static class DanhGiaViewHolder extends RecyclerView.ViewHolder {
        TextView tvTenNguoiDung, tvNoiDung, tvTrangThai, tvMonAn, tvNgay;
        RatingBar rbSoSao;
        Button btnAnHien, btnViPham, btnXoa;

        public DanhGiaViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTenNguoiDung = itemView.findViewById(R.id.tv_ten_nguoi_dung_dg);
            tvNoiDung = itemView.findViewById(R.id.tv_noi_dung_dg);
            rbSoSao = itemView.findViewById(R.id.rb_so_sao_dg);
            tvTrangThai = itemView.findViewById(R.id.tv_trang_thai_dg);
            tvMonAn = itemView.findViewById(R.id.tv_mon_an_dg);
            tvNgay = itemView.findViewById(R.id.tv_ngay_dg);
            btnAnHien = itemView.findViewById(R.id.btn_an_hien_dg);
            btnViPham = itemView.findViewById(R.id.btn_vi_pham_dg);
            btnXoa = itemView.findViewById(R.id.btn_xoa_dg);
        }
    }
}