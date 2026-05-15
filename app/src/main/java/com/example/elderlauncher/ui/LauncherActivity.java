package com.example.elderlauncher.ui;

import android.Manifest;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.elderlauncher.R;
import com.example.elderlauncher.data.FamilyContact;
import com.example.elderlauncher.data.FamilyContactRepository;
import com.example.elderlauncher.service.QuickShieldAccessibilityService;
import com.example.elderlauncher.util.PinStore;
import com.example.elderlauncher.util.TimeUtils;

import java.util.List;
import java.util.Locale;

public class LauncherActivity extends AppCompatActivity {
    private static final String TAG = "LauncherActivity";
    private static final String DOUYIN_PACKAGE = "com.ss.android.ugc.aweme";
    private static final long EXIT_HOLD_MS = 5000L;
    private static final long CAREGIVER_HOLD_MS = 1800L;

    private TextView tvTime;
    private TextView tvDate;
    private TextView tvEmpty;
    private RecyclerView rvContacts;
    private Button btnFlashlight;
    private Button btnCleanDouyin;

    private FamilyContactRepository repository;
    private FamilyContactAdapter adapter;
    private PinStore pinStore;
    private boolean debugUnlockEnabled;
    private CameraManager cameraManager;
    private String torchCameraId;
    private boolean torchOn;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            tvTime.setText(TimeUtils.formatTime(now));
            tvDate.setText(TimeUtils.formatDate(now));
            mainHandler.postDelayed(this, 30_000L);
        }
    };

    private FamilyContact pendingCallContact;

    private final ActivityResultLauncher<String> callPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted && pendingCallContact != null) {
                    executeCall(pendingCallContact.phone);
                } else if (!granted) {
                    Toast.makeText(this, R.string.call_permission_needed, Toast.LENGTH_LONG).show();
                }
                pendingCallContact = null;
            }
    );

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (!granted) {
                    Toast.makeText(this, R.string.camera_permission_needed, Toast.LENGTH_LONG).show();
                    return;
                }
                toggleFlashlight();
                updateFlashlightButtonText();
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pinStore = new PinStore(this);
        if (!pinStore.hasPin()) {
            Intent setupIntent = new Intent(this, PinSetupActivity.class);
            setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(setupIntent);
            finish();
            return;
        }

        setContentView(R.layout.activity_launcher);

        repository = new FamilyContactRepository(this);

        Button btnDouyin = findViewById(R.id.btnDouyin);
        btnFlashlight = findViewById(R.id.btnFlashlight);
        btnCleanDouyin = findViewById(R.id.btnCleanDouyin);
        tvTime = findViewById(R.id.tvTime);
        tvDate = findViewById(R.id.tvDate);
        tvEmpty = findViewById(R.id.tvNoContacts);
        rvContacts = findViewById(R.id.rvContacts);
        ImageButton btnExitHotspot = findViewById(R.id.btnExitHotspot);
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        torchCameraId = findTorchCameraId();

        adapter = new FamilyContactAdapter(false, new FamilyContactAdapter.ContactActionListener() {
            @Override
            public void onCall(FamilyContact contact) {
                attemptCall(contact);
            }

            @Override
            public void onEdit(FamilyContact contact) {
                // no-op in launcher
            }
        });
        rvContacts.setAdapter(adapter);

        tvEmpty.setText(R.string.no_launcher_hint);
        btnDouyin.setOnClickListener(v -> openDouyin());
        btnFlashlight.setOnClickListener(v -> toggleFlashlightWithPermission());
        btnCleanDouyin.setOnClickListener(v -> cleanDouyinBackground());
        applyActionButtonStyles(btnDouyin, btnFlashlight, btnCleanDouyin);
        setupExitHotspot(btnExitHotspot);
        updateFlashlightButtonText();

        LiveData<List<FamilyContact>> contactsLiveData = repository.observeEnabledContacts();
        contactsLiveData.observe(this, contacts -> {
            adapter.submitList(contacts);
            tvEmpty.setVisibility((contacts == null || contacts.isEmpty()) ? TextView.VISIBLE : TextView.GONE);
            updateGridSpan(contacts == null ? 0 : contacts.size());
        });

        mainHandler.post(clockRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        forceTorchOff();
        mainHandler.removeCallbacks(clockRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (debugUnlockEnabled) {
            maybeExitManagedLockTask();
        } else {
            applyImmersiveMode();
            maybeEnterManagedLockTask();
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (!debugUnlockEnabled) {
                applyImmersiveMode();
            }
        }
    }

    private void applyImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (getWindow().getInsetsController() != null) {
                getWindow().getInsetsController().hide(
                        android.view.WindowInsets.Type.statusBars()
                                | android.view.WindowInsets.Type.navigationBars());
                getWindow().getInsetsController().setSystemBarsBehavior(
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    private void setupExitHotspot(ImageButton button) {
        final Handler holdHandler = new Handler(Looper.getMainLooper());
        final Runnable holdRunnable = this::showCaregiverTools;

        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    holdHandler.postDelayed(holdRunnable, CAREGIVER_HOLD_MS);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    holdHandler.removeCallbacks(holdRunnable);
                    return true;
                default:
                    return false;
            }
        });
    }

    private void showCaregiverTools() {
        final String[] items = new String[]{
                getString(R.string.caregiver_item_settings),
                getString(R.string.caregiver_item_home),
                getString(R.string.caregiver_item_accessibility),
                debugUnlockEnabled
                        ? getString(R.string.caregiver_item_debug_off)
                        : getString(R.string.caregiver_item_debug_on)
        };

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.caregiver_tools) + "（" + getAccessibilityStatus() + "）")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showPinVerifyDialog();
                            break;
                        case 1:
                            confirmExitToSystemHomeSettings();
                            break;
                        case 2:
                            openAccessibilitySettings();
                            break;
                        case 3:
                            setDebugUnlockEnabled(!debugUnlockEnabled);
                            break;
                        default:
                            break;
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String getAccessibilityStatus() {
        if (debugUnlockEnabled) {
            return getString(R.string.accessibility_service_paused);
        }
        return isAccessibilityServiceEnabled()
                ? getString(R.string.accessibility_service_on)
                : getString(R.string.accessibility_service_off);
    }

    private void showPinVerifyDialog() {
        final EditText input = new EditText(this);
        input.setHint(R.string.pin_hint);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setMaxLines(1);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);

        new AlertDialog.Builder(this)
                .setTitle(R.string.verify_pin_title)
                .setView(input)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String pin = input.getText() == null ? "" : input.getText().toString().trim();
                    if (pinStore.verifyPin(pin)) {
                        maybeExitManagedLockTask();
                        Intent settingsIntent = new Intent(this, SettingsActivity.class);
                        startActivity(settingsIntent);
                    } else {
                        Toast.makeText(this, R.string.pin_incorrect, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmExitToSystemHomeSettings() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_exit_title)
                .setMessage(R.string.confirm_exit_message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    maybeExitManagedLockTask();
                    Intent intent = new Intent(Settings.ACTION_HOME_SETTINGS);
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, R.string.show_home_settings_failed, Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateGridSpan(int count) {
        int span;
        if (count <= 2) {
            span = 1;
        } else if (count <= 4) {
            span = 2;
        } else {
            span = 3;
        }
        rvContacts.setLayoutManager(new GridLayoutManager(this, span));
    }

    private void openDouyin() {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(DOUYIN_PACKAGE);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
            return;
        }

        try {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://search?q=" + DOUYIN_PACKAGE));
            startActivity(marketIntent);
        } catch (ActivityNotFoundException ex) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/search?q=" + DOUYIN_PACKAGE + "&c=apps"));
            startActivity(webIntent);
        }
        Toast.makeText(this, R.string.douyin_not_installed, Toast.LENGTH_LONG).show();
    }

    private void cleanDouyinBackground() {
        if (debugUnlockEnabled) {
            Toast.makeText(this, R.string.debug_unlock_on, Toast.LENGTH_SHORT).show();
            return;
        }
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager != null) {
            manager.killBackgroundProcesses(DOUYIN_PACKAGE);
        }
        Toast.makeText(this, R.string.clean_douyin_done, Toast.LENGTH_SHORT).show();
    }

    private void toggleFlashlightWithPermission() {
        if (torchCameraId == null) {
            Toast.makeText(this, R.string.flashlight_not_supported_simple, Toast.LENGTH_SHORT).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        toggleFlashlight();
        updateFlashlightButtonText();
    }

    private String findTorchCameraId() {
        if (cameraManager == null) {
            return null;
        }
        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics cc = cameraManager.getCameraCharacteristics(id);
                Boolean flashAvailable = cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer lensFacing = cc.get(CameraCharacteristics.LENS_FACING);
                if (Boolean.TRUE.equals(flashAvailable)
                        && lensFacing != null
                        && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return id;
                }
            }
        } catch (CameraAccessException ignored) {
        }
        return null;
    }

    private void toggleFlashlight() {
        if (torchCameraId == null || cameraManager == null) {
            Toast.makeText(this, R.string.flashlight_not_supported_simple, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            torchOn = !torchOn;
            cameraManager.setTorchMode(torchCameraId, torchOn);
            Toast.makeText(this,
                    torchOn ? R.string.flashlight_on_done : R.string.flashlight_off_done,
                    Toast.LENGTH_SHORT).show();
        } catch (CameraAccessException | IllegalArgumentException e) {
            torchOn = false;
            Toast.makeText(this, R.string.flashlight_not_supported_simple, Toast.LENGTH_SHORT).show();
        }
    }

    private void forceTorchOff() {
        if (!torchOn || cameraManager == null || torchCameraId == null) {
            return;
        }
        try {
            cameraManager.setTorchMode(torchCameraId, false);
        } catch (Exception ignored) {
        } finally {
            torchOn = false;
        }
    }

    private void updateFlashlightButtonText() {
        if (btnFlashlight != null) {
            btnFlashlight.setText(torchOn ? R.string.flashlight_off : R.string.flashlight_on);
        }
    }

    private void applyActionButtonStyles(Button btnDouyin, Button btnFlash, Button btnClean) {
        // Force custom backgrounds on OEM skins that override XML button style tints.
        ViewCompat.setBackgroundTintList(btnDouyin, null);
        ViewCompat.setBackgroundTintList(btnFlash, null);
        ViewCompat.setBackgroundTintList(btnClean, null);

        btnDouyin.setBackgroundResource(R.drawable.bg_douyin_hero);
        btnFlash.setBackgroundResource(R.drawable.bg_tool_flash);
        btnClean.setBackgroundResource(R.drawable.bg_tool_trash);

        btnDouyin.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_btn_douyin, 0, 0, 0);
        btnFlash.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_btn_flashlight, 0, 0, 0);
        btnClean.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_btn_trash, 0, 0, 0);
    }

    private void attemptCall(@NonNull FamilyContact contact) {
        if (TextUtils.isEmpty(contact.phone) || !PhoneNumberUtils.isGlobalPhoneNumber(contact.phone)) {
            Toast.makeText(this, R.string.invalid_phone, Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            executeCall(contact.phone);
            return;
        }

        pendingCallContact = contact;
        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE);
    }

    private void executeCall(@NonNull String phone) {
        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(phone)));
        try {
            startActivity(callIntent);
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.call_permission_needed, Toast.LENGTH_LONG).show();
        }
    }

    private void maybeEnterManagedLockTask() {
        if (debugUnlockEnabled) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        if (dpm == null || !dpm.isLockTaskPermitted(getPackageName())) {
            return;
        }
        try {
            startLockTask();
        } catch (IllegalStateException ignored) {
        }
    }

    private void maybeExitManagedLockTask() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (activityManager == null) {
            return;
        }

        boolean inLockTaskMode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            inLockTaskMode = activityManager.getLockTaskModeState() != ActivityManager.LOCK_TASK_MODE_NONE;
        } else {
            inLockTaskMode = activityManager.isInLockTaskMode();
        }

        if (!inLockTaskMode) {
            return;
        }

        try {
            stopLockTask();
        } catch (IllegalStateException ignored) {
        }
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, R.string.show_home_settings_failed, Toast.LENGTH_LONG).show();
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String enabledFlag = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED);
        if (!"1".equals(enabledFlag)) {
            return false;
        }

        String enabledServices = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabledServices == null) {
            return false;
        }

        String target = String.format(Locale.US,
                "%s/%s",
                getPackageName(),
                "com.example.elderlauncher.service.QuickShieldAccessibilityService").toLowerCase(Locale.US);
        String altTarget = String.format(Locale.US,
                "%s/.%s",
                getPackageName(),
                "service.QuickShieldAccessibilityService").toLowerCase(Locale.US);

        String[] entries = enabledServices.toLowerCase(Locale.US).split(":");
        for (String entry : entries) {
            String normalized = entry.trim();
            if (normalized.equals(target) || normalized.equals(altTarget)) {
                return true;
            }
        }
        Log.d(TAG, "Accessibility enabled list: " + enabledServices);
        return false;
    }

    private void setDebugUnlockEnabled(boolean enabled) {
        debugUnlockEnabled = enabled;
        QuickShieldAccessibilityService.setTemporarilyPaused(enabled);
        if (enabled) {
            maybeExitManagedLockTask();
            Toast.makeText(this, R.string.debug_unlock_on, Toast.LENGTH_SHORT).show();
        } else {
            applyImmersiveMode();
            maybeEnterManagedLockTask();
            Toast.makeText(this, R.string.debug_unlock_off, Toast.LENGTH_SHORT).show();
        }
    }
}
