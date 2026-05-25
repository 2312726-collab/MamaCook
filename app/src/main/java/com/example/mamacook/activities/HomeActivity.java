package com.example.mamacook.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.mamacook.R;
import com.example.mamacook.fragments.AccountFragment;
import com.example.mamacook.fragments.FavoriteFragment;
import com.example.mamacook.fragments.HomeFragment;
import com.example.mamacook.fragments.ScheduleFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private int currentId = -1;
    private ImageButton btnChatFloat;
    private String tenNguoiDung = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Làm trong suốt thanh trạng thái
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
            window.setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_home);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnChatFloat = findViewById(R.id.btn_chat_float);

        // Load tên người dùng để truyền sang ChatActivity
        loadUserName();

        btnChatFloat.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ChatActivity.class);
            intent.putExtra("che_do", "user");
            intent.putExtra("ten_user", tenNguoiDung);
            startActivity(intent);
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            showFragment(item.getItemId());
            return true;
        });

        // Khôi phục trạng thái hoặc hiển thị mặc định
        if (savedInstanceState != null) {
            currentId = savedInstanceState.getInt("currentId", R.id.nav_home);
            bottomNavigationView.setSelectedItemId(currentId);
        } else {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
            showFragment(R.id.nav_home);
        }

        // Xử lý nút Back
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentId != R.id.nav_home) {
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                } else {
                    showExitDialog();
                }
            }
        });
    }

    private void loadUserName() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("nguoi_dung").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tenNguoiDung = doc.getString("ho_ten");
                    }
                });
    }

    private void showFragment(int id) {
        if (currentId == id) return;

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Hiệu ứng trượt Slide
        if (currentId != -1) {
            int currentPos = getNavPosition(currentId);
            int nextPos = getNavPosition(id);
            if (nextPos > currentPos) {
                transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        }

        String nextTag = String.valueOf(id);
        Fragment nextFragment = fragmentManager.findFragmentByTag(nextTag);

        // Ẩn tất cả các Fragment đang có
        for (Fragment fragment : fragmentManager.getFragments()) {
            transaction.hide(fragment);
        }

        if (nextFragment == null) {
            nextFragment = createFragmentById(id);
            transaction.add(R.id.fragment_container, nextFragment, nextTag);
        } else {
            transaction.show(nextFragment);
        }

        transaction.commit();
        currentId = id;
    }

    private Fragment createFragmentById(int id) {
        if (id == R.id.nav_home) return new HomeFragment();
        if (id == R.id.nav_recipes) return new ScheduleFragment();
        if (id == R.id.nav_favorites) return new FavoriteFragment();
        if (id == R.id.nav_profile) return new AccountFragment();
        return new HomeFragment();
    }

    private int getNavPosition(int id) {
        if (id == R.id.nav_home) return 0;
        if (id == R.id.nav_recipes) return 1;
        if (id == R.id.nav_favorites) return 2;
        if (id == R.id.nav_profile) return 3;
        return 0;
    }

    private void showExitDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_exit_confirm, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(view)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        view.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.btn_dialog_exit).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        dialog.show();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentId", currentId);
    }
}
