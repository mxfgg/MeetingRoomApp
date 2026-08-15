package com.example.meetingroomapp.api.outlook;

import com.example.meetingroomapp.api.BaseApiClient;
import com.example.meetingroomapp.api.MeetingApi;
import com.example.meetingroomapp.config.ConfigManager;
import com.example.meetingroomapp.data.model.*;
import com.google.gson.*;
import okhttp3.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Outlook MeetingApi实现 — 封装Azure AD认证、日历视图查询和快速预约
 */
public class OutlookMeetingApi extends BaseApiClient implements MeetingApi {
    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8"), FORM_TYPE = MediaType.get("application/x-www-form-urlencoded; charset=utf-8");
    private final ConfigManager configManager;

    public OutlookMeetingApi(ConfigManager configManager) { this.configManager = configManager; }
    @Override protected String getBaseUrl() { return AppConfig.GRAPH_BASE_URL; }

    private void get(String path, String token, ApiCallback<String> cb) { executeWithRetry(new Request.Builder().url(getBaseUrl() + path).get().addHeader("Authorization", "Bearer " + token).addHeader("Content-Type", "application/json; charset=utf-8").addHeader("Prefer", "outlook.timezone=\"Asia/Shanghai\"").build(), cb); }
    private void postWithAuth(String path, String token, String body, ApiCallback<String> cb) { executeWithRetry(new Request.Builder().url(getBaseUrl() + path).post(RequestBody.create(body, JSON_TYPE)).addHeader("Authorization", "Bearer " + token).addHeader("Content-Type", "application/json; charset=utf-8").build(), cb); }
    private void postForm(String url, String body, ApiCallback<String> cb) { executeWithRetry(new Request.Builder().url(url).post(RequestBody.create(body, FORM_TYPE)).addHeader("Content-Type", "application/x-www-form-urlencoded").build(), cb); }

