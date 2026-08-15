package com.example.meetingroomapp;

import android.content.Context;
import android.content.Intent;
import android.os.Process;
import android.util.Log;

/**
 * 全局未捕获异常处理器 — 崩溃后自动重启应用
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public CrashHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void register() { Thread.setDefaultUncaughtExceptionHandler(this); }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Log.e(TAG, "Uncaught exception", e);
        Intent intent = new Intent(context, PlatformSelectActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
        if (defaultHandler != null) defaultHandler.uncaughtException(t, e);
        else Process.killProcess(Process.myPid());
    }
}
