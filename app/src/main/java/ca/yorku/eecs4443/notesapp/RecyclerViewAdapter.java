package ca.yorku.eecs4443.notesapp;

import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder> {

    private ArrayList<NoteItemData> noteDataArrayList;
    private Context context;

    private boolean fromTrash = false;

    public RecyclerViewAdapter(ArrayList<NoteItemData> noteDataArrayList, Context context, boolean fromTrash) {
        this.noteDataArrayList = noteDataArrayList;
        this.context = context;
        this.fromTrash = fromTrash; // know if in trash
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
        NoteItemData note = noteDataArrayList.get(position);

        // Title
        holder.noteTitleTV.setText(note.getTitle());

        // Content preview
        String contentHtml = note.getContent();
        if (contentHtml != null && !contentHtml.isEmpty()) {
            try {
                Spanned preview = Html.fromHtml(contentHtml, Html.FROM_HTML_MODE_LEGACY);
                holder.notePreviewTV.setText(preview, TextView.BufferType.SPANNABLE);
                holder.notePreviewTV.setMaxLines(3);
            } catch (Exception e) {
                String plain = Html.fromHtml(contentHtml, Html.FROM_HTML_MODE_COMPACT).toString();
                holder.notePreviewTV.setText(
                        plain.length() > 100 ? plain.substring(0, 100) + "..." : plain
                );
            }
        } else {
            holder.notePreviewTV.setText("");
        }

        // Timestamp
        if (note.getLastModified() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            holder.noteTimestampTV.setText(
                    "Last edited: " + sdf.format(note.getLastModified().toDate())
            );
        } else {
            holder.noteTimestampTV.setText("");
        }

        // CLICK → OPEN NOTE
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, NoteEditingActivity.class);
            intent.putExtra("id", note.getId());
            intent.putExtra("title", note.getTitle());
            intent.putExtra("content", note.getContent());
            intent.putExtra("fontSizes", note.getFontSizes());

            // make note read-only if from Trash
            intent.putExtra("readOnly", context instanceof TrashActivity);

            context.startActivity(intent);
        });

        // LONG CLICK LOGIC
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        holder.itemView.setOnLongClickListener(v -> {

            if (context instanceof TrashActivity) {

                // TRASH SCREEN + DELETE OR RECOVER
                new android.app.AlertDialog.Builder(context)
                        .setTitle("Choose Action")
                        .setMessage("What do you want to do with this note?")
                        .setPositiveButton("Delete", (dialog, which) -> {

                            // SECOND CONFIRMATION
                            new android.app.AlertDialog.Builder(context)
                                    .setTitle("Delete Permanently")
                                    .setMessage("This cannot be undone. Are you sure?")
                                    .setPositiveButton("Delete", (d, w) -> {

                                        FirebaseFirestore.getInstance()
                                                .collection("users")
                                                .document(userId)
                                                .collection("notes")
                                                .document(note.getId())
                                                .delete();

                                        int pos = holder.getAdapterPosition();
                                        if (pos != RecyclerView.NO_POSITION) {
                                            noteDataArrayList.remove(pos);
                                            notifyItemRemoved(pos);
                                        }

                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        })
                        .setNegativeButton("Recover", (dialog, which) -> {

                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(userId)
                                    .collection("notes")
                                    .document(note.getId())
                                    .update("isDeleted", false);

                            int pos = holder.getAdapterPosition();
                            if (pos != RecyclerView.NO_POSITION) {
                                noteDataArrayList.remove(pos);
                                notifyItemRemoved(pos);
                            }

                        })
                        .setNeutralButton("Cancel", null)
                        .show();

            } else {

                // MAIN NOTES + ONLY DELETE (NO RECOVER)
                new android.app.AlertDialog.Builder(context)
                        .setTitle("Move to Trash")
                        .setMessage("Do you want to move this note to trash?")
                        .setPositiveButton("Yes", (dialog, which) -> {

                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(userId)
                                    .collection("notes")
                                    .document(note.getId())
                                    .set(
                                            new java.util.HashMap<String, Object>() {{
                                                put("isDeleted", true);
                                            }},
                                            com.google.firebase.firestore.SetOptions.merge()
                                    );

                            int pos = holder.getAdapterPosition();
                            if (pos != RecyclerView.NO_POSITION) {
                                noteDataArrayList.remove(pos);
                                notifyItemRemoved(pos);
                            }

                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            return true;
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
    private void moveToTrash(String noteId) {

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("notes")
                .document(noteId)
                .update("isDeleted", true);
    }
}