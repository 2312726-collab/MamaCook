package com.example.mamacook.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ThongBaoAdapter extends RecyclerView.Adapter<ThongBaoAdapter.ThongBaoViewHolder> {

    private final Context context;
    private final List<DocumentSnapshot> thongBaoList;
    private final FirebaseFirestore db;
    private final boolean isAdmin;

    public ThongBaoAdapter(Context context, List<DocumentSnapshot> thongBaoList) {
        this(context, thongBaoList, false);
    }

    public ThongBaoAdapter(Context context, List<DocumentSnapshot> thongBaoList, boolean isAdmin) {
        this.context = context;
        this.thongBaoList = thongBaoList;
        this.db = FirebaseFirestore.getInstance();
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public ThongBaoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_thong_bao, parent, false);
        return new ThongBaoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ThongBaoViewHolder holder, int position) {
        DocumentSnapshot doc = thongBaoList.get(position);

        String id = doc.getId();
        String tieuDe = doc.getString("tieu_de");
        String noiDung = doc.getString("noi_dung");
        String loai = doc.getString("loai");
        Boolean daDoc = doc.getBoolean("da_doc");
        Timestamp ngayTao = doc.getTimestamp("ngay_tao");

        if (tieuDe == null) tieuDe = "Thông báo";
        if (noiDung == null) noiDung = "";
        if (loai == null) loai = "he_thong";
        if (daDoc == null) daDoc = false;

        holder.tvTieuDe.setText(tieuDe);
        holder.tvNoiDung.setText(noiDung);
        holder.tvLoai.setText(hienThiLoai(loai));

        if (ngayTao != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvNgayTao.setText(sdf.format(ngayTao.toDate()));
        } else {
            holder.tvNgayTao.setText("");
        }

        if (isAdmin) {
            holder.btnXoa.setVisibility(View.VISIBLE);
            holder.tvChamDo.setVisibility(View.GONE);
            holder.itemView.setAlpha(1f);

            holder.btnXoa.setOnClickListener(v -> xacNhanXoa(id));
            holder.itemView.setOnClickListener(null);

        } else {
            holder.btnXoa.setVisibility(View.GONE);

            if (daDoc) {
                holder.tvChamDo.setVisibility(View.GONE);
                holder.itemView.setAlpha(0.72f);
            } else {
                holder.tvChamDo.setVisibility(View.VISIBLE);
                holder.itemView.setAlpha(1f);
            }

            holder.itemView.setOnClickListener(v -> {
                db.collection("thong_bao")
                        .document(id)
                        .update("da_doc", true)
                        .addOnSuccessListener(unused ->
                                Toast.makeText(context, "Đã đọc thông báo", Toast.LENGTH_SHORT).show()
                        );
            });
        }
    }

    private void xacNhanXoa(String id) {
        new AlertDialog.Builder(context)
                .setTitle("Xóa thông báo")
                .setMessage("Bạn có chắc muốn xóa thông báo này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.collection("thong_bao")
                            .document(id)
                            .delete()
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(context, "Đã xóa thông báo", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private String hienThiLoai(String loai) {
        switch (loai) {
            case "he_thong":
                return "📢 Hệ thống";
            case "mon_an":
                return "🍲 Món ăn";
            case "danh_gia":
                return "⭐ Đánh giá";
            case "canh_bao":
                return "⚠️ Cảnh báo";
            default:
                return "📢 Thông báo";
        }
    }

    @Override
    public int getItemCount() {
        return thongBaoList == null ? 0 : thongBaoList.size();
    }

    static class ThongBaoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTieuDe, tvNoiDung, tvLoai, tvNgayTao, tvChamDo;
        Button btnXoa;

        public ThongBaoViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTieuDe = itemView.findViewById(R.id.tv_tieu_de_thong_bao);
            tvNoiDung = itemView.findViewById(R.id.tv_noi_dung_thong_bao);
            tvLoai = itemView.findViewById(R.id.tv_loai_thong_bao);
            tvNgayTao = itemView.findViewById(R.id.tv_ngay_tao_thong_bao);
            tvChamDo = itemView.findViewById(R.id.tv_cham_do_thong_bao);
            btnXoa = itemView.findViewById(R.id.btn_xoa_thong_bao);
        }
    }
}