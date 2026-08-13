package com.example.meetingroomapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.meetingroomapp.PlatformSelectActivity;

/**
 * 开机自启动广播接收器 — 监听BOOT_COMPLETED广播并启动PlatformSelectActivity
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent launch = new Intent(ctx, PlatformSelectActivity.class); launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(launch);
        }
    }
}
