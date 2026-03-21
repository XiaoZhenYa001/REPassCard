# REPassCard 项目开发计划

> 密码管理器 Android 应用开发计划
> 创建时间：2026-03-20
> 技术栈：Android (Kotlin) + Jetpack Compose

---

## 一、项目概述

### 1.1 项目名称
**REPassCard** - 密码管理器

### 1.2 设计来源
基于 `.pen` 原型文件设计，包含 5 个核心界面：
- 首页 (Home)
- 编辑界面 (Edit Login)
- 安全界面 (Security Center)
- 设置界面 (Settings)
- 底部导航栏 (Tab Bar)

### 1.3 设计规范

#### 颜色系统
```
主色调：
- Primary: #4F46E5 (紫色按钮/强调)
- Primary Dark: #18181B (黑色背景卡片)

背景色：
- Background: #FFFFFF (页面背景)
- Surface: #F4F4F5 (输入框/卡片背景)
- Surface Variant: #F3F4F6 (搜索框背景)

文字色：
- On Background: #000000 (主文字)
- On Surface: #666666 (次级文字)
- On Surface Variant: #71717A (辅助文字)
- Muted: #9CA3AF (占位符)
- On Primary: #FFFFFF (白色文字)

状态色：
- Error: #EF4444 (危险/删除)
- Warning: #F97316 (警告)
- Success: #22C55E (成功)

浅色背景变体：
- Error Container: #FEE2E2 (删除按钮背景)
- Error Light: #FEF2F2 (弱密码卡片)
- Warning Container: #FFF7ED (复用卡片)
- Warning Light: #FEF3C7 (警告背景)
- Success Container: #F0FDF4 (建议背景)
```

#### 字体系统
```
字体族：
- 标题/按钮: Outfit (weights: 400, 500, 600, 700, 800)
- 正文/输入: Inter (weights: 400, 500, 600, 700)

字号规范：
- Display: 40sp, weight 800 (设置标题)
- Headline: 24sp, weight 700 (用户名)
- Title Large: 20sp, weight 700 (安全中心标题)
- Title Medium: 18sp, weight 600 (Section 标题)
- Body Large: 16sp, weight 500/600 (输入框文字)
- Body Medium: 14sp, weight 400/500/600 (正文)
- Body Small: 12sp, weight 400 (辅助文字)
- Label: 10sp, weight 500 (Tab 标签)
```

#### 圆角规范
```
- Extra Small: 8dp (分类标签内元素、列表项图标)
- Small: 12dp (搜索框、列表项、列表项图标)
- Medium: 16dp (输入框、卡片、设置项)
- Large: 18dp (分类标签)
- Extra Large: 24dp (Logo 容器、安全评分卡片)
- Full: 36dp (底部导航栏、Tab 选中态、添加按钮)
```

#### 间距规范
```
- None: 0dp
- Extra Small: 4dp (分类标签内 gap、图标间距)
- Small: 8dp (输入框内元素 gap)
- Medium: 12dp (列表项 gap、图标与文字)
- Large: 16dp (输入框内边距、卡片内边距)
- Extra Large: 24dp (页面内边距、Section gap)
- XXL: 32dp (Section 之间)
```

#### 组件高度规范
```
- Status Bar: 47-62dp
- Nav Bar: 56dp
- Search Box: 48dp
- Input Field: 56dp
- List Item: 72dp
- Category Tag: 36dp
- Stat Card: 140dp
- Tab Bar: 72dp
- Tab Add Button: 54dp
- Icon Circle: 40dp
- Logo Container: 80dp
```

---

## 二、项目结构规划

