package com.phatvv9.wwarning;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.phatvv9.wwarning.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        }

        // Ẩn tab Admin nếu không phải tài khoản Admin (mặc định ẩn)
        hideAdminTab();
    }

    private void hideAdminTab() {
        Menu menu = binding.bottomNavigation.getMenu();
        MenuItem adminItem = menu.findItem(R.id.adminFragment);
        if (adminItem != null) {
            adminItem.setVisible(false);
        }
    }
}