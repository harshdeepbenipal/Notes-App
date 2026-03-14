package ca.yorku.eecs4443.notesapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

// FEEL FREE TO CHANGE / UPDATE / REPLACE THESE JAVA FILES
// Only used to visually see UI

// Referencing: geeksforgeeks.org/android/recyclerview-using-gridlayoutmanager-in-android-with-example/
public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder> {

    private ArrayList<NoteItemData> courseDataArrayList;
    private Context context;

    public RecyclerViewAdapter(ArrayList<NoteItemData> recyclerDataArrayList, Context context) {
        this.courseDataArrayList = recyclerDataArrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_layout, parent, false);
        return new RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        // bind data to title and thumbnail
        NoteItemData noteItemData = courseDataArrayList.get(position);
        holder.noteTV.setText(noteItemData.getTitle());
        holder.noteIV.setImageResource(noteItemData.getThumbnail());
    }

    @Override
    public int getItemCount() {
        return courseDataArrayList.size();
    }

    // View Holder Class
    public class RecyclerViewHolder extends RecyclerView.ViewHolder {

        private TextView noteTV;
        private ImageView noteIV;

        public RecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTV = itemView.findViewById(R.id.noteTitle);
            noteIV = itemView.findViewById(R.id.noteThumbnail);
        }
    }

}
