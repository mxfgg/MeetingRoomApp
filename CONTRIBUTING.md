# 贡献指南

感谢你对 MeetingRoomApp 项目的关注！欢迎提交 Issue 和 Pull Request。

## 如何贡献

### 报告 Bug

1. 在 [Issues](../../issues) 页面搜索是否已有相同问题
2. 如果没有，点击 **New Issue**，选择 **Bug Report** 模板
3. 按模板填写：复现步骤、预期行为、实际行为、环境信息

### 提交功能建议

1. 在 [Issues](../../issues) 页面搜索是否已有类似建议
2. 如果没有，点击 **New Issue**，选择 **Feature Request** 模板
3. 描述使用场景、期望行为和可能的实现思路

### 提交代码

1. Fork 本仓库
2. 创建功能分支：`git checkout -b feature/your-feature-name`
3. 编写代码并添加必要的测试
4. 确保所有测试通过：`./gradlew test`
5. 提交变更：`git commit -m "feat: 简要描述"`
6. 推送分支：`git push origin feature/your-feature-name`
7. 创建 Pull Request，按模板填写说明

### 提交规范

使用 [Conventional Commits](https://www.conventionalcommits.org/) 格式：

- `feat:` 新功能
- `fix:` 修复 Bug
- `docs:` 文档变更
- `style:` 代码格式（不影响功能）
- `refactor:` 重构
- `test:` 测试相关
- `chore:` 构建/工具变更

### 代码风格

- 遵循项目现有代码风格
- 每个方法应有清晰的 JavaDoc 注释
- 变量和方法命名应具有描述性

### 安全问题

如果发现安全漏洞，请**不要**在公开 Issue 中报告。请参阅 [SECURITY.md](SECURITY.md) 中的漏洞报告流程。

## 许可证

提交代码即表示你同意该代码以 MIT License 许可发布。
