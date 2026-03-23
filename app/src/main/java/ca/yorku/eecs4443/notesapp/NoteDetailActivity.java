package ca.yorku.eecs4443.notesapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class NoteDetailActivity extends AppCompatActivity {

    private static final int REQUEST_EDIT_NOTE = 100;

    private TextView titleTV;
    private TextView contentTV;

    private String noteId;
    private String noteTitle;
    private String noteContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        // Find views
        titleTV = findViewById(R.id.noteDetailTitle);
        contentTV = findViewById(R.id.noteDetailContent);
        Button editButton = findViewById(R.id.editNoteButton);

        // Get data from Intent
        Intent intent = getIntent();
        noteId = intent.getStringExtra("id");
        noteTitle = intent.getStringExtra("title");
        noteContent = intent.getStringExtra("content");

        // Populate views
        populateViews();

        // Edit button click to open NoteEditingActivity
        editButton.setOnClickListener(v -> {
            Intent editIntent = new Intent(NoteDetailActivity.this, NoteEditingActivity.class);
            editIntent.putExtra("id", noteId);
            editIntent.putExtra("title", noteTitle);
            editIntent.putExtra("content", noteContent);
            startActivityForResult(editIntent, REQUEST_EDIT_NOTE);
        });
    }

    private void populateViews() {
        titleTV.setText(noteTitle);
        contentTV.setText(noteContent);
    }

    // Handle result from editing activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_EDIT_NOTE && resultCode == RESULT_OK && data != null) {
            // Get updated note data
            noteTitle = data.getStringExtra("title");
            noteContent = data.getStringExtra("content");

            // Update views
            populateViews();
        }
    }
}