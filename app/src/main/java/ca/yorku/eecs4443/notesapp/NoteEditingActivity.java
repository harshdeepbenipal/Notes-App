package ca.yorku.eecs4443.notesapp;

import static android.content.Intent.getIntent;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
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
    private EditText currentWatcherEditText = null;
    private Stack<Editable> undoStack = new Stack<>();
    private Stack<Editable> redoStack = new Stack<>();
    private Stack<Integer> undoCursorStack = new Stack<>();
    private Stack<Integer> redoCursorStack = new Stack<>();
    private FrameLayout drawCanvasContainer;
    private DrawingView drawingView;
    private boolean isRestoring = false;
    private Runnable undoRunnable;

    // Autosave watcher
    private final TextWatcher masterWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            currentWatcherEditText = (titleEditText.getText() == s) ? titleEditText : contentEditText;

            if (isRestoring) return;

            // Ignore cursor-only moves
            if (count == 0 && after == 0) return;

            // ONLY TRACKs CONTENT
            if (currentWatcherEditText == contentEditText) {
                saveStateForUndo();
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (isRestoring) return;
            handler.removeCallbacks(saveRunnable);

            saveRunnable = () -> saveNote();
            handler.postDelayed(saveRunnable, 1000);
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (isRestoring || currentWatcherEditText == null) return;
            // Detect newline and continue bullet
            if (s.length() > 0) {
                currentWatcherEditText.post(() -> {
                    int cursor = currentWatcherEditText.getSelectionStart();
                    Editable editable = currentWatcherEditText.getText();

                    if (cursor > 0 && editable.charAt(cursor - 1) == '\n') {

                        currentWatcherEditText.postDelayed(() -> {
                            int newCursor = currentWatcherEditText.getSelectionStart();
                            Editable editableText = currentWatcherEditText.getText();

                            int prevLineStart = editableText.toString().lastIndexOf('\n', newCursor - 2);
                            prevLineStart = (prevLineStart == -1) ? 0 : prevLineStart + 1;

                            int prevLineEnd = newCursor - 1;

                            BulletSpan[] spans = editableText.getSpans(prevLineStart, prevLineEnd, BulletSpan.class);

                            if (spans.length > 0) {
                                int insertPos = newCursor;

                                // Ensure line has content
                                if (insertPos <= editableText.length()) {
                                    editableText.insert(insertPos, " ");

                                    editableText.setSpan(
                                            new BulletSpan(20, isDarkMode() ? Color.WHITE : Color.BLACK),
                                            insertPos,
                                            insertPos + 1,
                                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                    );
                                }
                            }
                        }, 10); // small delay for timing
                    }
                });
            }
            int cursor = currentWatcherEditText.getSelectionStart();
            if (cursor <= 0 || cursor > s.length()) return;

            int charStart = cursor - 1;
            int charEnd = cursor;

            if (currentWatcherEditText == contentEditText) {
                // Content: Rich formatting
                boolean isBullet = isBulletLine(s, charStart);
                handleTypingSpan(s, charStart, charEnd, isBoldActive, Typeface.BOLD);
                handleTypingSpan(s, charStart, charEnd, isItalicActive, Typeface.ITALIC);
                handleUnderline(s, charStart, charEnd, isUnderlineActive);
                if (isHighlightActive) {
                        handleHighlight(s, charStart, charEnd, true);
                }
            } else {
                // Title: Strip formatting
                stripAllFormatting(s);
            }
            updateFormattingState();
        }

        private void stripAllFormatting(Editable s) {
            StyleSpan[] styleSpans = s.getSpans(0, s.length(), StyleSpan.class);
            for (StyleSpan span : styleSpans) s.removeSpan(span);
            UnderlineSpan[] uSpans = s.getSpans(0, s.length(), UnderlineSpan.class);
            for (UnderlineSpan span : uSpans) s.removeSpan(span);
            BackgroundColorSpan[] hSpans = s.getSpans(0, s.length(), BackgroundColorSpan.class);
            for (BackgroundColorSpan span : hSpans) s.removeSpan(span);
        }

        private void handleTypingSpan(Editable s, int start, int end, boolean active, int style) {
            StyleSpan[] spans = s.getSpans(start, end, StyleSpan.class);

            if (active) {
                boolean hasStyle = false;
                for (StyleSpan span : spans) {
                    if (span.getStyle() == style) { hasStyle = true; break; }
                }
                if (!hasStyle) {
                    s.setSpan(new StyleSpan(style), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                }
            } else {
                for (StyleSpan span : spans) {
                    if (span.getStyle() == style) {
                        int spanStart = s.getSpanStart(span);
                        int spanEnd = s.getSpanEnd(span);
                        s.removeSpan(span);
                        if (spanStart < start) {
                            s.setSpan(new StyleSpan(style), spanStart, start, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                        }
                        if (spanEnd > end) {
                            s.setSpan(new StyleSpan(style), end, spanEnd, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                        }
                    }
                }
            }
        }
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

        backButton = findViewById(R.id.backButton);
        boldButton = findViewById(R.id.boldButton);
        italicButton = findViewById(R.id.italicButton);
        underlineButton = findViewById(R.id.underlineButton);
        highlightButton = findViewById(R.id.highlightButton);
        bulletButton = findViewById(R.id.bulletButton);
        undoButton = findViewById(R.id.undo);
        redoButton = findViewById(R.id.redo);
        drawButton = findViewById(R.id.draw);

        contentEditText.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                        android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                        android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        );
        contentEditText.setOnTouchListener((v, event) -> {
            v.post(() -> updateFormattingState());
            return false;
        });

        contentEditText.setOnKeyListener((v, keyCode, event) -> {
            v.post(() -> updateFormattingState());
            return false;
        });
        contentEditText.setOnEditorActionListener((v, actionId, event) -> {
            return false;
        });

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
                applyBullets(contentEditText.getText());;
            } catch (Exception e) {
                contentEditText.setText(noteContent);
            }
        }
        // Add autosave watcher
        titleEditText.addTextChangedListener(masterWatcher);
        contentEditText.addTextChangedListener(masterWatcher);

        // --- Formatting buttons ---
        backButton.setOnClickListener(v -> {
            handler.removeCallbacks(saveRunnable);
            if (readOnly) {
                Toast.makeText(this, "Viewing deleted note", Toast.LENGTH_SHORT).show();
            } else {
                if (saveNote()) {
                    Toast.makeText(this, "Note saved!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Empty note, not saved!", Toast.LENGTH_SHORT).show();
                }
            }
            finish();
        });
        undoButton.setOnClickListener(v -> {
            if (!undoStack.empty()) {
                isRestoring = true;

                // Save current to redo
                redoStack.push(new SpannableStringBuilder(contentEditText.getText()));
                redoCursorStack.push(contentEditText.getSelectionEnd());

                // Restore previous
                Editable prev = undoStack.pop();
                int cursor = undoCursorStack.pop();

                contentEditText.setText(prev);

                // ALWAYS GOes BACK TO CONTENT
                contentEditText.requestFocus();

                int safeCursor = Math.min(cursor, prev.length());
                contentEditText.setSelection(safeCursor);

                isRestoring = false;

                updateFormattingState();
                refreshUndoRedoButtons();
            }
        });
        redoButton.setOnClickListener(v -> {
            if (!redoStack.empty()) {
                isRestoring = true;

                // Save current to undo
                undoStack.push(new SpannableStringBuilder(contentEditText.getText()));
                undoCursorStack.push(contentEditText.getSelectionEnd());

                // Restore redo
                Editable next = redoStack.pop();
                int cursor = redoCursorStack.pop();

                contentEditText.setText(next);

                int safeCursor = Math.min(cursor, next.length());
                contentEditText.setSelection(safeCursor);

                isRestoring = false;

                updateFormattingState();
                refreshUndoRedoButtons();
            }
        });
        boldButton.setOnClickListener(v -> {
            ensureContentFocused();
            saveStateForUndo();

            isBoldActive = !isBoldActive;
            int start = contentEditText.getSelectionStart();
            int end = contentEditText.getSelectionEnd();

            if (start != end) {
                applyStyleToSelection(Typeface.BOLD, isBoldActive);
            } else {
                if (isBoldActive) applyTypingSpan(new StyleSpan(Typeface.BOLD));
                updateCursorFormattingPreview();
            }

            updateButtonState(boldButton, isBoldActive);
        });

        italicButton.setOnClickListener(v -> {
            ensureContentFocused();
            saveStateForUndo();

            isItalicActive = !isItalicActive;
            int start = contentEditText.getSelectionStart();
            int end = contentEditText.getSelectionEnd();

            if (start != end) {
                applyStyleToSelection(Typeface.ITALIC, isItalicActive);
            } else {
                if (isItalicActive) applyTypingSpan(new StyleSpan(Typeface.ITALIC));
                updateCursorFormattingPreview();
            }

            updateButtonState(italicButton, isItalicActive);
        });

        underlineButton.setOnClickListener(v -> {
            ensureContentFocused();
            saveStateForUndo();

            isUnderlineActive = !isUnderlineActive;

            int start = contentEditText.getSelectionStart();
            int end = contentEditText.getSelectionEnd();
            Editable s = contentEditText.getText();

            if (start != end) {
                handleUnderline(s, start, end, isUnderlineActive);
            } else {
                if (isUnderlineActive) applyTypingSpan(new UnderlineSpan());
                updateCursorFormattingPreview();
            }

            updateButtonState(underlineButton, isUnderlineActive);
        });

        highlightButton.setOnClickListener(v -> {
            ensureContentFocused();
            isHighlightActive = !isHighlightActive;

            int start = contentEditText.getSelectionStart();
            int end = contentEditText.getSelectionEnd();
            Editable s = contentEditText.getText();

            if (start != end) {
                handleHighlight(s, start, end, isHighlightActive);
            } else {
                if (isHighlightActive) {
                    applyTypingSpan(new BackgroundColorSpan(Color.parseColor("#8A6D00")));
                }
                updateCursorFormattingPreview();
            }

            updateButtonState(highlightButton, isHighlightActive);
        });
        bulletButton.setOnClickListener(v -> {
            ensureContentFocused();
            saveStateForUndo();
            toggleBullet(contentEditText);

            boolean isBulletActive = isBulletLine(contentEditText.getText(),
                    contentEditText.getSelectionStart());
            updateButtonState(bulletButton, isBulletActive);
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
        undoButton.setAlpha(0.1f);
        redoButton.setAlpha(0.1f);
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

        Spannable content = contentEditText.getText();

        String htmlContent = convertSpannableToHtmlFull(content);

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
    private void toggleBullet(EditText editText) {
        Editable text = editText.getText();

        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();

        if (start < 0 || end < 0) return;

        // Normalize selection
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }

        // Expand to full lines
        start = text.toString().lastIndexOf('\n', start - 1);
        start = (start == -1) ? 0 : start + 1;

        end = text.toString().indexOf('\n', end);
        end = (end == -1) ? text.length() : end;

        int lineStart = start;

        while (lineStart <= end) {

            int lineEnd = text.toString().indexOf('\n', lineStart);
            if (lineEnd == -1) lineEnd = text.length();

            // Ensure line has at least 1 char
            if (lineStart == lineEnd) {
                text.insert(lineStart, " ");
                lineEnd = lineStart + 1;
            }

            BulletSpan[] spans = text.getSpans(lineStart, lineEnd, BulletSpan.class);

            if (spans.length > 0) {
                for (BulletSpan span : spans) {
                    text.removeSpan(span);
                }
            } else {
                text.setSpan(
                        new BulletSpan(20, isDarkMode() ? Color.WHITE : Color.BLACK),
                        lineStart,
                        lineEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

            lineStart = lineEnd + 1;
        }
    }
    private String convertSpannableToHtmlFull(Spannable text) {
        StringBuilder html = new StringBuilder();
        boolean inBulletList = false;

        String[] lines = text.toString().split("\n");
        int currentPos = 0;

        for (String line : lines) {
            int lineStart = currentPos;
            int lineEnd = currentPos + line.length();

            // Check for spans in this specific range
            BulletSpan[] bulletSpans = text.getSpans(lineStart, lineEnd, BulletSpan.class);
            boolean isBullet = bulletSpans.length > 0;

            // Gets the "clean" content of the line (preserving bold/italic)
            CharSequence lineSeq = text.subSequence(lineStart, lineEnd);
            String lineHtml = Html.toHtml(new SpannableStringBuilder(lineSeq), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
                    .replaceAll("(?i)<p[^>]*>", "") // Remove <p>
                    .replaceAll("</p>", "")         // Remove </p>
                    .replace("•", "")               // Remove literal bullet chars to prevent doubles
                    .trim();

            // Handle List State
            if (isBullet) {
                if (!inBulletList) {
                    html.append("<ul>");
                    inBulletList = true;
                }
                html.append("<li>").append(lineHtml).append("</li>");
            } else {
                if (inBulletList) {
                    html.append("</ul>");
                    inBulletList = false;
                } else if (!lineHtml.isEmpty()) {
                    html.append(lineHtml).append("<br>");
                }
            }
            currentPos = lineEnd + 1; // +1 for the newline
        }

        if (inBulletList) html.append("</ul>");
        return html.toString();
    }
    private void applyBullets(Editable text) {
        int start = 0;
        while (start < text.length()) {
            int lineEnd = text.toString().indexOf('\n', start);
            if (lineEnd == -1) lineEnd = text.length();

            // Get spans created by Html.fromHtml
            BulletSpan[] spans = text.getSpans(start, lineEnd, BulletSpan.class);

            if (spans.length > 0) {
                // Remove the default small Android bullet spans
                for (BulletSpan span : spans) text.removeSpan(span);

                // Re-apply the BulletSpan with correct styling/padding
                text.setSpan(
                        new BulletSpan(20, isDarkMode() ? Color.WHITE : Color.BLACK),
                        start,
                        lineEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
            start = lineEnd + 1;
        }
    }
    private void saveStateForUndo() {
        // Save a copy of current content
        Editable current = new SpannableStringBuilder(contentEditText.getText());
        undoStack.push(current);
        undoCursorStack.push(contentEditText.getSelectionStart());

        // Clear redo stack whenever user types new content
        redoStack.clear();
        refreshUndoRedoButtons();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
    private boolean isDarkMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
    private void applyTypingSpan(Object span) {
        int start = contentEditText.getSelectionStart();
        Editable text = contentEditText.getText();

        // Use 0-length span at cursor
        text.setSpan(span, start, start, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        contentEditText.setSelection(start);
    }
    private void applyActiveFormattingAtCursor() {
        int cursor = contentEditText.getSelectionStart();
        Editable text = contentEditText.getText();

        if (isBoldActive) {
            text.setSpan(new StyleSpan(Typeface.BOLD), cursor, cursor, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        if (isItalicActive) {
            text.setSpan(new StyleSpan(Typeface.ITALIC), cursor, cursor, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        if (isUnderlineActive) {
            text.setSpan(new UnderlineSpan(), cursor, cursor, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        if (isHighlightActive) {
            text.setSpan(new BackgroundColorSpan(Color.parseColor("#8A6D00")), cursor, cursor, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }
    private void updateButtonState(ImageButton button, boolean isActive) {
        if (isActive) {
            button.setBackgroundResource(R.drawable.format_button_active);
            button.setColorFilter(Color.WHITE); // icon color
        } else {
            button.setBackgroundResource(R.drawable.format_button_inactive);
            button.setColorFilter(isDarkMode() ? Color.WHITE : Color.parseColor("#5E6A80"));
        }
    }
    private void applyStyleToSelection(int style, boolean activate) {
        int start = contentEditText.getSelectionStart();
        int end = contentEditText.getSelectionEnd();
        Editable s = contentEditText.getText();

        if (activate) {
            s.setSpan(new StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            StyleSpan[] spans = s.getSpans(start, end, StyleSpan.class);
            for (StyleSpan span : spans) {
                if (span.getStyle() == style) s.removeSpan(span);
            }
        }
    }
    private void updateCursorFormattingPreview() {
        int cursor = contentEditText.getSelectionStart();
        Editable text = contentEditText.getText();

        // Apply ALL active formats as 0-length preview spans at cursor
        if (isBoldActive) {
            text.setSpan(new StyleSpan(Typeface.BOLD), cursor, cursor, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        if (isItalicActive) {
            text.setSpan(new StyleSpan(Typeface.ITALIC), cursor, cursor, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        if (isUnderlineActive) {
            text.setSpan(new UnderlineSpan(), cursor, cursor, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        if (isHighlightActive) {
            text.setSpan(new BackgroundColorSpan(Color.parseColor("#8A6D00")),
                    cursor, cursor, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }
    private boolean isBulletLine(Editable s, int start) {
        int lineStart = s.toString().lastIndexOf('\n', start - 1);
        lineStart = (lineStart == -1) ? 0 : lineStart + 1;

        int lineEnd = s.toString().indexOf('\n', lineStart);
        if (lineEnd == -1) lineEnd = s.length();

        BulletSpan[] spans = s.getSpans(lineStart, lineEnd, BulletSpan.class);
        return spans.length > 0;
    }
    private void handleUnderline(Editable s, int start, int end, boolean active) {
        UnderlineSpan[] spans = s.getSpans(start, end, UnderlineSpan.class);

        if (active) {
            if (spans.length == 0) {
                s.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
        } else {
            for (UnderlineSpan span : spans) {
                int spanStart = s.getSpanStart(span);
                int spanEnd = s.getSpanEnd(span);

                s.removeSpan(span);

                if (spanStart < start) {
                    s.setSpan(new UnderlineSpan(), spanStart, start, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                }
                if (spanEnd > end) {
                    s.setSpan(new UnderlineSpan(), end, spanEnd, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
                }
            }
        }
    }
    private void handleHighlight(Editable s, int start, int end, boolean active) {
        BackgroundColorSpan[] spans = s.getSpans(start, end, BackgroundColorSpan.class);

        if (active) {
            // Remove existing first (prevents conflicts)
            for (BackgroundColorSpan span : spans) {
                s.removeSpan(span);
            }

            s.setSpan(
                    new BackgroundColorSpan(Color.parseColor("#8A6D00")),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );

        } else {
            for (BackgroundColorSpan span : spans) {
                int spanStart = s.getSpanStart(span);
                int spanEnd = s.getSpanEnd(span);

                s.removeSpan(span);

                if (spanStart < start) {
                    s.setSpan(new BackgroundColorSpan(Color.parseColor("#8A6D00")),
                            spanStart, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (spanEnd > end) {
                    s.setSpan(new BackgroundColorSpan(Color.parseColor("#8A6D00")),
                            end, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }
    private void updateFormattingState() {
        int start = contentEditText.getSelectionStart();
        int end = contentEditText.getSelectionEnd();
        Editable s = contentEditText.getText();

        if (start < 0) return;

        int checkPos = (start == end && start > 0) ? start - 1 : start;

        // Reset first
        isBoldActive = false;
        isItalicActive = false;
        isUnderlineActive = false;
        isHighlightActive = false;

        // BOLD / ITALIC
        StyleSpan[] styleSpans = s.getSpans(checkPos, checkPos + 1, StyleSpan.class);
        for (StyleSpan span : styleSpans) {
            if (span.getStyle() == Typeface.BOLD) isBoldActive = true;
            if (span.getStyle() == Typeface.ITALIC) isItalicActive = true;
        }

        // UNDERLINE
        isUnderlineActive = s.getSpans(checkPos, checkPos + 1, UnderlineSpan.class).length > 0;

        // HIGHLIGHT
        isHighlightActive = s.getSpans(checkPos, checkPos + 1, BackgroundColorSpan.class).length > 0;

        // BULLET
        boolean isBulletActive = isBulletLine(s, start);
        updateButtonState(bulletButton, isBulletActive);

        // Update UI
        updateButtonState(boldButton, isBoldActive);
        updateButtonState(italicButton, isItalicActive);
        updateButtonState(underlineButton, isUnderlineActive);
        updateButtonState(highlightButton, isHighlightActive);
    }
    private void refreshUndoRedoButtons() {
        boolean canUndo = !undoStack.isEmpty();
        boolean canRedo = !redoStack.isEmpty();

        undoButton.setEnabled(canUndo);
        undoButton.setAlpha(canUndo ? 1f : 0.1f);

        redoButton.setEnabled(canRedo);
        redoButton.setAlpha(canRedo ? 1f : 0.1f);
    }
}