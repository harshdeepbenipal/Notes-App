package ca.yorku.eecs4443.notesapp;

// Class for the data of the note item in navigation
public class NoteItemData {

    private String title;
    private int thumbnail;

    public NoteItemData(String title, int thumbnail) {
        this.title = title;
        this.thumbnail = thumbnail;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public int getThumbnail() {
        return thumbnail;
    }
    public void setThumbnail(int thumbnail) {
        this.thumbnail = thumbnail;
    }
}
