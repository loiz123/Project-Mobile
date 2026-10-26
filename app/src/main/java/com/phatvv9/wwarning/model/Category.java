package com.phatvv9.wwarning.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;

public class Category {
    @DocumentId
    private String id;
    private String name;
    private String iconUrl;
    private boolean isRisk;
    private boolean isActive;

    // Constructor rỗng bắt buộc cho Firestore
    public Category() {}

    public Category(String name, String iconUrl, boolean isRisk, boolean isActive) {
        this.name = name;
        this.iconUrl = iconUrl;
        this.isRisk = isRisk;
        this.isActive = isActive;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    @PropertyName("isRisk")
    public boolean isRisk() { return isRisk; }

    @PropertyName("isRisk")
    public void setRisk(boolean risk) { isRisk = risk; }

    @PropertyName("isActive")
    public boolean isActive() { return isActive; }

    @PropertyName("isActive")
    public void setActive(boolean active) { isActive = active; }
}
