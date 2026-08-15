package com.example.meetingroomapp.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import com.example.meetingroomapp.R;
import com.example.meetingroomapp.data.model.*;
import com.google.gson.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.regex.Pattern;

/**
 * 配置管理器 — 统一管理config.json的读写、校验、加密、平台选择和缓存
 */
public class ConfigManager {
    private static final String TAG = "ConfigManager", KEYSTORE = "AndroidKeyStore", TRANSFORM = "AES/GCM/NoPadding";
    private static final int IV_LEN = 12, TAG_LEN = 128;
    private static final Pattern UUID = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private final Context context;
    private final SharedPreferences prefs;
    private AppConfig cachedConfig;

    public ConfigManager(Context ctx) { this.context = ctx.getApplicationContext(); this.prefs = context.getSharedPreferences(AppConfig.PLATFORM_PREFS_NAME, Context.MODE_PRIVATE); }

    // ========== 结果类 ==========
    public enum ErrorType { FILE_NOT_FOUND, FIELD_MISSING, VALIDATION_FAILED, JSON_PARSE_ERROR }

    public static class LoadResult {
        public final AppConfig config;
        private final boolean success;
        private final ErrorType errorType;
        private final String errorMessage;
        private LoadResult(AppConfig config, boolean success, ErrorType errorType, String errorMessage) { this.config = config; this.success = success; this.errorType = errorType; this.errorMessage = errorMessage; }
        public static LoadResult ok(AppConfig config) { return new LoadResult(config, true, null, null); }
        public static LoadResult err(ErrorType type, String msg) { return new LoadResult(null, false, type, msg); }
        public boolean isSuccess() { return success; }
    }

    public static class SaveResult {
        private final boolean success;
        private final ValidationResult validationResult;
        private final String errorMessage;
        private SaveResult(boolean success, ValidationResult vr, String msg) { this.success = success; this.validationResult = vr; this.errorMessage = msg; }
        public static SaveResult ok() { return new SaveResult(true, null, null); }
        public static SaveResult validationFailed(ValidationResult vr) { return new SaveResult(false, vr, null); }
        public static SaveResult err(String msg) { return new SaveResult(false, null, msg); }
        public boolean isSuccess() { return success; }
        public ValidationResult getValidationResult() { return validationResult; }
        public String getErrorMessage() { return errorMessage; }
    }

    public static class ValidationResult {
        private final java.util.Map<String, String> fieldErrors = new java.util.HashMap<>();
        public void addError(String field, String message) { fieldErrors.put(field, message); }
        public boolean isValid() { return fieldErrors.isEmpty(); }
        public String getAppIdError() { return fieldErrors.get("appId"); }
        public String getAppSecretError() { return fieldErrors.get("appSecret"); }
        public String getRoomIdError() { return fieldErrors.get("roomId"); }
        public String getClientIdError() { return fieldErrors.get("clientId"); }
        public String getClientSecretError() { return fieldErrors.get("clientSecret"); }
        public String getTenantIdError() { return fieldErrors.get("tenantId"); }
        public String getRoomEmailError() { return fieldErrors.get("roomEmail"); }
    }

    // ========== 平台选择 ==========
    public Platform getSelectedPlatform() { String name = prefs.getString(AppConfig.PLATFORM_KEY, null); if (name == null) return null; try { return Platform.valueOf(name); } catch (Exception e) { return null; } }
    public void savePlatform(Platform platform) { prefs.edit().putString(AppConfig.PLATFORM_KEY, platform.name()).apply(); }

    // ========== 校验 ==========
    private ValidationResult validateFeishu(String appId, String appSecret, String roomId) {
        ValidationResult r = new ValidationResult();
        if (appId == null || appId.trim().isEmpty()) r.addError("appId", context.getString(R.string.err_app_id_empty));
        else if (!appId.startsWith(AppConfig.APP_ID_PREFIX)) r.addError("appId", context.getString(R.string.err_app_id_prefix));
        if (appSecret == null || appSecret.trim().isEmpty()) r.addError("appSecret", context.getString(R.string.err_app_secret_empty));
        else if (appSecret.length() < AppConfig.APP_SECRET_MIN_LENGTH) r.addError("appSecret", context.getString(R.string.err_app_secret_short));
        if (roomId == null || roomId.trim().isEmpty()) r.addError("roomId", context.getString(R.string.err_room_id_empty));
        else if (!roomId.startsWith(AppConfig.ROOM_ID_PREFIX)) r.addError("roomId", context.getString(R.string.err_room_id_prefix));
        return r;
    }

