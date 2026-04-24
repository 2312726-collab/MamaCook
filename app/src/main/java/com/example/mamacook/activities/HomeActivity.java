package com.example.mamacook.activities;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.mamacook.R;
import com.example.mamacook.fragments.FavoriteFragment;
import com.example.mamacook.fragments.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private int currentTabPosition = -1;
    private Fragment activeFragment;
    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            int newPosition = getTabPosition(id);

            if (newPosition == currentTabPosition) return true;

            showFragment(id, newPosition > currentTabPosition);
            currentTabPosition = newPosition;
            return true;
        });

        if (savedInstanceState == null) {
            // Mặc định ban đầu
            showFragment(R.id.nav_home, true);
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    private int getTabPosition(int id) {
        if (id == R.id.nav_home) return 0;
        if (id == R.id.nav_recipes) return 1;
        if (id == R.id.nav_favorites) return 2;
        if (id == R.id.nav_profile) return 3;
        return -1;
    }

    private void showFragment(int navId, boolean isNext) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        // Thiết lập hiệu ứng chuyển động mượt mà
        if (currentTabPosition != -1) {
            if (isNext) {
                transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        }

        // Ẩn fragment hiện tại (nếu có)
        if (activeFragment != null) {
            transaction.hide(activeFragment);
        }

        // Lấy hoặc tạo mới fragment cần hiển thị
        Fragment targetFragment = fragmentMap.get(navId);
        if (targetFragment == null) {
            if (navId == R.id.nav_home) targetFragment = new HomeFragment();
            else if (navId == R.id.nav_favorites) targetFragment = new FavoriteFragment();
            // else if (navId == R.id.nav_recipes) targetFragment = new RecipeFragment();
            // else if (navId == R.id.nav_profile) targetFragment = new AccountFragment();

            if (targetFragment != null) {
                fragmentMap.put(navId, targetFragment);
                transaction.add(R.id.fragment_container, targetFragment);
            }
        } else {
            transaction.show(targetFragment);
        }

        if (targetFragment != null) {
            activeFragment = targetFragment;
            transaction.commit();
        }
    }
}
