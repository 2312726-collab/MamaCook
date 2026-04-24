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
import com.example.mamacook.fragments.AccountFragment;
import com.example.mamacook.fragments.FavoriteFragment;
import com.example.mamacook.fragments.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Map<Integer, Fragment> fragmentMap = new HashMap<>();
    private int currentId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hiếu: Làm trong suốt thanh trạng thái
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

        // Đăng ký các Fragment vào Map
        fragmentMap.put(R.id.nav_home, new HomeFragment());
        fragmentMap.put(R.id.nav_favorites, new FavoriteFragment());
        fragmentMap.put(R.id.nav_profile, new AccountFragment());

        // Lắng nghe sự kiện click Menu
        bottomNavigationView.setOnItemSelectedListener(item -> {
            showFragment(item.getItemId());
            return true;
        });

        // Mặc định hiển thị Trang chủ khi vừa vào
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    private void showFragment(int id) {
        if (currentId == id) return;
        
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        
        // Hiệu ứng trượt Slide
        if (currentId != -1) {
            if (id > currentId) {
                transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        }

        // Ẩn Fragment hiện tại
        Fragment currentFragment = fragmentManager.findFragmentByTag(String.valueOf(currentId));
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }

        // Hiển thị hoặc Thêm mới Fragment được chọn
        Fragment nextFragment = fragmentManager.findFragmentByTag(String.valueOf(id));
        if (nextFragment == null) {
            nextFragment = fragmentMap.get(id);
            transaction.add(R.id.fragment_container, nextFragment, String.valueOf(id));
        } else {
            transaction.show(nextFragment);
        }

        transaction.commit();
        currentId = id;
    }
}
