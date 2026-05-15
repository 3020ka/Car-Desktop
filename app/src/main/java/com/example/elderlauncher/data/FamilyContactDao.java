package com.example.elderlauncher.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FamilyContactDao {
    @Query("SELECT * FROM family_contacts WHERE enabled = 1 ORDER BY sortOrder ASC, id ASC")
    LiveData<List<FamilyContact>> observeEnabledContacts();

    @Query("SELECT * FROM family_contacts ORDER BY sortOrder ASC, id ASC")
    LiveData<List<FamilyContact>> observeAllContacts();

    @Insert
    long insert(FamilyContact contact);

    @Update
    void update(FamilyContact contact);

    @Delete
    void delete(FamilyContact contact);

    @Query("SELECT COUNT(*) FROM family_contacts")
    int countAll();
}
