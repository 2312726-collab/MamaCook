package com.example.mamacook.adapters;

import android.text.format.DateUtils;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class BinhLuanDocAdapter extends RecyclerView.Adapter<BinhLuanDocAdapter.ViewHolder> {
    private List<DanhGia> danhSachBinhLuan;

    public BinhLuanDocAdapter(List<DanhGia> danhSachBinhLuan) {
        this.danhSachBinhLuan = danhSachBinhLuan;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_binh_luan_doc, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DanhGia dg = danhSachBinhLuan.get(position);
        
        holder.tvTen.setText(dg.getTen_nguoi_dung() != null ? dg.getTen_nguoi_dung() : "Khách hàng");
        holder.tvNoiDung.setText(dg.getNoi_dung());
        holder.rbSao.setRating(dg.getSo_sao());
        
        if (dg.getNgay_danh_gia() != null) {
            long time = dg.getNgay_danh_gia().getTime();
            String timeAgo = (String) DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            holder.tvNgay.setText(timeAgo);
        } else {
            holder.tvNgay.setText("Vừa xong");
        }

        // Load Avatar thật từ Firestore
        if (dg.getId_nguoi_dung() != null) {
            FirebaseFirestore.getInstance().collection("nguoi_dung").document(dg.getId_nguoi_dung())
                    .get().addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String avatar = doc.getString("anh_dai_dien");
                            Glide.with(holder.itemView.getContext())
                                    .load(avatar)
                                    .placeholder(R.drawable.ic_mama)
                                    .circleCrop()
                                    .into(holder.imgAvatar);
                        }
                    });
        }

        // Load ảnh đính kèm nếu có
        if (dg.getHinh_anh_url() != null && !dg.getHinh_anh_url().isEmpty()) {
            holder.imgDinhKem.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(dg.getHinh_anh_url())
                    .centerCrop()
                    .into(holder.imgDinhKem);
        } else {
            holder.imgDinhKem.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return danhSachBinhLuan != null ? danhSachBinhLuan.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTen, tvNoiDung, tvNgay;
        RatingBar rbSao;
        ImageView imgAvatar, imgDinhKem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTen = itemView.findViewById(R.id.tvTenNguoiBinhLuan);
            tvNoiDung = itemView.findViewById(R.id.tvNoiDungBinhLuan);
            tvNgay = itemView.findViewById(R.id.tvNgayBinhLuan);
            rbSao = itemView.findViewById(R.id.rbSaoBinhLuan);
            imgAvatar = itemView.findViewById(R.id.imgAvatarBinhLuan);
            imgDinhKem = itemView.findViewById(R.id.imgBinhLuanDinhKem);
        }
    }
}
