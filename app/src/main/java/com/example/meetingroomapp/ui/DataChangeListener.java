package com.example.meetingroomapp.ui;

import com.example.meetingroomapp.data.model.MeetingInfo;
import com.example.meetingroomapp.data.model.SimpleResult;
import java.util.List;

/**
 * 数据变化监听接口 — 由MainActivity实现
 */
public interface DataChangeListener {
    void onDataUpdated(List<MeetingInfo> meetings, MeetingInfo currentMeeting, boolean isFree);
    void onNetworkError();
    void onAuthError(String message);
    void onQuickBookResult(SimpleResult result);
}
