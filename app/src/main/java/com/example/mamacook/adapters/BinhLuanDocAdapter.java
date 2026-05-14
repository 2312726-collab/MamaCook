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
import com.google.firebase.Timestamp;
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_binh_luan_doc, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            DanhGia dg = danhSachBinhLuan.get(position);

            // Hiển thị tên người dùng
            holder.tvTen.setText(
                    dg.getTen_nguoi_dung() != null ? dg.getTen_nguoi_dung() : "Khách hàng"
            );

            // Hiển thị nội dung bình luận
            holder.tvNoiDung.setText(
                    dg.getNoi_dung() != null ? dg.getNoi_dung() : ""
            );

            // Hiển thị số sao
            holder.rbSao.setRating(dg.getSo_sao());

            // Hiển thị thời gian - FIX LỖI: dùng Timestamp.toDate().getTime()
            // thay vì Timestamp.getTime() (không tồn tại)
            Timestamp ngayDanhGia = dg.getNgay_danh_gia();
            if (ngayDanhGia != null) {
                try {
                    // Timestamp của Firebase có method toDate() trả về java.util.Date
                    // java.util.Date mới có method getTime() trả về long milliseconds
                    long timeMillis = ngayDanhGia.toDate().getTime();
                    CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                            timeMillis,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS
                    );
                    holder.tvNgay.setText(timeAgo);
                } catch (Exception e) {
                    holder.tvNgay.setText("Vừa xong");
                }
            } else {
                holder.tvNgay.setText("Vừa xong");
            }

            // Load Avatar từ Firestore theo id_nguoi_dung
            String idNguoiDung = dg.getId_nguoi_dung();
            if (idNguoiDung != null && !idNguoiDung.isEmpty()) {
                FirebaseFirestore.getInstance()
                        .collection("nguoi_dung")
                        .document(idNguoiDung)
                        .get()
                        .addOnSuccessListener(doc -> {
                            try {
                                if (doc != null && doc.exists()) {
                                    String avatarUrl = doc.getString("anh_dai_dien");
                                    Glide.with(holder.itemView.getContext())
                                            .load(avatarUrl)
                                            .placeholder(R.drawable.ic_mama)
                                            .error(R.drawable.ic_mama)
                                            .circleCrop()
                                            .into(holder.imgAvatar);
                                } else {
                                    // Document không tồn tại, dùng ảnh mặc định
                                    Glide.with(holder.itemView.getContext())
                                            .load(R.drawable.ic_mama)
                                            .circleCrop()
                                            .into(holder.imgAvatar);
                                }
                            } catch (Exception e) {
                                // Glide context có thể bị destroyed, bỏ qua
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Lỗi mạng hoặc Firestore, dùng ảnh mặc định
                            try {
                                Glide.with(holder.itemView.getContext())
                                        .load(R.drawable.ic_mama)
                                        .circleCrop()
                                        .into(holder.imgAvatar);
                            } catch (Exception ignored) {
                            }
                        });
            } else {
                // Không có id, load ảnh avatar từ url trực tiếp nếu có
                String avatarUrl = dg.getAvatar_url();
                Glide.with(holder.itemView.getContext())
                        .load(avatarUrl != null ? avatarUrl : R.drawable.ic_mama)
                        .placeholder(R.drawable.ic_mama)
                        .error(R.drawable.ic_mama)
                        .circleCrop()
                        .into(holder.imgAvatar);
            }

            // Load ảnh đính kèm nếu có
            String hinhAnhUrl = dg.getHinh_anh_url();
            if (hinhAnhUrl != null && !hinhAnhUrl.isEmpty()) {
                holder.imgDinhKem.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext())
                        .load(hinhAnhUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_mama)
                        .error(R.drawable.ic_mama)
                        .into(holder.imgDinhKem);
            } else {
                holder.imgDinhKem.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            // Bắt mọi exception không mong đợi để chống crash toàn bộ RecyclerView
            e.printStackTrace();
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