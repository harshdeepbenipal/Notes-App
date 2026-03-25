package ca.yorku.eecs4443.notesapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private Button logoutButton;
    private FirebaseAuth mAuth;

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Firebase
        mAuth = FirebaseAuth.getInstance();

        // Views
        logoutButton = findViewById(R.id.logoutButton);
        drawerLayout = findViewById(R.id.drawer_layout);

        ImageButton menuButton = findViewById(R.id.optionsButton);
        NavigationView navView = findViewById(R.id.nav_view);

        // OPEN DRAWER WHEN CLICKING 3 LINES
        menuButton.setOnClickListener(v -> {
            drawerLayout.openDrawer(android.view.Gravity.START);
        });

        // HANDLE NAVIGATION MENU
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

        // LOGOUT BUTTON
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();

            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}