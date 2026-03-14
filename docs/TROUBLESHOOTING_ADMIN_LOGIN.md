# 管理员登录问题排查

## 关于“Microsoft 错误”弹窗（Correlation Id / Tag 7q6ca / Code 2147943726）

若登录时出现 **Microsoft 品牌** 的错误弹窗（标题为“错误”，内容含 Correlation Id、DPTI、Tag、Code 2147943726 等），该错误**并非本项目后端返回**，而是来自：

- **Cursor 内置浏览器 / 嵌入式预览** 或 Windows/Edge 的身份验证组件
- 系统或浏览器扩展触发的 Microsoft 登录/SSO 流程失败

### 建议操作

1. **改用系统浏览器访问**  
   用 Chrome、Firefox 或 Edge 直接打开前端地址（如 `http://localhost:5173`），在浏览器中登录管理员账号，看是否仍出现该弹窗。若仅在 Cursor 预览中出现，可视为环境问题。

2. **确认后端已启动且可访问**  
   - 后端默认端口：`9090`  
   - 在浏览器中打开：`http://localhost:9090/api/ping`，应返回正常结果。

3. **确认管理员账号存在**  
   - 本系统使用 **JWT + 用户名/密码** 登录，无 Microsoft 登录。  
   - 若数据库为新建或未插入过管理员，会报“用户名或密码错误”。  
   - 解决：在 `application.yml` 中设置 `admin.auto-create: true`，重启后端即可自动创建默认管理员（用户名/密码见 `admin.username`、`admin.password`）；或在数据库中手动插入 `role=ADMIN` 的用户。

4. **查看真实接口错误**  
   打开浏览器开发者工具 → 网络（Network），再次尝试登录，查看 `/api/auth/login` 的响应状态码和响应体。若为本系统错误，会返回 `code !== 200` 及 `message`（如“用户名或密码错误”“账号已被禁用”），前端会以 Element Plus 消息框展示，而不会出现 Microsoft 弹窗。

## 常见后端错误与处理

| 现象 | 可能原因 | 处理 |
|------|----------|------|
| 用户名或密码错误 | 用户不存在或密码错误；或未创建管理员 | 检查账号/密码；开启 `admin.auto-create: true` 或手动插入管理员 |
| 账号已被禁用 | 对应用户 `status = 0` | 在数据库中将该用户 `status` 改为 1 |
| 网络错误 / 请求超时 | 后端未启动或端口/代理配置错误 | 确认后端运行在 9090，前端代理指向 `/api` → 后端地址 |

## 开发环境快速恢复管理员

在 `backend/src/main/resources/application.yml` 中：

```yaml
admin:
  username: admin
  password: 123456
  real-name: 系统管理员
  auto-create: true   # 改为 true，重启后端即可自动创建
```

首次启动后若控制台出现“管理员账号已自动创建”，即可使用上述用户名和密码登录。
