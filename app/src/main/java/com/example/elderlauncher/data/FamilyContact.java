package com.example.elderlauncher.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "family_contacts")
public class FamilyContact {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public String phone;
    public String avatarUri;
    public int sortOrder;
    public boolean enabled;

    public FamilyContact(String name, String phone, String avatarUri, int sortOrder, boolean enabled) {
        this.name = name;
        this.phone = phone;
        this.avatarUri = avatarUri;
        this.sortOrder = sortOrder;
        this.enabled = enabled;
    }
}