### 2.1 目录结构
```
D:\Temp\REPassCard\
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/repasscard/app/
│   │   │   │   ├── REPassCardApp.kt              # Application 类
│   │   │   │   ├── MainActivity.kt               # 主 Activity
│   │   │   │   │
│   │   │   │   ├── ui/                           # UI 层
│   │   │   │   │   ├── theme/                    # 主题配置
│   │   │   │   │   │   ├── Color.kt              # 颜色定义
│   │   │   │   │   │   ├── Type.kt               # 字体定义
│   │   │   │   │   │   ├── Theme.kt              # 主题配置
│   │   │   │   │   │   └── Shape.kt              # 形状定义
│   │   │   │   │   │
│   │   │   │   │   ├── components/               # 通用组件
│   │   │   │   │   │   ├── TabBar.kt             # 底部导航栏
│   │   │   │   │   │   ├── SearchBar.kt          # 搜索框
│   │   │   │   │   │   ├── InputField.kt         # 输入框
│   │   │   │   │   │   ├── CategoryTag.kt        # 分类标签
│   │   │   │   │   │   ├── StatCard.kt           # 统计卡片
│   │   │   │   │   │   ├── PasswordListItem.kt   # 密码列表项
│   │   │   │   │   │   ├── SettingItem.kt        # 设置项
│   │   │   │   │   │   └── SecurityScoreCard.kt  # 安全评分卡
│   │   │   │   │   │
│   │   │   │   │   ├── screens/                  # 页面
│   │   │   │   │   │   ├── HomeScreen.kt         # 首页
│   │   │   │   │   │   ├── EditScreen.kt         # 编辑界面
│   │   │   │   │   │   ├── SecurityScreen.kt     # 安全界面
│   │   │   │   │   │   ├── SettingsScreen.kt     # 设置界面
│   │   │   │   │   │   └── MainScreen.kt         # 主容器 (含 TabBar)
│   │   │   │   │   │
│   │   │   │   │   └── navigation/               # 导航
│   │   │   │   │       └── NavGraph.kt           # 导航图
│   │   │   │   │
│   │   │   │   ├── data/                         # 数据层
│   │   │   │   │   ├── model/                    # 数据模型
│   │   │   │   │   │   ├── PasswordEntry.kt      # 密码条目
│   │   │   │   │   │   ├── Category.kt           # 分类
│   │   │   │   │   │   └── SecurityStats.kt      # 安全统计
│   │   │   │   │   │
│   │   │   │   │   ├── repository/               # 数据仓库
│   │   │   │   │   │   └── PasswordRepository.kt
│   │   │   │   │   │
│   │   │   │   │   └── local/                    # 本地存储
│   │   │   │   │       ├── PasswordDao.kt        # 数据访问对象
│   │   │   │   │       ├── PasswordDatabase.kt   # Room 数据库
│   │   │   │   │       └── Converters.kt         # 类型转换器
│   │   │   │   │
│   │   │   │   ├── domain/                       # 业务层
│   │   │   │   │   ├── usecase/                  # 用例
│   │   │   │   │   │   ├── GetPasswordsUseCase.kt
│   │   │   │   │   │   ├── AddPasswordUseCase.kt
│   │   │   │   │   │   ├── UpdatePasswordUseCase.kt
│   │   │   │   │   │   ├── DeletePasswordUseCase.kt
│   │   │   │   │   │   └── GetSecurityStatsUseCase.kt
│   │   │   │   │   │
│   │   │   │   │   └── service/                  # 服务
│   │   │   │   │       ├── PasswordGenerator.kt  # 密码生成器
│   │   │   │   │       └── SecurityChecker.kt    # 安全检查器
│   │   │   │   │
│   │   │   │   └── util/                         # 工具类
│   │   │   │       ├── EncryptionUtil.kt         # 加密工具
│   │   │   │       ├── BiometricUtil.kt          # 生物识别工具
│   │   │   │       └── ClipboardUtil.kt          # 剪贴板工具
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── drawable/                     # 图标资源
│   │   │   │   ├── values/
│   │   │   │   │   ├── strings.xml               # 字符串资源
│   │   │   │   │   └── themes.xml                # 主题 (备用)
│   │   │   │   └── xml/
│   │   │   │       └── backup_rules.xml          # 备份规则
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   ├── test/                                 # 单元测试
│   │   │   └── java/com/repasscard/app/
│   │   │
│   │   └── androidTest/                          # 仪器测试
│   │       └── java/com/repasscard/app/
│   │
│   └── build.gradle.kts                          # App 模块构建配置
│
├── gradle/
│   ├── wrapper/
│   │   └── gradle-wrapper.properties
│   └── libs.versions.toml                        # 依赖版本目录
│
├── build.gradle.kts                              # 项目构建配置
├── settings.gradle.kts                           # 项目设置
├── gradle.properties                             # Gradle 属性
├── gradlew                                       # Gradle Wrapper (Unix)
├── gradlew.bat                                   # Gradle Wrapper (Windows)
│
└── PROJECT_PLAN.md                               # 本文档
```

### 2.2 技术选型

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 语言 | Kotlin | 1.9+ | 主要开发语言 |
| UI | Jetpack Compose | BOM 2024.x | 声明式 UI |
| 导航 | Compose Navigation | 2.7+ | 页面导航 |
| 数据库 | Room | 2.6+ | 本地数据持久化 |
| 依赖注入 | Hilt | 2.48+ | DI 框架 |
| 加密 | Android Security Crypto | 1.1+ | 数据加密 |
| 生物识别 | Biometric | 1.2+ | 指纹/面容解锁 |
| 图标 | Lucide Icons (自定义) | - | 图标库 |
| 协程 | Kotlin Coroutines | 1.7+ | 异步处理 |
| 序列化 | Kotlinx Serialization | 1.6+ | JSON 序列化 |