    private void getAccessToken(String clientId, String clientSecret, String tenantId, ApiCallback<TokenResult> cb) {
        try {
            String body = "grant_type=client_credentials&client_id=" + URLEncoder.encode(clientId, "UTF-8") + "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8") + "&scope=" + URLEncoder.encode(AppConfig.GRAPH_SCOPE, "UTF-8");
            postForm(String.format(AppConfig.GRAPH_AUTH_URL_TEMPLATE, tenantId), body, new ApiCallback<String>() {
            @Override public void onSuccess(String result) { try { JsonObject json = JsonParser.parseString(result).getAsJsonObject(); if (json.has("access_token")) cb.onSuccess(TokenResult.ok(json.get("access_token").getAsString(), json.has("expires_in") ? json.get("expires_in").getAsInt() : 3600)); else cb.onSuccess(TokenResult.err("认证失败: " + (json.has("error") ? json.get("error").getAsString() : "unknown") + " - " + (json.has("error_description") ? json.get("error_description").getAsString() : ""))); } catch (Exception e) { cb.onSuccess(TokenResult.err("解析认证响应失败")); } }
            @Override public void onFailure(String msg) { cb.onFailure(msg); }
        });
        } catch (Exception e) { cb.onFailure("构建认证请求失败: " + e.getMessage()); }
    }

    @Override
    public void fetchMeetings(ApiCallback<List<MeetingInfo>> callback) {
        withToken(token -> {
            String roomEmail = configManager.getRoomEmail();
            if (roomEmail == null) { callback.onFailure("会议室邮箱未配置"); return; }
            SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Calendar cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -1); cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            String start = isoFmt.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, AppConfig.QUERY_DAYS_AHEAD + 1); cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            String end = isoFmt.format(cal.getTime());
            try {
                String path = "/users/" + URLEncoder.encode(roomEmail, "UTF-8") + "/calendarView?startDateTime=" + URLEncoder.encode(start, "UTF-8") + "&endDateTime=" + URLEncoder.encode(end, "UTF-8");
                get(path, token, new ApiCallback<String>() {
                    @Override public void onSuccess(String result) { try { JsonObject json = JsonParser.parseString(result).getAsJsonObject(); if (json.has("error")) { callback.onFailure("日历查询失败: " + (json.getAsJsonObject("error").has("message") ? json.getAsJsonObject("error").get("message").getAsString() : "查询失败")); return; } callback.onSuccess(parseCalendarEvents(json)); } catch (Exception e) { callback.onFailure("解析日历数据失败"); } }
                    @Override public void onFailure(String msg) { callback.onFailure(msg); }
                });
            } catch (Exception e) { callback.onFailure("构建请求URL失败: " + e.getMessage()); }
        }, msg -> callback.onFailure(msg));
    }

    @Override
    public void performQuickBook(ApiCallback<SimpleResult> callback) {
        withToken(token -> {
            String roomEmail = configManager.getRoomEmail();
            if (roomEmail == null) { callback.onFailure("会议室邮箱未配置"); return; }
            SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            long now = System.currentTimeMillis();
            try {
                String path = "/users/" + URLEncoder.encode(roomEmail, "UTF-8") + "/calendar/events";
                JsonObject body = new JsonObject(); body.addProperty("subject", AppConfig.QUICK_BOOK_SUMMARY);
                body.add("start", timeObj(isoFmt.format(new Date(now)), AppConfig.TIMEZONE)); body.add("end", timeObj(isoFmt.format(new Date(now + AppConfig.QUICK_BOOK_DURATION_MS)), AppConfig.TIMEZONE));
                JsonObject loc = new JsonObject(); loc.addProperty("displayName", roomEmail); loc.addProperty("locationEmailAddress", roomEmail); body.add("location", loc);
                JsonArray attendees = new JsonArray(); JsonObject att = new JsonObject(), ea = new JsonObject(); ea.addProperty("address", roomEmail); ea.addProperty("name", roomEmail); att.add("emailAddress", ea); att.addProperty("type", "resource"); attendees.add(att); body.add("attendees", attendees);
                postWithAuth(path, token, body.toString(), new ApiCallback<String>() {
                    @Override public void onSuccess(String result) { try { JsonObject json = JsonParser.parseString(result).getAsJsonObject(); if (json.has("id")) callback.onSuccess(SimpleResult.success()); else if (json.has("error")) callback.onFailure("事件创建出错: " + (json.getAsJsonObject("error").has("message") ? json.getAsJsonObject("error").get("message").getAsString() : "创建失败")); else callback.onFailure("事件创建出错: 未知响应"); } catch (Exception e) { callback.onFailure("事件创建出错: 解析响应失败"); } }
                    @Override public void onFailure(String msg) { callback.onFailure("事件创建失败: " + msg); }
                });
            } catch (Exception e) { callback.onFailure("构建创建请求失败: " + e.getMessage()); }
        }, msg -> callback.onFailure(msg));
    }

    @Override public void invalidateToken() { super.invalidateToken(); }

    private List<MeetingInfo> parseCalendarEvents(JsonObject json) {
        List<MeetingInfo> meetings = new ArrayList<>();
        if (!json.has("value")) return meetings;
        long now = System.currentTimeMillis();
        for (JsonElement e : json.getAsJsonArray("value")) {
            try {
                JsonObject o = e.getAsJsonObject();
                String id = o.has("id") ? o.get("id").getAsString() : "";
                String subject = o.has("subject") && !o.get("subject").isJsonNull() ? o.get("subject").getAsString() : "无主题";
                String startDt = o.has("start") && o.getAsJsonObject("start").has("dateTime") ? o.getAsJsonObject("start").get("dateTime").getAsString() : "";
                String endDt = o.has("end") && o.getAsJsonObject("end").has("dateTime") ? o.getAsJsonObject("end").get("dateTime").getAsString() : "";
                String orgName = "匿名";
                if (o.has("organizer")) { JsonObject org = o.getAsJsonObject("organizer"); if (org.has("emailAddress") && org.getAsJsonObject("emailAddress").has("name")) orgName = org.getAsJsonObject("emailAddress").get("name").getAsString(); }
                MeetingInfo m = MeetingInfo.fromTimestamps(id, MeetingInfo.parseIso8601(startDt), MeetingInfo.parseIso8601(endDt), orgName, subject);
                if (m.getEndTimestamp() > now) meetings.add(m);
            } catch (Exception ignored) {}
        }
        return meetings;
    }

    private JsonObject timeObj(String dt, String tz) { JsonObject o = new JsonObject(); o.addProperty("dateTime", dt); o.addProperty("timeZone", tz); return o; }

    private void withToken(java.util.function.Consumer<String> onSuccess, java.util.function.Consumer<String> onFailure) {
        String clientId = configManager.getClientId(), secret = configManager.getClientSecret(), tenantId = configManager.getTenantId();
        if (clientId == null || secret == null || tenantId == null) { onFailure.accept("配置缺失"); return; }
        super.getToken(authCb -> getAccessToken(clientId, secret, tenantId, authCb), new ApiCallback<String>() {
            @Override public void onSuccess(String token) { onSuccess.accept(token); }
            @Override public void onFailure(String msg) { onFailure.accept("令牌获取失败: " + msg); }
        });
    }
}
