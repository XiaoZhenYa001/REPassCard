# REPassCard 已实现功能文档

> 文档更新时间：2026-03-21
> 版本：v1.0

---

## 一、界面概览

### 1.1 首页 (Home)
**文件位置：** `app/src/main/java/com/example/passcard/ui/screens/MainScreen.kt`

**功能：**
- 顶部状态栏（显示时间）
- 欢迎语 + 用户名
- 搜索框（模糊搜索）
- 统计卡片（密码总数、安全评分）
- 分类标签筛选
- 最近登录列表

### 1.2 所有密码页面 (All Passwords)
**文件位置：** `app/src/main/java/com/example/passcard/ui/screens/AllPasswordsScreen.kt`

**功能：**
- 顶部导航栏（返回 + 标题 + 搜索）
- 搜索框
- 密码数量统计
- 完整密码列表

### 1.3 编辑页面 (Edit)
**文件位置：** `app/src/main/java/com/example/passcard/ui/screens/EditScreen.kt`

**功能：**
- 新建/编辑密码切换
- Logo 头像显示
- 完整表单字段
- 分类选择器
- 密码显示/隐藏切换
- 删除密码

### 1.4 安全中心 (Security)
**文件位置：** `app/src/main/java/com/example/passcard/ui/screens/MainScreen.kt` (SecurityContent)

**功能：**
- 安全评分卡
- 统计网格（总数/弱密码/复用）
- 待处理事项列表
- 安全建议

### 1.5 设置页面 (Settings)
**文件位置：** `app/src/main/java/com/example/passcard/ui/screens/MainScreen.kt` (SettingsContent)

**功能：**
- 账户信息卡片
- 主密码设置
- 主题外观设置
- 声音反馈开关
- 导出密码
- 导入密码
- 使用帮助、隐私条款、关于我们

### 1.6 导入预览页面
**文件位置：** `app/src/main/java/com/example/passcard/ui/screens/ImportPreviewScreen.kt`

**功能：**
- 导入统计（总数/邮箱/手机/分类）
- 待导入条目预览列表

---

## 二、核心交互功能

### 2.1 密码列表项交互

```
┌─────────────────────────────────────────────────────┐
│ [Logo]  Google Account                     [>]      │
│         alex@gmail.com                           │
└─────────────────────────────────────────────────────┘
     ↑           ↑                              ↑
  单击进入    长按/双击                      单击进入
  编辑页面    复制密码                       编辑页面
```

| 操作区域 | 操作方式 | 结果 |
|---------|---------|------|
| Logo 图标 | 单击 | 进入编辑页面 |
| Logo 图标 | 长按 | 无操作 |
| 中间内容区域 | 单击 | 无操作 |
| 中间内容区域 | 长按/双击 | 复制密码到剪贴板，显示 Toast 提示 |
| > 箭头 | 单击 | 进入编辑页面 |
| > 箭头 | 长按 | 无操作 |

**相关文件：**
- `app/src/main/java/com/example/passcard/ui/components/PasswordListItem.kt`

---

### 2.2 搜索功能

**搜索范围：**
- 服务名称 (name)
- 用户名 (username)
- 邮箱 (email)
- 手机号 (phone)
- 备注 (note)

**搜索方式：**
- 模糊搜索（不区分大小写）
- 实时过滤

**相关文件：**
- `app/src/main/java/com/example/passcard/ui/components/SearchBar.kt`

---

### 2.3 分类筛选

**支持分类：**
- All（全部）
- Social Media
- Work
- Finance
- Shopping
- Entertainment
- AI
- Gaming
- Education
- Other

**筛选逻辑：**
- 选择分类后只显示该分类的密码
- 分类为空时显示所有密码
- 支持分类 + 搜索组合

**相关文件：**
- `app/src/main/java/com/example/passcard/ui/components/CategoryTag.kt`

---

### 2.4 密码复制

**实现方式：**
- 长按密码列表项复制
- 双击密码列表项复制

**反馈方式：**
- Toast 提示 "Copied to clipboard"
- 自动复制到系统剪贴板

**相关文件：**
- `app/src/main/java/com/example/passcard/util/ClipboardHelper.kt`

---

## 三、数据导入/导出

### 3.1 CSV 导入

**支持格式：**
```
服务,用户名,手机号,邮箱,密码,备注,分类
```

**示例：**
```csv
服务,用户名,手机号,邮箱,密码,备注,分类
芜职大教育企业邮箱,李四,18888888888,23000000@whit.edu.cn,Me72916i!,绑定了微信和QQ,Work
硅基流动,,18888888888,,,微信登陆,AI
```

**功能特点：**
- 自动跳过标题行
- 自动跳过 UTF-8 BOM
- 支持带引号的字段
- 支持可选的分类字段