---

## 三、开发阶段规划

### 阶段一：项目初始化与基础架构 (Day 1)

#### 任务 1.1：创建 Android 项目
```bash
# 在 D:\Temp\REPassCard 目录下创建项目
# 使用 Android Studio 或命令行工具
```

**检查清单：**
- [ ] 项目结构正确创建
- [ ] Gradle 同步成功
- [ ] 能在模拟器/真机上运行空白 Activity

#### 任务 1.2：配置依赖
**build.gradle.kts (Project level):**
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
```

**build.gradle.kts (App level):**
```kotlin
dependencies {
    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    
    // Navigation
    implementation(libs.navigation.compose)
    
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    
    // Security
    implementation(libs.security.crypto)
    implementation(libs.biometric)
    
    // Coroutines
    implementation(libs.coroutines.android)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Debug
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
```

**验证点：**
- [ ] 所有依赖正确下载
- [ ] 无版本冲突
- [ ] 编译成功

#### 任务 1.3：创建主题系统
**文件顺序：**
1. `ui/theme/Color.kt` - 定义所有颜色
2. `ui/theme/Type.kt` - 定义字体样式
3. `ui/theme/Shape.kt` - 定义圆角形状
4. `ui/theme/Theme.kt` - 组合主题

**Color.kt 内容：**
```kotlin
package com.repasscard.app.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Colors
val Primary = Color(0xFF4F46E5)
val PrimaryDark = Color(0xFF18181B)

// Background Colors
val Background = Color(0xFFFFFFFF)
val Surface = Color(0xFFF4F4F5)
val SurfaceVariant = Color(0xFFF3F4F6)

// Text Colors
val OnBackground = Color(0xFF000000)
val OnSurface = Color(0xFF666666)
val OnSurfaceVariant = Color(0xFF71717A)
val Muted = Color(0xFF9CA3AF)
val OnPrimary = Color(0xFFFFFFFF)

// Status Colors
val Error = Color(0xFFEF4444)
val Warning = Color(0xFFF97316)
val Success = Color(0xFF22C55E)

// Container Colors
val ErrorContainer = Color(0xFFFEE2E2)
val ErrorLight = Color(0xFFFEF2F2)
val WarningContainer = Color(0xFFFFF7ED)
val WarningLight = Color(0xFFFEF3C7)
val SuccessContainer = Color(0xFFF0FDF4)

// Border Colors
val Border = Color(0xFFE5E7EB)
val BorderLight = Color(0xFFF4F4F5)

// Tab Bar
val TabInactive = Color(0xFFA1A1AA)
```

**验证点：**
- [ ] 所有颜色值与设计稿一致
- [ ] 主题可正确应用

---

### 阶段二：通用组件开发 (Day 2)

#### 任务 2.1：底部导航栏组件
**文件：** `ui/components/TabBar.kt`

**设计规范：**
- 容器：345dp × 72dp，圆角 36dp
- 背景：白色，边框 1dp #F4F4F5
- 阴影：0dp 4dp 12dp rgba(0,0,0,0.1)
- 内边距：4dp 8dp
- Tab 项：等宽填充，圆角 26dp
- 添加按钮：64dp × 54dp，黑色圆角 27dp

**组件接口：**
```kotlin
@Composable
fun TabBar(
    selectedTab: TabItem,
    onTabSelected: (TabItem) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
)

enum class TabItem {
    HOME, SECURITY, PLACEHOLDER, SETTINGS
}
```

**验证点：**
- [ ] 尺寸与设计稿一致
- [ ] 点击反馈正确
- [ ] 添加按钮居中显示
- [ ] 选中态颜色正确

#### 任务 2.2：搜索框组件
**文件：** `ui/components/SearchBar.kt`

**设计规范：**
- 容器：填充宽度 × 48dp，圆角 12dp
- 背景：#F3F4F6
- 内边距：0dp 16dp
- 图标：🔍 16sp，颜色 #9CA3AF
- 占位符：16sp，颜色 #9CA3AF

**组件接口：**
```kotlin
@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search passwords...",
    modifier: Modifier = Modifier
)
```

**验证点：**
- [ ] 样式与设计稿一致
- [ ] 输入状态正确响应

#### 任务 2.3：输入框组件
**文件：** `ui/components/InputField.kt`

**设计规范：**
- 容器：填充宽度 × 56dp，圆角 16dp
- 背景：#F4F4F5
- 内边距：0dp 16dp
- 标签：14sp，weight 600，Outfit
- 输入文字：16sp，weight 500，Inter

**组件接口：**
```kotlin
@Composable
fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isPassword: Boolean = false,
    isMultiline: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
)

