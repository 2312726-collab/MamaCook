package com.example.mamacook.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class TinNhanAdapter extends RecyclerView.Adapter<TinNhanAdapter.TinNhanViewHolder> {

    private final Context context;
    private final List<DocumentSnapshot> tinNhanList;
    private final String cheDo;

    public TinNhanAdapter(Context context, List<DocumentSnapshot> tinNhanList, String cheDo) {
        this.context = context;
        this.tinNhanList = tinNhanList;
        this.cheDo = cheDo;
    }

    @NonNull
    @Override
    public TinNhanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tin_nhan, parent, false);
        return new TinNhanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TinNhanViewHolder holder, int position) {
        DocumentSnapshot doc = tinNhanList.get(position);

        String noiDung = doc.getString("noi_dung");
        String nguoiGui = doc.getString("nguoi_gui");

        if (noiDung == null) noiDung = "";
        if (nguoiGui == null) nguoiGui = "user";

        holder.tvTinNhan.setText(noiDung);

        boolean laTinNhanCuaMinh = nguoiGui.equals(cheDo);

        if (laTinNhanCuaMinh) {
            holder.layoutItem.setGravity(Gravity.END);
            holder.tvTinNhan.setBackgroundResource(R.drawable.bg_chat_mine);
        } else {
            holder.layoutItem.setGravity(Gravity.START);
            holder.tvTinNhan.setBackgroundResource(R.drawable.bg_chat_other);
        }
    }

    @Override
    public int getItemCount() {
        return tinNhanList == null ? 0 : tinNhanList.size();
    }

    static class TinNhanViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutItem;
        TextView tvTinNhan;

        public TinNhanViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutItem = itemView.findViewById(R.id.layout_item_tin_nhan);
            tvTinNhan = itemView.findViewById(R.id.tv_tin_nhan);
        }
    }
}