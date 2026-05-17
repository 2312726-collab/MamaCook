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
import com.example.mamacook.activities.SeeAllActivity;
import com.example.mamacook.models.MonAn;
import com.example.mamacook.utils.RatingUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

/**
 * Adapter chính hiển thị danh sách món ăn ở màn hình Home (Dạng ngang).
 * Có tích hợp nút "Xem tất cả" khi danh sách vượt quá 10 món.
 */
public class MonAnAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM    = 1;
    private static final int TYPE_SEE_ALL = 2;

    private List<MonAn> monAnList;
    private String sectionType = "";
    private String categoryId  = "";

    // Lưu trữ thông tin bộ lọc để gửi sang SeeAllActivity
    private String filterDifficulty = "Tất cả";
    private String filterTime       = "Tất cả";
    private String filterRating     = "Tất cả";

    public MonAnAdapter(List<MonAn> monAnList) {
        this.monAnList = monAnList;
    }

    /**
     * Cung cấp thông tin để nút "Xem tất cả" biết cần chuyển sang mục nào và mang theo bộ lọc.
     */
    public void setSectionInfo(String type, String catId,
                               String difficulty, String time, String rating) {
        this.sectionType       = type;
        this.categoryId        = catId;
        this.filterDifficulty  = difficulty;
        this.filterTime        = time;
        this.filterRating      = rating;
    }

    // Overload giữ lại hàm cũ để không làm hỏng các chỗ gọi khác (Featured, New, History)
    public void setSectionInfo(String type, String catId) {
        setSectionInfo(type, catId, "Tất cả", "Tất cả", "Tất cả");
    }

    @Override
    public int getItemViewType(int position) {
        // Hiện nút "Xem tất cả" tại vị trí thứ 11 nếu danh sách > 10 món.
        if (monAnList.size() > 10 && position == 10) {
            return TYPE_SEE_ALL;
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SEE_ALL) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_see_all, parent, false);
            return new SeeAllViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mon_an_home, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {
            MonAn monAn = monAnList.get(position);
            ItemViewHolder itemHolder = (ItemViewHolder) holder;

            itemHolder.tvTenMon.setText(monAn.getTen_mon());
            itemHolder.tvThoiGian.setText(monAn.getThoi_gian_nau() + " phút");
            itemHolder.tvRating.setText(RatingUtils.getRatingOnly(monAn));
            itemHolder.tvDoKho.setText("Độ khó: " + monAn.getDo_kho());

            itemHolder.layoutBadgeAi.setVisibility(View.GONE);

            checkIsFavorite(monAn.getId_mon_an(), itemHolder.btnFavorite);

            // Load ảnh
            String hinhAnh = monAn.getHinh_anh();
            if (hinhAnh != null && !hinhAnh.isEmpty()) {
                if (hinhAnh.startsWith("http")) {
                    Glide.with(itemHolder.itemView.getContext())
                            .load(hinhAnh)
                            .placeholder(R.drawable.bg_splash)
                            .into(itemHolder.imgMonAn);
                } else {
                    StorageReference storageRef = FirebaseStorage.getInstance()
                            .getReference()
                            .child(hinhAnh);
                    Glide.with(itemHolder.itemView.getContext())
                            .load(storageRef)
                            .placeholder(R.drawable.bg_splash)
                            .into(itemHolder.imgMonAn);
                }
            }

            // ✅ FIX: Dùng createIntent() thay vì tự tạo Intent
            // Truyền toàn bộ data (tên, ảnh, rating, nguyên liệu...) vào Intent
            // → DetailMonAnActivity hiển thị ngay lập tức, không cần chờ Firestore
            itemHolder.itemView.setOnClickListener(v -> {
                Intent intent = DetailMonAnActivity.createIntent(v.getContext(), monAn);
                v.getContext().startActivity(intent);
                if (v.getContext() instanceof android.app.Activity) {
                    ((android.app.Activity) v.getContext()).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
            });

        } else if (holder instanceof SeeAllViewHolder) {
            SeeAllViewHolder seeAllHolder = (SeeAllViewHolder) holder;
            seeAllHolder.btnSeeAllCircle.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), SeeAllActivity.class);
                intent.putExtra("SECTION_TYPE",       sectionType);
                intent.putExtra("CATEGORY_ID",        categoryId);
                intent.putExtra("FILTER_DIFFICULTY",  filterDifficulty);
                intent.putExtra("FILTER_TIME",        filterTime);
                intent.putExtra("FILTER_RATING",      filterRating);
                v.getContext().startActivity(intent);
                if (v.getContext() instanceof android.app.Activity) {
                    ((android.app.Activity) v.getContext()).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
            });
        }
    }

    private void checkIsFavorite(String dishId, ImageView btnFavorite) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("mon_da_luu")
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
        if (monAnList == null) {
            return 0;
        }
        return monAnList.size() > 10 ? 11 : monAnList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView    imgMonAn, btnFavorite;
        TextView     tvTenMon, tvThoiGian, tvRating, tvDoKho, tvMatchScore;
        LinearLayout layoutBadgeAi;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMonAn      = itemView.findViewById(R.id.img_mon_an);
            btnFavorite   = itemView.findViewById(R.id.btn_favorite);
            tvTenMon      = itemView.findViewById(R.id.tv_ten_mon);
            tvThoiGian    = itemView.findViewById(R.id.tv_thoi_gian);
            tvRating      = itemView.findViewById(R.id.tv_rating);
            tvDoKho       = itemView.findViewById(R.id.tv_do_kho);
            layoutBadgeAi = itemView.findViewById(R.id.layout_badge_ai);
            tvMatchScore  = itemView.findViewById(R.id.tv_match_score);
        }
    }

    public static class SeeAllViewHolder extends RecyclerView.ViewHolder {
        View btnSeeAllCircle;

        public SeeAllViewHolder(@NonNull View itemView) {
            super(itemView);
            btnSeeAllCircle = itemView.findViewById(R.id.btn_see_all_circle);
        }
    }
}