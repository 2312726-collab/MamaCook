package com.example.mamacook;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class MonAnAdapter extends RecyclerView.Adapter<MonAnAdapter.ViewHolder> {
    private List<MonAn> monAnList;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId = FirebaseAuth.getInstance().getUid();

    public MonAnAdapter(List<MonAn> monAnList) {
        this.monAnList = monAnList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mon_an_home, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonAn monAn = monAnList.get(position);
        holder.tvTenMon.setText(monAn.getTen_mon());
        holder.tvThoiGian.setText(monAn.getThoi_gian_nau() + " phút");
        holder.tvRating.setText(String.valueOf(monAn.getRating()));
        
        if (monAn.getHinh_anh() != null && !monAn.getHinh_anh().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                .load(monAn.getHinh_anh())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .placeholder(R.drawable.bg_splash)
                .error(R.drawable.ic_mama)
                .into(holder.imgMonAn);
        }

        // Xử lý nút yêu thích (Trái tim)
        holder.btnFavorite.setOnClickListener(v -> {
            // Hiệu ứng đổi màu ngay lập tức
            boolean isCurrentlyWhite = holder.btnFavorite.getColorFilter() == null || 
                                       holder.btnFavorite.getTag() == null || 
                                       (int)holder.btnFavorite.getTag() == Color.WHITE;

            if (isCurrentlyWhite) {
                holder.btnFavorite.setColorFilter(Color.RED);
                holder.btnFavorite.setTag(Color.RED);
                luuMonAn(monAn);
            } else {
                holder.btnFavorite.setColorFilter(Color.WHITE);
                holder.btnFavorite.setTag(Color.WHITE);
                xoaMonAnLuu(monAn);
            }
        });
    }

    private void luuMonAn(MonAn monAn) {
        if (userId == null) return;
        MonDaLuu monDaLuu = new MonDaLuu();
        monDaLuu.setId_nguoi_dung(userId);
        monDaLuu.setId_mon_an(monAn.getId_mon_an());
        monDaLuu.setId_luu(userId + "_" + monAn.getId_mon_an());

        db.collection("mon_da_luu").document(monDaLuu.getId_luu())
                .set(monDaLuu)
                .addOnSuccessListener(aVoid -> Toast.makeText(db.getApp().getApplicationContext(), "Đã lưu vào yêu thích", Toast.LENGTH_SHORT).show());
    }

    private void xoaMonAnLuu(MonAn monAn) {
        if (userId == null) return;
        String idLuu = userId + "_" + monAn.getId_mon_an();
        db.collection("mon_da_luu").document(idLuu)
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(db.getApp().getApplicationContext(), "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return monAnList != null ? monAnList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgMonAn, btnFavorite;
        TextView tvTenMon, tvThoiGian, tvRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMonAn = itemView.findViewById(R.id.img_mon_an);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
            tvTenMon = itemView.findViewById(R.id.tv_ten_mon);
            tvThoiGian = itemView.findViewById(R.id.tv_thoi_gian);
            tvRating = itemView.findViewById(R.id.tv_rating);
        }
    }
}
