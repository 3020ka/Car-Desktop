package com.example.elderlauncher.data;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FamilyContactRepository {
    private final FamilyContactDao dao;
    private final ExecutorService ioExecutor;

    public FamilyContactRepository(Context context) {
        this.dao = AppDatabase.getInstance(context).familyContactDao();
        this.ioExecutor = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<FamilyContact>> observeEnabledContacts() {
        return dao.observeEnabledContacts();
    }

    public LiveData<List<FamilyContact>> observeAllContacts() {
        return dao.observeAllContacts();
    }

    public void insert(FamilyContact contact) {
        ioExecutor.execute(() -> {
            if (contact.sortOrder <= 0) {
                int count = dao.countAll();
                contact.sortOrder = count + 1;
            }
            dao.insert(contact);
        });
    }

    public void update(FamilyContact contact) {
        ioExecutor.execute(() -> dao.update(contact));
    }

    public void delete(FamilyContact contact) {
        ioExecutor.execute(() -> dao.delete(contact));
    }
}
