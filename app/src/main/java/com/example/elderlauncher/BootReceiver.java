package com.example.elderlauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.elderlauncher.ui.LauncherActivity;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : null;
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            Intent launchIntent = new Intent(context, LauncherActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(launchIntent);
        }
    }
}
