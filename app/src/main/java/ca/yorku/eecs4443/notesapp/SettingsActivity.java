package ca.yorku.eecs4443.notesapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private Button logoutButton;
    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;

    private RadioGroup themeRadioGroup;
    private RadioButton radioLight, radioDark, radioSystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemePreferenceHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();

        logoutButton = findViewById(R.id.logoutButton);
        drawerLayout = findViewById(R.id.drawer_layout);

        themeRadioGroup = findViewById(R.id.themeRadioGroup);
        radioLight = findViewById(R.id.radioLight);
        radioDark = findViewById(R.id.radioDark);
        radioSystem = findViewById(R.id.radioSystem);

        ImageButton menuButton = findViewById(R.id.optionsButton);
        NavigationView navView = findViewById(R.id.nav_view);

        String savedMode = ThemePreferenceHelper.getThemeMode(this);
        switch (savedMode) {
            case ThemePreferenceHelper.MODE_LIGHT:
                radioLight.setChecked(true);
                break;
            case ThemePreferenceHelper.MODE_DARK:
                radioDark.setChecked(true);
                break;
            default:
                radioSystem.setChecked(true);
                break;
        }

        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioLight) {
                ThemePreferenceHelper.saveThemeMode(this, ThemePreferenceHelper.MODE_LIGHT);
            } else if (checkedId == R.id.radioDark) {
                ThemePreferenceHelper.saveThemeMode(this, ThemePreferenceHelper.MODE_DARK);
            } else if (checkedId == R.id.radioSystem) {
                ThemePreferenceHelper.saveThemeMode(this, ThemePreferenceHelper.MODE_SYSTEM);
            }

            ThemePreferenceHelper.applyTheme(this);
            recreate();
        });

        menuButton.setOnClickListener(v -> {
            drawerLayout.openDrawer(android.view.Gravity.START);
        });

        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_notes) {
                startActivity(new Intent(SettingsActivity.this, NoteNavigation.class));
            }

            if (id == R.id.nav_trash) {
                startActivity(new Intent(SettingsActivity.this, TrashActivity.class));
            }

            if (id == R.id.nav_settings) {
                // already here
            }

            drawerLayout.closeDrawers();
            return true;
        });

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();

            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}