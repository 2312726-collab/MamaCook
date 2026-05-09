package com.example.mamacook.adapters;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import java.util.Locale;

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

        // Gemini: Dùng Utils để đồng bộ hiển thị Rating chuẩn toàn app
        holder.tvRating.setText(RatingUtils.getRatingString(monAn));

        // Gemini: Hiển thị Badge AI Match Score
        if (monAn.getMatch_score() >= 50) {
            holder.layoutBadgeAi.setVisibility(View.VISIBLE);
            holder.tvMatchScore.setText("🔥 " + monAn.getMatch_score() + "%");
        } else {
            holder.layoutBadgeAi.setVisibility(View.GONE);
        }

        holder.tvDoKho.setText("Độ khó: " + monAn.getDo_kho());

        checkIsFavorite(monAn.getId_mon_an(), holder.btnFav);

        holder.btnFav.setOnClickListener(v -> {
            toggleFavorite(monAn, holder.itemView.getContext());
        });

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
    }

    private void toggleFavorite(MonAn monAn, android.content.Context context) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        String idLuu = uid + "_" + monAn.getId_mon_an();

        FirebaseFirestore.getInstance().collection("mon_da_luu").document(idLuu).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        new AlertDialog.Builder(context)
                                .setTitle("Xác nhận")
                                .setMessage("Bạn có chắc chắn muốn bỏ yêu thích món ăn này không?")
                                .setPositiveButton("Có", (dialog, which) -> {
                                    FirebaseFirestore.getInstance().collection("mon_da_luu").document(idLuu).delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(context, "Đã xóa khỏi món ăn yêu thích!", Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                                .show();
                    } else {
                        java.util.Map<String, Object> data = new java.util.HashMap<>();
                        data.put("id_nguoi_dung", uid);
                        data.put("id_mon_an", monAn.getId_mon_an());
                        data.put("id_luu", idLuu);
                        FirebaseFirestore.getInstance().collection("mon_da_luu").document(idLuu).set(data)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(context, "Đã thêm vào yêu thích!", Toast.LENGTH_SHORT).show();
                                });
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
        TextView tvTen, tvThoiGian, tvRating, tvDoKho, tvMatchScore;
        LinearLayout layoutBadgeAi;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img_mon_an);
            btnFav = itemView.findViewById(R.id.btn_favorite);
            tvTen = itemView.findViewById(R.id.tv_ten_mon);
            tvThoiGian = itemView.findViewById(R.id.tv_thoi_gian);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvDoKho = itemView.findViewById(R.id.tv_do_kho);
            layoutBadgeAi = itemView.findViewById(R.id.layout_badge_ai);
            tvMatchScore = itemView.findViewById(R.id.tv_match_score);
        }
    }
}
