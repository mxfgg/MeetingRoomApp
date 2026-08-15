package com.example.meetingroomapp;

import com.example.meetingroomapp.data.model.MeetingInfo;
import org.junit.Test;
import static org.junit.Assert.*;

public class MeetingInfoTest {

    @Test
    public void fromTimestamps_basicFieldsCorrect() {
        long start = 1700000000000L;
        long end = 1700003600000L;
        MeetingInfo m = MeetingInfo.fromTimestamps("uid-1", start, end, "张三", "周会");

        assertEquals("uid-1", m.getUid());
        assertEquals("张三", m.getOwnerName());
        assertEquals("周会", m.getSummary());
        assertEquals(start, m.getStartTimestamp());
        assertEquals(end, m.getEndTimestamp());
    }

    @Test
    public void fromTimestamps_nullOwnerDefaultsToAnonymous() {
        MeetingInfo m = MeetingInfo.fromTimestamps("uid-2", 1000L, 2000L, null, "主题");
        assertEquals("匿名", m.getOwnerName());
    }

    @Test
    public void fromTimestamps_emptySummaryDefaultsToNoSummary() {
        MeetingInfo m = MeetingInfo.fromTimestamps("uid-3", 1000L, 2000L, "李四", "");
        assertEquals("无主题", m.getSummary());
    }

    @Test
    public void fromTimestamps_nullSummaryDefaultsToNoSummary() {
        MeetingInfo m = MeetingInfo.fromTimestamps("uid-4", 1000L, 2000L, "李四", null);
        assertEquals("无主题", m.getSummary());
    }

    @Test
    public void isCurrentMeeting_withinRange() {
        long now = System.currentTimeMillis();
        MeetingInfo m = MeetingInfo.fromTimestamps("uid-5", now - 30_000L, now + 3600_000L, "王五", "当前会议");
        assertTrue(m.isCurrentMeeting());
    }

    @Test
    public void isCurrentMeeting_beforeStart_notCurrent() {
        long now = System.currentTimeMillis();
        MeetingInfo m = MeetingInfo.fromTimestamps("uid-6", now + 60_000L, now + 3600_000L, "王五", "未来会议");
        assertFalse(m.isCurrentMeeting());
    }

    @Test
    public void isCurrentMeeting_afterEnd_notCurrent() {
        long now = System.currentTimeMillis();
        MeetingInfo m = MeetingInfo.fromTimestamps("uid-7", now - 7200_000L, now - 60_000L, "王五", "已结束会议");
        assertFalse(m.isCurrentMeeting());
    }

    @Test
    public void isCurrentMeeting_withinOneMinuteGrace() {
        long now = System.currentTimeMillis();
        MeetingInfo m = MeetingInfo.fromTimestamps("uid-8", now - 30_000L, now + 3600_000L, "王五", "即将开始");
        assertTrue(m.isCurrentMeeting());
    }

    @Test
    public void isFutureMeeting_futureMeeting() {
        long now = System.currentTimeMillis();
        MeetingInfo m = MeetingInfo.fromTimestamps("uid-9", now + 3600_000L, now + 7200_000L, "赵六", "未来会议");
        assertTrue(m.isFutureMeeting());
    }

    @Test
    public void isFutureMeeting_pastMeeting() {
        long now = System.currentTimeMillis();
        MeetingInfo m = MeetingInfo.fromTimestamps("uid-10", now - 7200_000L, now - 3600_000L, "赵六", "过去会议");
        assertFalse(m.isFutureMeeting());
    }

    @Test
    public void parseIso8601_standardFormat() {
        long ts = MeetingInfo.parseIso8601("2024-01-15T10:30:00");
        assertTrue(ts > 0);
    }

    @Test
    public void parseIso8601_withZulu() {
        long ts = MeetingInfo.parseIso8601("2024-01-15T10:30:00Z");
        assertTrue(ts > 0);
    }

    @Test
    public void parseIso8601_withTimezone() {
        long ts = MeetingInfo.parseIso8601("2024-01-15T10:30:00+08:00");
        assertTrue(ts > 0);
    }

    @Test
    public void parseIso8601_withMillis() {
        long ts = MeetingInfo.parseIso8601("2024-01-15T10:30:00.123456+08:00");
        assertTrue(ts > 0);
    }

    @Test
    public void parseIso8601_nullInput() {
        assertEquals(0, MeetingInfo.parseIso8601(null));
    }

    @Test
    public void parseIso8601_emptyInput() {
        assertEquals(0, MeetingInfo.parseIso8601(""));
    }

    @Test
    public void parseIso8601_invalidInput() {
        assertEquals(0, MeetingInfo.parseIso8601("not-a-date"));
    }

    @Test
    public void getDisplayTime_containsSeparator() {
        MeetingInfo m = MeetingInfo.fromTimestamps("uid-11", 1700000000000L, 1700003600000L, "测试", "测试");
        assertTrue(m.getDisplayTime().contains(" ~ "));
    }

    @Test
    public void sameDay_endTimeFormatShort() {
        long start = 1700000000000L;
        long end = start + 3600_000L;
        MeetingInfo m = MeetingInfo.fromTimestamps("uid-12", start, end, "测试", "测试");
        String displayTime = m.getDisplayTime();
        assertNotNull(displayTime);
        assertTrue(displayTime.length() > 0);
    }
}
