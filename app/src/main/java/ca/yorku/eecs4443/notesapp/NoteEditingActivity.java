package ca.yorku.eecs4443.notesapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import android.text.TextWatcher;
import android.text.Editable;

public class NoteEditingActivity extends AppCompatActivity {

    private EditText titleEditText, contentEditText;
    private ImageButton backButton;
    private ImageButton boldButton, italicButton, underlineButton, highlightButton, checkboxButton, bulletButton;
    private ImageButton undoButton, redoButton, drawButton;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String noteId;

    private Handler handler = new Handler();
    private Runnable saveRunnable;

    private boolean isBoldActive = false;
    private boolean isItalicActive = false;
    private boolean isUnderlineActive = false;
    private boolean isHighlightActive = false;
    private Stack<CharSequence> undoStack = new Stack<>();
    private Stack<CharSequence> redoStack = new Stack<>();
    private FrameLayout drawCanvasContainer;
    private DrawingView drawingView;

    // Autosave watcher
    private final TextWatcher simpleWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            handler.removeCallbacks(saveRunnable);
            saveRunnable = () -> saveNote();
            handler.postDelayed(saveRunnable, 1000);
        }
        @Override public void afterTextChanged(Editable s) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_editing);

        drawCanvasContainer = findViewById(R.id.drawCanvasContainer);
        drawingView = null; // starts with no drawing

        // Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Find views
        titleEditText = findViewById(R.id.noteTitleEdit);
        contentEditText = findViewById(R.id.noteContentEdit);

        // Enter after 4 lines in title moves cursor to content
        titleEditText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                    event.getAction() == android.view.KeyEvent.ACTION_DOWN) {

                int lineCount = titleEditText.getLineCount();

                if (lineCount >= 4) {
                    contentEditText.requestFocus();
                    contentEditText.setSelection(contentEditText.getText().length());
                    return true; // block newline
                }
            }
            return false;
        });

        // keyboard "Next" button
        titleEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT) {
                contentEditText.requestFocus();
                contentEditText.setSelection(contentEditText.getText().length());
                return true;
            }
            return false;
        });
        titleEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (titleEditText.getLineCount() > 4) {
                    int cursor = titleEditText.getSelectionStart();

                    if (cursor > 0) {
                        titleEditText.getText().delete(cursor - 1, cursor);
                    }

                    contentEditText.requestFocus();
                    contentEditText.setSelection(contentEditText.getText().length());
                }
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        backButton = findViewById(R.id.backButton);
        boldButton = findViewById(R.id.boldButton);
        italicButton = findViewById(R.id.italicButton);
        underlineButton = findViewById(R.id.underlineButton);
        highlightButton = findViewById(R.id.highlightButton);
        bulletButton = findViewById(R.id.bulletButton);
        checkboxButton = findViewById(R.id.checkboxButton);
        undoButton = findViewById(R.id.undo);
        redoButton = findViewById(R.id.redo);
        drawButton = findViewById(R.id.draw);

        // Read-only mode
        boolean readOnly = getIntent().getBooleanExtra("readOnly", false);
        if (readOnly) {
            titleEditText.setEnabled(false);
            titleEditText.setKeyListener(null);
            contentEditText.setEnabled(false);
            contentEditText.setKeyListener(null);

            boldButton.setVisibility(View.GONE);
            italicButton.setVisibility(View.GONE);
            underlineButton.setVisibility(View.GONE);
            highlightButton.setVisibility(View.GONE);
            bulletButton.setVisibility(View.GONE);
            checkboxButton.setVisibility(View.GONE);
            undoButton.setVisibility(View.GONE);
            redoButton.setVisibility(View.GONE);
            drawButton.setVisibility(View.GONE);
        }

        // Load intent data
        Intent intent = getIntent();
        noteId = intent.getStringExtra("id");
        if (noteId == null || noteId.isEmpty()) {
            noteId = db.collection("users")
                    .document(mAuth.getCurrentUser().getUid())
                    .collection("notes")
                    .document()
                    .getId();
        }

        String noteTitle = intent.getStringExtra("title");
        String noteContent = intent.getStringExtra("content");
        if (noteTitle != null) titleEditText.setText(noteTitle);
        if (noteContent != null) {
            try {
                Spanned spanned = Html.fromHtml(noteContent, Html.FROM_HTML_MODE_COMPACT);
                contentEditText.setText(spanned);
                Editable content = contentEditText.getText();
                applyBullets(contentEditText.getText());
            } catch (Exception e) {
                contentEditText.setText(noteContent);
            }
        }

        // Add autosave watcher
        titleEditText.addTextChangedListener(simpleWatcher);
        contentEditText.addTextChangedListener(new TextWatcher() {
            private boolean ignoreChange = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (ignoreChange) return;

                if (count == 1 && s.charAt(start) == '\n') { // Enter pressed
                    ignoreChange = true;
                    Editable text = contentEditText.getText();

                    // Find current line
                    int lineStart = text.toString().lastIndexOf('\n', start - 1);
                    lineStart = (lineStart == -1) ? 0 : lineStart + 1;
                    int lineEnd = start;

                    BulletSpan[] spans = text.getSpans(lineStart, lineEnd, BulletSpan.class);
                    String lineText = text.subSequence(lineStart, lineEnd).toString().trim();

                    if (spans.length > 0) {
                        // CONTINUE/EXIT bullet list
                        if (lineText.isEmpty()) {
                            for (BulletSpan span : spans) text.removeSpan(span);
                        } else {
                            int newLineStart = start + 1;
                            text.insert(newLineStart, " ");
                            text.setSpan(new BulletSpan(20, Color.BLACK), newLineStart, newLineStart + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            contentEditText.setSelection(newLineStart + 1);
                        }
                    } else if (lineText.startsWith("☐") || lineText.startsWith("☑")) {
                        // Continue checkbox list
                        text.insert(start + 1, "☐ ");
                        contentEditText.setSelection(start + 3);
                    }

                    ignoreChange = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // --- Formatting buttons ---
        backButton.setOnClickListener(v -> {
            handler.removeCallbacks(saveRunnable);
            if (saveNote()) {
                Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Empty note, not saved!", Toast.LENGTH_SHORT).show();
            }
            finish();
        });
        undoButton.setOnClickListener(v -> {
            if (!undoStack.empty()) {
                redoStack.push(contentEditText.getText().toString());
                contentEditText.setText(undoStack.pop());
            }
        });

        boldButton.setOnClickListener(v -> toggleFormat(Typeface.BOLD, boldButton));
        italicButton.setOnClickListener(v -> toggleFormat(Typeface.ITALIC, italicButton));
        underlineButton.setOnClickListener(v -> toggleFormatUnderline(underlineButton));
        highlightButton.setOnClickListener(v -> toggleFormatHighlight(highlightButton));

        bulletButton.setOnClickListener(v -> {
            ensureContentFocused();
            toggleBulletOnCurrentLine(contentEditText);
        });
        checkboxButton.setOnClickListener(v -> {
            ensureContentFocused();
            toggleCheckboxToolbar(contentEditText);
        });
        drawButton.setOnClickListener(v -> {
            if (drawingView == null) {
                drawingView = new DrawingView(this);
                drawCanvasContainer.addView(drawingView);
                drawCanvasContainer.setVisibility(View.VISIBLE);

                // Disable typing
                titleEditText.setEnabled(false);
                contentEditText.setEnabled(false);
                Toast.makeText(this, "Drawing mode ON", Toast.LENGTH_SHORT).show();
            } else {
                drawCanvasContainer.setVisibility(View.GONE);
                drawCanvasContainer.removeView(drawingView);
                drawingView = null;

                // Re-enable typing
                titleEditText.setEnabled(true);
                contentEditText.setEnabled(true);
                Toast.makeText(this, "Drawing mode OFF", Toast.LENGTH_SHORT).show();
            }
        });
        ConstraintLayout rootLayout = findViewById(R.id.noteEditingLayout);
        View bottomToolbar = findViewById(R.id.bottomBar);

        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootLayout.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootLayout.getRootView().getHeight();
            int keyboardHeight = screenHeight - r.bottom;

            if (keyboardHeight > screenHeight * 0.15) { // keyboard is visible
                bottomToolbar.setTranslationY(-keyboardHeight);
            } else {
                bottomToolbar.setTranslationY(0);
            }
        });
    }

    private void toggleFormat(int style, ImageButton button) {
        ensureContentFocused();
        Spannable text = contentEditText.getText();
        int start = contentEditText.getSelectionStart();
        int end = contentEditText.getSelectionEnd();
        if (style == Typeface.BOLD) isBoldActive = !isBoldActive;
        if (style == Typeface.ITALIC) isItalicActive = !isItalicActive;
        button.setAlpha((style == Typeface.BOLD ? isBoldActive : isItalicActive) ? 0.35f : 1.0f);

        if (start != end) {
            StyleSpan[] spans = text.getSpans(start, end, StyleSpan.class);
            boolean hasStyle = false;
            for (StyleSpan span : spans) {
                if (span.getStyle() == style) {
                    hasStyle = true;
                    text.removeSpan(span);
                }
            }
            if (!hasStyle) text.setSpan(new StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void toggleFormatUnderline(ImageButton button) {
        ensureContentFocused();
        isUnderlineActive = !isUnderlineActive;
        button.setAlpha(isUnderlineActive ? 0.35f : 1.0f);
        int start = contentEditText.getSelectionStart();
        int end = contentEditText.getSelectionEnd();
        if (start != end) {
            Spannable text = contentEditText.getText();
            UnderlineSpan[] spans = text.getSpans(start, end, UnderlineSpan.class);
            if (spans.length > 0) for (UnderlineSpan span : spans) text.removeSpan(span);
            else text.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void toggleFormatHighlight(ImageButton button) {
        ensureContentFocused();
        isHighlightActive = !isHighlightActive;
        button.setAlpha(isHighlightActive ? 0.35f : 1.0f);
        int start = contentEditText.getSelectionStart();
        int end = contentEditText.getSelectionEnd();
        if (start != end) {
            Spannable text = contentEditText.getText();
            BackgroundColorSpan[] spans = text.getSpans(start, end, BackgroundColorSpan.class);
            if (spans.length > 0) for (BackgroundColorSpan span : spans) text.removeSpan(span);
            else text.setSpan(new BackgroundColorSpan(Color.YELLOW), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void ensureContentFocused() {
        if (!contentEditText.hasFocus()) {
            contentEditText.requestFocus();
            contentEditText.setSelection(contentEditText.getText().length());
        }
    }

    private boolean saveNote() {
        String updatedTitle = titleEditText.getText().toString().trim();
        String contentText = contentEditText.getText().toString().trim();

        if (updatedTitle.isEmpty() && contentText.isEmpty()) return false;
        if (mAuth.getCurrentUser() == null) return false;

        String htmlContent = convertSpannableToHtmlWithBullets(contentEditText.getText());

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", updatedTitle);
        updates.put("content", htmlContent);
        updates.put("lastModified", Timestamp.now());

        db.collection("users")
                .document(mAuth.getCurrentUser().getUid())
                .collection("notes")
                .document(noteId)
                .set(updates)
                .addOnSuccessListener(aVoid -> {
                    // Success handled by autosave toast
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        return true;
    }
    private void toggleBulletOnCurrentLine(EditText editText) {
        Editable text = editText.getText();
        int cursor = editText.getSelectionStart();

        int lineStart = text.toString().lastIndexOf('\n', cursor - 1);
        lineStart = (lineStart == -1) ? 0 : lineStart + 1;

        int lineEnd = text.toString().indexOf('\n', lineStart);
        lineEnd = (lineEnd == -1) ? text.length() : lineEnd;

        BulletSpan[] spans = text.getSpans(lineStart, lineEnd, BulletSpan.class);
        for (BulletSpan span : spans) {
            text.removeSpan(span);
        }

        if (spans.length == 0) {
            // Add bullet
            if (lineStart == lineEnd) {
                text.insert(lineStart, " ");
                lineEnd = lineStart + 1;
            }

            text.setSpan(
                    new BulletSpan(20, Color.BLACK),
                    lineStart,
                    lineEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
    private String convertSpannableToHtmlWithBullets(Spannable text) {
        StringBuilder html = new StringBuilder();
        boolean inList = false;

        int start = 0;
        String fullText = text.toString();

        while (start < fullText.length()) {
            int lineEnd = fullText.indexOf('\n', start);
            if (lineEnd == -1) lineEnd = fullText.length();

            String line = fullText.substring(start, lineEnd);
            BulletSpan[] spans = text.getSpans(start, lineEnd, BulletSpan.class);

            // FIXED: Detect by "• " prefix OR BulletSpan (works with invisible spans)
            boolean isBulletLine = line.startsWith("• ") || spans.length > 0;

            String content;
            if (isBulletLine) {
                // Extract content after "• "
                content = line.startsWith("• ") ? line.substring(2).trim() : line.trim();
                if (!inList) {
                    html.append("<ul>");
                    inList = true;
                }
                html.append("<li>").append(Html.escapeHtml(content)).append("</li>");
            } else {
                content = line.trim();
                if (inList) {
                    html.append("</ul>");
                    inList = false;
                }
                if (!content.isEmpty()) {
                    html.append(Html.escapeHtml(content)).append("<br>");
                }
            }

            start = lineEnd + 1;
        }

        if (inList) html.append("</ul>");
        return html.toString();
    }
    private void applyBullets(Editable text) {
        int start = 0;

        while (start < text.length()) {
            int lineEnd = text.toString().indexOf('\n', start);
            if (lineEnd == -1) lineEnd = text.length();

            BulletSpan[] bulletSpans = text.getSpans(start, lineEnd, BulletSpan.class);
            String lineText = text.subSequence(start, lineEnd).toString();

            if (bulletSpans.length > 0) {
                // Remove old spans
                for (BulletSpan span : bulletSpans) {
                    text.removeSpan(span);
                }

                // Ensure "• " prefix
                if (!lineText.startsWith("• ")) {
                    text.insert(start, "• ");
                    lineEnd += 2;
                }

                // Add INVISIBLE BulletSpan for Enter detection ONLY (gap=0)
                text.setSpan(
                        new BulletSpan(0, Color.TRANSPARENT),  // 0 gap, invisible
                        start,
                        lineEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

            start = lineEnd + 1;
        }
    }
    private void toggleCheckboxToolbar(EditText editText) {
        Editable text = editText.getText();
        int cursor = editText.getSelectionStart();

        int lineStart = text.toString().lastIndexOf('\n', cursor - 1);
        lineStart = (lineStart == -1) ? 0 : lineStart + 1;

        int lineEnd = text.toString().indexOf('\n', lineStart);
        if (lineEnd == -1) lineEnd = text.length();

        String lineText = text.subSequence(lineStart, lineEnd).toString();

        if (lineText.startsWith("☐") || lineText.startsWith("☑")) {
            // Already has checkbox → remove it
            text.delete(lineStart, lineStart + 2); // removes "☐ " or "☑ "
            editText.setSelection(lineStart); // move cursor to start of line
        } else {
            // Add new unchecked checkbox
            text.insert(lineStart, "☐ ");
            editText.setSelection(lineStart + 2); // move cursor **after checkbox**
        }

        // Make checkboxes clickable
        applyClickableCheckboxes(editText);
    }
    private void applyClickableCheckboxes(EditText editText) {
        Editable text = editText.getText();
        int start = 0;

        while (start < text.length()) {
            int lineEnd = text.toString().indexOf('\n', start);
            if (lineEnd == -1) lineEnd = text.length();

            // Make sure we have at least 1 character
            if (lineEnd - start >= 1) {
                char firstChar = text.charAt(start);
                if (firstChar == '☐' || firstChar == '☑') {

                    // Remove old spans
                    ClickableSpan[] existing = text.getSpans(start, start + 1, ClickableSpan.class);
                    for (ClickableSpan span : existing) text.removeSpan(span);

                    final int pos = start;
                    text.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(View widget) {
                            Editable editable = ((EditText) widget).getText();
                            if (pos >= editable.length()) return; // Safety check
                            char current = editable.charAt(pos);
                            char replacement = current == '☐' ? '☑' : '☐';
                            editable.replace(pos, pos + 1, String.valueOf(replacement));
                        }

                        @Override
                        public void updateDrawState(TextPaint ds) {
                            ds.setUnderlineText(false);
                        }
                    }, start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            start = lineEnd + 1;
        }

        editText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void saveStateForUndo() {
        undoStack.push(contentEditText.getText().toString());
        redoStack.clear();
    }
    private boolean isBulletLine(Editable text, int lineStart, int lineEnd) {
        String line = text.subSequence(lineStart, lineEnd).toString().trim();
        BulletSpan[] spans = text.getSpans(lineStart, lineEnd, BulletSpan.class);
        return line.startsWith("• ") || spans.length > 0;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

}