package com.example.mamacook;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.List;

public class MonAnAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_SEE_ALL = 2;
    
    private List<MonAn> monAnList;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId = FirebaseAuth.getInstance().getUid();
    private String sectionType = ""; // Loại section (DANH_MUC, MOI, LICH_SU...)
    private String categoryId = "";   // Dùng nếu là DANH_MUC

    public MonAnAdapter(List<MonAn> monAnList) {
        this.monAnList = monAnList;
    }

    public void setSectionInfo(String type, String catId) {
        this.sectionType = type;
        this.categoryId = catId;
    }

    @Override
    public int getItemViewType(int position) {
        // Nếu số lượng món > 10 và đây là vị trí cuối cùng -> Hiện nút Xem tất cả
        if (monAnList.size() > 10 && position == 10) {
            return TYPE_SEE_ALL;
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SEE_ALL) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_see_all, parent, false);
            return new SeeAllViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mon_an_home, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ItemViewHolder) {
            MonAn monAn = monAnList.get(position);
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            itemHolder.tvTenMon.setText(monAn.getTen_mon());
            itemHolder.tvThoiGian.setText(monAn.getThoi_gian_nau() + " phút");
            itemHolder.tvRating.setText(String.valueOf(monAn.getRating()));
            
            String hinhAnh = monAn.getHinh_anh();
            if (hinhAnh != null && !hinhAnh.isEmpty()) {
                if (hinhAnh.startsWith("http")) {
                    Glide.with(itemHolder.itemView.getContext()).load(hinhAnh).placeholder(R.drawable.bg_splash).into(itemHolder.imgMonAn);
                } else {
                    StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(hinhAnh);
                    Glide.with(itemHolder.itemView.getContext()).load(storageRef).placeholder(R.drawable.bg_splash).into(itemHolder.imgMonAn);
                }
            }

            itemHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), DetailMonAnActivity.class);
                intent.putExtra("ID_MON_AN", monAn.getId_mon_an());
                v.getContext().startActivity(intent);
            });
        } else if (holder instanceof SeeAllViewHolder) {
            SeeAllViewHolder seeAllHolder = (SeeAllViewHolder) holder;
            
            // Xóa click listener cũ trên toàn bộ item view
            seeAllHolder.itemView.setOnClickListener(null);
            
            // Gán click listener CHỈ cho nút hình tròn trắng
            seeAllHolder.btnSeeAllCircle.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), SeeAllActivity.class);
                intent.putExtra("SECTION_TYPE", sectionType);
                intent.putExtra("CATEGORY_ID", categoryId);
                v.getContext().startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        if (monAnList == null) return 0;
        // Nếu > 10 món thì chỉ hiện 11 item (10 món + 1 nút Xem tất cả)
        return monAnList.size() > 10 ? 11 : monAnList.size();
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView imgMonAn, btnFavorite;
        TextView tvTenMon, tvThoiGian, tvRating;
        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMonAn = itemView.findViewById(R.id.img_mon_an);
            btnFavorite = itemView.findViewById(R.id.btn_favorite);
            tvTenMon = itemView.findViewById(R.id.tv_ten_mon);
            tvThoiGian = itemView.findViewById(R.id.tv_thoi_gian);
            tvRating = itemView.findViewById(R.id.tv_rating);
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
