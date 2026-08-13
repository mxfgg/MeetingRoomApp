# 会议室电子门牌系统

一款运行于Android平板设备的会议室信息展示应用，支持**飞书**和**Microsoft Outlook(Azure AD)**双平台。安装于会议室门口，以横屏全屏模式持续展示当前会议室的预约状态和会议信息。

## 核心特性

- **双平台支持**：启动时选择飞书或Outlook平台，配置后自动记忆
- 深色科技感UI设计，适配电子门牌场景
- 当前会议放大高亮展示，下一场会议醒目提示
- 一键快速预约功能（空闲状态下可用）
- 防烧屏保护机制（偏移/屏保/夜间暗屏）
- 凭证加密存储（Android Keystore + AES-256-GCM）
- 开机自启动，无人值守运行

## 运行环境

| 项目 | 要求 |
|------|------|
| 最低SDK | Android 8.0 (API 26) |
| 目标SDK | Android 15 (API 35) |
| Java版本 | Java 8+ |
| 屏幕方向 | 横屏 (sensorLandscape) |
| 网络 | 需要互联网访问对应平台API |

## 凭证获取方式

### 飞书平台

1. 访问[飞书开放平台](https://open.feishu.cn/)创建企业自建应用
2. 获取 **APP_ID**（`cli_` 开头）和 **APP_SECRET**
3. 在飞书管理后台获取会议室的 **ROOM_ID**（`omm_` 开头）
4. 为应用授予以下权限：会议室忙闲读取、日历读写、参会者管理

### Outlook(Azure AD)平台

1. 访问[Azure Portal](https://portal.azure.com/)注册应用
2. 获取 **CLIENT_ID**（UUID格式）和 **CLIENT_SECRET**
3. 获取 **TENANT_ID**（UUID格式）
4. 获取会议室邮箱 **ROOM_EMAIL**
5. 为应用授予 `Calendars.Read` 和 `Calendars.ReadWrite` 权限（Microsoft Graph API）

## 使用方法

1. 在Android Studio中打开项目
2. 编译并安装到Android平板设备
3. 首次启动选择平台（飞书/Outlook）
4. 输入对应平台的凭据信息
5. 配置成功后自动进入主界面，后续启动直接进入

## 技术栈

- OkHttp 4.12.0（网络请求）
- Gson 2.11.0（JSON解析）
- Material Design 3（UI组件）
- RecyclerView（列表展示）
- Android Keystore（凭证加密）

## 项目结构

```
app/src/main/java/com/example/meetingroomapp/
├── PlatformSelectActivity.java       # 平台选择界面
├── MainActivity.java                 # 主界面
├── ConfigActivity.java               # 配置填写界面
├── api/
│   ├── BaseApiClient.java            # HTTP基类+令牌缓存
│   ├── MeetingApi.java               # 统一接口
│   ├── feishu/FeishuMeetingApi.java  # 飞书API实现
│   └── outlook/OutlookMeetingApi.java# Outlook API实现
├── config/ConfigManager.java         # 配置管理+校验+加密
├── data/
│   ├── MeetingRepository.java        # 数据层+轮询调度
│   └── model/                        # 数据模型
├── burnin/BurnInManager.java         # 防烧屏保护
├── ui/                               # UI组件
└── receiver/BootCompletedReceiver.java # 开机自启动
```

## 开源协议

MIT License - 详见 [LICENSE](LICENSE)