@Composable
fun InputFieldPreview()
```

**验证点：**
- [ ] 高度 56dp
- [ ] 密码模式显示遮罩
- [ ] 多行模式高度自适应

#### 任务 2.4：分类标签组件
**文件：** `ui/components/CategoryTag.kt`

**设计规范：**
- 容器：高度 36dp，圆角 18dp
- 选中：背景 #111827，文字白色
- 未选中：背景 #F3F4F6，文字 #374151
- 内边距：0dp 16dp
- 字体：14sp，weight 500/600

**组件接口：**
```kotlin
@Composable
fun CategoryTag(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun CategoryTagRow(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
)
```

**验证点：**
- [ ] 选中/未选中状态正确
- [ ] 横向可滚动

#### 任务 2.5：统计卡片组件
**文件：** `ui/components/StatCard.kt`

**设计规范：**
- 容器：填充宽度 × 140dp，圆角 16dp
- 内边距：16dp
- 图标圆圈：40dp × 40dp，圆角 20dp
- 字体：标题 16sp weight 600

**组件接口：**
```kotlin
@Composable
fun StatCard(
    icon: @Composable () -> Unit,
    value: String,
    backgroundColor: Color,
    contentColor: Color,
    iconBackgroundColor: Color,
    modifier: Modifier = Modifier
)

@Composable
fun StatCardPair(
    card1Data: StatCardData,
    card2Data: StatCardData,
    modifier: Modifier = Modifier
)

data class StatCardData(
    val emoji: String,
    val value: String,
    val isPrimary: Boolean
)
```

**验证点：**
- [ ] 两卡片等宽并排
- [ ] 紫色卡片文字为白色

#### 任务 2.6：密码列表项组件
**文件：** `ui/components/PasswordListItem.kt`

**设计规范：**
- 容器：填充宽度 × 72dp，圆角 12dp
- 背景：白色
- 边框：1dp #E5E7EB
- 内边距：16dp
- 图标：40dp × 40dp，圆角 8dp
- 标题：14sp weight 600
- 副标题：12sp weight 400，颜色 #888888

**组件接口：**
```kotlin
@Composable
fun PasswordListItem(
    name: String,
    email: String,
    iconText: String,
    iconBackgroundColor: Color,
    iconTextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
)
```

**验证点：**
- [ ] 尺寸与设计稿一致
- [ ] 点击涟漪效果正确

#### 任务 2.7：设置项组件
**文件：** `ui/components/SettingItem.kt`

**设计规范：**
- 容器：填充宽度，圆角 16dp
- 背景：#F4F4F5
- 内边距：16dp
- 图标：20dp
- 文字：16sp weight 600
- 箭头：chevron-right

**组件接口：**
```kotlin
@Composable
fun SettingItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    showToggle: Boolean = false,
    toggleValue: Boolean = false,
    onToggleChange: ((Boolean) -> Unit)? = null
)
```

**验证点：**
- [ ] 开关样式正确
- [ ] 右侧箭头显示正确

#### 任务 2.8：安全评分卡片组件
**文件：** `ui/components/SecurityScoreCard.kt`

**设计规范：**
- 容器：填充宽度 × 200dp，圆角 24dp
- 背景：#18181B
- 内边距：24dp
- 分数：64sp weight 800
- 描述：14sp

**组件接口：**
```kotlin
@Composable
fun SecurityScoreCard(
    score: Int,
    description: String,
    modifier: Modifier = Modifier
)
```

**验证点：**
- [ ] 分数大号显示
- [ ] 描述文字换行正确

---

### 阶段三：页面开发 (Day 3-4)

#### 任务 3.1：首页 (HomeScreen)
**文件：** `ui/screens/HomeScreen.kt`

**布局结构：**
```
Column
├── Header (Welcome back + Username)
├── SearchBar
├── StatCardPair (42 Passwords + 98% Secure)
├── CategoryTagRow (All, Wi-Fi, Notes, Cards)
└── RecentLoginsSection
    ├── Header (Recent Logins + See All)
    └── PasswordListItem × N
```

**组件参数：**
```kotlin
@Composable
fun HomeScreen(
    onNavigateToEdit: (String?) -> Unit,
    onNavigateToAllPasswords: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
)

