package com.example.mamacook.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mamacook.R;
import com.example.mamacook.models.DanhGia;

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

        String ten = (dg.getTen_nguoi_dung() != null && !dg.getTen_nguoi_dung().isEmpty()) ? dg.getTen_nguoi_dung() : "Người dùng";
        holder.tvTen.setText(ten);
        holder.tvNoiDung.setText(dg.getNoi_dung());
        holder.rbSao.setRating(dg.getSo_sao());

        // Load avatar
        if (dg.getAvatar_url() != null && !dg.getAvatar_url().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(dg.getAvatar_url())
                    .placeholder(R.drawable.ic_user_placeholder)
                    .circleCrop()
                    .into(holder.imgAvatar);
        } else {
            holder.imgAvatar.setImageResource(R.drawable.ic_user_placeholder);
        }

        // Load comment image if exists
        if (dg.getHinh_anh_url() != null && !dg.getHinh_anh_url().isEmpty()) {
            holder.imgBinhLuan.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(dg.getHinh_anh_url())
                    .into(holder.imgBinhLuan);
        } else {
            holder.imgBinhLuan.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return danhSachBinhLuan != null ? danhSachBinhLuan.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTen, tvNoiDung;
        RatingBar rbSao;
        ImageView imgAvatar, imgBinhLuan;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTen = itemView.findViewById(R.id.tvTenNguoiBinhLuan);
            tvNoiDung = itemView.findViewById(R.id.tvNoiDungBinhLuan);
            rbSao = itemView.findViewById(R.id.rbSaoBinhLuan);
            imgAvatar = itemView.findViewById(R.id.imgAvatarBinhLuan);
            imgBinhLuan = itemView.findViewById(R.id.imgBinhLuan);
        }
    }
}