    private ValidationResult validateOutlook(String clientId, String clientSecret, String tenantId, String roomEmail) {
        ValidationResult r = new ValidationResult();
        if (clientId == null || clientId.trim().isEmpty()) r.addError("clientId", context.getString(R.string.err_client_id_empty));
        else if (!UUID.matcher(clientId.trim()).matches()) r.addError("clientId", context.getString(R.string.err_client_id_format));
        if (clientSecret == null || clientSecret.trim().isEmpty()) r.addError("clientSecret", context.getString(R.string.err_client_secret_empty));
        else if (clientSecret.length() < AppConfig.CLIENT_SECRET_MIN_LENGTH) r.addError("clientSecret", context.getString(R.string.err_client_secret_short));
        if (tenantId == null || tenantId.trim().isEmpty()) r.addError("tenantId", context.getString(R.string.err_tenant_id_empty));
        else if (!UUID.matcher(tenantId.trim()).matches()) r.addError("tenantId", context.getString(R.string.err_tenant_id_format));
        if (roomEmail == null || roomEmail.trim().isEmpty()) r.addError("roomEmail", context.getString(R.string.err_room_email_empty));
        else if (!roomEmail.contains("@")) r.addError("roomEmail", context.getString(R.string.err_room_email_format));
        return r;
    }

    // ========== 加密 ==========
    private String encrypt(String plain) {
        try {
            Cipher c = Cipher.getInstance(TRANSFORM); c.init(Cipher.ENCRYPT_MODE, getKey());
            byte[] iv = c.getIV(), enc = c.doFinal(plain.getBytes("UTF-8"));
            byte[] combined = new byte[iv.length + enc.length];
            System.arraycopy(iv, 0, combined, 0, iv.length); System.arraycopy(enc, 0, combined, iv.length, enc.length);
            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) { return null; }
    }

