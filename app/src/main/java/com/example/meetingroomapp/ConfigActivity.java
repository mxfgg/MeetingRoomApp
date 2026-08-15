package com.example.meetingroomapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.meetingroomapp.api.MeetingApi;
import com.example.meetingroomapp.api.feishu.FeishuMeetingApi;
import com.example.meetingroomapp.api.outlook.OutlookMeetingApi;
import com.example.meetingroomapp.config.ConfigManager;
import com.example.meetingroomapp.config.ConfigManager.SaveResult;
import com.example.meetingroomapp.config.ConfigManager.ValidationResult;
import com.example.meetingroomapp.data.model.ApiCallback;
import com.example.meetingroomapp.data.model.AppConfig;
import com.example.meetingroomapp.data.model.MeetingInfo;
import com.example.meetingroomapp.data.model.Platform;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.List;

/**
 * 配置填写界面 — 根据平台显示对应凭据字段，验证通过后进入主界面
 */
public class ConfigActivity extends AppCompatActivity {
    private ConfigManager configManager;
    private Platform platform;
    private MaterialButton btnSave;
    private TextView tvConfigError;
    private TextInputLayout tilAppId, tilAppSecret, tilRoomId, tilClientId, tilClientSecret, tilTenantId, tilRoomEmail;
    private TextInputEditText etAppId, etAppSecret, etRoomId, etClientId, etClientSecret, etTenantId, etRoomEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        configManager = new ConfigManager(this);
        String platformStr = getIntent().getStringExtra("platform");
        if (platformStr == null) { startActivity(new Intent(this, PlatformSelectActivity.class)); finish(); return; }
        try { platform = Platform.valueOf(platformStr); } catch (IllegalArgumentException e) { startActivity(new Intent(this, PlatformSelectActivity.class)); finish(); return; }
        initViews();
        setupPlatformFields();
    }

    private void initViews() {
        tilAppId = findViewById(R.id.til_app_id); tilAppSecret = findViewById(R.id.til_app_secret); tilRoomId = findViewById(R.id.til_room_id);
        tilClientId = findViewById(R.id.til_client_id); tilClientSecret = findViewById(R.id.til_client_secret);
        tilTenantId = findViewById(R.id.til_tenant_id); tilRoomEmail = findViewById(R.id.til_room_email);
        etAppId = findViewById(R.id.et_app_id); etAppSecret = findViewById(R.id.et_app_secret); etRoomId = findViewById(R.id.et_room_id);
        etClientId = findViewById(R.id.et_client_id); etClientSecret = findViewById(R.id.et_client_secret);
        etTenantId = findViewById(R.id.et_tenant_id); etRoomEmail = findViewById(R.id.et_room_email);
        TextView tvHint = findViewById(R.id.tv_config_hint);
        btnSave = findViewById(R.id.btn_save_config);
        tvConfigError = findViewById(R.id.tv_config_error);
        if (platform == Platform.FEISHU) {
            tvHint.setText(R.string.config_hint_feishu);
            etAppId.setText(AppConfig.APP_ID_PREFIX); etRoomId.setText(AppConfig.ROOM_ID_PREFIX);
            etAppId.setSelection(etAppId.getText().length()); etRoomId.setSelection(etRoomId.getText().length());
        } else { tvHint.setText(R.string.config_hint_outlook); }
        InputFilter noNewLine = (s, st, e, d, ds, de) -> { for (int i = st; i < e; i++) if (s.charAt(i) == '\n' || s.charAt(i) == '\r') return ""; return null; };
        for (TextInputEditText et : new TextInputEditText[]{etAppId, etAppSecret, etRoomId, etClientId, etClientSecret, etTenantId, etRoomEmail}) et.setFilters(new InputFilter[]{noNewLine});
    }

    private void setupPlatformFields() {
        boolean isFeishu = platform == Platform.FEISHU;
        tilAppId.setVisibility(isFeishu ? android.view.View.VISIBLE : android.view.View.GONE);
        tilAppSecret.setVisibility(isFeishu ? android.view.View.VISIBLE : android.view.View.GONE);
        tilRoomId.setVisibility(isFeishu ? android.view.View.VISIBLE : android.view.View.GONE);
        tilClientId.setVisibility(isFeishu ? android.view.View.GONE : android.view.View.VISIBLE);
        tilClientSecret.setVisibility(isFeishu ? android.view.View.GONE : android.view.View.VISIBLE);
        tilTenantId.setVisibility(isFeishu ? android.view.View.GONE : android.view.View.VISIBLE);
        tilRoomEmail.setVisibility(isFeishu ? android.view.View.GONE : android.view.View.VISIBLE);
        TextInputEditText[] ets = isFeishu ? new TextInputEditText[]{etAppId, etAppSecret, etRoomId} : new TextInputEditText[]{etClientId, etClientSecret, etTenantId, etRoomEmail};
        for (TextInputEditText et : ets) { TextInputLayout til = (TextInputLayout) et.getParent().getParent(); et.addTextChangedListener(clearErrorWatcher(til)); }
        btnSave.setOnClickListener(v -> onSubmit());
    }

    private TextWatcher clearErrorWatcher(TextInputLayout til) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { til.setError(null); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        };
    }

    private void onSubmit() {
        SaveResult r = platform == Platform.FEISHU
                ? configManager.saveFeishuConfig(text(etAppId), text(etAppSecret), text(etRoomId))
                : configManager.saveOutlookConfig(text(etClientId), text(etClientSecret), text(etTenantId), text(etRoomEmail));
        if (!r.isSuccess()) { showErrors(r); return; }
        btnSave.setEnabled(false);
        tvConfigError.setText(R.string.config_validating);
        tvConfigError.setTextColor(getColor(R.color.text_secondary));
        tvConfigError.setVisibility(android.view.View.VISIBLE);
        MeetingApi api = platform == Platform.FEISHU ? new FeishuMeetingApi(configManager) : new OutlookMeetingApi(configManager);
        api.fetchMeetings(new ApiCallback<List<MeetingInfo>>() {
            @Override public void onSuccess(List<MeetingInfo> meetings) { runOnUiThread(() -> { startActivity(new Intent(ConfigActivity.this, MainActivity.class)); finish(); }); }
            @Override public void onFailure(String msg) { runOnUiThread(() -> { btnSave.setEnabled(true); tvConfigError.setTextColor(getColor(R.color.status_error)); tvConfigError.setText(formatError(msg)); tvConfigError.setVisibility(android.view.View.VISIBLE); }); }
        });
    }

    private void showErrors(SaveResult r) {
        if (r.getValidationResult() != null) {
            ValidationResult vr = r.getValidationResult();
            if (vr.getAppIdError() != null) tilAppId.setError(vr.getAppIdError());
            if (vr.getAppSecretError() != null) tilAppSecret.setError(vr.getAppSecretError());
            if (vr.getRoomIdError() != null) tilRoomId.setError(vr.getRoomIdError());
            if (vr.getClientIdError() != null) tilClientId.setError(vr.getClientIdError());
            if (vr.getClientSecretError() != null) tilClientSecret.setError(vr.getClientSecretError());
            if (vr.getTenantIdError() != null) tilTenantId.setError(vr.getTenantIdError());
            if (vr.getRoomEmailError() != null) tilRoomEmail.setError(vr.getRoomEmailError());
        } else { tvConfigError.setText(r.getErrorMessage()); tvConfigError.setVisibility(android.view.View.VISIBLE); }
    }

    private int formatError(String msg) {
        if (msg == null) return R.string.config_err_default;
        if (msg.contains("认证失败") || msg.contains("API错误(4") || msg.contains("令牌获取失败")) return R.string.config_err_invalid;
        if (msg.contains("网络") || msg.contains("timeout") || msg.contains("connect")) return R.string.config_err_network;
        if (msg.contains("会议室") || msg.contains("Room")) return platform == Platform.FEISHU ? R.string.config_err_room_feishu : R.string.config_err_room_outlook;
        return R.string.config_err_default;
    }

    private String text(TextInputEditText et) { android.text.Editable e = et.getText(); return e != null ? e.toString().trim() : ""; }
    @Override public void onBackPressed() { finishAffinity(); }
}
