package com.example.elderlauncher.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderlauncher.R;
import com.example.elderlauncher.data.FamilyContact;
import com.example.elderlauncher.data.FamilyContactRepository;
import com.example.elderlauncher.util.PinStore;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private FamilyContactRepository repository;
    private FamilyContactAdapter adapter;
    private PinStore pinStore;

    private Uri currentPickedAvatarUri;
    private EditContactSession activeEditSession;

    private final ActivityResultLauncher<String[]> pickAvatarLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri == null || activeEditSession == null) {
                    return;
                }
                try {
                    getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                    // Some providers do not support persistable permission. Reading still works for many cases.
                }
                currentPickedAvatarUri = uri;
                if (activeEditSession.preview != null) {
                    activeEditSession.preview.setImageURI(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        repository = new FamilyContactRepository(this);
        pinStore = new PinStore(this);

        RecyclerView rvContacts = findViewById(R.id.rvSettingsContacts);
        Button btnAdd = findViewById(R.id.btnAddContact);
        Button btnChangePin = findViewById(R.id.btnChangePin);
        Button btnClose = findViewById(R.id.btnCloseSettings);

        adapter = new FamilyContactAdapter(true, new FamilyContactAdapter.ContactActionListener() {
            @Override
            public void onCall(FamilyContact contact) {
                // no-op in settings
            }

            @Override
            public void onEdit(FamilyContact contact) {
                showContactDialog(contact);
            }
        });

        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        rvContacts.setAdapter(adapter);

        LiveData<List<FamilyContact>> allContacts = repository.observeAllContacts();
        allContacts.observe(this, contacts -> adapter.submitList(contacts));

        btnAdd.setOnClickListener(v -> showContactDialog(null));
        btnChangePin.setOnClickListener(v -> showChangePinDialog());
        btnClose.setOnClickListener(v -> finish());
    }

    private void showContactDialog(FamilyContact existing) {
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_contact, null);
        EditText etName = view.findViewById(R.id.etContactName);
        EditText etPhone = view.findViewById(R.id.etContactPhone);
        ImageView ivAvatar = view.findViewById(R.id.ivContactAvatarPreview);
        Button btnPickAvatar = view.findViewById(R.id.btnPickAvatar);

        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        currentPickedAvatarUri = null;

        if (existing != null) {
            etName.setText(existing.name);
            etPhone.setText(existing.phone);
            if (!TextUtils.isEmpty(existing.avatarUri)) {
                ivAvatar.setImageURI(Uri.parse(existing.avatarUri));
            }
        }

        activeEditSession = new EditContactSession(ivAvatar);

        btnPickAvatar.setOnClickListener(v -> pickAvatarLauncher.launch(new String[]{"image/*"}));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? R.string.new_contact : R.string.edit_contact)
                .setView(view)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, (d, w) -> activeEditSession = null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                String name = textOf(etName);
                String phone = textOf(etPhone);
                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
                    Toast.makeText(this, R.string.empty_name_or_phone, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (existing == null) {
                    FamilyContact newItem = new FamilyContact(
                            name,
                            phone,
                            currentPickedAvatarUri != null ? currentPickedAvatarUri.toString() : "",
                            0,
                            true
                    );
                    repository.insert(newItem);
                } else {
                    existing.name = name;
                    existing.phone = phone;
                    if (currentPickedAvatarUri != null) {
                        existing.avatarUri = currentPickedAvatarUri.toString();
                    }
                    repository.update(existing);
                }

                activeEditSession = null;
                dialog.dismiss();
            });

            if (existing != null) {
                Button deleteBtn = new Button(this);
                deleteBtn.setText(R.string.delete);
                deleteBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                deleteBtn.setOnClickListener(v -> {
                    repository.delete(existing);
                    activeEditSession = null;
                    dialog.dismiss();
                });
                ((android.widget.LinearLayout) view).addView(deleteBtn);
            }
        });

        dialog.show();
    }

    private void showChangePinDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_change_pin, null);
        EditText etOld = view.findViewById(R.id.etOldPin);
        EditText etNew = view.findViewById(R.id.etNewPin);

        etOld.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etNew.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle(R.string.pin_change_title)
                .setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String oldPin = textOf(etOld);
                    String newPin = textOf(etNew);
                    if (!pinStore.verifyPin(oldPin) || !pinStore.isValidPinFormat(newPin)) {
                        Toast.makeText(this, R.string.pin_change_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pinStore.savePin(newPin);
                    Toast.makeText(this, R.string.pin_change_success, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String textOf(@NonNull EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private static class EditContactSession {
        final ImageView preview;

        EditContactSession(ImageView preview) {
            this.preview = preview;
        }
    }
}
