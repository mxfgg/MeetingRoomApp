package com.example.meetingroomapp.data.model;

/**
 * 应用配置模型及业务常量
 */
public class AppConfig {
    private Platform platform;
    private String appId, appSecret, roomId, clientId, clientSecret, tenantId, roomEmail;

    public static final long POLLING_INTERVAL_MS = 10_000L, QUICK_BOOK_DURATION_MS = 3_600_000L, OFFSET_INTERVAL_MS = 300_000L, SCREEN_SAVER_IDLE_MS = 7_200_000L, STATUS_BAR_SWITCH_MS = 1_800_000L;
    public static final int MAX_RETRY_COUNT = 3, CONSECUTIVE_FAILURE_THRESHOLD = 3, QUERY_DAYS_AHEAD = 7, OFFSET_MIN_PX = 2, OFFSET_MAX_PX = 8, NIGHT_START_HOUR = 22, NIGHT_END_HOUR = 7, APP_SECRET_MIN_LENGTH = 10, CLIENT_SECRET_MIN_LENGTH = 10;
    public static final float SCREEN_SAVER_BRIGHTNESS = 0.2f, NIGHT_BRIGHTNESS = 0.3f;
    public static final String QUICK_BOOK_SUMMARY = "【自动创建】临时会议", QUICK_BOOK_DESCRIPTION = "这是一场通过电子门牌自动创建的快速会议。", TIMEZONE = "Asia/Shanghai";
    public static final String CONFIG_FILE_NAME = "config.json", PLATFORM_PREFS_NAME = "platform_prefs", PLATFORM_KEY = "selected_platform", KEYSTORE_ALIAS = "meeting_room_app_secret_key";
    public static final String KEY_PLATFORM = "platform", KEY_APP_ID = "app_id", KEY_APP_SECRET = "app_secret", KEY_ROOM_ID = "room_id", KEY_CLIENT_ID = "client_id", KEY_CLIENT_SECRET = "client_secret", KEY_TENANT_ID = "tenant_id", KEY_ROOM_EMAIL = "room_email";
    public static final String APP_ID_PREFIX = "cli_", ROOM_ID_PREFIX = "omm_";
    public static final String GRAPH_BASE_URL = "https://graph.microsoft.com/v1.0", GRAPH_AUTH_URL_TEMPLATE = "https://login.microsoftonline.com/%s/oauth2/v2.0/token", GRAPH_SCOPE = "https://graph.microsoft.com/.default";

    public AppConfig() {}
    public AppConfig(Platform platform) { this.platform = platform; }

    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRoomEmail() { return roomEmail; }
    public void setRoomEmail(String roomEmail) { this.roomEmail = roomEmail; }
}
