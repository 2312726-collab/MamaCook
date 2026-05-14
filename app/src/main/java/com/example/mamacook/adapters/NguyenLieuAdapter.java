package com.example.mamacook.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.models.MonAn;

import java.util.List;

public class NguyenLieuAdapter extends RecyclerView.Adapter<NguyenLieuAdapter.ViewHolder> {
    private List<MonAn.ChiTietNguyenLieu> danhSachNguyenLieu;

    public NguyenLieuAdapter(List<MonAn.ChiTietNguyenLieu> danhSachNguyenLieu) {
        this.danhSachNguyenLieu = danhSachNguyenLieu;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nguyen_lieu_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonAn.ChiTietNguyenLieu nl = danhSachNguyenLieu.get(position);

        holder.tvTen.setText(nl.ten_nguyen_lieu);

        // Format số lượng
        String soLuong = formatSoLuong(nl.so_luong);
        String donVi = nl.don_vi != null ? nl.don_vi : "";
        holder.tvSoLuong.setText(soLuong + " " + donVi);
    }

    @Override
    public int getItemCount() {
        return danhSachNguyenLieu != null ? danhSachNguyenLieu.size() : 0;
    }

    private String formatSoLuong(double soLuong) {
        if (soLuong == (long) soLuong) {
            return String.valueOf((long) soLuong);
        } else {
            return String.valueOf(soLuong);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTen, tvSoLuong;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTen = itemView.findViewById(R.id.tv_ten_nguyen_lieu);
            tvSoLuong = itemView.findViewById(R.id.tv_so_luong);
        }
    }
}
