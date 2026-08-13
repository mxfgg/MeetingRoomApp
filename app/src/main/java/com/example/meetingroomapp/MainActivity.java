package com.example.meetingroomapp;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.meetingroomapp.api.MeetingApi;
import com.example.meetingroomapp.api.feishu.FeishuMeetingApi;
import com.example.meetingroomapp.api.outlook.OutlookMeetingApi;
import com.example.meetingroomapp.burnin.BurnInManager;
import com.example.meetingroomapp.config.ConfigManager;
import com.example.meetingroomapp.data.MeetingRepository;
import com.example.meetingroomapp.data.model.*;
import com.example.meetingroomapp.ui.DataChangeListener;
import com.example.meetingroomapp.ui.MeetingListAdapter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 主界面 — 横屏全屏电子门牌，每10秒轮询刷新会议数据
 */
public class MainActivity extends AppCompatActivity implements DataChangeListener, BurnInManager.BurnInListener {
    private ConfigManager configManager;
    private MeetingRepository repository;
    private BurnInManager burnInManager;
    private MeetingListAdapter adapter;
    private Handler clockHandler;
    private List<MeetingInfo> cachedMeetings = new ArrayList<>();
    private boolean isNetworkError = false;
    private final Random random = new Random();
    private View rootView;
    private TextView tvCurrentTime, tvStatusBanner, tvNextMeetingHint, tvNextMeetingApplicant, tvFreeStatus;
    private FrameLayout currentMeetingContainer;
    private View nextMeetingContainer;
    private RecyclerView rvMeetings;
    private ImageView refreshIndicator;
    private com.google.android.material.button.MaterialButton btnQuickBook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configManager = new ConfigManager(this);
        if (!configManager.isConfigReady() && !configManager.loadConfig().isSuccess()) { startActivity(new Intent(this, PlatformSelectActivity.class)); finish(); return; }
        setupFullScreen();
        setContentView(R.layout.activity_main);
        initViews();
        initRepository();
        burnInManager = new BurnInManager(); burnInManager.setListener(this); burnInManager.start();
        startClock();
    }

    private void setupFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private void initViews() {
        rootView = findViewById(R.id.root_layout);
        tvCurrentTime = findViewById(R.id.tv_current_time); tvStatusBanner = findViewById(R.id.tv_status_banner);
        currentMeetingContainer = findViewById(R.id.current_meeting_container); nextMeetingContainer = findViewById(R.id.next_meeting_container);
        tvNextMeetingHint = findViewById(R.id.tv_next_meeting_hint); tvNextMeetingApplicant = findViewById(R.id.tv_next_meeting_applicant);
        rvMeetings = findViewById(R.id.rv_meetings); tvFreeStatus = findViewById(R.id.tv_free_status);
        btnQuickBook = findViewById(R.id.btn_quick_book); refreshIndicator = findViewById(R.id.refresh_indicator);
        adapter = new MeetingListAdapter();
        rvMeetings.setLayoutManager(new LinearLayoutManager(this)); rvMeetings.setAdapter(adapter);
        btnQuickBook.setOnClickListener(v -> {
            if (isNetworkError) { Toast.makeText(this, R.string.toast_network_error, Toast.LENGTH_LONG).show(); return; }
            btnQuickBook.setEnabled(false); repository.performQuickBook();
        });
    }

    /** 根据平台类型创建对应的API实现 */
    private void initRepository() {
        MeetingApi api = configManager.getPlatform() == Platform.FEISHU ? new FeishuMeetingApi(configManager) : new OutlookMeetingApi(configManager);
        repository = new MeetingRepository(api); repository.setListener(this); repository.refreshData(); repository.startPolling();
    }

    private void startClock() {
        clockHandler = new Handler(Looper.getMainLooper());
        clockHandler.postDelayed(new Runnable() {
            @Override public void run() { tvCurrentTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date())); clockHandler.postDelayed(this, 1000); }
        }, 0);
    }

    @Override protected void onDestroy() { super.onDestroy(); if (repository != null) repository.stopPolling(); if (burnInManager != null) burnInManager.stop(); if (clockHandler != null) clockHandler.removeCallbacksAndMessages(null); }
    @Override public boolean dispatchTouchEvent(MotionEvent ev) { if (burnInManager != null) burnInManager.onUserInteraction(); return super.dispatchTouchEvent(ev); }

    @Override
    public void onDataUpdated(List<MeetingInfo> meetings, MeetingInfo currentMeeting, boolean isFree) {
        runOnUiThread(() -> {
            cachedMeetings = meetings;
            updateCurrentMeetingDisplay(currentMeeting);
            adapter.setMeetings(meetings, currentMeeting);
            animateContentChange();
            updateStatusDisplay(isFree, currentMeeting);
            if (burnInManager != null) burnInManager.onMeetingStatusChanged(isFree);
        });
    }

    private void updateCurrentMeetingDisplay(MeetingInfo currentMeeting) {
        if (currentMeeting != null) {
            currentMeetingContainer.setVisibility(View.VISIBLE);
            View card = currentMeetingContainer.findViewById(R.id.current_meeting_card);
            if (card != null) { setText(card, R.id.tv_current_summary, currentMeeting.getSummary()); setText(card, R.id.tv_current_time, currentMeeting.getDisplayTime()); setText(card, R.id.tv_current_applicant, safeName(currentMeeting.getOwnerName())); }
            updateNextMeetingHint(currentMeeting);
        } else { currentMeetingContainer.setVisibility(View.GONE); nextMeetingContainer.setVisibility(View.GONE); }
    }

    private void updateNextMeetingHint(MeetingInfo currentMeeting) {
        MeetingInfo next = null; long now = System.currentTimeMillis();
        for (MeetingInfo m : cachedMeetings) if (m != currentMeeting && m.getStartTimestamp() > now && (next == null || m.getStartTimestamp() < next.getStartTimestamp())) next = m;
        if (next != null) { nextMeetingContainer.setVisibility(View.VISIBLE); tvNextMeetingHint.setText(getString(R.string.next_meeting_prefix) + next.getDisplayTime() + " · " + next.getSummary()); tvNextMeetingApplicant.setText(safeName(next.getOwnerName())); }
        else nextMeetingContainer.setVisibility(View.GONE);
    }

    private void updateStatusDisplay(boolean isFree, MeetingInfo currentMeeting) {
        isNetworkError = false;
        if (currentMeeting != null) { setBanner(R.string.status_meeting, R.color.status_error, 0x1AF44336); tvFreeStatus.setVisibility(View.GONE); btnQuickBook.setVisibility(View.GONE); setIndicatorColor(R.color.status_error); }
        else if (isFree) { setBanner(R.string.status_free, R.color.status_free, 0x1A00c853); tvFreeStatus.setVisibility(View.VISIBLE); tvFreeStatus.setTextColor(getColor(R.color.status_free_dim)); tvFreeStatus.setText(R.string.current_free); btnQuickBook.setVisibility(View.VISIBLE); btnQuickBook.setEnabled(true); setIndicatorColor(R.color.status_free); }
        else { setBanner(R.string.status_booked, R.color.status_warning, 0x1Aff9800); tvFreeStatus.setVisibility(View.GONE); btnQuickBook.setVisibility(View.GONE); setIndicatorColor(R.color.status_free); }
    }

    @Override
    public void onNetworkError() {
        runOnUiThread(() -> {
            isNetworkError = true;
            setBanner(R.string.status_network_error, R.color.status_warning, 0x1Aff9800);
            setIndicatorColor(R.color.status_warning);
            if (tvFreeStatus.getVisibility() == View.VISIBLE) tvFreeStatus.setTextColor(getColor(R.color.status_warning));
            if (cachedMeetings.isEmpty() && repository != null && repository.getCachedMeetings().isEmpty()) { tvFreeStatus.setVisibility(View.VISIBLE); tvFreeStatus.setText(R.string.status_no_data); tvFreeStatus.setTextColor(getColor(R.color.status_warning)); }
        });
    }

    @Override
    public void onAuthError(String message) {
        runOnUiThread(() -> { setBanner(R.string.status_auth_error, R.color.status_error, 0x1AF44336); tvFreeStatus.setVisibility(View.VISIBLE); tvFreeStatus.setText(R.string.check_config); tvFreeStatus.setTextColor(getColor(R.color.status_error)); });
    }

    @Override
    public void onQuickBookResult(SimpleResult result) {
        runOnUiThread(() -> {
            if (result.isSuccess()) Toast.makeText(this, R.string.toast_book_success, Toast.LENGTH_LONG).show();
            else { Toast.makeText(this, result.getErrorMessage(), Toast.LENGTH_LONG).show(); btnQuickBook.setEnabled(true); }
        });
    }

    @Override public void onApplyOffset(int x, int y) { runOnUiThread(() -> { if (rootView != null) { rootView.setTranslationX(x); rootView.setTranslationY(y); } }); }
    @Override public void onSwitchStatusBarPosition(boolean toTop) { }
    @Override public void onEnterScreenSaver() { setBrightness(0.2f); }
    @Override public void onExitScreenSaver() { setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE); }
    @Override public void onNightDimEnable(float b) { setBrightness(b); }
    @Override public void onNightDimDisable() { setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE); }

    private void animateContentChange() {
        if (refreshIndicator == null) return;
        ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) refreshIndicator.getLayoutParams();
        int margin = (int) getResources().getDimension(R.dimen.spacing_medium);
        if (random.nextBoolean()) { p.startToStart = R.id.root_layout; p.endToEnd = -1; p.setMarginStart(margin); p.setMarginEnd(0); }
        else { p.startToStart = -1; p.endToEnd = R.id.root_layout; p.setMarginStart(0); p.setMarginEnd(margin); }
        refreshIndicator.setLayoutParams(p); refreshIndicator.setAlpha(0f); refreshIndicator.animate().alpha(1f).setDuration(800).start();
    }

    private void setBanner(int textRes, int textColor, int bgColor) { tvStatusBanner.setText(textRes); tvStatusBanner.setTextColor(getColor(textColor)); tvStatusBanner.setBackgroundColor(bgColor); }
    private void setIndicatorColor(int colorRes) { if (refreshIndicator != null) refreshIndicator.setColorFilter(getColor(colorRes)); }
    private void setBrightness(float value) { runOnUiThread(() -> { WindowManager.LayoutParams p = getWindow().getAttributes(); p.screenBrightness = value; getWindow().setAttributes(p); }); }
    private void setText(View parent, int id, String text) { TextView tv = parent.findViewById(id); if (tv != null) tv.setText(text); }
    private String safeName(String name) { return name != null ? name : getString(R.string.anonymous); }
}
