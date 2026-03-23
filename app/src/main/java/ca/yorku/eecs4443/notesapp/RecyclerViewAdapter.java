package ca.yorku.eecs4443.notesapp;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder> {

    private ArrayList<NoteItemData> noteDataArrayList;
    private Context context;

    public RecyclerViewAdapter(ArrayList<NoteItemData> noteDataArrayList, Context context) {
        this.noteDataArrayList = noteDataArrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_layout, parent, false);
        return new RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewHolder holder, int position) {
        NoteItemData noteItem = noteDataArrayList.get(position);

        holder.noteTitleTV.setText(noteItem.getTitle());

        String preview = noteItem.getContent();
        if (preview.length() > 100) preview = preview.substring(0, 100) + "...";
        holder.notePreviewTV.setText(preview);

        if (noteItem.getLastModified() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            holder.noteTimestampTV.setText("Last edited: " + sdf.format(noteItem.getLastModified().toDate()));
        } else {
            holder.noteTimestampTV.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, NoteDetailActivity.class);
            intent.putExtra("id", noteItem.getId());
            intent.putExtra("title", noteItem.getTitle());
            intent.putExtra("content", noteItem.getContent());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return noteDataArrayList.size();
    }

    public static class RecyclerViewHolder extends RecyclerView.ViewHolder {
        TextView noteTitleTV, notePreviewTV, noteTimestampTV;

        public RecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTitleTV = itemView.findViewById(R.id.noteTitle);
            notePreviewTV = itemView.findViewById(R.id.notePreview);
            noteTimestampTV = itemView.findViewById(R.id.noteTimestamp);
        }
    }
}