    private String decrypt(String cipherText) {
        try {
            byte[] combined = Base64.decode(cipherText, Base64.NO_WRAP);
            byte[] iv = new byte[IV_LEN], enc = new byte[combined.length - IV_LEN];
            System.arraycopy(combined, 0, iv, 0, IV_LEN); System.arraycopy(combined, IV_LEN, enc, 0, enc.length);
            Cipher c = Cipher.getInstance(TRANSFORM); c.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(TAG_LEN, iv));
            return new String(c.doFinal(enc), "UTF-8");
        } catch (Exception e) { return null; }
    }

    private SecretKey getKey() throws Exception {
        KeyStore ks = KeyStore.getInstance(KEYSTORE); ks.load(null);
        if (ks.containsAlias(AppConfig.KEYSTORE_ALIAS)) return ((KeyStore.SecretKeyEntry) ks.getEntry(AppConfig.KEYSTORE_ALIAS, null)).getSecretKey();
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        kg.init(new KeyGenParameterSpec.Builder(AppConfig.KEYSTORE_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
        return kg.generateKey();
    }

    // ========== 配置读写 ==========
    public synchronized LoadResult loadConfig() {
        if (cachedConfig != null) return LoadResult.ok(cachedConfig);
        File file = new File(context.getFilesDir(), AppConfig.CONFIG_FILE_NAME);
        if (!file.exists()) return LoadResult.err(ErrorType.FILE_NOT_FOUND, "配置文件不存在");
        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder(); char[] buf = new char[1024]; int len;
            while ((len = reader.read(buf)) != -1) sb.append(buf, 0, len); reader.close();
            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            String platformStr = optStr(json, AppConfig.KEY_PLATFORM);
            if (platformStr == null) return LoadResult.err(ErrorType.FIELD_MISSING, "配置文件缺少平台字段");
            AppConfig config = new AppConfig(Platform.valueOf(platformStr));
            if (config.getPlatform() == Platform.FEISHU) {
                String appId = optStr(json, AppConfig.KEY_APP_ID), encSecret = optStr(json, AppConfig.KEY_APP_SECRET), roomId = optStr(json, AppConfig.KEY_ROOM_ID);
                if (appId == null || encSecret == null || roomId == null) return LoadResult.err(ErrorType.FIELD_MISSING, "配置文件缺少必要字段");
                String appSecret = decrypt(encSecret);
                if (appSecret == null) return LoadResult.err(ErrorType.VALIDATION_FAILED, "APP_SECRET解密失败");
                if (!validateFeishu(appId, appSecret, roomId).isValid()) return LoadResult.err(ErrorType.VALIDATION_FAILED, "配置格式校验失败");
                config.setAppId(appId); config.setAppSecret(appSecret); config.setRoomId(roomId);
            } else {
                String clientId = optStr(json, AppConfig.KEY_CLIENT_ID), encSecret = optStr(json, AppConfig.KEY_CLIENT_SECRET), tenantId = optStr(json, AppConfig.KEY_TENANT_ID), roomEmail = optStr(json, AppConfig.KEY_ROOM_EMAIL);
                if (clientId == null || encSecret == null || tenantId == null || roomEmail == null) return LoadResult.err(ErrorType.FIELD_MISSING, "配置文件缺少必要字段");
                String clientSecret = decrypt(encSecret);
                if (clientSecret == null) return LoadResult.err(ErrorType.VALIDATION_FAILED, "CLIENT_SECRET解密失败");
                if (!validateOutlook(clientId, clientSecret, tenantId, roomEmail).isValid()) return LoadResult.err(ErrorType.VALIDATION_FAILED, "配置格式校验失败");
                config.setClientId(clientId); config.setClientSecret(clientSecret); config.setTenantId(tenantId); config.setRoomEmail(roomEmail);
            }
            config.setRoomName(optStr(json, AppConfig.KEY_ROOM_NAME));
            cachedConfig = config;
            return LoadResult.ok(cachedConfig);
        } catch (JsonSyntaxException e) { return LoadResult.err(ErrorType.JSON_PARSE_ERROR, "配置文件JSON格式错误");
        } catch (Exception e) { Log.e(TAG, "Load failed", e); return LoadResult.err(ErrorType.JSON_PARSE_ERROR, "读取配置文件失败"); }
    }

    private String optStr(JsonObject json, String key) { return json.has(key) ? json.get(key).getAsString() : null; }

    public synchronized SaveResult saveFeishuConfig(String appId, String appSecret, String roomId) {
        ValidationResult vr = validateFeishu(appId, appSecret, roomId);
        if (!vr.isValid()) return SaveResult.validationFailed(vr);
        String encSecret = encrypt(appSecret);
        if (encSecret == null) return SaveResult.err("APP_SECRET加密失败");
        JsonObject json = new JsonObject();
        json.addProperty(AppConfig.KEY_PLATFORM, Platform.FEISHU.name());
        json.addProperty(AppConfig.KEY_APP_ID, appId); json.addProperty(AppConfig.KEY_APP_SECRET, encSecret); json.addProperty(AppConfig.KEY_ROOM_ID, roomId);
        String rn = cachedConfig != null ? cachedConfig.getRoomName() : null; if (rn != null) json.addProperty(AppConfig.KEY_ROOM_NAME, rn);
        return saveConfig(json, () -> { cachedConfig = new AppConfig(Platform.FEISHU); cachedConfig.setAppId(appId); cachedConfig.setAppSecret(appSecret); cachedConfig.setRoomId(roomId); if (rn != null) cachedConfig.setRoomName(rn); });
    }

    public synchronized SaveResult saveOutlookConfig(String clientId, String clientSecret, String tenantId, String roomEmail) {
        ValidationResult vr = validateOutlook(clientId, clientSecret, tenantId, roomEmail);
        if (!vr.isValid()) return SaveResult.validationFailed(vr);
        String encSecret = encrypt(clientSecret);
        if (encSecret == null) return SaveResult.err("CLIENT_SECRET加密失败");
        JsonObject json = new JsonObject();
        json.addProperty(AppConfig.KEY_PLATFORM, Platform.OUTLOOK.name());
        json.addProperty(AppConfig.KEY_CLIENT_ID, clientId); json.addProperty(AppConfig.KEY_CLIENT_SECRET, encSecret); json.addProperty(AppConfig.KEY_TENANT_ID, tenantId); json.addProperty(AppConfig.KEY_ROOM_EMAIL, roomEmail);
        String rn = cachedConfig != null ? cachedConfig.getRoomName() : null; if (rn != null) json.addProperty(AppConfig.KEY_ROOM_NAME, rn);
        return saveConfig(json, () -> { cachedConfig = new AppConfig(Platform.OUTLOOK); cachedConfig.setClientId(clientId); cachedConfig.setClientSecret(clientSecret); cachedConfig.setTenantId(tenantId); cachedConfig.setRoomEmail(roomEmail); if (rn != null) cachedConfig.setRoomName(rn); });
    }

    private SaveResult saveConfig(JsonObject json, Runnable cacheUpdater) {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(new File(context.getFilesDir(), AppConfig.CONFIG_FILE_NAME)), StandardCharsets.UTF_8)) { writer.write(json.toString()); writer.flush(); }
        catch (Exception e) { Log.e(TAG, "Save failed", e); return SaveResult.err("保存配置文件失败"); }
        cacheUpdater.run();
        return SaveResult.ok();
    }

    // ========== 访问器 ==========
    public Platform getPlatform() { return cachedConfig != null ? cachedConfig.getPlatform() : null; }
    public String getAppId() { return cachedConfig != null ? cachedConfig.getAppId() : null; }
    public String getAppSecret() { return cachedConfig != null ? cachedConfig.getAppSecret() : null; }
    public String getRoomId() { return cachedConfig != null ? cachedConfig.getRoomId() : null; }
    public String getClientId() { return cachedConfig != null ? cachedConfig.getClientId() : null; }
    public String getClientSecret() { return cachedConfig != null ? cachedConfig.getClientSecret() : null; }
    public String getTenantId() { return cachedConfig != null ? cachedConfig.getTenantId() : null; }
    public String getRoomEmail() { return cachedConfig != null ? cachedConfig.getRoomEmail() : null; }
    public String getRoomName() { return cachedConfig != null ? cachedConfig.getRoomName() : null; }
    public void setRoomName(String name) { if (cachedConfig != null) cachedConfig.setRoomName(name); }
    public boolean isConfigReady() { return cachedConfig != null; }
    public synchronized void clearConfig() { cachedConfig = null; File file = new File(context.getFilesDir(), AppConfig.CONFIG_FILE_NAME); if (file.exists()) file.delete(); prefs.edit().remove(AppConfig.PLATFORM_KEY).apply(); }
}
