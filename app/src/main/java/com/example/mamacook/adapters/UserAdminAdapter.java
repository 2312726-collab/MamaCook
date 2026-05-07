package com.example.mamacook.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UserAdminAdapter extends RecyclerView.Adapter<UserAdminAdapter.UserViewHolder> {

    private final Context context;
    private final List<DocumentSnapshot> userList;
    private final FirebaseFirestore db;

    public UserAdminAdapter(Context context, List<DocumentSnapshot> userList) {
        this.context = context;
        this.userList = userList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_admin, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        DocumentSnapshot doc = userList.get(position);

        String userId = doc.getId();
        String hoTen = doc.getString("ho_ten");
        String email = doc.getString("email");
        String vaiTro = doc.getString("vai_tro");
        String trangThai = doc.getString("trang_thai_tai_khoan");
        Long soLanViPham = doc.getLong("so_lan_vi_pham");

        holder.tvHoTen.setText(hoTen == null ? "Chưa có tên" : hoTen);
        holder.tvEmail.setText(email == null ? "Chưa có email" : email);
        holder.tvVaiTro.setText("Vai trò: " + (vaiTro == null ? "user" : vaiTro));
        holder.tvTrangThai.setText("Trạng thái: " + (trangThai == null ? "hoat_dong" : trangThai));

        holder.tvSoLanViPham.setText(
                "Số lần vi phạm: " + (soLanViPham == null ? 0 : soLanViPham)
        );

        if (soLanViPham != null && soLanViPham > 0) {
            holder.tvSoLanViPham.setVisibility(View.VISIBLE);
            holder.tvSoLanViPham.setTextColor(Color.RED);
        } else {
            holder.tvSoLanViPham.setVisibility(View.VISIBLE);
            holder.tvSoLanViPham.setTextColor(Color.parseColor("#777777"));
        }

        if ("bi_khoa".equals(trangThai)) {
            holder.tvTrangThai.setTextColor(Color.RED);
            holder.btnKhoaMo.setText("Mở khóa");
        } else {
            holder.tvTrangThai.setTextColor(Color.parseColor("#2E7D32"));
            holder.btnKhoaMo.setText("Khóa");
        }

        if ("admin".equals(vaiTro)) {
            holder.btnDoiVaiTro.setText("Đổi thành user");
        } else {
            holder.btnDoiVaiTro.setText("Đổi thành admin");
        }

        holder.btnKhoaMo.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION
                    || currentPosition >= userList.size()) {
                return;
            }

            DocumentSnapshot currentDoc = userList.get(currentPosition);
            String currentUserId = currentDoc.getId();
            String currentTrangThai = currentDoc.getString("trang_thai_tai_khoan");

            String trangThaiMoi;

            if ("bi_khoa".equals(currentTrangThai)) {
                trangThaiMoi = "dang_hoat_dong";
            } else {
                trangThaiMoi = "bi_khoa";
            }

            final String finalTrangThaiMoi = trangThaiMoi;

            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận")
                    .setMessage("Bạn có chắc muốn cập nhật trạng thái tài khoản này không?")
                    .setPositiveButton("Có", (dialog, which) -> db.collection("nguoi_dung")
                            .document(currentUserId)
                            .update("trang_thai_tai_khoan", finalTrangThaiMoi)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(context, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show();

                                db.collection("nguoi_dung")
                                        .document(currentUserId)
                                        .get()
                                        .addOnSuccessListener(newDoc -> {
                                            capNhatUserTheoId(currentUserId, newDoc);
                                        });
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                    .setNegativeButton("Không", null)
                    .show();
        });

        holder.btnDoiVaiTro.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();

            if (currentPosition == RecyclerView.NO_POSITION
                    || currentPosition >= userList.size()) {
                return;
            }

            DocumentSnapshot currentDoc = userList.get(currentPosition);
            String currentUserId = currentDoc.getId();
            String currentVaiTro = currentDoc.getString("vai_tro");

            String vaiTroMoi;

            if ("admin".equals(currentVaiTro)) {
                vaiTroMoi = "user";
            } else {
                vaiTroMoi = "admin";
            }

            final String finalVaiTroMoi = vaiTroMoi;

            new AlertDialog.Builder(context)
                    .setTitle("Xác nhận")
                    .setMessage("Bạn có chắc muốn đổi vai trò tài khoản này không?")
                    .setPositiveButton("Có", (dialog, which) -> db.collection("nguoi_dung")
                            .document(currentUserId)
                            .update("vai_tro", finalVaiTroMoi)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(context, "Đã cập nhật vai trò", Toast.LENGTH_SHORT).show();

                                db.collection("nguoi_dung")
                                        .document(currentUserId)
                                        .get()
                                        .addOnSuccessListener(newDoc -> {
                                            capNhatUserTheoId(currentUserId, newDoc);
                                        });
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()))
                    .setNegativeButton("Không", null)
                    .show();
        });
    }

    private void capNhatUserTheoId(String userId, DocumentSnapshot newDoc) {
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).getId().equals(userId)) {
                userList.set(i, newDoc);
                notifyItemChanged(i);
                return;
            }
        }
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
}