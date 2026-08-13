package com.example.meetingroomapp.api.feishu;

import com.example.meetingroomapp.api.BaseApiClient;
import com.example.meetingroomapp.api.MeetingApi;
import com.example.meetingroomapp.config.ConfigManager;
import com.example.meetingroomapp.data.model.*;
import com.google.gson.*;
import android.util.Log;
import okhttp3.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 飞书MeetingApi实现 — 封装认证、会议室验证、忙闲查询和快速预约的完整流程
 */
public class FeishuMeetingApi extends BaseApiClient implements MeetingApi {
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String AUTH_PATH = "/open-apis/auth/v3/tenant_access_token/internal/";
    private static final String FREEBUSY_PATH = "/open-apis/meeting_room/freebusy/batch_get";
    private static final String ROOM_INFO_PATH = "/open-apis/vc/v1/rooms/";
    private static final String SUMMARY_PATH = "/open-apis/meeting_room/summary/batch_get";
    private static final String CREATE_PATH = "/open-apis/calendar/v4/calendars/primary/events";
    private static final String ATTENDEE_PATH = "/open-apis/calendar/v4/calendars/primary/events/%s/attendees";
    private final ConfigManager configManager;
    private String currentRoomId;

    public FeishuMeetingApi(ConfigManager configManager) { this.configManager = configManager; }
    @Override protected String getBaseUrl() { return "https://open.feishu.cn"; }

    private void post(String path, String body, ApiCallback<String> cb) { executeWithRetry(new Request.Builder().url(getBaseUrl() + path).post(RequestBody.create(body, JSON_TYPE)).addHeader("Content-Type", "application/json; charset=utf-8").build(), cb); }
    private void get(String path, String token, ApiCallback<String> cb) { executeWithRetry(new Request.Builder().url(getBaseUrl() + path).get().addHeader("Authorization", "Bearer " + token).addHeader("Content-Type", "application/json; charset=utf-8").build(), cb); }
    private void postWithAuth(String path, String token, String body, ApiCallback<String> cb) { executeWithRetry(new Request.Builder().url(getBaseUrl() + path).post(RequestBody.create(body, JSON_TYPE)).addHeader("Authorization", "Bearer " + token).addHeader("Content-Type", "application/json; charset=utf-8").build(), cb); }

    private void getTenantAccessToken(String appId, String appSecret, ApiCallback<TokenResult> cb) {
        JsonObject body = new JsonObject(); body.addProperty("app_id", appId); body.addProperty("app_secret", appSecret);
        post(AUTH_PATH, body.toString(), new ApiCallback<String>() {
            @Override public void onSuccess(String result) { try { JsonObject json = JsonParser.parseString(result).getAsJsonObject(); if (json.has("code") && json.get("code").getAsInt() == 0) cb.onSuccess(TokenResult.ok(json.get("tenant_access_token").getAsString(), json.get("expire").getAsInt())); else cb.onSuccess(TokenResult.err(json.has("msg") ? json.get("msg").getAsString() : "认证失败")); } catch (Exception e) { cb.onSuccess(TokenResult.err("解析认证响应失败")); } }
            @Override public void onFailure(String msg) { cb.onFailure(msg); }
        });
    }

    @Override
    public void fetchMeetings(ApiCallback<List<MeetingInfo>> callback) {
        withToken(token -> {
            String roomId = configManager.getRoomId();
            if (roomId == null) { callback.onFailure("会议室ID未配置"); return; }
            checkRoomExists(token, roomId, new ApiCallback<Boolean>() {
                @Override public void onSuccess(Boolean exists) { if (exists) queryFreeBusy(token, callback); else callback.onFailure("会议室ID不存在"); }
                @Override public void onFailure(String msg) { callback.onFailure("会议室查询失败: " + msg); }
            });
        }, msg -> callback.onFailure(msg));
    }

    @Override
    public void performQuickBook(ApiCallback<SimpleResult> callback) {
        withToken(token -> {
            long now = System.currentTimeMillis();
            createEvent(token, AppConfig.QUICK_BOOK_SUMMARY, AppConfig.QUICK_BOOK_DESCRIPTION, now, now + AppConfig.QUICK_BOOK_DURATION_MS, AppConfig.TIMEZONE, "public", new ApiCallback<String>() {
                @Override public void onSuccess(String eventId) { addRoomAttendee(token, eventId, callback); }
                @Override public void onFailure(String msg) { callback.onFailure("日程创建失败: " + msg); }
            });
        }, msg -> callback.onFailure(msg));
    }

    @Override public void invalidateToken() { super.invalidateToken(); }

