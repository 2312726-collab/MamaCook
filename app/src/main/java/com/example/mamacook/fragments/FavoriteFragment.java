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

import java.util.ArrayList;
import java.util.List;

public class FavoriteFragment extends Fragment {

    private FirebaseFirestore db;
    private RecyclerView rvFavorite;
    private MonAnVerticalAdapter adapterFavorite;
    private List<MonAn> listFavorite = new ArrayList<>();
    private TextView tvEmptyMessage;

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

                    tvEmptyMessage.setVisibility(View.GONE);
                    rvFavorite.setVisibility(View.VISIBLE);

                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                    for (String id : listIdFavorite) {
                        tasks.add(db.collection("mon_an").document(id).get());
                    }

                    Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                        if (isRemoving() || !isAdded()) return;
                        listFavorite.clear();
                        for (Object result : results) {
                            DocumentSnapshot doc = (DocumentSnapshot) result;
                            if (doc.exists()) {
                                MonAn mon = doc.toObject(MonAn.class);
                                if (mon != null) {
                                    mon.setId_mon_an(doc.getId());
                                    listFavorite.add(mon);
                                }
                            }
                        }
                        adapterFavorite.notifyDataSetChanged();
                    });
                });
    }
}
