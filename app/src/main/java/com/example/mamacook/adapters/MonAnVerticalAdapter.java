package com.example.mamacook.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mamacook.R;
import com.example.mamacook.activities.DetailMonAnActivity;
import com.example.mamacook.models.MonAn;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonAnVerticalAdapter extends RecyclerView.Adapter<MonAnVerticalAdapter.ViewHolder> {

    private final List<MonAn> monAnList;
    private OnItemLongClickListener longClickListener;
    private boolean isAdminMode = false;
    private String preSelectedDate, preSelectedMeal;

    public void setPreSelectedData(String date, String meal) {
        this.preSelectedDate = date;
        this.preSelectedMeal = meal;
    }

    public void setAdminMode(boolean adminMode) {
        this.isAdminMode = adminMode;
    }

    public interface OnItemLongClickListener {
        void onLongClick(MonAn monAn);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

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
        holder.tvThoiGian.setText(String.format(Locale.getDefault(), "%d phút", monAn.getThoi_gian_nau()));
        holder.tvRating.setText(String.format(Locale.getDefault(), "%.1f", monAn.getRating()));
        
        String doKho = monAn.getDo_kho();
        if (doKho == null || doKho.isEmpty()) {
            doKho = "Chưa xác định";
        }
        holder.tvDoKho.setText(String.format("Độ khó: %s", doKho));

        // Ẩn badge AI/Hợp gu theo yêu cầu
        holder.layoutBadgeAi.setVisibility(View.GONE);

        // Kiểm tra yêu thích
        checkIsFavorite(monAn.getId_mon_an(), holder.btnFavorite);
        holder.btnFavorite.setOnClickListener(v -> toggleFavorite(monAn, holder.itemView.getContext()));

        // Load ảnh
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

        // Chuyển màn hình chi tiết
        holder.itemView.setOnClickListener(v -> {
            Intent intent = DetailMonAnActivity.createIntent(v.getContext(), monAn);
            if (preSelectedDate != null && preSelectedMeal != null) {
                intent.putExtra("PRE_SELECTED_DATE", preSelectedDate);
                intent.putExtra("PRE_SELECTED_MEAL", preSelectedMeal);
            }
            v.getContext().startActivity(intent);
            if (v.getContext() instanceof android.app.Activity) {
                ((android.app.Activity) v.getContext()).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });

        // Nút AI: Chỉ hiện khi ở chế độ Quản lý (Admin)
        holder.btnAI.setVisibility(isAdminMode ? View.VISIBLE : View.GONE);
        holder.btnAI.setOnClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(monAn);
            }
        });
    }

    private void toggleFavorite(MonAn monAn, Context context) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String idLuu = uid + "_" + monAn.getId_mon_an();

        FirebaseFirestore.getInstance().collection("mon_da_luu").document(idLuu).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        new AlertDialog.Builder(context)
                                .setTitle("Xác nhận")
                                .setMessage("Bạn có chắc chắn muốn bỏ yêu thích món ăn này không?")
                                .setPositiveButton("Có", (dialog, which) ->
                                    FirebaseFirestore.getInstance().collection("mon_da_luu").document(idLuu).delete()
                                            .addOnSuccessListener(aVoid -> Toast.makeText(context, "Đã xóa khỏi món ăn yêu thích!", Toast.LENGTH_SHORT).show())
                                )
                                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                                .show();
                    } else {
                        Map<String, Object> data = new HashMap<>();
                        data.put("id_nguoi_dung", uid);
                        data.put("id_mon_an", monAn.getId_mon_an());
                        data.put("id_luu", idLuu);
                        FirebaseFirestore.getInstance().collection("mon_da_luu").document(idLuu).set(data)
                                .addOnSuccessListener(aVoid -> Toast.makeText(context, "Đã thêm vào yêu thích!", Toast.LENGTH_SHORT).show());
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
        Button btnAI;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMonAn = itemView.findViewById(R.id.img_mon_an);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
            tvTenMon = itemView.findViewById(R.id.tv_ten_mon);
            tvThoiGian = itemView.findViewById(R.id.tv_thoi_gian);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvDoKho = itemView.findViewById(R.id.tv_do_kho_vertical);
            layoutBadgeAi = itemView.findViewById(R.id.layout_badge_ai);
            tvMatchScore = itemView.findViewById(R.id.tv_match_score);
            btnAI = itemView.findViewById(R.id.btn_ai_review);
        }
    }
}
