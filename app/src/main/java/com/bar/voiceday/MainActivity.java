package com.bar.voiceday;

import static java.security.AccessController.getContext;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

//    https://material.io/components/bottom-navigation/android#using-bottom-navigation
//    https://developer.android.com/training/basics/fragments/pass-data-between?hl=ko#java
//    https://developer.android.com/guide/fragments/fragmentmanager?hl=ko
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();

                // Home 화면
                if (itemId == R.id.page_1) {
                    transferTo(HomeFragment.newInstance("param1", "param2"));
                    return true;
                }

                 // Schedule 화면
                if (itemId == R.id.page_2) {
                    Intent intent = new Intent(MainActivity.this, ScheduleActivity.class);
                    startActivity(intent);
                    return true;
                }

//                // Timetable 화면
//                if (itemId == R.id.page_3) {
//                    transferTo(TimetableFragment.newInstance("param1", "param2"));
//                    return true;
//                }
//
                  // Club 화면
//                if (itemId == R.id.page_4) {
//                    Intent intent = new Intent(MainActivity.this, ClubActivity.class);
//                    startActivity(intent);
//                    return true;
//                }

                return false;
            }
        });

        bottomNavigationView.setOnItemReselectedListener(new NavigationBarView.OnItemReselectedListener() {
            @Override
            public void onNavigationItemReselected(@NonNull MenuItem menuItem) {

            }
        });

        transferTo(HomeFragment.newInstance("param1", "param2"));
    }

    private void transferTo(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}