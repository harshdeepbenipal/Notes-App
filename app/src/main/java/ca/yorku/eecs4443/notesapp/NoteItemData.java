package ca.yorku.eecs4443.notesapp;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Map;

public class NoteItemData {
    private String id;
    private String title;
    private String content;
    private int thumbnail;
    private Timestamp lastModified;
    private ArrayList<Map<String, Object>> fontSizes;

    // Updated Constructor
    public NoteItemData(String id, String title, String content, int thumbnail,
                        Timestamp lastModified) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.thumbnail = thumbnail;
        this.lastModified = lastModified;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Timestamp getLastModified() { return lastModified; }
    public ArrayList<Map<String, Object>> getFontSizes() {
        return fontSizes;
    }

    public void setFontSizes(ArrayList<Map<String, Object>> fontSizes) {
        this.fontSizes = fontSizes;
    }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setLastModified(Timestamp lastModified) { this.lastModified = lastModified; }
}