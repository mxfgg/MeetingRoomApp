package com.example.meetingroomapp;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.meetingroomapp.config.ConfigManager;
import com.example.meetingroomapp.data.model.Platform;

/**
 * 平台选择界面 — 已有有效配置时直接进入主界面
 */
public class PlatformSelectActivity extends AppCompatActivity {
    private ConfigManager configManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupFullScreen();
        setContentView(R.layout.activity_platform_select);
        configManager = new ConfigManager(this);
        if (configManager.loadConfig().isSuccess()) { startActivity(new Intent(this, MainActivity.class)); finish(); return; }
        Button btnFeishu = findViewById(R.id.btn_select_feishu), btnOutlook = findViewById(R.id.btn_select_outlook);
        btnFeishu.setOnClickListener(v -> selectPlatform(Platform.FEISHU));
        btnOutlook.setOnClickListener(v -> selectPlatform(Platform.OUTLOOK));
    }

    private void selectPlatform(Platform platform) {
        configManager.savePlatform(platform);
        startActivity(new Intent(this, ConfigActivity.class).putExtra("platform", platform.name()));
        finish();
    }

    private void setupFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
}
