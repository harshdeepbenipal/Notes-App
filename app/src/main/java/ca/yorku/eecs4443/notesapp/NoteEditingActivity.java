package ca.yorku.eecs4443.notesapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class NoteEditingActivity extends AppCompatActivity {

    private EditText titleEditText, contentEditText;
    private Button saveButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String noteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_editing);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        titleEditText = findViewById(R.id.noteTitleEdit);
        contentEditText = findViewById(R.id.noteContentEdit);
        saveButton = findViewById(R.id.saveButton);

        Intent intent = getIntent();
        noteId = intent.getStringExtra("id");
        String noteTitle = intent.getStringExtra("title");
        String noteContent = intent.getStringExtra("content");

        titleEditText.setText(noteTitle);
        contentEditText.setText(noteContent);

        saveButton.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String updatedTitle = titleEditText.getText().toString().trim();
        String updatedContent = contentEditText.getText().toString().trim();
        if (updatedTitle.isEmpty()) updatedTitle = "Untitled Note";

        String userId = mAuth.getCurrentUser().getUid();
        Timestamp now = Timestamp.now();

        if (noteId == null || noteId.isEmpty()) {
            // NEW NOTE
            Map<String, Object> newNote = new HashMap<>();
            newNote.put("title", updatedTitle);
            newNote.put("content", updatedContent);
            newNote.put("lastModified", now);

            db.collection("users")
                    .document(userId)
                    .collection("notes")
                    .add(newNote)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Note created!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to create note: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } else {
            // EXISTING NOTE
            db.collection("users")
                    .document(userId)
                    .collection("notes")
                    .document(noteId)
                    .update(
                            "title", updatedTitle,
                            "content", updatedContent,
                            "lastModified", now
                    )
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Note updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to update note: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }
    }
}