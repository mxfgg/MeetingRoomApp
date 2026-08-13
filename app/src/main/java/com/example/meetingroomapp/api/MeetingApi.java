package com.example.meetingroomapp.api;

import com.example.meetingroomapp.data.model.ApiCallback;
import com.example.meetingroomapp.data.model.MeetingInfo;
import com.example.meetingroomapp.data.model.SimpleResult;

import java.util.List;

/**
 * 会议平台统一接口 — 飞书和Outlook各自实现此接口
 */
public interface MeetingApi {
    void fetchMeetings(ApiCallback<List<MeetingInfo>> callback);
    void performQuickBook(ApiCallback<SimpleResult> callback);
    void invalidateToken();
}
