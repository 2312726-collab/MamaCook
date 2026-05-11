package com.example.mamacook.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mon_an_vertical, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonAn monAn = monAnList.get(position);
        holder.tvTen.setText(monAn.getTen_mon());
        
        // Sử dụng String.format để tránh cảnh báo setText và Locale để định dạng chuẩn
        holder.tvThoiGian.setText(String.format(Locale.getDefault(), "%d phút", monAn.getThoi_gian_nau()));
        holder.tvRating.setText(String.format(Locale.getDefault(), "%.1f", monAn.getRating()));
        holder.tvDoKho.setText(String.format("Độ khó: %s", monAn.getDo_kho()));

        checkIsFavorite(monAn.getId_mon_an(), holder.btnFav);

        holder.btnFav.setOnClickListener(v -> toggleFavorite(monAn, holder.itemView.getContext()));

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
            intent.putExtra("HINH_ANH", monAn.getHinh_anh());
            v.getContext().startActivity(intent);
        });

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
        if (uid == null || btnFavorite == null) return;

        FirebaseFirestore.getInstance().collection("mon_da_luu")
                .document(uid + "_" + dishId)
                .addSnapshotListener((doc, error) -> {
                    if (doc != null && doc.exists()) {
                        btnFavorite.setColorFilter(Color.RED);
                    } else {
                        btnFavorite.setColorFilter(Color.GRAY);
                    }
                });
    }

    @Override
    public int getItemCount() {
        return monAnList != null ? monAnList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img, btnFav;
        Button btnAI;
        TextView tvTen, tvThoiGian, tvRating, tvDoKho;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img_mon_an);
            btnFav = itemView.findViewById(R.id.btn_favorite);
            btnAI = itemView.findViewById(R.id.btn_ai_review);
            tvTen = itemView.findViewById(R.id.tv_ten_mon);
            tvThoiGian = itemView.findViewById(R.id.tv_thoi_gian);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvDoKho = itemView.findViewById(R.id.tv_do_kho);
        }
    }
}
