package com.example.meetingroomapp.data.model;

/**
 * 简单操作结果 — 封装成功/失败状态和错误消息
 */
public class SimpleResult {
    private boolean success;
    private String errorMessage;

    private SimpleResult() {}
    public static SimpleResult success() { SimpleResult r = new SimpleResult(); r.success = true; return r; }
    public static SimpleResult error(String msg) { SimpleResult r = new SimpleResult(); r.errorMessage = msg; return r; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
}
