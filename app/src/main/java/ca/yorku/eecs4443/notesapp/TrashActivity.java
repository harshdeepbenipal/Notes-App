package ca.yorku.eecs4443.notesapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;

public class TrashActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArrayList<NoteItemData> trashList;
    private RecyclerViewAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trash_activity);

        recyclerView = findViewById(R.id.trashRecyclerView);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        trashList = new ArrayList<>();
        adapter = new RecyclerViewAdapter(trashList, this, true);
        recyclerView.setAdapter(adapter);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        loadTrashNotes();


        // DRAWER
        DrawerLayout drawerLayout = findViewById(R.id.trash_drawer_layout);
        NavigationView navView = findViewById(R.id.trash_nav_view);

        ImageButton menuBtn = findViewById(R.id.trashOptionsButton);
        menuBtn.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_notes) {
                startActivity(new Intent(this, NoteNavigation.class));
            }

            if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            }

            if (id == R.id.nav_trash) {
                drawerLayout.closeDrawers();
                return true;
            }

            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void loadTrashNotes() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .collection("notes")
                .whereEqualTo("isDeleted", true)
                .orderBy("lastModified", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Log.e("Firestore", "Error: ", error);
                        return;
                    }

                    if (value != null) {
                        trashList.clear();

                        for (QueryDocumentSnapshot doc : value) {

                            String title = doc.getString("title");
                            String content = doc.getString("content");
                            Timestamp lastMod = doc.getTimestamp("lastModified");

                            trashList.add(new NoteItemData(
                                    doc.getId(),
                                    title != null ? title : "Untitled",
                                    content != null ? content : "",
                                    R.drawable.note,
                                    lastMod
                            ));
                        }

                        adapter.notifyDataSetChanged();
                    }
                });
    }
}