package com.example.mamacook.adapters;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mamacook.R;
import com.example.mamacook.activities.DetailMonAnActivity;
import com.example.mamacook.models.MonAn;
import com.example.mamacook.utils.RatingUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mon_an_vertical, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonAn monAn = monAnList.get(position);

        holder.tvTenMon.setText(monAn.getTen_mon());
        holder.tvThoiGian.setText(monAn.getThoi_gian_nau() + " phút");
        holder.tvRating.setText(RatingUtils.getRatingOnly(monAn));
        holder.tvDoKho.setText("Độ khó: " + monAn.getDo_kho());

        if (monAn.getMatch_score() >= 50) {
            holder.layoutBadgeAi.setVisibility(View.VISIBLE);
            holder.tvMatchScore.setText("🔥 " + monAn.getMatch_score() + "%");
        } else {
            holder.layoutBadgeAi.setVisibility(View.GONE);
        }

        checkIsFavorite(monAn.getId_mon_an(), holder.btnFavorite);

        String hinhAnh = monAn.getHinh_anh();
        if (hinhAnh != null && !hinhAnh.isEmpty()) {
            if (hinhAnh.startsWith("http")) {
                Glide.with(holder.itemView.getContext())
                        .load(hinhAnh)
                        .placeholder(R.drawable.bg_splash)
                        .into(holder.imgMonAn);
            } else {
                StorageReference storageRef = FirebaseStorage.getInstance()
                        .getReference()
                        .child(hinhAnh);
                Glide.with(holder.itemView.getContext())
                        .load(storageRef)
                        .placeholder(R.drawable.bg_splash)
                        .into(holder.imgMonAn);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = DetailMonAnActivity.createIntent(v.getContext(), monAn);
            v.getContext().startActivity(intent);
            if (v.getContext() instanceof android.app.Activity) {
                ((android.app.Activity) v.getContext()).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
    }

    private void checkIsFavorite(String dishId, ImageView btnFavorite) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance()
                .collection("mon_da_luu")
                .document(uid + "_" + dishId)
                .addSnapshotListener((doc, error) -> {
                    if (doc != null && doc.exists()) {
                        btnFavorite.setImageResource(R.drawable.ic_heart_filled);
                        btnFavorite.setColorFilter(Color.RED);
                    } else {
                        btnFavorite.setImageResource(R.drawable.ic_nav_favorites);
                        btnFavorite.setColorFilter(Color.GRAY);
                    }
                });
    }

    @Override
    public int getItemCount() {
        return monAnList != null ? monAnList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgMonAn, btnFavorite;
        TextView tvTenMon, tvThoiGian, tvRating, tvDoKho, tvMatchScore;
        LinearLayout layoutBadgeAi;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMonAn = itemView.findViewById(R.id.img_mon_an);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
            tvTenMon = itemView.findViewById(R.id.tv_ten_mon);
            tvThoiGian = itemView.findViewById(R.id.tv_thoi_gian);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvDoKho = itemView.findViewById(R.id.tv_do_kho);
            layoutBadgeAi = itemView.findViewById(R.id.layout_badge_ai);
            tvMatchScore = itemView.findViewById(R.id.tv_match_score);
        }
    }
}