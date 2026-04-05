package com.example.mamacook;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.List;

public class MonAnVerticalAdapter extends RecyclerView.Adapter<MonAnVerticalAdapter.ViewHolder> {
    private List<MonAn> monAnList;

    public MonAnVerticalAdapter(List<MonAn> monAnList) {
        this.monAnList = monAnList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mon_an_vertical, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonAn monAn = monAnList.get(position);
        holder.tvTen.setText(monAn.getTen_mon());
        holder.tvThoiGian.setText(monAn.getThoi_gian_nau() + " phút");
        holder.tvRating.setText(String.valueOf(monAn.getRating()));

        String hinhAnh = monAn.getHinh_anh();
        if (hinhAnh != null && !hinhAnh.isEmpty()) {
            if (hinhAnh.startsWith("http")) {
                Glide.with(holder.itemView.getContext()).load(hinhAnh).placeholder(R.drawable.bg_splash).into(holder.img);
            } else {
                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(hinhAnh);
                Glide.with(holder.itemView.getContext()).load(storageRef).placeholder(R.drawable.bg_splash).into(holder.img);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), DetailMonAnActivity.class);
            intent.putExtra("ID_MON_AN", monAn.getId_mon_an());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return monAnList != null ? monAnList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img, btnFav;
        TextView tvTen, tvThoiGian, tvRating;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img_mon_an);
            btnFav = itemView.findViewById(R.id.btn_favorite);
            tvTen = itemView.findViewById(R.id.tv_ten_mon);
            tvThoiGian = itemView.findViewById(R.id.tv_thoi_gian);
            tvRating = itemView.findViewById(R.id.tv_rating);
        }
    }
}
