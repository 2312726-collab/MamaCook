package com.example.mamacook.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.models.User;

import java.util.ArrayList;
import java.util.List;

public class UserAdminAdapter extends RecyclerView.Adapter<UserAdminAdapter.UserViewHolder> {

    public interface OnUserActionListener {
        void onToggleStatus(User user);
        void onChangeRole(User user);
    }

    private final Context context;
    private List<User> userList;
    private final OnUserActionListener listener;

    public UserAdminAdapter(Context context, List<User> userList, OnUserActionListener listener) {
        this.context = context;
        this.userList = new ArrayList<>(userList);
        this.listener = listener;
    }

    // Sử dụng DiffUtil để cập nhật danh sách mượt mà và tối ưu hiệu năng
    public void updateList(List<User> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new UserDiffCallback(this.userList, newList));
        this.userList.clear();
        this.userList.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_admin, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        holder.tvHoTen.setText(user.getHo_ten() != null ? user.getHo_ten() : "Chưa có tên");
        holder.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "Chưa có email");
        holder.tvVaiTro.setText("Role: " + user.getRole());
        
        String trangThai = user.getTrang_thai_tai_khoan();
        holder.tvTrangThai.setText("Trạng thái: " + (trangThai == null ? "dang_hoat_dong" : trangThai));

        int viPham = user.getSo_lan_vi_pham();
        holder.tvSoLanViPham.setText("Số lần vi phạm: " + viPham);
        holder.tvSoLanViPham.setTextColor(viPham > 0 ? Color.RED : Color.parseColor("#777777"));

        if ("bi_khoa".equals(trangThai)) {
            holder.tvTrangThai.setTextColor(Color.RED);
            holder.btnKhoaMo.setText("Mở khóa");
        } else {
            holder.tvTrangThai.setTextColor(Color.parseColor("#2E7D32"));
            holder.btnKhoaMo.setText("Khóa");
        }

        holder.btnDoiVaiTro.setText("admin".equals(user.getRole()) ? "Đổi thành user" : "Đổi thành admin");

        holder.btnKhoaMo.setOnClickListener(v -> {
            if (listener != null) listener.onToggleStatus(user);
        });

        holder.btnDoiVaiTro.setOnClickListener(v -> {
            if (listener != null) listener.onChangeRole(user);
        });
    }

    @Override
    public int getItemCount() {
        return userList == null ? 0 : userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvHoTen, tvEmail, tvVaiTro, tvTrangThai, tvSoLanViPham;
        Button btnKhoaMo, btnDoiVaiTro;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHoTen = itemView.findViewById(R.id.tv_ho_ten_user);
            tvEmail = itemView.findViewById(R.id.tv_email_user);
            tvVaiTro = itemView.findViewById(R.id.tv_vai_tro_user);
            tvTrangThai = itemView.findViewById(R.id.tv_trang_thai_user);
            tvSoLanViPham = itemView.findViewById(R.id.tv_so_lan_vi_pham_user);
            btnKhoaMo = itemView.findViewById(R.id.btn_khoa_mo_user);
            btnDoiVaiTro = itemView.findViewById(R.id.btn_doi_vai_tro_user);
        }
    }

    // Lớp hỗ trợ so sánh sự khác biệt giữa hai danh sách người dùng
    private static class UserDiffCallback extends DiffUtil.Callback {
        private final List<User> oldList;
        private final List<User> newList;

        public UserDiffCallback(List<User> oldList, List<User> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList.size(); }

        @Override
        public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId_nguoi_dung().equals(newList.get(newItemPosition).getId_nguoi_dung());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            User oldUser = oldList.get(oldItemPosition);
            User newUser = newList.get(newItemPosition);
            return oldUser.getRole().equals(newUser.getRole()) &&
                   oldUser.getTrang_thai_tai_khoan().equals(newUser.getTrang_thai_tai_khoan()) &&
                   oldUser.getSo_lan_vi_pham() == newUser.getSo_lan_vi_pham();
        }
    }
}
