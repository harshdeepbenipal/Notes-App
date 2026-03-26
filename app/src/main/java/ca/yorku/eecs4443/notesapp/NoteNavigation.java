package ca.yorku.eecs4443.notesapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.navigation.NavigationView;

public class NoteNavigation extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArrayList<NoteItemData> recyclerDataArrayList;
    private RecyclerViewAdapter adapter;
    private DrawerLayout drawerLayout;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private EditText searchBar;
    private ArrayList<NoteItemData> fullList; // keeps original notes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_activity);

        recyclerView = findViewById(R.id.recyclerView);
        searchBar = findViewById(R.id.searchBar);
        fullList = new ArrayList<>();
        drawerLayout = findViewById(R.id.drawer_layout);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        recyclerDataArrayList = new ArrayList<>();
        adapter = new RecyclerViewAdapter(recyclerDataArrayList, this,false);
        recyclerView.setAdapter(adapter);

        // Works on Cassandra's phone, if possible would be nice to get the sizes of the card width
        int cardWidth = 200;
        int numCol = calColumns(getApplicationContext(), cardWidth);
        recyclerView.setLayoutManager(new GridLayoutManager(this, numCol));

        // ONLY SHOW NON-DELETED NOTES
        db.collection("users")
                .document(userId)
                .collection("notes")
                .orderBy("lastModified", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Log.e("Firestore", "Error: ", error);
                        return;
                    }

                    if (value != null) {
                        recyclerDataArrayList.clear();
                        fullList.clear();

                        for (QueryDocumentSnapshot doc : value) {

                            Boolean isDeleted = doc.getBoolean("isDeleted");
                            if (isDeleted != null && isDeleted) continue;

                            String title = doc.getString("title");
                            String content = doc.getString("content");
                            Timestamp lastMod = doc.getTimestamp("lastModified");

                            NoteItemData note = new NoteItemData(
                                    doc.getId(),
                                    title != null ? title : "Untitled",
                                    content != null ? content : "",
                                    R.drawable.note,
                                    lastMod
                            );

                            recyclerDataArrayList.add(note);
                            fullList.add(note);
                        }

                        adapter.notifyDataSetChanged();
                    }
                });

        ImageButton menuButton = findViewById(R.id.optionsButton);
        menuButton.setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        ImageButton createButton = findViewById(R.id.createButton);
        createButton.setOnClickListener(v -> {
            Intent intent = new Intent(NoteNavigation.this, NoteEditingActivity.class);
            intent.putExtra("id", "");
            intent.putExtra("title", "");
            intent.putExtra("content", "");
            startActivity(intent);
        });

        NavigationView navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_notes) {
                drawerLayout.closeDrawers();
                return true;
            }

            if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            }

            if (id == R.id.nav_trash) {
                startActivity(new Intent(this, TrashActivity.class));
            }

            drawerLayout.closeDrawers();
            return true;
        });
        searchBar.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    public int calColumns(Context context, float columnWidthDP) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float screenWidthDP = displayMetrics.widthPixels / displayMetrics.density;
        return (int) (screenWidthDP / columnWidthDP + 0.5);
    }
    private void filterNotes(String query) {
        if (query.trim().isEmpty()) {
            recyclerDataArrayList.clear();
            recyclerDataArrayList.addAll(fullList);
            adapter.notifyDataSetChanged();
            return;
        }

        ArrayList<NoteItemData> filteredList = new ArrayList<>();
        String[] keywords = query.toLowerCase().split(" ");

        for (NoteItemData note : fullList) {
            String title = note.getTitle().toLowerCase();
            String content = note.getContent().toLowerCase();

            boolean matchesAll = true;

            for (String word : keywords) {
                if (!title.contains(word) && !content.contains(word)) {
                    matchesAll = false;
                    break;
                }
            }

            if (matchesAll) {
                filteredList.add(note);
            }
        }

        recyclerDataArrayList.clear();
        recyclerDataArrayList.addAll(filteredList);
        adapter.notifyDataSetChanged();
    }
    @Override
    protected void onResume() {
        super.onResume();

        searchBar.setText(""); // clears search
    }

}