**相关文件：**
- `app/src/main/java/com/example/passcard/util/CsvImporter.kt`
- `app/src/main/java/com/example/passcard/ui/screens/ImportPreviewScreen.kt`

### 3.2 CSV 导出

**导出格式：**
```
服务,用户名,手机号,邮箱,密码,备注,分类
```

**功能特点：**
- 自动添加 UTF-8 BOM
- 自动转义特殊字符（逗号、引号）
- 自动添加时间戳文件名
- 支持分享到其他应用

**相关文件：**
- `app/src/main/java/com/example/passcard/util/CsvExporter.kt`

---

## 四、数据模型

### 4.1 密码条目 (PasswordItem)

```kotlin
data class PasswordItem(
    val id: String,
    val name: String,           // 服务名称
    val username: String,       // 用户名
    val phone: String = "",     // 手机号
    val email: String = "",     // 邮箱
    val password: String,       // 密码
    val category: String = "",  // 分类
    val note: String = ""       // 备注
)
```

### 4.2 导入条目 (ImportEntry)

```kotlin
data class ImportEntry(
    val service: String,
    val username: String,
    val phone: String,
    val email: String,
    val password: String,
    val note: String,
    val category: String = ""
)
```

### 4.3 导出条目 (ExportPasswordEntry)

```kotlin
data class ExportPasswordEntry(
    val service: String,
    val username: String,
    val phone: String = "",
    val email: String = "",
    val password: String,
    val note: String = "",
    val category: String = ""
)
```

---

## 五、组件列表

### 5.1 UI 组件

| 组件 | 文件位置 | 说明 |
|------|---------|------|
| TabBar | `components/TabBar.kt` | 底部导航栏 |
| SearchBar | `components/SearchBar.kt` | 搜索框 |
| CategoryTag | `components/CategoryTag.kt` | 分类标签 |
| StatCard | `components/StatCard.kt` | 统计卡片 |
| PasswordListItem | `components/PasswordListItem.kt` | 可复制密码列表项 |
| SettingItem | `components/SettingItem.kt` | 设置项 |
| ProfileCard | `components/SettingItem.kt` | 用户信息卡片 |
| SecurityScoreCard | `components/SecurityComponents.kt` | 安全评分卡 |
| SecurityStatCard | `components/SecurityComponents.kt` | 安全统计卡 |
| SecurityListItem | `components/SecurityComponents.kt` | 安全列表项 |

### 5.2 页面组件

| 页面 | 文件位置 | 说明 |
|------|---------|------|
| MainScreen | `screens/MainScreen.kt` | 主容器（含 Tab 切换） |
| HomeContent | `screens/MainScreen.kt` | 首页内容 |
| AllPasswordsScreen | `screens/AllPasswordsScreen.kt` | 所有密码页面 |
| EditScreen | `screens/EditScreen.kt` | 编辑页面 |
| SecurityContent | `screens/MainScreen.kt` | 安全中心内容 |
| SettingsContent | `screens/MainScreen.kt` | 设置页面内容 |
| ImportPreviewScreen | `screens/ImportPreviewScreen.kt` | 导入预览页面 |

### 5.3 工具类

| 工具 | 文件位置 | 说明 |
|------|---------|------|
| CsvImporter | `util/CsvImporter.kt` | CSV 导入解析 |
| CsvExporter | `util/CsvExporter.kt` | CSV 导出生成 |
| ClipboardHelper | `util/ClipboardHelper.kt` | 剪贴板操作 |

---

## 六、主题配置

### 6.1 颜色系统

```kotlin
// 主色调
Primary = #4F46E5 (紫色)
PrimaryDark = #18181B (黑色)

// 背景色
Background = #FFFFFF
Surface = #F4F4F5
SurfaceVariant = #F3F4F6

// 文字色
TextPrimary = #000000
TextSecondary = #888888
OnSurfaceVariant = #71717A

// 状态色
Error = #EF4444
Warning = #F97316
Success = #22C55E
```

### 6.2 文件位置

`app/src/main/java/com/example/passcard/ui/theme/`
- Color.kt - 颜色定义
- Type.kt - 字体定义
- Theme.kt - 主题配置

---

## 七、待完善功能

| 功能 | 优先级 | 说明 |
|------|--------|------|
| 数据持久化 | 高 | Room 数据库存储 |
| 主密码/生物识别 | 高 | 安全性保障 |
| 密码生成器 | 中 | 自动生成强密码 |
| 安全检查 | 中 | 检测弱密码/泄露密码 |
| 云同步 | 低 | 可选功能 |
| 浏览器扩展 | 低 | 可选功能 |

---

## 八、Known Issues

1. **数据存储**：当前数据在内存中，重启后丢失
2. **分类为空处理**：分类为空时显示为空白，非 "未分类"
3. **重复导入**：导入时未处理重复密码检测

---

**文档版本：** 1.0
**最后更新：** 2026-03-21
