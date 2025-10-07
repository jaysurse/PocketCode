package com.example.coderunner.data;

public class Snippet {
    public long id;

    public String title;
    public String languageId;
    public String code;
    public long lastEdited;

    // Room works best in Java with either public fields or a no-arg constructor.
    // Provide a public no-arg constructor so Room can create instances.
    public Snippet() {
    }

    public Snippet(String title, String languageId, String code, long lastEdited) {
        this.title = title;
        this.languageId = languageId;
        this.code = code;
        this.lastEdited = lastEdited;
    }
}