data class HomeUiState(
    val userName: String = "Alex Smith",
    val totalPasswords: Int = 42,
    val securityScore: Int = 98,
    val selectedCategory: String? = null,
    val categories: List<String> = listOf("All", "Wi-Fi", "Notes", "Cards"),
    val recentPasswords: List<PasswordEntry> = emptyList()
)
```

**验证点：**
- [ ] 所有元素位置正确
- [ ] 滚动流畅
- [ ] 点击事件正确触发

#### 任务 3.2：编辑界面 (EditScreen)
**文件：** `ui/screens/EditScreen.kt`

**布局结构：**
```
Column
├── Nav Bar (← Back + Edit Login + Save)
├── ScrollContent
│   ├── Logo Container (Icon + Change Icon)
│   ├── Form Stack
│   │   ├── InputField (Name)
│   │   ├── InputField (Username)
│   │   ├── InputField (Phone)
│   │   ├── InputField (Email)
│   │   ├── InputField (Password) + Copy/Eye icons
│   │   ├── Category Selector
│   │   └── InputField (Note) - multiline
│   └── Delete Button
```

**组件参数：**
```kotlin
@Composable
fun EditScreen(
    passwordId: String?,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    viewModel: EditViewModel = hiltViewModel()
)

data class EditUiState(
    val isNew: Boolean = true,
    val name: String = "",
    val username: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val category: String? = null,
    val note: String = "",
    val icon: String = "key",
    val categories: List<String> = listOf("Social Media", "Work", "Finance", "Shopping", "Other")
)
```

**验证点：**
- [ ] 新建/编辑模式切换正确
- [ ] 密码显示/隐藏切换
- [ ] 复制功能正常
- [ ] 删除确认对话框

#### 任务 3.3：安全界面 (SecurityScreen)
**文件：** `ui/screens/SecurityScreen.kt`

**布局结构：**
```
Column
├── Nav Bar (← Back + Security Center)
├── ScrollContent
│   ├── SecurityScoreCard
│   ├── Stats Grid (3 cards)
│   │   ├── Total Passwords: 142
│   │   ├── Weak Passwords: 3 (red)
│   │   └── Reused: 12 (orange)
│   ├── Attention Needed Section
│   │   ├── Compromised Passwords
│   │   └── Weak Passwords
│   └── Security Suggestions Section
│       └── Enable 2-Factor Auth
```

**组件参数：**
```kotlin
@Composable
fun SecurityScreen(
    onBack: () -> Unit,
    onNavigateToCompromised: () -> Unit,
    onNavigateToWeak: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel()
)

data class SecurityUiState(
    val securityScore: Int = 85,
    val totalPasswords: Int = 142,
    val weakPasswords: Int = 3,
    val reusedPasswords: Int = 12,
    val compromisedCount: Int = 1,
    val suggestions: List<SecuritySuggestion> = emptyList()
)

data class SecuritySuggestion(
    val title: String,
    val description: String,
    val type: SuggestionType
)

enum class SuggestionType {
    TWO_FACTOR, BIOMETRIC, MASTER_PASSWORD
}
```

**验证点：**
- [ ] 分数动态计算
- [ ] 卡片颜色区分
- [ ] 点击跳转正确

#### 任务 3.4：设置界面 (SettingsScreen)
**文件：** `ui/screens/SettingsScreen.kt`

**布局结构：**
```
Column
├── Header (设置)
├── Account Section
│   ├── Profile Card (Avatar + Name + Email)
│   └── Security (主密码)
├── App Settings Section
│   ├── Theme (主题外观)
│   └── Sound (声音反馈) + Toggle
├── Data Management Section
│   ├── Export Passwords
│   └── Import Passwords
└── More Section
    ├── Help
    ├── Privacy
    └── About
```

**组件参数：**
```kotlin
@Composable
fun SettingsScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToMasterPassword: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
)

