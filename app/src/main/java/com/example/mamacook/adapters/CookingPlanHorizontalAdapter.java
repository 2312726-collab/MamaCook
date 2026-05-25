package com.example.mamacook.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.mamacook.R;
import com.example.mamacook.activities.DetailMonAnActivity;
import com.example.mamacook.models.CookingPlan;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.List;

public class CookingPlanHorizontalAdapter extends RecyclerView.Adapter<CookingPlanHorizontalAdapter.ViewHolder> {
    private List<CookingPlan> planList;

    public CookingPlanHorizontalAdapter(List<CookingPlan> planList) {
        this.planList = planList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plan_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CookingPlan plan = planList.get(position);
        holder.tvTen.setText(plan.getTen_mon());

        String hinhAnh = plan.getHinh_anh();
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
            intent.putExtra("ID_MON_AN", plan.getId_mon_an());
            v.getContext().startActivity(intent);
        });

        // Xử lý nút xóa món khỏi thực đơn
        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa món '" + plan.getTen_mon() + "' khỏi thực đơn không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        deletePlanFromFirestore(plan.getId_plan(), v.getContext());
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }

    private void deletePlanFromFirestore(String planId, android.content.Context context) {
        if (planId == null) return;
        FirebaseFirestore.getInstance().collection("ke_hoach_nau_an")
                .document(planId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Đã xóa món ăn khỏi kế hoạch!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() { return planList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView img, btnDelete;
        TextView tvTen;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img_plan_h);
            tvTen = itemView.findViewById(R.id.tv_plan_h_ten);
            btnDelete = itemView.findViewById(R.id.btn_delete_plan);
        }
    }
}
