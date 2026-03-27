package ca.yorku.eecs4443.notesapp;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.text.TextWatcher;
import android.text.Editable;

public class NoteEditingActivity extends AppCompatActivity {

    private EditText titleEditText, contentEditText;
    private ImageButton backButton;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String noteId;

    private Handler handler = new Handler();
    private Runnable saveRunnable;

    EditText editor;
    int currentFontSize = 16;

    private ImageButton boldButton, italicButton, underlineButton, highlightButton, fontSizeButton, bulletButton, undoButton, redoButton, drawButton;
    private boolean isBoldActive = false;
    private boolean isItalicActive = false;
    private boolean isUnderlineActive = false;
    private boolean isHighlightActive = false;

    // Autosave watcher
    private final TextWatcher simpleWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            handler.removeCallbacks(saveRunnable);
            saveRunnable = () -> saveNote();
            handler.postDelayed(saveRunnable, 1000); // 1 sec delay
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_editing);

        // Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Views
        titleEditText = findViewById(R.id.noteTitleEdit);
        contentEditText = findViewById(R.id.noteContentEdit); // matches your XML
        backButton = findViewById(R.id.backButton);

        boldButton = findViewById(R.id.boldButton);
        italicButton = findViewById(R.id.italicButton);
        underlineButton = findViewById(R.id.underlineButton);
        highlightButton = findViewById(R.id.highlightButton);
        fontSizeButton = findViewById(R.id.fontSizeButton);
        bulletButton = findViewById(R.id.bulletButton);

        // Tool Bar Buttons
        undoButton = findViewById(R.id.undo);
        redoButton = findViewById(R.id.redo);
        drawButton = findViewById(R.id.draw);

        // Inside onCreate() after loading note content
        boolean readOnly = getIntent().getBooleanExtra("readOnly", false);

        if (readOnly) {
            // Disable typing completely
            titleEditText.setEnabled(false);
            titleEditText.setKeyListener(null);

            contentEditText.setEnabled(false);
            contentEditText.setKeyListener(null);

            // Hide formatting buttons
            boldButton.setVisibility(View.GONE);
            italicButton.setVisibility(View.GONE);
            underlineButton.setVisibility(View.GONE);
            highlightButton.setVisibility(View.GONE);
            fontSizeButton.setVisibility(View.GONE);
            bulletButton.setVisibility(View.GONE);

            // Hide Toolbar Buttons
            undoButton.setVisibility(View.GONE);
            redoButton.setVisibility(View.GONE);
            drawButton.setVisibility(View.GONE);

            // Remove autosave TextWatchers
            titleEditText.removeTextChangedListener(simpleWatcher);
            contentEditText.removeTextChangedListener(simpleWatcher);
        }

        // Load intent data - COMPLETE FIX
        Intent intent = getIntent();
        noteId = intent.getStringExtra("id");
        if (noteId == null || noteId.isEmpty()) {
            noteId = db.collection("users")
                    .document(mAuth.getCurrentUser().getUid())
                    .collection("notes")
                    .document()
                    .getId();
        }

        // FIXED: Load BOTH title AND content
        String noteTitle = intent.getStringExtra("title");
        String noteContent = intent.getStringExtra("content");

        // Set title
        if (noteTitle != null && !noteTitle.isEmpty()) {
            titleEditText.setText(noteTitle);
        }

        // Set content with proper HTML parsing
        if (noteContent != null && !noteContent.isEmpty()) {
            Spanned spanned = null;
            try {
                spanned = Html.fromHtml(noteContent, Html.FROM_HTML_MODE_LEGACY);
                contentEditText.setText(spanned);
                contentEditText.setSelection(contentEditText.length());

                // --- RESTORE FONT SIZES ---
                ArrayList<Map<String, Object>> fontSizes =
                        (ArrayList<Map<String, Object>>) getIntent().getSerializableExtra("fontSizes");

                if (fontSizes != null && !fontSizes.isEmpty()) {
                    Spannable text = contentEditText.getText();
                    for (Map<String, Object> map : fontSizes) {
                        int start = ((Long) map.get("start")).intValue();
                        int end = ((Long) map.get("end")).intValue();
                        int size = ((Long) map.get("size")).intValue();

                        text.setSpan(new AbsoluteSizeSpan(size, true),
                                start,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }

            } catch (Exception e) {
                contentEditText.setText(noteContent); // Fallback
            }
            // After: contentEditText.setText(spanned);
            ArrayList<Map<String, Object>> fontSizes =
                    (ArrayList<Map<String, Object>>) getIntent().getSerializableExtra("fontSizes");

            if (fontSizes != null && !fontSizes.isEmpty()) {
                Spannable text = contentEditText.getText();
                for (Map<String, Object> map : fontSizes) {
                    int start = ((Long) map.get("start")).intValue();
                    int end = ((Long) map.get("end")).intValue();
                    int size = ((Long) map.get("size")).intValue();

                    text.setSpan(new AbsoluteSizeSpan(size, true),
                            start,
                            end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                // Set currentFontSize to last span so typing continues with it
                currentFontSize = ((Long) fontSizes.get(fontSizes.size() - 1).get("size")).intValue();
            }
            contentEditText.setSelection(contentEditText.length());
        }
        // Autosave watcher
        titleEditText.addTextChangedListener(simpleWatcher);
        contentEditText.addTextChangedListener(simpleWatcher);

        // Back button
        backButton.setOnClickListener(v -> {
            if (!readOnly) {
                handler.removeCallbacks(saveRunnable);
                if (saveNote()) {
                    Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Empty note, not saved!", Toast.LENGTH_SHORT).show();
                }
            }
            finish();
        });

        // TextWatcher: apply current font size to new text
        contentEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Apply current font size to NEWLY TYPED text only
                if (count > 0) {
                    Spannable text = contentEditText.getText();

                    // Apply font size
                    text.setSpan(new AbsoluteSizeSpan(currentFontSize, true),
                            start, start + count,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    // Restore last used font size if you saved it in intent (optional)
                    ArrayList<Map<String, Object>> fontSizes =
                            (ArrayList<Map<String, Object>>) getIntent().getSerializableExtra("fontSizes");

                    if (fontSizes != null && !fontSizes.isEmpty()) {
                        // pick the size of the first span as the default
                        currentFontSize = ((Long) fontSizes.get(0).get("size")).intValue();
                    }
                    // Apply bold if active
                    if (isBoldActive) {
                        text.setSpan(new StyleSpan(Typeface.BOLD),
                                start,
                                start + count,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    // Apply italic if active
                    if (isItalicActive) {
                        text.setSpan(new StyleSpan(Typeface.ITALIC),
                                start,
                                start + count,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    // Apply underline if active
                    if (isUnderlineActive) {
                        text.setSpan(new UnderlineSpan(),
                                start,
                                start + count,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    if (isHighlightActive) {
                        text.setSpan(new BackgroundColorSpan(Color.YELLOW),
                                start,
                                start + count,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // --- Formatting buttons ---
        boldButton.setOnClickListener(v -> {
            isBoldActive = !isBoldActive;
            boldButton.setAlpha(isBoldActive ? 0.35f : 1.0f);

            int start = contentEditText.getSelectionStart();
            int end = contentEditText.getSelectionEnd();

            if (start != end) {
                toggleBoldOnSelection(contentEditText, start, end);
            }
        });
        italicButton.setOnClickListener(v -> {
            isItalicActive = !isItalicActive;

            // visual feedback
            italicButton.setAlpha(isItalicActive ? 1.0f : 0.5f);

            int start = contentEditText.getSelectionStart();
            int end = contentEditText.getSelectionEnd();

            if (start != end) {
                toggleItalicOnSelection(contentEditText, start, end);
            }
        });
        underlineButton.setOnClickListener(v -> {
            isUnderlineActive = !isUnderlineActive;

            // visual feedback
            underlineButton.setAlpha(isUnderlineActive ? 1.0f : 0.5f);

            int start = contentEditText.getSelectionStart();
            int end = contentEditText.getSelectionEnd();

            if (start != end) {
                toggleUnderlineOnSelection(contentEditText, start, end);
            }
        });
        highlightButton.setOnClickListener(v -> {
            isHighlightActive = !isHighlightActive;

            // visual feedback
            highlightButton.setAlpha(isHighlightActive ? 1.0f : 0.5f);

            int start = contentEditText.getSelectionStart();
            int end = contentEditText.getSelectionEnd();

            if (start != end) {
                toggleHighlightOnSelection(contentEditText, start, end);
            }
        });

        // Font size button
        fontSizeButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, fontSizeButton);
            popup.getMenu().add("12");
            popup.getMenu().add("14");
            popup.getMenu().add("16");
            popup.getMenu().add("18");
            popup.getMenu().add("20");
            popup.getMenu().add("24");

            popup.setOnMenuItemClickListener(item -> {
                currentFontSize = Integer.parseInt(item.getTitle().toString());

                // apply to selected text
                int start = contentEditText.getSelectionStart();
                int end = contentEditText.getSelectionEnd();
                if (start != end && start >= 0 && end >= 0) {
                    Spannable text = contentEditText.getText();
                    // Remove old sizes
                    AbsoluteSizeSpan[] oldSizes = text.getSpans(start, end, AbsoluteSizeSpan.class);
                    for (AbsoluteSizeSpan span : oldSizes) {
                        text.removeSpan(span);
                    }
                    // Apply new size
                    text.setSpan(new AbsoluteSizeSpan(currentFontSize, true), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                Toast.makeText(this, "Font: " + currentFontSize + "px (new text)", Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        });

    }
    private void toggleBoldOnSelection(EditText editText, int start, int end) {
        Spannable text = editText.getText();

        StyleSpan[] spans = text.getSpans(start, end, StyleSpan.class);

        boolean hasBold = false;
        for (StyleSpan span : spans) {
            if (span.getStyle() == Typeface.BOLD) {
                hasBold = true;
                text.removeSpan(span);
            }
        }
        if (!hasBold) {
            text.setSpan(new StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private boolean saveNote() {
        String updatedTitle = titleEditText.getText().toString().trim();
        String contentText = contentEditText.getText().toString().trim();

        // Prevent completely empty note
        if (updatedTitle.isEmpty() && contentText.isEmpty()) {
            return false; // do NOT save anything
        }
        if (mAuth.getCurrentUser() == null) return false;

        String userId = mAuth.getCurrentUser().getUid();

        Spannable text = contentEditText.getText();

        // Save normal HTML (bold, italic, etc.)
        String htmlContent = Html.toHtml(text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);

        // Extract font sizes
        AbsoluteSizeSpan[] spans = text.getSpans(0, text.length(), AbsoluteSizeSpan.class);

        ArrayList<Map<String, Object>> sizeData = new ArrayList<>();

        for (AbsoluteSizeSpan span : spans) {
            Map<String, Object> map = new HashMap<>();
            map.put("start", text.getSpanStart(span));
            map.put("end", text.getSpanEnd(span));
            map.put("size", span.getSize());
            sizeData.add(map);
        }

        Timestamp now = Timestamp.now();

        // PUT EVERYTHING INTO FIRESTORE MAP
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", updatedTitle);
        updates.put("content", htmlContent);
        updates.put("lastModified", now);
        updates.put("fontSizes", sizeData);

        // SAVE TO FIRESTORE
        db.collection("users")
                .document(userId)
                .collection("notes")
                .document(noteId)
                .set(updates);

        return true;
    }
    private void toggleItalicOnSelection(EditText editText, int start, int end) {
        Spannable text = editText.getText();

        StyleSpan[] spans = text.getSpans(start, end, StyleSpan.class);

        boolean hasItalic = false;
        for (StyleSpan span : spans) {
            if (span.getStyle() == Typeface.ITALIC) {
                hasItalic = true;
                text.removeSpan(span);
            }
        }

        if (!hasItalic) {
            text.setSpan(new StyleSpan(Typeface.ITALIC),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    private void toggleUnderlineOnSelection(EditText editText, int start, int end) {
        Spannable text = editText.getText();

        UnderlineSpan[] spans = text.getSpans(start, end, UnderlineSpan.class);

        boolean hasUnderline = false;
        for (UnderlineSpan span : spans) {
            hasUnderline = true;
            text.removeSpan(span);
        }

        if (!hasUnderline) {
            text.setSpan(new UnderlineSpan(),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
    private void toggleHighlightOnSelection(EditText editText, int start, int end) {
        Spannable text = editText.getText();

        BackgroundColorSpan[] spans = text.getSpans(start, end, BackgroundColorSpan.class);

        boolean hasHighlight = false;
        for (BackgroundColorSpan span : spans) {
            hasHighlight = true;
            text.removeSpan(span);
        }

        if (!hasHighlight) {
            text.setSpan(new BackgroundColorSpan(Color.YELLOW),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
