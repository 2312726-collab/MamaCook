package com.example.mamacook.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mamacook.R;
import com.example.mamacook.models.MonAn;

import java.util.List;

public class BuocNauAdapter extends RecyclerView.Adapter<BuocNauAdapter.ViewHolder> {
    private List<MonAn.BuocNau> danhSachBuocNau;
    private int expandedPosition = -1; // Theo dõi vị trí bước đang mở rộng

    public BuocNauAdapter(List<MonAn.BuocNau> danhSachBuocNau) {
        this.danhSachBuocNau = danhSachBuocNau;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_buoc_nau_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonAn.BuocNau buoc = danhSachBuocNau.get(position);
        boolean isExpanded = (position == expandedPosition);

        // Số bước
        holder.tvSoBuoc.setText(String.valueOf(buoc.so_buoc));

        // Tiêu đề bước
        if (buoc.tieu_de != null && !buoc.tieu_de.isEmpty()) {
            holder.tvTieuDe.setText(buoc.tieu_de);
        } else {
            holder.tvTieuDe.setText("Bước " + buoc.so_buoc);
        }

        // Thời gian (nếu có)
        if (buoc.thoi_gian_buoc > 0) {
            holder.tvThoiGian.setVisibility(View.VISIBLE);
            holder.tvThoiGian.setText(buoc.thoi_gian_buoc + " phút");
        } else {
            holder.tvThoiGian.setVisibility(View.GONE);
        }

        // PHẦN CHI TIẾT (Nội dung + Ảnh): Chỉ hiện khi được mở rộng
        holder.layoutChiTiet.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        
        if (isExpanded) {
            // Nội dung
            holder.tvNoiDung.setText(buoc.mo_ta);

            // Ảnh minh họa
            if (buoc.hinh_anh_buoc != null && !buoc.hinh_anh_buoc.isEmpty()) {
                holder.imgBuoc.setVisibility(View.VISIBLE);
                if (buoc.hinh_anh_buoc.startsWith("http")) {
                    Glide.with(holder.itemView.getContext())
                            .load(buoc.hinh_anh_buoc)
                            .placeholder(R.drawable.bg_splash)
                            .into(holder.imgBuoc);
                } else {
                    com.google.firebase.storage.StorageReference storageRef = 
                        com.google.firebase.storage.FirebaseStorage.getInstance()
                            .getReference()
                            .child(buoc.hinh_anh_buoc);
                    Glide.with(holder.itemView.getContext())
                            .load(storageRef)
                            .placeholder(R.drawable.bg_splash)
                            .into(holder.imgBuoc);
                }
            } else {
                holder.imgBuoc.setVisibility(View.GONE);
            }
        }

        // Xử lý click để đóng/mở
        holder.itemView.setOnClickListener(v -> {
            int previousExpanded = expandedPosition;
            expandedPosition = isExpanded ? -1 : holder.getAdapterPosition();
            
            notifyItemChanged(previousExpanded);
            notifyItemChanged(expandedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return danhSachBuocNau != null ? danhSachBuocNau.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSoBuoc, tvTieuDe, tvThoiGian, tvNoiDung;
        ImageView imgBuoc;
        View layoutChiTiet;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSoBuoc = itemView.findViewById(R.id.tv_so_buoc);
            tvTieuDe = itemView.findViewById(R.id.tv_tieu_de_buoc);
            tvThoiGian = itemView.findViewById(R.id.tv_thoi_gian_buoc);
            tvNoiDung = itemView.findViewById(R.id.tv_noi_dung_buoc);
            imgBuoc = itemView.findViewById(R.id.img_buoc_nau);
            layoutChiTiet = itemView.findViewById(R.id.layout_chi_tiet_buoc); // Cần bọc tvNoiDung và imgBuoc vào một layout này
        }
    }
}
