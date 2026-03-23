package ca.yorku.eecs4443.notesapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageButton;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class NoteNavigation extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArrayList<NoteItemData> recyclerDataArrayList;
    private RecyclerViewAdapter adapter;
    private DrawerLayout drawerLayout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_activity);

        recyclerView = findViewById(R.id.recyclerView);
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
        adapter = new RecyclerViewAdapter(recyclerDataArrayList, this);
        recyclerView.setAdapter(adapter);

        int cardWidth = 180;
        int numCol = calColumns(getApplicationContext(), cardWidth);
        recyclerView.setLayoutManager(new GridLayoutManager(this, numCol));

        // Load notes sorted by lastModified descending
        db.collection("users")
                .document(userId)
                .collection("notes")
                .orderBy("lastModified", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) { Log.e("Firestore", "Error: ", error); return; }
                    if (value != null) {
                        recyclerDataArrayList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            String title = doc.getString("title");
                            if (title == null) title = "Untitled Note";
                            String content = doc.getString("content");
                            Timestamp lastMod = doc.getTimestamp("lastModified");
                            recyclerDataArrayList.add(
                                    new NoteItemData(doc.getId(), title, content != null ? content : "", R.drawable.note, lastMod)
                            );
                        }
                        adapter.notifyDataSetChanged();
                    }
                });

        ImageButton menuButton = findViewById(R.id.optionsButton);
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(android.view.Gravity.LEFT));

        ImageButton createButton = findViewById(R.id.createButton);
        createButton.setOnClickListener(v -> {
            Intent intent = new Intent(NoteNavigation.this, NoteEditingActivity.class);
            intent.putExtra("id", "");
            intent.putExtra("title", "");
            intent.putExtra("content", "");
            startActivity(intent);
        });
    }

    public int calColumns(Context context, float columnWidthDP) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float screenWidthDP = displayMetrics.widthPixels / displayMetrics.density;
        return (int) (screenWidthDP / columnWidthDP + 0.5);
    }
}