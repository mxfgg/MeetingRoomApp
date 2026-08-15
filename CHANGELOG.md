# Changelog

本项目遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/) 版本规范。

## [1.0.1] - 2026-08-15

### 安全修复

- 移除生产环境中的敏感 API 响应日志输出（`FeishuMeetingApi`）
- 配置备份规则排除 `config.json` 和 `SharedPreferences`，防止凭证通过备份泄露
- 禁用 `android:allowBackup`，避免自动备份包含加密凭证
- Release 构建启用 R8/ProGuard 代码混淆（`minifyEnabled true`）
- Outlook OAuth2 请求中 `client_secret` 使用 URL 编码，避免特殊字符问题

### 新增

- 添加 `CODE_OF_CONDUCT.md`（Contributor Covenant v2.0）
- 添加 `CONTRIBUTING.md` 贡献指南
- 添加 `SECURITY.md` 安全漏洞报告策略
- 添加 GitHub Issue 模板（Bug Report / Feature Request）
- 添加 GitHub Pull Request 模板
- README 中添加 AI 辅助生成声明
- 添加业务逻辑单元测试（`MeetingInfoTest`、`SimpleResultTest`、`ConfigManagerValidationTest`）

### 修复

- `SimpleResult` 状态字段显式初始化，消除对默认值的隐式依赖

## [1.0.0] - 2026-08-12

### 新增

- 会议室电子门牌系统 v1.0 初始版本
- 飞书和 Outlook(Azure AD) 双平台支持
- 深色科技感 UI 设计
- 当前会议放大高亮展示
- 一键快速预约功能
- 防烧屏保护机制
- 凭证加密存储（Android Keystore + AES-256-GCM）
- 开机自启动