    /** 查询会议室是否存在 — code=0存在，121003/121004不存在 */
    private void checkRoomExists(String token, String roomId, ApiCallback<Boolean> cb) {
        get(ROOM_INFO_PATH + roomId, token, new ApiCallback<String>() {
            @Override public void onSuccess(String result) { Log.d("FeishuApi", "checkRoom: " + result); int code = parseCode(result); if (code == 0) cb.onSuccess(true); else if (code == 121003 || code == 121004) cb.onSuccess(false); else cb.onFailure("查询会议室失败"); }
            @Override public void onFailure(String msg) { Log.d("FeishuApi", "checkRoom fail: " + msg); int code = parseCode(msg); if (code == 121003 || code == 121004) cb.onSuccess(false); else cb.onFailure(msg); }
        });
    }

    private void queryFreeBusy(String token, ApiCallback<List<MeetingInfo>> callback) {
        String roomId = configManager.getRoomId();
        if (roomId == null) { callback.onFailure("会议室ID未配置"); return; }
        currentRoomId = roomId;
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Calendar cal = Calendar.getInstance(); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            long timeMin = cal.getTimeInMillis() / 1000;
            cal.add(Calendar.DAY_OF_YEAR, AppConfig.QUERY_DAYS_AHEAD); cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            long timeMax = cal.getTimeInMillis() / 1000;
            String url = FREEBUSY_PATH + "?room_ids=" + URLEncoder.encode(roomId, "UTF-8") + "&time_min=" + URLEncoder.encode(fmt.format(new Date(timeMin * 1000)) + "+08:00", "UTF-8") + "&time_max=" + URLEncoder.encode(fmt.format(new Date(timeMax * 1000)) + "+08:00", "UTF-8");
            get(url, token, new ApiCallback<String>() {
                @Override public void onSuccess(String result) {
                    Log.d("FeishuApi", "freebusy: " + result);
                    try { JsonObject json = JsonParser.parseString(result).getAsJsonObject(); int code = json.has("code") ? json.get("code").getAsInt() : -1; if (code != 0) { callback.onFailure("忙闲查询失败: " + (json.has("msg") ? json.get("msg").getAsString() : "查询失败")); return; } List<MeetingInfo> meetings = parseFreeBusyEvents(json); if (meetings.isEmpty()) callback.onSuccess(meetings); else querySummaries(token, meetings, callback); }
                    catch (Exception e) { callback.onFailure("解析忙闲数据失败"); }
                }
                @Override public void onFailure(String msg) { callback.onFailure(msg); }
            });
        } catch (Exception e) { callback.onFailure("构建请求URL失败: " + e.getMessage()); }
    }

    private List<MeetingInfo> parseFreeBusyEvents(JsonObject json) {
        List<MeetingInfo> events = new ArrayList<>();
        try {
            JsonObject data = json.getAsJsonObject("data");
            JsonElement fb = data.has("free_busy") ? data.get("free_busy") : data.get("freebusy");
            if (fb == null || fb.isJsonNull()) return events;
            if (fb.isJsonObject()) { JsonObject obj = fb.getAsJsonObject(); if (currentRoomId != null && obj.has(currentRoomId)) parseEventArray(obj.getAsJsonArray(currentRoomId), events); else parseEventArray(obj.has("events") ? obj.getAsJsonArray("events") : null, events); }
            else if (fb.isJsonArray()) for (JsonElement e : fb.getAsJsonArray()) parseEventArray(e.getAsJsonObject().has("events") ? e.getAsJsonObject().getAsJsonArray("events") : null, events);
        } catch (Exception ignored) {}
        return events;
    }

    private void parseEventArray(JsonArray arr, List<MeetingInfo> events) {
        if (arr == null) return;
        for (JsonElement e : arr) {
            try {
                JsonObject o = e.getAsJsonObject();
                String uid = o.has("uid") ? o.get("uid").getAsString() : "";
                String org = o.has("organizer_info") && o.getAsJsonObject("organizer_info").has("name") ? o.getAsJsonObject("organizer_info").get("name").getAsString() : o.has("organizer_name") ? o.get("organizer_name").getAsString() : "匿名";
                events.add(MeetingInfo.fromTimestamps(uid, parseTime(o, "start_time"), parseTime(o, "end_time"), org, null));
            } catch (Exception ignored) {}
        }
    }

    private long parseTime(JsonObject obj, String field) {
        try {
            JsonElement e = obj.get(field);
            if (e == null) return 0;
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) return e.getAsLong() * 1000;
            String s = e.getAsString();
            try { return Long.parseLong(s) * 1000; } catch (NumberFormatException ignored) {}
            s = s.replace("Z", "").replace("+08:00", "").replace("+00:00", "");
            for (String fmt : new String[]{"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss"}) { try { return new SimpleDateFormat(fmt, Locale.getDefault()).parse(s).getTime(); } catch (Exception ignored) {} }
        } catch (Exception ignored) {}
        return 0;
    }

    private void querySummaries(String token, List<MeetingInfo> meetings, ApiCallback<List<MeetingInfo>> callback) {
        JsonArray arr = new JsonArray();
        for (MeetingInfo m : meetings) if (m.getUid() != null && !m.getUid().isEmpty()) { JsonObject o = new JsonObject(); o.addProperty("uid", m.getUid()); o.addProperty("original_time", 0); arr.add(o); }
        if (arr.size() == 0) { callback.onSuccess(meetings); return; }
        JsonObject body = new JsonObject(); body.add("EventUids", arr);
        postWithAuth(SUMMARY_PATH, token, body.toString(), new ApiCallback<String>() {
            @Override public void onSuccess(String result) {
                Map<String, String> summaryMap = new HashMap<>();
                try { JsonObject json = JsonParser.parseString(result).getAsJsonObject(); if (json.has("code") && json.get("code").getAsInt() == 0) { JsonObject data = json.getAsJsonObject("data"); JsonArray sums = data.has("summaries") ? data.getAsJsonArray("summaries") : data.has("EventInfos") ? data.getAsJsonArray("EventInfos") : null; if (sums != null) for (int i = 0; i < sums.size(); i++) { JsonObject s = sums.get(i).getAsJsonObject(); String uid = s.has("uid") ? s.get("uid").getAsString() : s.has("event_uid") ? s.get("event_uid").getAsString() : ""; String summary = s.has("summary") && !s.get("summary").getAsString().isEmpty() ? s.get("summary").getAsString() : "无主题"; if (!uid.isEmpty()) summaryMap.put(uid, summary); } } } catch (Exception ignored) {}
                for (MeetingInfo m : meetings) if (m.getUid() != null && summaryMap.containsKey(m.getUid())) m.setSummary(summaryMap.get(m.getUid()));
                callback.onSuccess(meetings);
            }
            @Override public void onFailure(String msg) { callback.onSuccess(meetings); }
        });
    }

    private void createEvent(String token, String summary, String description, long startTs, long endTs, String timezone, String visibility, ApiCallback<String> cb) {
        JsonObject body = new JsonObject(); body.addProperty("summary", summary); body.addProperty("description", description); body.addProperty("visibility", visibility);
        body.add("start_time", timeObj(startTs, timezone)); body.add("end_time", timeObj(endTs, timezone));
        postWithAuth(CREATE_PATH, token, body.toString(), new ApiCallback<String>() {
            @Override public void onSuccess(String result) { try { JsonObject json = JsonParser.parseString(result).getAsJsonObject(); int code = json.has("code") ? json.get("code").getAsInt() : -1; if (code == 0) cb.onSuccess(json.getAsJsonObject("data").getAsJsonObject("event").get("event_id").getAsString()); else cb.onFailure("创建失败: " + (json.has("msg") ? json.get("msg").getAsString() : "未知错误")); } catch (Exception e) { cb.onFailure("创建响应解析失败"); } }
            @Override public void onFailure(String msg) { cb.onFailure(msg); }
        });
    }

    private void addRoomAttendee(String token, String eventId, ApiCallback<SimpleResult> cb) {
        JsonObject body = new JsonObject(); JsonArray attendees = new JsonArray(); JsonObject att = new JsonObject(); att.addProperty("type", "resource"); att.addProperty("room_id", configManager.getRoomId()); attendees.add(att); body.add("attendees", attendees); body.addProperty("need_notification", false);
        postWithAuth(String.format(ATTENDEE_PATH, eventId), token, body.toString(), new ApiCallback<String>() {
            @Override public void onSuccess(String result) { try { JsonObject json = JsonParser.parseString(result).getAsJsonObject(); int code = json.has("code") ? json.get("code").getAsInt() : -1; if (code == 0) cb.onSuccess(SimpleResult.success()); else cb.onFailure("添加参会者失败: " + (json.has("msg") ? json.get("msg").getAsString() : "未知错误")); } catch (Exception e) { cb.onFailure("添加参会者响应解析失败"); } }
            @Override public void onFailure(String msg) { cb.onFailure(msg); }
        });
    }

    private JsonObject timeObj(long ts, String tz) { JsonObject o = new JsonObject(); o.addProperty("timestamp", String.valueOf(ts / 1000)); o.addProperty("timezone", tz); return o; }
    private int parseCode(String s) { try { int idx = s.indexOf("{"); if (idx >= 0) { JsonObject json = JsonParser.parseString(s.substring(idx)).getAsJsonObject(); return json.has("code") ? json.get("code").getAsInt() : -1; } } catch (Exception ignored) {} return -1; }

    private void withToken(java.util.function.Consumer<String> onSuccess, java.util.function.Consumer<String> onFailure) {
        String appId = configManager.getAppId(), secret = configManager.getAppSecret();
        if (appId == null || secret == null) { onFailure.accept("配置缺失"); return; }
        super.getToken(authCb -> getTenantAccessToken(appId, secret, authCb), new ApiCallback<String>() {
            @Override public void onSuccess(String token) { onSuccess.accept(token); }
            @Override public void onFailure(String msg) { onFailure.accept("令牌获取失败: " + msg); }
        });
    }
}
