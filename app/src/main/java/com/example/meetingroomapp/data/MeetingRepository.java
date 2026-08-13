package com.example.meetingroomapp.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.example.meetingroomapp.api.MeetingApi;
import com.example.meetingroomapp.data.model.*;
import com.example.meetingroomapp.ui.DataChangeListener;
import java.util.*;

/**
 * 数据编排器 — 通过MeetingApi获取数据，协调缓存和轮询调度
 */
public class MeetingRepository {
    private static final String TAG = "MeetingRepository";
    private final MeetingApi meetingApi;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private boolean polling;
    private List<MeetingInfo> cachedMeetings = new ArrayList<>();
    private int consecutiveFailures = 0;
    private boolean isNextHourFree = true;
    private DataChangeListener listener;

    public MeetingRepository(MeetingApi meetingApi) { this.meetingApi = meetingApi; }
    public void setListener(DataChangeListener listener) { this.listener = listener; }
    public List<MeetingInfo> getCachedMeetings() { return cachedMeetings; }

    public void startPolling() {
        if (polling) return; polling = true;
        refreshData();
        pollRunnable = new Runnable() { @Override public void run() { if (polling) { refreshData(); handler.postDelayed(this, AppConfig.POLLING_INTERVAL_MS); } } };
        handler.postDelayed(pollRunnable, AppConfig.POLLING_INTERVAL_MS);
    }

    public void stopPolling() { polling = false; if (pollRunnable != null) { handler.removeCallbacks(pollRunnable); pollRunnable = null; } }

    public void refreshData() {
        meetingApi.fetchMeetings(new ApiCallback<List<MeetingInfo>>() {
            @Override public void onSuccess(List<MeetingInfo> m) { consecutiveFailures = 0; cachedMeetings = m; long now = System.currentTimeMillis(); isNextHourFree = true; for (MeetingInfo mi : m) if (mi.getStartTimestamp() < now + AppConfig.QUICK_BOOK_DURATION_MS && mi.getEndTimestamp() > now) { isNextHourFree = false; break; } if (listener != null) listener.onDataUpdated(m, detectCurrent(m), isNextHourFree); }
            @Override public void onFailure(String msg) {
                consecutiveFailures++; Log.w(TAG, "Fetch failed (" + consecutiveFailures + "): " + msg);
                if (consecutiveFailures >= AppConfig.CONSECUTIVE_FAILURE_THRESHOLD) { if (listener != null) listener.onNetworkError(); }
                else if (listener != null) listener.onDataUpdated(cachedMeetings, detectCurrent(cachedMeetings), isNextHourFree);
                if (msg.contains("认证失败")) { meetingApi.invalidateToken(); if (listener != null) listener.onAuthError(msg); }
            }
        });
    }

    private MeetingInfo detectCurrent(List<MeetingInfo> meetings) { for (MeetingInfo m : meetings) if (m.isCurrentMeeting()) return m; return null; }

    public void performQuickBook() {
        meetingApi.performQuickBook(new ApiCallback<SimpleResult>() {
            @Override public void onSuccess(SimpleResult r) { if (listener != null) { listener.onQuickBookResult(r); if (r.isSuccess()) refreshData(); } }
            @Override public void onFailure(String msg) { if (listener != null) listener.onQuickBookResult(SimpleResult.error(msg)); }
        });
    }
}
