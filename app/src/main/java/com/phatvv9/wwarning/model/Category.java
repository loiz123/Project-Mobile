package com.phatvv9.wwarning.model;

import com.google.firebase.firestore.DocumentId;

public class Category {
    @DocumentId
    private String id;
    private String name;
    private String icon_url;
    private int severity_level;
    private boolean is_risk;
    private boolean is_active;

    // Constructor rỗng bắt buộc cho Firestore
    public Category() {}

    public Category(String name, String icon_url, int severity_level, boolean is_risk, boolean is_active) {
        this.name = name;
        this.icon_url = icon_url;
        this.severity_level = severity_level;
        this.is_risk = is_risk;
        this.is_active = is_active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon_url() { return icon_url; }
    public void setIcon_url(String icon_url) { this.icon_url = icon_url; }
    public int getSeverity_level() { return severity_level; }
    public void setSeverity_level(int severity_level) { this.severity_level = severity_level; }
    public boolean isIs_risk() { return is_risk; }
    public void setIs_risk(boolean is_risk) { this.is_risk = is_risk; }
    public boolean isIs_active() { return is_active; }
    public void setIs_active(boolean is_active) { this.is_active = is_active; }
}