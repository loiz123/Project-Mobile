package com.phatvv9.wwarning.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoryEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String iconUrl;
    private boolean isRisk;
    private boolean isActive;

    public CategoryEntity(@NonNull String id, String name, String iconUrl, boolean isRisk, boolean isActive) {
        this.id = id;
        this.name = name;
        this.iconUrl = iconUrl;
        this.isRisk = isRisk;
        this.isActive = isActive;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public boolean isRisk() { return isRisk; }
    public void setRisk(boolean risk) { isRisk = risk; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
