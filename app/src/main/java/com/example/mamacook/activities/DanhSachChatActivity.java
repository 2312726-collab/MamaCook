package com.example.mamacook.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mamacook.R;
import com.example.mamacook.adapters.DanhSachChatAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DanhSachChatActivity extends AppCompatActivity {

    private RecyclerView rvDanhSachChat;
    private ImageButton btnBack;
    private FirebaseFirestore db;
    private ListenerRegistration listener;
    private DanhSachChatAdapter adapter;
    private final List<DocumentSnapshot> chatList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_danh_sach_chat);

        db = FirebaseFirestore.getInstance();

        rvDanhSachChat = findViewById(R.id.rv_danh_sach_chat);
        btnBack = findViewById(R.id.btn_back_danh_sach_chat);

        adapter = new DanhSachChatAdapter(this, chatList, doc -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("id_cuoc_tro_chuyen", doc.getId());
            intent.putExtra("che_do", "admin");
            intent.putExtra("ten_user", doc.getString("ten_user"));
            startActivity(intent);
        });

        rvDanhSachChat.setLayoutManager(new LinearLayoutManager(this));
        rvDanhSachChat.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        loadDanhSachChat();
    }

    private void loadDanhSachChat() {
        listener = db.collection("cuoc_tro_chuyen")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải chat: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (querySnapshot == null) return;

                    chatList.clear();
                    chatList.addAll(querySnapshot.getDocuments());

                    Collections.sort(chatList, (d1, d2) -> {
                        Timestamp t1 = d1.getTimestamp("thoi_gian_cap_nhat");
                        Timestamp t2 = d2.getTimestamp("thoi_gian_cap_nhat");

                        if (t1 == null && t2 == null) return 0;
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;

                        return t2.compareTo(t1);
                    });

                    adapter.notifyDataSetChanged();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}