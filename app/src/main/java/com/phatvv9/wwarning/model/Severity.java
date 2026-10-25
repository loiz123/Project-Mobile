package com.phatvv9.wwarning.model;

import com.google.firebase.firestore.DocumentId;

public class Severity {
    @DocumentId
    private String id;
    private String categoryId;
    private String name;
    private int level;

    public Severity() {}

    public Severity(String categoryId, String name, int level) {
        this.categoryId = categoryId;
        this.name = name;
        this.level = level;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
}
