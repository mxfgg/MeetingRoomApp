package com.example.meetingroomapp;

import com.example.meetingroomapp.config.ConfigManager;
import org.junit.Test;
import static org.junit.Assert.*;

public class ConfigManagerValidationTest {

    @Test
    public void validationResult_initiallyValid() {
        ConfigManager.ValidationResult vr = new ConfigManager.ValidationResult();
        assertTrue(vr.isValid());
    }

    @Test
    public void validationResult_withError_notValid() {
        ConfigManager.ValidationResult vr = new ConfigManager.ValidationResult();
        vr.addError("appId", "APP_ID不能为空");
        assertFalse(vr.isValid());
    }

    @Test
    public void validationResult_appIdError() {
        ConfigManager.ValidationResult vr = new ConfigManager.ValidationResult();
        vr.addError("appId", "APP_ID需以cli_开头");
        assertEquals("APP_ID需以cli_开头", vr.getAppIdError());
        assertNull(vr.getAppSecretError());
        assertNull(vr.getRoomIdError());
    }

    @Test
    public void validationResult_multipleErrors() {
        ConfigManager.ValidationResult vr = new ConfigManager.ValidationResult();
        vr.addError("appId", "APP_ID不能为空");
        vr.addError("appSecret", "APP_SECRET不能为空");
        assertFalse(vr.isValid());
        assertNotNull(vr.getAppIdError());
        assertNotNull(vr.getAppSecretError());
    }

    @Test
    public void validationResult_outlookFields() {
        ConfigManager.ValidationResult vr = new ConfigManager.ValidationResult();
        vr.addError("clientId", "格式错误");
        vr.addError("clientSecret", "不能为空");
        vr.addError("tenantId", "格式错误");
        vr.addError("roomEmail", "格式错误");
        assertFalse(vr.isValid());
        assertEquals("格式错误", vr.getClientIdError());
        assertEquals("不能为空", vr.getClientSecretError());
        assertEquals("格式错误", vr.getTenantIdError());
        assertEquals("格式错误", vr.getRoomEmailError());
    }

    @Test
    public void validationResult_noError_returnsNull() {
        ConfigManager.ValidationResult vr = new ConfigManager.ValidationResult();
        assertNull(vr.getAppIdError());
        assertNull(vr.getAppSecretError());
        assertNull(vr.getRoomIdError());
        assertNull(vr.getClientIdError());
        assertNull(vr.getClientSecretError());
        assertNull(vr.getTenantIdError());
        assertNull(vr.getRoomEmailError());
    }
}