data class SettingsUiState(
    val userName: String = "Alex Morgan",
    val userEmail: String = "alex@example.com",
    val theme: AppTheme = AppTheme.LIGHT,
    val soundEnabled: Boolean = true
)

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}
```

**验证点：**
- [ ] Toggle 状态保存
- [ ] 主题切换生效
- [ ] 导航正确

#### 任务 3.5：主容器 (MainScreen)
**文件：** `ui/screens/MainScreen.kt`

**功能：**
- 管理 TabBar 状态
- 切换页面内容
- 处理添加按钮点击

**组件参数：**
```kotlin
@Composable
fun MainScreen(
    onNavigateToAddPassword: () -> Unit
)
```

**验证点：**
- [ ] Tab 切换动画流畅
- [ ] 添加按钮浮动显示

---

### 阶段四：数据层开发 (Day 5-6)

#### 任务 4.1：数据模型定义
**文件：** `data/model/PasswordEntry.kt`

```kotlin
@Entity(tableName = "passwords")
data class PasswordEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String,
    val username: String,
    val email: String? = null,
    val phone: String? = null,
    val password: String,  // 加密存储
    val website: String? = null,
    val category: String? = null,
    val note: String? = null,
    val icon: String = "key",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null
)
```

**文件：** `data/model/Category.kt`

```kotlin
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val name: String,
    val icon: String,
    val color: String,
    val order: Int
)
```

**文件：** `data/model/SecurityStats.kt`

```kotlin
data class SecurityStats(
    val totalPasswords: Int,
    val weakPasswords: Int,
    val reusedPasswords: Int,
    val compromisedPasswords: Int,
    val securityScore: Int
)
```

**验证点：**
- [ ] 实体定义正确
- [ ] 字段类型匹配

#### 任务 4.2：Room 数据库
**文件：** `data/local/PasswordDao.kt`

```kotlin
@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords ORDER BY lastUsedAt DESC")
    fun getAllPasswords(): Flow<List<PasswordEntry>>
    
    @Query("SELECT * FROM passwords WHERE id = :id")
    suspend fun getPasswordById(id: Long): PasswordEntry?
    
    @Query("SELECT * FROM passwords WHERE category = :category ORDER BY lastUsedAt DESC")
    fun getPasswordsByCategory(category: String): Flow<List<PasswordEntry>>
    
    @Query("SELECT * FROM passwords WHERE name LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%'")
    fun searchPasswords(query: String): Flow<List<PasswordEntry>>
    
    @Query("SELECT * FROM passwords ORDER BY lastUsedAt DESC LIMIT :limit")
    fun getRecentPasswords(limit: Int = 5): Flow<List<PasswordEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: PasswordEntry): Long
    
    @Update
    suspend fun updatePassword(password: PasswordEntry)
    
    @Delete
    suspend fun deletePassword(password: PasswordEntry)
    
    @Query("SELECT COUNT(*) FROM passwords")
    fun getTotalCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM passwords WHERE LENGTH(password) < 8")
    fun getWeakPasswordCount(): Flow<Int>
}
```

**文件：** `data/local/PasswordDatabase.kt`

```kotlin
@Database(
    entities = [PasswordEntry::class, Category::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PasswordDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao
    abstract fun categoryDao(): CategoryDao
}
```

**验证点：**
- [ ] 数据库创建成功
- [ ] CRUD 操作正常
- [ ] Flow 数据流正确

#### 任务 4.3：数据仓库
**文件：** `data/repository/PasswordRepository.kt`

```kotlin
class PasswordRepository @Inject constructor(
    private val passwordDao: PasswordDao,
    private val encryptionUtil: EncryptionUtil
) {
    fun getAllPasswords(): Flow<List<PasswordEntry>> = passwordDao.getAllPasswords()
    
    fun getRecentPasswords(limit: Int = 5): Flow<List<PasswordEntry>> = 
        passwordDao.getRecentPasswords(limit)
    
    suspend fun getPasswordById(id: Long): PasswordEntry? = 
        passwordDao.getPasswordById(id)
    
    fun searchPasswords(query: String): Flow<List<PasswordEntry>> = 
        passwordDao.searchPasswords(query)
    
    suspend fun addPassword(entry: PasswordEntry): Long {
        val encryptedEntry = entry.copy(
            password = encryptionUtil.encrypt(entry.password)
        )
        return passwordDao.insertPassword(encryptedEntry)
    }
    
    suspend fun updatePassword(entry: PasswordEntry) {
        val encryptedEntry = entry.copy(
            password = encryptionUtil.encrypt(entry.password),
            updatedAt = System.currentTimeMillis()
        )
        passwordDao.updatePassword(encryptedEntry)
    }
    
    suspend fun deletePassword(entry: PasswordEntry) {
        passwordDao.deletePassword(entry)
    }
    
    fun getSecurityStats(): Flow<SecurityStats> = combine(
        passwordDao.getTotalCount(),
        passwordDao.getWeakPasswordCount(),
        // ... 其他统计
    ) { total, weak ->
        SecurityStats(
            totalPasswords = total,
            weakPasswords = weak,
            // ...
        )
    }
}
```

**验证点：**
- [ ] 加密/解密正确
- [ ] 数据流正确

---

### 阶段五：业务层开发 (Day 7)

#### 任务 5.1：加密工具
**文件：** `util/EncryptionUtil.kt`

```kotlin
class EncryptionUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secret_shared_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun encrypt(data: String): String {
        // 使用 Android Keystore 加密
    }
    
    fun decrypt(encryptedData: String): String {
        // 解密数据
    }
}
```

**验证点：**
- [ ] 加密/解密对称
- [ ] KeyStore 安全存储

#### 任务 5.2：密码生成器
**文件：** `domain/service/PasswordGenerator.kt`

```kotlin
class PasswordGenerator {
    fun generate(
        length: Int = 16,
        includeUppercase: Boolean = true,
        includeLowercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        // 生成随机密码
    }
    
