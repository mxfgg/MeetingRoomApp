package com.example.meetingroomapp.data.model;

/**
 * 统一API回调接口，飞书和Outlook的HTTP客户端共用此接口定义。
 */
public interface ApiCallback<T> {
    void onSuccess(T result);
    void onFailure(String errorMessage);
}
