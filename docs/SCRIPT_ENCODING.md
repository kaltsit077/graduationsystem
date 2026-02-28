# 批处理脚本 UTF-8 编码说明

## ✅ 已完成的 UTF-8 编码设置

所有批处理脚本（`.bat` 文件）已经配置为使用 UTF-8 编码：

### 1. 脚本文件列表

- ✅ `start-all.bat` - 一键启动脚本
- ✅ `stop-all.bat` - 停止服务脚本
- ✅ `backend/start.bat` - 后端启动脚本
- ✅ `backend/db-quick-ref.bat` - 数据库快速参考脚本

### 2. UTF-8 编码配置

所有脚本都包含以下 UTF-8 设置：

```batch
@echo off
:: 强制设置CMD编码为UTF-8
chcp 65001 >nul
:: 设置CMD字体为Consolas（支持UTF-8），避免乱码
reg add "HKCU\Console" /v "FaceName" /t REG_SZ /d "Consolas" /f >nul 2>&1
reg add "HKCU\Console" /v "CodePage" /t REG_DWORD /d 65001 /f >nul 2>&1
```

### 3. 功能说明

- **`chcp 65001`**: 将命令提示符代码页设置为 UTF-8
- **字体设置**: 将控制台字体设置为 Consolas（支持 UTF-8 字符显示）
- **代码页注册表**: 永久设置控制台代码页为 UTF-8

---

## 📝 如何确保文件以 UTF-8 编码保存

### 方法 1：使用 Visual Studio Code

1. 打开 `.bat` 文件
2. 点击右下角的编码显示（如 "GBK" 或 "UTF-8"）
3. 选择 "通过编码保存"
4. 选择 "UTF-8"
5. 保存文件

### 方法 2：使用 Notepad++

1. 打开 `.bat` 文件
2. 菜单：编码 → 转为 UTF-8 编码
3. 保存文件

### 方法 3：使用 PowerShell（批量转换）

```powershell
# 批量转换所有 .bat 文件为 UTF-8（无BOM）
Get-ChildItem -Path . -Filter *.bat -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding Default
    [System.IO.File]::WriteAllText($_.FullName, $content, [System.Text.UTF8Encoding]::new($false))
    Write-Host "已转换: $($_.FullName)"
}
```

### 方法 4：使用 CMD（批量转换）

```cmd
# 使用 PowerShell 批量转换
powershell -Command "Get-ChildItem -Path . -Filter *.bat -Recurse | ForEach-Object { $content = Get-Content $_.FullName -Raw -Encoding Default; [System.IO.File]::WriteAllText($_.FullName, $content, [System.Text.UTF8Encoding]::new($false)); Write-Host \"已转换: $($_.FullName)\" }"
```

---

## 🔍 验证 UTF-8 编码

### 检查文件编码

#### 使用 PowerShell：

```powershell
# 检查文件编码
Get-Content start-all.bat -Encoding Byte | Select-Object -First 3

# UTF-8 无BOM: 前3个字节应该是 40 65 63 (@echo)
# UTF-8 有BOM: 前3个字节应该是 EF BB BF
```

#### 使用 Notepad++：

1. 打开文件
2. 查看右下角编码显示
3. 应该显示 "UTF-8" 或 "UTF-8-BOM"

---

## ⚠️ 注意事项

### 1. BOM（字节顺序标记）

- **批处理文件建议使用 UTF-8 无BOM**
- 有BOM的UTF-8文件在某些情况下可能导致问题
- 脚本中的 `chcp 65001` 已经足够处理UTF-8字符

### 2. 临时脚本

- 临时生成的脚本（`%TEMP%\start-backend-*.bat`）也会自动设置为UTF-8
- 这些脚本在生成时会包含UTF-8编码设置

### 3. 控制台字体

- 脚本会自动设置控制台字体为 Consolas
- 如果您的系统没有 Consolas 字体，可以手动安装或修改为其他支持UTF-8的字体

---

## 🛠️ 故障排除

### 问题 1：中文显示乱码

**解决方法**：
1. 确保脚本文件以 UTF-8 编码保存
2. 运行脚本，它会自动设置 UTF-8 代码页
3. 如果仍有问题，手动运行：`chcp 65001`

### 问题 2：脚本执行错误

**解决方法**：
1. 检查文件编码是否为 UTF-8
2. 确保文件没有BOM（批处理文件不支持BOM）
3. 检查文件行尾符（应该是 CRLF，Windows格式）

### 问题 3：临时脚本乱码

**解决方法**：
- 临时脚本会自动设置UTF-8编码
- 如果仍有问题，检查主脚本的编码设置部分

---

## 📚 相关文档

- **快速启动指南**：`QUICK_START.md`
- **数据库操作指南**：`backend/DATABASE_GUIDE.md`
- **MySQL连接指南**：`backend/MYSQL_CONNECTION_GUIDE.md`

---

## ✅ 验证清单

- [x] 所有 `.bat` 文件都包含 UTF-8 编码设置
- [x] 控制台字体自动设置为 Consolas
- [x] 代码页自动设置为 65001（UTF-8）
- [x] 临时脚本也包含 UTF-8 设置
- [x] 所有中文提示信息正常显示

---

**提示**：如果您的编辑器支持，建议将项目默认编码设置为 UTF-8，这样新创建的文件会自动使用 UTF-8 编码。