    fun checkStrength(password: String): PasswordStrength {
        // 检查密码强度
    }
}

enum class PasswordStrength {
    WEAK, MEDIUM, STRONG, VERY_STRONG
}
```

**验证点：**
- [ ] 密码随机性
- [ ] 强度检测准确

#### 任务 5.3：安全检查器
**文件：** `domain/service/SecurityChecker.kt`

```kotlin
class SecurityChecker @Inject constructor(
    private val passwordDao: PasswordDao
) {
    suspend fun checkCompromised(password: String): Boolean {
        // 检查密码是否在已知泄露数据库中
        // 可使用 Have I Been Pwned API
    }
    
    fun calculateSecurityScore(stats: SecurityStats): Int {
        // 计算安全分数 (0-100)
    }
}
```

**验证点：**
- [ ] 分数计算合理
- [ ] 泄露检测准确

---

### 阶段六：ViewModel 开发 (Day 8)

#### 任务 6.1：HomeViewModel
**文件：** `ui/screens/HomeViewModel.kt`

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PasswordRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getRecentPasswords(),
                repository.getTotalCount(),
                repository.getSecurityStats()
            ) { recent, total, stats ->
                _uiState.update { state ->
                    state.copy(
                        recentPasswords = recent,
                        totalPasswords = total,
                        securityScore = stats.securityScore
                    )
                }
            }.collect()
        }
    }
    
    fun onCategorySelected(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
    
    fun onSearch(query: String) {
        // 搜索逻辑
    }
}
```

**验证点：**
- [ ] 数据加载正确
- [ ] 状态更新及时

#### 任务 6.2：EditViewModel
**文件：** `ui/screens/EditViewModel.kt`

```kotlin
@HiltViewModel
class EditViewModel @Inject constructor(
    private val repository: PasswordRepository,
    private val passwordGenerator: PasswordGenerator,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val passwordId: String? = savedStateHandle["passwordId"]
    
    private val _uiState = MutableStateFlow(EditUiState(isNew = passwordId == null))
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()
    
    init {
        passwordId?.let { loadPassword(it) }
    }
    
    fun generatePassword() {
        val newPassword = passwordGenerator.generate()
        _uiState.update { it.copy(password = newPassword) }
    }
    
    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }
    
    fun save() {
        viewModelScope.launch {
            // 保存逻辑
        }
    }
}
```

**验证点：**
- [ ] 编辑/新建模式正确
- [ ] 保存功能正常

---

### 阶段七：导航与集成 (Day 9)

#### 任务 7.1：导航图
**文件：** `ui/navigation/NavGraph.kt`

```kotlin
@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "main",
        modifier = modifier
    ) {
        composable("main") {
            MainScreen(
                onNavigateToAddPassword = {
                    navController.navigate("edit/new")
                }
            )
        }
        
        composable(
            route = "edit/{passwordId}",
            arguments = listOf(navArgument("passwordId") { nullable = true })
        ) { backStackEntry ->
            val passwordId = backStackEntry.arguments?.getString("passwordId")
            EditScreen(
                passwordId = passwordId,
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() },
                onDelete = { navController.popBackStack() }
            )
        }
        
        composable("security") {
            SecurityScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCompromised = { /* ... */ },
                onNavigateToWeak = { /* ... */ }
            )
        }
        
        composable("settings") {
            SettingsScreen(/* ... */)
        }
    }
}
```

**验证点：**
- [ ] 导航正确
- [ ] 参数传递正确

