package com.example.meetingroomapp.api;

import android.os.Handler;
import android.os.Looper;
import com.example.meetingroomapp.data.model.ApiCallback;
import com.example.meetingroomapp.data.model.AppConfig;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.*;

/**
 * HTTP客户端基类 — 封装OkHttp请求、重试机制、主线程回调和令牌缓存
 */
public abstract class BaseApiClient {
    protected static final int[] RETRY_DELAYS = {1000, 2000, 4000};
    private static final long REFRESH_AHEAD_MS = 5 * 60 * 1000L;
    protected final OkHttpClient httpClient = new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS).build();
    protected final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String cachedToken;
    private long expireTime;

    protected abstract String getBaseUrl();
    protected void executeWithRetry(Request request, ApiCallback<String> callback) { retryInternal(request, callback, 0); }

    private void retryInternal(Request original, ApiCallback<String> callback, int retry) {
        httpClient.newCall(original.newBuilder().build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { if (retry < AppConfig.MAX_RETRY_COUNT) mainHandler.postDelayed(() -> retryInternal(original, callback, retry + 1), RETRY_DELAYS[Math.min(retry, RETRY_DELAYS.length - 1)]); else mainHandler.post(() -> callback.onFailure("网络请求失败: " + e.getMessage())); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) mainHandler.post(() -> callback.onSuccess(body));
                else if (retry < AppConfig.MAX_RETRY_COUNT && response.code() >= 500) mainHandler.postDelayed(() -> retryInternal(original, callback, retry + 1), RETRY_DELAYS[Math.min(retry, RETRY_DELAYS.length - 1)]);
                else mainHandler.post(() -> callback.onFailure("API错误(" + response.code() + "): " + body));
            }
        });
    }

    // ========== 令牌缓存 ==========
    /** 认证令牌结果 */
    public static class TokenResult {
        public final String token;
        public final int expireSeconds;
        private final boolean success;
        private final String errorMessage;
        private TokenResult(String token, int expireSeconds, boolean success, String errorMessage) { this.token = token; this.expireSeconds = expireSeconds; this.success = success; this.errorMessage = errorMessage; }
        public static TokenResult ok(String token, int expireSeconds) { return new TokenResult(token, expireSeconds, true, null); }
        public static TokenResult err(String msg) { return new TokenResult(null, 0, false, msg); }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    public synchronized void invalidateToken() { cachedToken = null; expireTime = 0; }

    /** 获取令牌 — 有缓存则直接返回，否则调用authFn获取 */
    public void getToken(Consumer<ApiCallback<TokenResult>> authFn, ApiCallback<String> callback) {
        if (cachedToken != null && System.currentTimeMillis() < expireTime - REFRESH_AHEAD_MS) { callback.onSuccess(cachedToken); return; }
        authFn.accept(new ApiCallback<TokenResult>() {
            @Override public void onSuccess(TokenResult r) {
                if (r.isSuccess()) { synchronized (BaseApiClient.this) { cachedToken = r.token; expireTime = System.currentTimeMillis() + (long) r.expireSeconds * 1000; } callback.onSuccess(r.token); }
                else callback.onFailure("认证失败: " + r.getErrorMessage());
            }
            @Override public void onFailure(String msg) { callback.onFailure(msg); }
        });
    }
}
