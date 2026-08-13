package com.example.meetingroomapp.data.model;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 会议信息业务模型，包含展示数据及状态判断
 */
public class MeetingInfo {
    private static final String ANONYMOUS = "匿名", NO_SUMMARY = "无主题";
    private String uid, startTime, endTime, ownerName, summary;
    private long startTimestamp, endTimestamp;

    public MeetingInfo() {}

    public static MeetingInfo fromTimestamps(String uid, long startTs, long endTs, String ownerName, String summary) {
        MeetingInfo info = new MeetingInfo();
        info.uid = uid; info.startTimestamp = startTs; info.endTimestamp = endTs;
        info.ownerName = ownerName != null ? ownerName : ANONYMOUS;
        info.summary = (summary != null && !summary.isEmpty()) ? summary : NO_SUMMARY;
        SimpleDateFormat fullFmt = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()), timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
        info.startTime = fullFmt.format(new Date(startTs));
        Calendar sc = Calendar.getInstance(); sc.setTimeInMillis(startTs);
        Calendar ec = Calendar.getInstance(); ec.setTimeInMillis(endTs);
        boolean sameDay = sc.get(Calendar.YEAR) == ec.get(Calendar.YEAR) && sc.get(Calendar.MONTH) == ec.get(Calendar.MONTH) && sc.get(Calendar.DAY_OF_MONTH) == ec.get(Calendar.DAY_OF_MONTH);
        info.endTime = sameDay ? timeFmt.format(new Date(endTs)) : fullFmt.format(new Date(endTs));
        return info;
    }

    public static long parseIso8601(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return 0;
        try {
            String s = isoTime.trim();
            int dotIdx = s.indexOf('.');
            if (dotIdx > 0) { int tzIdx = Math.max(Math.max(s.indexOf('+', dotIdx), s.indexOf('-', dotIdx)), s.indexOf('Z', dotIdx)); s = tzIdx > 0 ? s.substring(0, dotIdx) + s.substring(tzIdx) : s.substring(0, dotIdx); }
            s = s.replace("Z", "").replaceAll("[+-]\\d{2}:\\d{2}$", "");
            for (String fmt : new String[]{"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd HH:mm:ss"}) { try { return new SimpleDateFormat(fmt, Locale.getDefault()).parse(s).getTime(); } catch (Exception ignored) {} }
        } catch (Exception ignored) {}
        return 0;
    }

    public boolean isCurrentMeeting() { long now = System.currentTimeMillis(); return now >= startTimestamp - 60_000L && now < endTimestamp; }
    public boolean isFutureMeeting() { return System.currentTimeMillis() < startTimestamp; }
    public String getDisplayTime() { return startTime + " ~ " + endTime; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public long getStartTimestamp() { return startTimestamp; }
    public void setStartTimestamp(long startTimestamp) { this.startTimestamp = startTimestamp; }
    public long getEndTimestamp() { return endTimestamp; }
    public void setEndTimestamp(long endTimestamp) { this.endTimestamp = endTimestamp; }
}