#### 任务 7.2：MainActivity
**文件：** `MainActivity.kt`

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            REPassCardTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}
```

**验证点：**
- [ ] 应用启动正常
- [ ] 主题应用正确

---

### 阶段八：测试与优化 (Day 10-11)

#### 任务 8.1：单元测试
**测试范围：**
- EncryptionUtil 加密/解密测试
- PasswordGenerator 测试
- SecurityChecker 测试
- ViewModel 测试

#### 任务 8.2：UI 测试
**测试范围：**
- 各组件渲染测试
- 导航测试
- 用户交互测试

#### 任务 8.3：性能优化
**优化项：**
- 列表性能 (LazyColumn)
- 数据库查询优化
- 内存管理

#### 任务 8.4：安全审计
**检查项：**
- 敏感数据加密
- KeyStore 安全
- 日志脱敏
- 备份排除

---

## 四、安全检查清单

### 4.1 数据安全
- [ ] 所有密码使用 AES-256 加密存储
- [ ] 主密码使用 PBKDF2 或 Argon2 哈希
- [ ] 使用 Android Keystore 存储加密密钥
- [ ] 数据库文件权限正确
- [ ] 备份文件加密

### 4.2 通信安全
- [ ] 如有云同步，使用 TLS 1.3
- [ ] 证书校验正确
- [ ] 不在日志中记录敏感信息

### 4.3 认证安全
- [ ] 生物识别正确集成
- [ ] 锁屏超时设置
- [ ] 失败次数限制

### 4.4 代码安全
- [ ] ProGuard/R8 混淆启用
- [ ] 无硬编码密钥
- [ ] 无调试代码残留
- [ ] Intent 数据验证

---

## 五、开发进度跟踪

### Week 1
| Day | 任务 | 状态 | 备注 |
|-----|------|------|------|
| 1 | 项目初始化 + 主题系统 | ⬜ 待开始 | |
| 2 | 通用组件开发 | ⬜ 待开始 | |
| 3 | 首页 + 编辑界面 | ⬜ 待开始 | |
| 4 | 安全界面 + 设置界面 | ⬜ 待开始 | |
| 5 | 数据层开发 | ⬜ 待开始 | |

### Week 2
| Day | 任务 | 状态 | 备注 |
|-----|------|------|------|
| 6 | 数据层完善 | ⬜ 待开始 | |
| 7 | 业务层 + ViewModel | ⬜ 待开始 | |
| 8 | 导航集成 | ⬜ 待开始 | |
| 9 | 功能完善 | ⬜ 待开始 | |
| 10 | 测试与优化 | ⬜ 待开始 | |

---

## 六、每次开发后的检查流程

### 6.1 编译检查
```bash
./gradlew clean build
```

### 6.2 静态分析
```bash
./gradlew detekt
./gradlew lint
```

### 6.3 测试运行
```bash
./gradlew test
./gradlew connectedAndroidTest
```

### 6.4 安全扫描
- 检查敏感信息泄露
- 检查依赖漏洞 (OWASP Dependency Check)

### 6.5 代码审查清单
- [ ] 符合设计规范
- [ ] 无硬编码字符串
- [ ] 无内存泄漏风险
- [ ] 异常处理完整
- [ ] 日志级别正确

---

## 七、备注

### 7.1 图标资源
使用 Lucide Icons，需要创建 SVG 转 Vector Drawable：
- arrow-left
- house
- shield-check
- shield-alert
- settings
- plus
- search
- copy
- eye
- eye-off
- trash-2
- chevron-right
- chevron-down
- x
- unlock
- alert-triangle
- alert-circle
- database
- refresh-cw
- moon
- volume-2
- upload
- download
- help-circle
- lock
- info

### 7.2 后续功能
- 云同步 (可选)
- 密码分享
- 浏览器扩展
- 自动填充服务

### 7.3 CSV 导入格式
支持导入 CSV 文件，格式如下：
```
服务,用户名,手机号,邮箱,密码,备注
```
示例：
```
服务,用户名,手机号,邮箱,密码,备注
芜职大教育企业邮箱,李四,18888888888,23000000@whit.edu.cn,Me72916i!,绑定了微信，和qq邮箱，手机号
硅基流动,,18888888888,,,微信登陆（AI集合网站api）
```

### 7.4 Compose 必要 Import 清单
每次创建新文件时，确保包含以下 import：
```kotlin
// 基础
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable  // 长按/双击复制
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField

// 图标
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*

// Material3
import androidx.compose.material3.*

// 运行时
import androidx.compose.runtime.*

// UI
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
```

### 7.5 用户交互逻辑

#### 密码列表项交互
| 区域 | 操作 | 结果 |
|------|------|------|
| Logo/图标区域 | 单击 | 进入编辑页面 |
| Logo/图标区域 | 长按 | 无操作 |
| 中间内容区域 | 单击 | 无操作（编辑页面用） |
| 中间内容区域 | 长按/双击 | 复制密码到剪贴板 |
| > 箭头区域 | 单击 | 进入编辑页面 |
| > 箭头区域 | 长按 | 无操作 |

#### 搜索功能
- 模糊搜索所有字段：name, username, email, phone, note
- 支持分类筛选 + 搜索组合
- 首页显示筛选后的前5条

#### 导入/导出格式
CSV 格式（带 BOM UTF-8）：
```
服务,用户名,手机号,邮箱,密码,备注,分类
```

### 7.6 分类管理
- 支持分类：Social Media, Work, Finance, Shopping, Entertainment, AI, Gaming, Education, Other
- 导入时分类为空则默认为空
- 分类为空时不过滤（显示所有）

---

**文档版本：** 1.2
**最后更新：** 2026-03-20
**作者：** OpenClaw Assistant
