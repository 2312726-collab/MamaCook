package com.example.mamacook.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.adapters.MonAnVerticalAdapter;
import com.example.mamacook.models.MonAn;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class FavoriteFragment extends Fragment {

    private FirebaseFirestore db;
    private RecyclerView rvFavorite;
    private MonAnVerticalAdapter adapterFavorite;
    private List<MonAn> listFavorite = new ArrayList<>();
    private TextView tvEmptyMessage;
    private final List<ListenerRegistration> monAnListeners = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorite, container, false);

        db = FirebaseFirestore.getInstance();
        rvFavorite = view.findViewById(R.id.rv_favorite);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_favorite);

        rvFavorite.setLayoutManager(new LinearLayoutManager(getContext()));
        adapterFavorite = new MonAnVerticalAdapter(listFavorite);
        rvFavorite.setAdapter(adapterFavorite);

        loadFavoriteRecipes();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Gemini: Hủy tất cả listener để tránh leak memory và cập nhật sai data
        for (ListenerRegistration lr : monAnListeners) {
            if (lr != null) lr.remove();
        }
        monAnListeners.clear();
    }

    private void loadFavoriteRecipes() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Sử dụng addSnapshotListener để cập nhật danh sách yêu thích ngay khi có thay đổi
        db.collection("mon_da_luu")
                .whereEqualTo("id_nguoi_dung", uid)
                .addSnapshotListener((value, error) -> {
                    if (isRemoving() || !isAdded()) return; // Kiểm tra Fragment còn tồn tại không
                    if (error != null) return;

                    if (value == null || value.isEmpty()) {
                        listFavorite.clear();
                        adapterFavorite.notifyDataSetChanged();
                        tvEmptyMessage.setVisibility(View.VISIBLE);
                        rvFavorite.setVisibility(View.GONE);
                        return;
                    }

                    List<String> listIdFavorite = new ArrayList<>();
                    for (DocumentSnapshot doc : value) {
                        String idMonAn = doc.getString("id_mon_an");
                        if (idMonAn != null) listIdFavorite.add(idMonAn);
                    }

                    listFavorite.clear(); // Clear để nhận data realtime
                    
                    // Hủy listener cũ
                    for (ListenerRegistration lr : monAnListeners) lr.remove();
                    monAnListeners.clear();

                    tvEmptyMessage.setVisibility(View.GONE);
                    rvFavorite.setVisibility(View.VISIBLE);

                    for (String id : listIdFavorite) {
                        // Gemini: Lắng nghe realtime từng món ăn, lưu reference để hủy sau này
                        ListenerRegistration lr = db.collection("mon_an").document(id).addSnapshotListener((monDoc, monError) -> {
                            if (!isAdded() || isRemoving()) return;
                            if (monDoc != null && monDoc.exists()) {
                                MonAn mon = monDoc.toObject(MonAn.class);
                                if (mon != null) {
                                    mon.setId_mon_an(monDoc.getId());
                                    
                                    int index = -1;
                                    for (int i = 0; i < listFavorite.size(); i++) {
                                        if (listFavorite.get(i).getId_mon_an().equals(mon.getId_mon_an())) {
                                            index = i;
                                            break;
                                        }
                                    }
                                    
                                    if (index != -1) {
                                        listFavorite.set(index, mon);
                                    } else {
                                        listFavorite.add(mon);
                                    }
                                    adapterFavorite.notifyDataSetChanged();
                                }
                            }
                        });
                        monAnListeners.add(lr);
                    }
                });
    }
}
