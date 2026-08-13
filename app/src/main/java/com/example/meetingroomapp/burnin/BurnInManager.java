package com.example.meetingroomapp.burnin;

import android.os.Handler;
import android.os.Looper;
import com.example.meetingroomapp.data.model.AppConfig;
import java.util.*;

/**
 * 防烧屏总调度器 — 协调像素偏移、屏保和夜间暗屏
 */
public class BurnInManager {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private BurnInListener listener;
    private boolean running, nightDimActive, screenSaverActive, isRoomFree = true;
    private long lastInteractionTime = System.currentTimeMillis();
    private int dirIndex = 0;

    public interface BurnInListener {
        void onApplyOffset(int offsetX, int offsetY);
        void onSwitchStatusBarPosition(boolean toTop);
        void onEnterScreenSaver();
        void onExitScreenSaver();
        void onNightDimEnable(float brightness);
        void onNightDimDisable();
    }

    public void setListener(BurnInListener l) { this.listener = l; }

    public void start() {
        if (running) return; running = true;
        schedule(AppConfig.OFFSET_INTERVAL_MS, this::doOffset);
        schedule(AppConfig.STATUS_BAR_SWITCH_MS, () -> notify(l -> l.onSwitchStatusBarPosition(true)));
        schedule(60_000L, this::doScreenSaverCheck);
        schedule(300_000L, this::doNightDimCheck);
    }

    public void stop() { running = false; handler.removeCallbacksAndMessages(null); }

    public void onUserInteraction() { lastInteractionTime = System.currentTimeMillis(); if (screenSaverActive) exitScreenSaver(); }
    public void onMeetingStatusChanged(boolean isFree) { isRoomFree = isFree; if (!isFree && screenSaverActive) exitScreenSaver(); }

    private void schedule(long interval, Runnable action) { handler.postDelayed(new Runnable() { @Override public void run() { if (running) { action.run(); handler.postDelayed(this, interval); } } }, interval); }

    /** 每5分钟计算2-8像素随机偏移，方向循环 */
    private void doOffset() {
        int dir = dirIndex++ % 4, px = AppConfig.OFFSET_MIN_PX + random.nextInt(AppConfig.OFFSET_MAX_PX - AppConfig.OFFSET_MIN_PX + 1);
        int x = dir == 1 ? px : dir == 3 ? -px : 0, y = dir == 2 ? px : dir == 0 ? -px : 0;
        notify(l -> l.onApplyOffset(x, y));
        handler.postDelayed(() -> notify(l -> l.onApplyOffset(0, 0)), AppConfig.OFFSET_INTERVAL_MS / 2);
    }

    /** 2小时无交互且会议室空闲时触发屏保 */
    private void doScreenSaverCheck() { if (!screenSaverActive && System.currentTimeMillis() - lastInteractionTime >= AppConfig.SCREEN_SAVER_IDLE_MS && isRoomFree) { screenSaverActive = true; notify(l -> l.onEnterScreenSaver()); } }
    private void exitScreenSaver() { screenSaverActive = false; lastInteractionTime = System.currentTimeMillis(); notify(l -> l.onExitScreenSaver()); }

    /** 22:00~07:00自动降低亮度 */
    private void doNightDimCheck() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean dim = hour >= AppConfig.NIGHT_START_HOUR || hour < AppConfig.NIGHT_END_HOUR;
        if (dim && !nightDimActive) { nightDimActive = true; notify(l -> l.onNightDimEnable(AppConfig.NIGHT_BRIGHTNESS)); }
        else if (!dim && nightDimActive) { nightDimActive = false; notify(l -> l.onNightDimDisable()); }
    }

    private void notify(java.util.function.Consumer<BurnInListener> action) { if (listener != null) action.accept(listener); }
}
