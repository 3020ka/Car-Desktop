package com.example.elderlauncher.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.elderlauncher.R;
import com.example.elderlauncher.util.PinStore;

public class PinSetupActivity extends AppCompatActivity {
    private PinStore pinStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_setup);

        pinStore = new PinStore(this);

        EditText etPinFirst = findViewById(R.id.etPinFirst);
        EditText etPinSecond = findViewById(R.id.etPinSecond);
        Button btnSavePin = findViewById(R.id.btnSavePin);

        etPinFirst.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPinSecond.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        btnSavePin.setOnClickListener(v -> {
            String first = textOf(etPinFirst);
            String second = textOf(etPinSecond);

            if (!pinStore.isValidPinFormat(first) || !first.equals(second)) {
                Toast.makeText(this, R.string.pin_set_mismatch, Toast.LENGTH_SHORT).show();
                return;
            }

            pinStore.savePin(first);
            Toast.makeText(this, R.string.pin_set_success, Toast.LENGTH_SHORT).show();

            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(settingsIntent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Force pin setup on first run.
    }

    private String textOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }
}
