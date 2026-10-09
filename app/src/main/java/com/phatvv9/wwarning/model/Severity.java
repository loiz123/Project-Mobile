package com.phatvv9.wwarning.model;

import com.google.firebase.firestore.DocumentId;

public class Severity {
    @DocumentId
    private String id;
    private String category_id;
    private String name;
    private int level;

    public Severity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCategory_id() { return category_id; }
    public void setCategory_id(String category_id) { this.category_id = category_id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
}