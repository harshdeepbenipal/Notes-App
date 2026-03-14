package ca.yorku.eecs4443.notesapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.View;

import android.util.Log;

// FEEL FREE TO CHANGE / UPDATE / REPLACE THESE JAVA FILES
// Only used to visually see UI

// Referencing: geeksforgeeks.org/android/recyclerview-using-gridlayoutmanager-in-android-with-example/

public class NoteNavigation extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArrayList<NoteItemData> recyclerDataArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_activity);
        recyclerView=findViewById(R.id.recyclerView);

        Intent intent = getIntent();

        // Array List for hardcoded in notes (just to see the visual of note item in recycler view)
        String TAG = "MyActivityTag";
        Log.d(TAG, "Start Create array now");

        recyclerDataArrayList = new ArrayList<>();

        recyclerDataArrayList.add(new NoteItemData("Lecture Notes", R.drawable.note));
        recyclerDataArrayList.add(new NoteItemData("To Do", R.drawable.note));
        recyclerDataArrayList.add(new NoteItemData("Groceries", R.drawable.note));
        recyclerDataArrayList.add(new NoteItemData("Services", R.drawable.note));
        recyclerDataArrayList.add(new NoteItemData("Meeting Notes", R.drawable.note));

        Log.d(TAG, "Array Made");

        RecyclerViewAdapter adapter = new RecyclerViewAdapter(recyclerDataArrayList, this);

        recyclerView.setAdapter(adapter);

        // Pretty Pretty please find a way to get the width of the card_layout XML (id/cardLayout) so there can be an accurate scaling of how many note items we can fit on a screen
        int cardWidth = 180;

        int numCol = this.calColumns(getApplicationContext(), cardWidth);
        // 2 represents how many colunmes in each grid view (this needs to update with orientation / screen size?)
        GridLayoutManager layoutManager = new GridLayoutManager(this, numCol);


        recyclerView.setLayoutManager(layoutManager);
        //recyclerView.setAdapter(adapter);
    }

    public int calColumns(Context context, float columnWidthDP) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float screenWidthDP = displayMetrics.widthPixels / displayMetrics.density;
        int num = (int) (screenWidthDP / columnWidthDP + 0.5);
        return num;
    }
}
