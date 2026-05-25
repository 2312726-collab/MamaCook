package com.example.mamacook.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mamacook.R;
import com.example.mamacook.activities.DetailMonAnActivity;
import com.example.mamacook.models.CookingPlan;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CookingPlanAdapter extends RecyclerView.Adapter<CookingPlanAdapter.PlanViewHolder> {

    private List<CookingPlan> planList;
    private boolean isShoppingMode; // true: GioHangActivity, false: ChuanBiNauActivity
    private Set<String> expandedIds = new HashSet<>();

    public CookingPlanAdapter(List<CookingPlan> planList, boolean isShoppingMode) {
        this.planList = planList;
        this.isShoppingMode = isShoppingMode;
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cooking_plan, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        CookingPlan plan = planList.get(position);
        holder.tvTenMon.setText(plan.getTen_mon());

        // Mặc định ẩn các nút, sẽ hiện lại dựa theo logic bên dưới
        holder.btnFinishBuying.setVisibility(View.GONE);
        holder.btnFinishCooking.setVisibility(View.GONE);

        String hinhAnh = plan.getHinh_anh();
        if (hinhAnh != null && !hinhAnh.isEmpty()) {
            if (hinhAnh.startsWith("http")) {
                Glide.with(holder.itemView.getContext()).load(hinhAnh).placeholder(R.drawable.bg_splash).into(holder.imgMonAn);
            } else {
                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(hinhAnh);
                Glide.with(holder.itemView.getContext()).load(storageRef).placeholder(R.drawable.bg_splash).into(holder.imgMonAn);
            }
        }

        if (isShoppingMode) {
            boolean isExpanded = expandedIds.contains(plan.getId_mon_an());
            holder.layoutIngredients.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            
            if (isExpanded) {
                setupIngredients(holder, plan);
            }
            
            holder.itemView.setOnClickListener(v -> {
                if (expandedIds.contains(plan.getId_mon_an())) {
                    expandedIds.remove(plan.getId_mon_an());
                    holder.layoutIngredients.setVisibility(View.GONE);
                    holder.btnFinishBuying.setVisibility(View.GONE);
                } else {
                    expandedIds.add(plan.getId_mon_an());
                    holder.layoutIngredients.setVisibility(View.VISIBLE);
                    setupIngredients(holder, plan);
                }
            });
        } else {
            holder.layoutIngredients.setVisibility(View.GONE);
            holder.btnFinishCooking.setVisibility(View.VISIBLE);

            holder.btnFinishCooking.setOnClickListener(v -> {
                if (plan.getId_plan() == null) return;
                
                // Cập nhật giao diện ngay lập tức (Optimistic UI)
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    planList.remove(currentPos);
                    notifyItemRemoved(currentPos);
                    Toast.makeText(v.getContext(), "Chúc mừng bạn đã hoàn thành món ăn!", Toast.LENGTH_SHORT).show();
                }

                FirebaseFirestore.getInstance().collection("ke_hoach_nau_an")
                        .document(plan.getId_plan())
                        .delete();
            });

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), DetailMonAnActivity.class);
                intent.putExtra("ID_MON_AN", plan.getId_mon_an());
                v.getContext().startActivity(intent);
            });
        }
    }

    private void setupIngredients(PlanViewHolder holder, CookingPlan plan) {
        holder.layoutIngredientsList.removeAllViews();
        List<CookingPlan.IngredientItem> ingredients = plan.getDanh_sach_nguyen_lieu();
        if (ingredients == null) return;

        for (int i = 0; i < ingredients.size(); i++) {
            CookingPlan.IngredientItem item = ingredients.get(i);
            View view = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.item_ingredient_checkbox, holder.layoutIngredientsList, false);
            CheckBox cb = view.findViewById(R.id.cb_ingredient);
            cb.setText(item.getSo_luong() + " " + item.getDon_vi() + " " + item.getTen_nguyen_lieu());
            cb.setChecked(item.isDa_mua());

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.setDa_mua(isChecked);
                updatePlanInFirestore(plan);
                checkAllBought(holder, plan);
            });
            holder.layoutIngredientsList.addView(view);
        }
        checkAllBought(holder, plan);

        holder.btnFinishBuying.setOnClickListener(v -> {
            if (plan.getId_plan() == null) return;

            // Cập nhật giao diện ngay lập tức (Optimistic UI)
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                planList.remove(currentPos);
                notifyItemRemoved(currentPos);
                Toast.makeText(v.getContext(), "Đã chuyển sang danh sách chuẩn bị nấu!", Toast.LENGTH_SHORT).show();
            }

            FirebaseFirestore.getInstance().collection("ke_hoach_nau_an")
                    .document(plan.getId_plan())
                    .update("trang_thai", "cho_nau");
        });
    }

    private void checkAllBought(PlanViewHolder holder, CookingPlan plan) {
        if (!isShoppingMode || !expandedIds.contains(plan.getId_mon_an())) {
            holder.btnFinishBuying.setVisibility(View.GONE);
            return;
        }

        boolean allBought = true;
        if (plan.getDanh_sach_nguyen_lieu() != null && !plan.getDanh_sach_nguyen_lieu().isEmpty()) {
            for (CookingPlan.IngredientItem item : plan.getDanh_sach_nguyen_lieu()) {
                if (!item.isDa_mua()) {
                    allBought = false;
                    break;
                }
            }
        } else {
            allBought = false;
        }
        
        holder.btnFinishBuying.setVisibility(allBought ? View.VISIBLE : View.GONE);
        holder.btnFinishBuying.setEnabled(allBought);
    }

    private void updatePlanInFirestore(CookingPlan plan) {
        if (plan.getId_plan() == null) return;

        FirebaseFirestore.getInstance().collection("ke_hoach_nau_an")
                .document(plan.getId_plan())
                .update("danh_sach_nguyen_lieu", plan.getDanh_sach_nguyen_lieu());
    }

    @Override
    public int getItemCount() {
        return planList != null ? planList.size() : 0;
    }

    public static class PlanViewHolder extends RecyclerView.ViewHolder {
        ImageView imgMonAn;
        TextView tvTenMon;
        LinearLayout layoutIngredients, layoutIngredientsList;
        Button btnFinishBuying, btnFinishCooking;

        public PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMonAn = itemView.findViewById(R.id.img_plan_mon_an);
            tvTenMon = itemView.findViewById(R.id.tv_plan_ten_mon);
            layoutIngredients = itemView.findViewById(R.id.layout_plan_ingredients);
            layoutIngredientsList = itemView.findViewById(R.id.layout_plan_ingredients_list);
            btnFinishBuying = itemView.findViewById(R.id.btn_finish_buying);
            btnFinishCooking = itemView.findViewById(R.id.btn_finish_cooking);
        }
    }
}
