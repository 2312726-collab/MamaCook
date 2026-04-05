package com.example.mamacook;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class BinhLuanNgangAdapter extends RecyclerView.Adapter<BinhLuanNgangAdapter.ViewHolder> {
    private List<DanhGia> danhSachBinhLuan;

    public BinhLuanNgangAdapter(List<DanhGia> danhSachBinhLuan) {
        this.danhSachBinhLuan = danhSachBinhLuan;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_binh_luan_ngang, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DanhGia dg = danhSachBinhLuan.get(position);
        
        String ten = (dg.getHo_ten() != null && !dg.getHo_ten().isEmpty()) ? dg.getHo_ten() : "Khách hàng ẩn danh";
        holder.tvTen.setText(ten);
        holder.tvNoiDung.setText(dg.getNoi_dung_danh_gia());
        
        // Hiển thị số sao người dùng đã chấm
        holder.rbSao.setRating(dg.getSo_sao());
    }

    @Override
    public int getItemCount() {
        return danhSachBinhLuan != null ? danhSachBinhLuan.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTen, tvNoiDung;
        RatingBar rbSao;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTen = itemView.findViewById(R.id.tvTenNguoiBinhLuan);
            tvNoiDung = itemView.findViewById(R.id.tvNoiDungBinhLuan);
            rbSao = itemView.findViewById(R.id.rbSaoBinhLuan);
        }
    }
}
