package com.example.mamacook.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DanhSachChatAdapter extends RecyclerView.Adapter<DanhSachChatAdapter.ChatViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(DocumentSnapshot doc);
    }

    private final Context context;
    private final List<DocumentSnapshot> chatList;
    private final OnChatClickListener listener;

    public DanhSachChatAdapter(Context context, List<DocumentSnapshot> chatList, OnChatClickListener listener) {
        this.context = context;
        this.chatList = chatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_danh_sach_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        DocumentSnapshot doc = chatList.get(position);

        String tenUser = doc.getString("ten_user");
        String tinNhanCuoi = doc.getString("tin_nhan_cuoi");
        Timestamp time = doc.getTimestamp("thoi_gian_cap_nhat");

        holder.tvTenUser.setText(tenUser == null ? "Người dùng" : tenUser);
        holder.tvTinNhanCuoi.setText(tinNhanCuoi == null ? "Chưa có tin nhắn" : tinNhanCuoi);

        if (time != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            holder.tvThoiGian.setText(sdf.format(time.toDate()));
        } else {
            holder.tvThoiGian.setText("");
        }

        holder.itemView.setOnClickListener(v -> listener.onChatClick(doc));
    }

    @Override
    public int getItemCount() {
        return chatList == null ? 0 : chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvTenUser, tvTinNhanCuoi, tvThoiGian;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTenUser = itemView.findViewById(R.id.tv_ten_user_chat);
            tvTinNhanCuoi = itemView.findViewById(R.id.tv_tin_nhan_cuoi);
            tvThoiGian = itemView.findViewById(R.id.tv_thoi_gian_chat);
        }
    }
}