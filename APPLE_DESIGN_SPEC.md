# REPassCard · Apple 风格设计规范 v1.0

> **文档说明**：本文档是 REPassCard Android 应用的完整视觉设计规范，基于已确认的 HTML 预览稿整理，任何 AI 或开发者均可依此文档独立复现全套 UI 效果。加密备份页面（CloudSyncScreen）维持原样，不在本规范范围内。

---

## 目录

1. [设计哲学](#1-设计哲学)
2. [颜色系统](#2-颜色系统)
3. [字体系统](#3-字体系统)
4. [间距系统](#4-间距系统)
5. [圆角系统](#5-圆角系统)
6. [阴影系统](#6-阴影系统)
7. [动画规范](#7-动画规范)
8. [组件规范](#8-组件规范)
9. [页面布局规范](#9-页面布局规范)
10. [实施优先级](#10-实施优先级)
11. [Kotlin/Compose 代码片段](#11-kotlincompose-代码片段)

---

## 1. 设计哲学

### 1.1 简洁即高级（Simplicity is Sophistication）

Apple 设计语言的核心在于：**去掉所有不必要的元素，让每一个留下来的像素都有存在的理由**。REPassCard 的重新设计遵循同一原则——删除装饰性边框、多余的分割线和视觉噪声，以克制的方式传递功能层次。用户打开应用时感受到的第一印象应该是「干净」和「有质感」，而非「花哨」。

具体体现：输入框不用全包边框，改用底部细线或焦点时的高亮边框；列表项不用描边卡片，改用轻量阴影浮起来；颜色不用纯色块，改用渐变和半透明叠加。

### 1.2 阴影代替边框（Shadow over Border）

层次感是 UI 高级感的关键。低质量界面习惯用 `border: 1px solid #ccc` 来区分元素，而 Apple 风格用**多层阴影**代替边框来制造浮起感。

规则：
- **浅色模式**：Surface 通过阴影浮起，背景为系统灰 `#F2F2F7`，卡片为纯白 `#FFFFFF`
- **深色模式**：背景为纯黑 `#000000`，卡片为深灰 `#1C1C1E`，阴影更强
- `border` 仅在以下场景使用：深色模式下极淡的 `rgba(255,255,255,0.06)` 描边，或输入框 Focus 状态的彩色高亮边
- 永远不用灰色实线边框区分卡片

### 1.3 留白即设计（Whitespace is Design）

中文界面常见的错误是把所有内容塞满屏幕，生怕有空白浪费空间。Apple 风格相反——**充足的留白本身就是设计的一部分**，它让用户的眼睛有呼吸的空间，自然引导注意力到关键内容。

规则：
- 页面内容区域水平边距固定 `20dp`（非 16dp）
- Section 之间间距 `24dp`，不同功能区之间 `28–32dp`
- 列表项高度不低于 `56dp`（iOS HIG 标准触控高度）
- 不在同一卡片内堆砌超过 4 个字段

### 1.4 字体是第一印象（Typography First）

用户接触界面的第一反应来自于字体。系统默认字体（Roboto）在精品感上远不如专门引入的字体。

字体策略：
- **Outfit**（Google Fonts）：专用于标题、数字、品牌名——带有几何感和个性，对标 Apple SF Pro Display
- **Inter**（Google Fonts）：专用于正文、标签、说明——极高易读性，各字重表现优秀
- **回退**：`FontFamily.SansSerif`（Android 系统 Sans-Serif 字体）

---

## 2. 颜色系统

所有颜色以 CSS/Compose `Color` Token 形式定义，支持浅色/深色两套主题。

### 2.1 浅色模式（Light Mode）

| Token | HEX | 用途 |
|-------|-----|------|
| `colorPrimary` | `#5E5CE6` | 主色（苹果紫），按钮、选中态、Focus 边框 |
| `colorPrimaryDark` | `#4B49C8` | 主色按压态 |
| `colorPrimaryGradStart` | `#5E5CE6` | 渐变起始色 |
| `colorPrimaryGradEnd` | `#7B79F7` | 渐变结束色 |
| `colorPrimaryLight` | `rgba(94,92,230,0.08)` | 主色半透明背景（hover / 标签未选中） |
| `colorSuccess` | `#30D158` | iOS 绿，成功状态、强密码标签 |
| `colorError` | `#FF3B30` | iOS 红，错误状态、弱密码标签、删除 |
| `colorWarning` | `#FF9F0A` | iOS 橙，警告、中等密码强度 |
| `colorBlue` | `#007AFF` | iOS 蓝，链接、主密码图标 |
| `colorCyan` | `#32ADE6` | iOS 青蓝，随机密码图标 |
| `colorBackground` | `#F2F2F7` | 页面背景（iOS 系统灰） |
| `colorSurface` | `#FFFFFF` | 卡片、输入框背景 |
| `colorSurface2` | `#F2F2F7` | 次级 Surface（图标背景、筛选条件背景） |
| `colorLabel1` | `#000000` | 主文字 |
| `colorLabel2` | `#6C6C70` | 次级文字（副标题、说明） |
| `colorLabel3` | `#AEAEB2` | 三级文字（占位符、图标、Section 标题） |
| `colorLabel4` | `#D1D1D6` | 最淡文字（禁用、分隔线图标） |
| `colorSeparator` | `rgba(60,60,67,0.09)` | 分隔线（0.5px 使用） |
| `colorTabBarBg` | `rgba(255,255,255,0.92)` | TabBar 毛玻璃背景 |
| `colorTabActive` | `#5E5CE6` | Tab 选中色 |
| `colorTabInactive` | `#AEAEB2` | Tab 未选中色 |
| `colorSuccessContainer` | `rgba(48,209,88,0.10)` | 成功状态背景容器 |
| `colorErrorContainer` | `rgba(255,59,48,0.10)` | 错误状态背景容器 |
| `colorWarningContainer` | `rgba(255,159,10,0.10)` | 警告状态背景容器 |
| `colorBlueContainer` | `rgba(0,122,255,0.10)` | 蓝色信息容器 |
| `colorScoreCardBg` | `#1C1C1E` | 安全评分卡固定深色背景（不随主题切换） |

### 2.2 深色模式（Dark Mode）

| Token | HEX | 用途 |
|-------|-----|------|
| `colorPrimary` | `#6E6CE8` | 深色主色（稍亮，保证对比度） |
| `colorPrimaryDark` | `#5856C9` | 深色主色按压态 |
| `colorPrimaryGradStart` | `#6E6CE8` | 深色渐变起始 |
| `colorPrimaryGradEnd` | `#8E8CF9` | 深色渐变结束 |
| `colorPrimaryLight` | `rgba(110,108,232,0.12)` | 深色半透明主色背景 |
| `colorSuccess` | `#30D158` | 不变 |
| `colorError` | `#FF453A` | 深色模式红（稍亮） |
| `colorWarning` | `#FF9F0A` | 不变 |
| `colorBlue` | `#0A84FF` | 深色模式蓝（稍亮） |
| `colorCyan` | `#64D2FF` | 深色模式青蓝（稍亮） |
| `colorBackground` | `#000000` | 纯黑背景 |
| `colorSurface` | `#1C1C1E` | 深灰卡片 |
| `colorSurface2` | `#2C2C2E` | 次级 Surface 深色版 |
| `colorLabel1` | `#FFFFFF` | 主文字白 |
| `colorLabel2` | `#8E8E93` | 次级文字 |
| `colorLabel3` | `#636366` | 三级文字 |
| `colorLabel4` | `#48484A` | 最淡文字 |
| `colorSeparator` | `rgba(84,84,88,0.36)` | 深色分隔线 |
| `colorTabBarBg` | `rgba(28,28,30,0.95)` | 深色 TabBar 毛玻璃 |
| `colorTabActive` | `#6E6CE8` | 深色 Tab 选中 |
| `colorTabInactive` | `#636366` | 深色 Tab 未选中 |
| `colorSuccessContainer` | `rgba(48,209,88,0.15)` | 深色成功容器 |
| `colorErrorContainer` | `rgba(255,69,58,0.15)` | 深色错误容器 |
| `colorWarningContainer` | `rgba(255,159,10,0.15)` | 深色警告容器 |
| `colorBlueContainer` | `rgba(0,122,255,0.15)` | 深色蓝色信息容器 |

### 2.3 功能图标专属颜色（SettingItem Icon Colors）

每个设置项图标有固定专属颜色，与功能语义对应，不随主题切换（始终为实色背景 + 白色图标）。

| 功能 | 图标背景色 | HEX |
|------|-----------|-----|
| 主密码 / 锁 | iOS 蓝 | `#007AFF` |
| 指纹解锁 | iOS 绿 | `#30D158` |
| 主题外观 | iOS 橙 | `#FF9F0A` |
| 语言 | 苹果紫 | `#5E5CE6` |
| 随机密码 | iOS 青蓝 | `#32ADE6` |
| 导出密码 | 浅绿 | `#34C759` |
| 导入密码 | iOS 蓝 | `#007AFF` |
| 使用帮助 | 橙 | `#FF9F0A` |
| 隐私条款 | 灰 | `#8E8E93` |
| 关于 | 苹果紫 | `#5E5CE6` |
| 声音反馈 | iOS 绿 | `#30D158` |
| 自动清除剪贴板 | iOS 红 | `#FF3B30` |
| 云同步 | iOS 蓝 | `#007AFF` |

---

## 3. 字体系统

### 3.1 字族引入

**Android Compose 引入方式（build.gradle.kts）**：

```kotlin
// build.gradle.kts (app)
dependencies {
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.0")
}
```

**Type.kt 实现**：

```kotlin
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val InterFont = GoogleFont("Inter")
val OutfitFont = GoogleFont("Outfit")

val InterFamily = FontFamily(
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.ExtraBold),
)

val OutfitFamily = FontFamily(
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = OutfitFont, fontProvider = provider, weight = FontWeight.ExtraBold),
)
```

### 3.2 字阶（Type Scale）

| Token | 字族 | 字重 | 字号 | 行高 | 字间距 | 用途 |
|-------|------|------|------|------|--------|------|
| `displayLarge` | Outfit | 700 | 34sp | 40sp | -0.5sp | 页面大标题（设置、安全中心） |
| `headlineLarge` | Outfit | 700 | 32sp | 38sp | -0.8sp | 首页「我的保险库」 |
| `headlineMedium` | Outfit | 700 | 22sp | 28sp | -0.4sp | 编辑页服务名称 Hero |
| `titleLarge` | Outfit | 700 | 18sp | 24sp | -0.3sp | Nav 导航栏标题 |
| `titleMedium` | Inter | 700 | 18sp | 24sp | -0.3sp | Section 主标题（最近使用、需要关注） |
| `bodyLarge` | Inter | 600 | 15sp | 22sp | -0.15sp | 列表项主文字、设置项标签 |
| `bodyMedium` | Inter | 500 | 15sp | 22sp | -0.15sp | 输入框已填写文字 |
| `bodySmall` | Inter | 400 | 13sp | 19sp | 0sp | 列表项副文字（邮箱、账号） |
| `labelLarge` | Inter | 700 | 16sp | 22sp | -0.2sp | 按钮主文字 |
| `labelMedium` | Inter | 600 | 14sp | 20sp | 0sp | 次级按钮、「查看全部」链接 |
| `labelSmall` | Inter | 600 | 12sp | 17sp | 0.5sp | Section 分组标题（全大写）、强度条标签 |
| `caption` | Inter | 500 | 11sp | 16sp | 0.3sp | 时间戳、计数标签、密码强度详情 |
| `tabLabel` | Inter | 600 | 10sp | 14sp | 0sp | TabBar 图标下方标签（选中 W600） |
| `scoreNumber` | Outfit | 800 | 64sp | 64sp | -1sp | 安全评分大数字 |
| `statNumber` | Outfit | 700 | 22sp | 26sp | -0.5sp | 统计卡片数字（迷你版） |

### 3.3 Section 分组标题规范

Section 分组标题（如「保险库」「应用设置」「登录信息」「附加信息」）使用 `labelSmall` 变体：

```kotlin
Text(
    text = sectionTitle,  // 中文保持原样，英文可用 .uppercase()
    style = MaterialTheme.typography.labelSmall.copy(
        fontFamily = InterFamily,
        fontWeight = FontWeight.W700,
        fontSize = 12.sp,
        letterSpacing = 0.8.sp,
        color = colorLabel3
    )
)
```

> **注意**：中文 Section 标题保持原样（如「保险库」），英文 Section 标题使用全大写（如 `"LOGIN INFO".uppercase()`）。颜色为 `colorLabel3`，左侧 padding 4dp。

### 3.4 数字与品牌名字体

所有统计数字、评分数字、服务名称（大标题形式）以及导航栏标题，一律使用 **Outfit** 字族，不使用 Inter。

原因：Outfit 的几何感和字重分级比 Inter 更适合展示性数字，视觉冲击力更强，与 Apple SF Pro Display 的气质接近。

---

## 4. 间距系统

所有间距以 `dp` 为单位，在 Compose 中使用 `Dp` 类型定义。建议在 `Dimens.kt` 中统一声明，禁止在组件内直接写魔法数字。

### 4.1 基础间距 Token

| Token | 数值 | 用途 |
|-------|------|------|
| `spacing2` | 2dp | 最小间距，密码强度条段间隙 |
| `spacing4` | 4dp | 图标与文字极近距离，角标偏移 |
| `spacing6` | 6dp | Section 标题与内容卡片间距 |
| `spacing8` | 8dp | 小组件间距，分类标签间隙 |
| `spacing10` | 10dp | 列表项间距（PasswordListItem 垂直间隔） |
| `spacing12` | 12dp | 行内图标与文字间距 |
| `spacing14` | 14dp | 分组卡片行内边距 |
| `spacing16` | 16dp | 标准内边距（卡片、行内 padding） |
| `spacing20` | 20dp | 页面水平边距、卡片内边距 |
| `spacing24` | 24dp | Section 间距 |
| `spacing28` | 28dp | TabBar 底部安全距离 |
| `spacing32` | 32dp | 大 Section 间距 |
| `spacing48` | 48dp | 大间距（锁屏图标与标题间距等） |

### 4.2 页面级边距规范

- 页面水平边距：20dp（左右各 20dp）
- Section 标题距上方内容：4dp padding-top
- Section 标题距下方卡片：8dp
- 相邻 Section 间距：24dp
- 功能区块间距（如评分卡与统计行）：24dp
- 页面顶部标题距 StatusBar：20dp
- 列表底部距 TabBar：16dp

### 4.3 组件内边距规范

| 组件 | 内边距规范 |
|------|-----------|
| PasswordListItem | 水平 14dp，垂直居中（高度 68dp） |
| SettingItem | 水平 16dp，垂直居中（高度 56dp） |
| SearchBar | 水平 14dp，图标与文字间距 10dp |
| GroupCard 行 | 水平 16dp，图标与内容间距 14dp |
| StatCard | 所有方向 20dp |
| ScoreCard | 所有方向 22-24dp |
| CategoryTag | 水平 16dp（高度 34dp） |
| InputField | 水平 16dp（高度 52-56dp） |
| ActionButton | 高度 52-54dp，圆角 16dp |
| NavBar 返回按钮 | 34x34dp 点击区，圆角 12dp |

---

## 5. 圆角系统

Apple 风格圆角视觉接近「连续曲线圆角」（Squircle），Android 中用 `RoundedCornerShape` 近似实现。

### 5.1 圆角 Token

| Token | 数值 | 用途 |
|-------|------|------|
| `radius4` | 4dp | 强度条段、极小元素 |
| `radius8` | 8dp | 小角标、导航栏计数气泡 |
| `radius10` | 10dp | SettingItem 图标容器（36x36dp） |
| `radius12` | 12dp | 导航栏返回按钮（34x34dp） |
| `radius13` | 13dp | PasswordListItem 图标容器（42x42dp） |
| `radius14` | 14dp | 次级操作按钮、生成密码按钮、SearchBar |
| `radius16` | 16dp | 主操作按钮（ActionButton）、SettingItem |
| `radius17` | 17dp | CategoryTag 全圆（高34dp一半） |
| `radius18` | 18dp | GroupCard（分组信息卡） |
| `radius20` | 20dp | StatCard（统计卡片） |
| `radius22` | 22dp | 编辑页图标容器（80x80dp） |
| `radius24` | 24dp | ScoreCard（安全评分卡） |
| `radius26` | 26dp | TabBar 单个 Tab 按钮 |
| `radius34` | 34dp | TabBar 整体胶囊（高68dp一半） |
| `radiusCircle` | 50% | Add按钮、指纹按钮、图标光圈、头像 |

### 5.2 圆角使用原则

1. **尺寸越大，圆角越大**：小图标 8-10dp，大卡片 18-24dp
2. **同类组件圆角统一**：所有 SettingItem 都是 16dp，所有 GroupCard 都是 18dp
3. **图标容器圆角** = 容器尺寸 x 0.30（近似）
4. **全圆按钮**：圆角 = 高度 / 2
5. **禁止**：不同页面同类组件使用不同圆角值

---

## 6. 阴影系统

### 6.1 阴影层级定义

Apple 风格使用多层叠加阴影（非单层），第一层细阴影提供边界感，第二层大阴影提供浮起感。

**注意**：Compose 的 `Modifier.shadow()` 只支持单层。多层阴影需通过 `Modifier.drawBehind` + Canvas 自定义绘制（见第 11 章）。

| 层级 | 名称 | 浅色模式 | 深色模式 |
|------|------|---------|---------|
| Level 0 | Flat | 无 | 无 |
| Level 1 | Card | 0 0.5px 1px rgba(0,0,0,.02) + 0 2px 8px rgba(0,0,0,.04) + 0 4px 16px rgba(0,0,0,.05) | 透明度升至 .15/.25/.35 |
| Level 2 | Elevated | 0 2px 8px rgba(0,0,0,.06) + 0 8px 24px rgba(0,0,0,.08) | 透明度升至 .40/.50 |
| Level 3 | FloatBtn | 0 4px 16px rgba(94,92,230,.30) + 0 1px 3px rgba(94,92,230,.20) | 主色换 #6E6CE8，透明度 .40/.25 |
| Level 4 | TabBar | 0 -1px 0 rgba(0,0,0,.04) + 0 8px 32px rgba(0,0,0,.10) | 上线换 rgba(255,255,255,.06)，底阴影 .50 |
| Level 5 | IconGlow | 0 4px 12px rgba(accent,.30) + 0 8px 28px rgba(accent,.18) | 同左，accent 为品牌色 |

### 6.2 各组件阴影层级对照

| 组件 | 层级 | 备注 |
|------|------|------|
| PasswordListItem | Level 1 | 无 border |
| SettingItem / GroupCard | Level 1 | 无 border |
| SearchBar（静态） | Level 1 | — |
| SearchBar（Focus） | Level 1 + 主色光晕 | border 1.5dp primary |
| StatCard（中性） | Level 1 | 白色卡片 |
| StatCard（渐变强调） | Level 3 | 主色发光 |
| TabBar | Level 4 | hair-line + 大柔和阴影 |
| FAB（Add 按钮） | Level 3 | 紫色发光 |
| 编辑页图标容器 | Level 5 | 品牌色发光 |
| ScoreCard | 自定义 0 4px 20px rgba(0,0,0,.25) | 固定深色背景 |
| 主操作按钮（保存） | Level 3 | 紫色发光 |

### 6.3 多层阴影 Compose 工具函数

```kotlin
data class ShadowSpec(
    val offsetX: Dp = 0.dp,
    val offsetY: Dp,
    val blurRadius: Dp,
    val color: Color
)

fun Modifier.multiShadow(
    vararg layers: ShadowSpec,
    cornerRadius: Dp = 16.dp
): Modifier = drawBehind {
    layers.forEach { spec ->
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(
                        spec.blurRadius.toPx(), spec.offsetX.toPx(),
                        spec.offsetY.toPx(), spec.color.toArgb()
                    )
                }
            }
            val r = cornerRadius.toPx()
            canvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
        }
    }
}

// Level 1 浅色预设
val ShadowL1Light = arrayOf(
    ShadowSpec(offsetY = 2.dp, blurRadius = 8.dp,  color = Color(0x0A000000)),
    ShadowSpec(offsetY = 4.dp, blurRadius = 16.dp, color = Color(0x0D000000))
)
// Level 1 深色预设
val ShadowL1Dark = arrayOf(
    ShadowSpec(offsetY = 2.dp, blurRadius = 8.dp,  color = Color(0x40000000)),
    ShadowSpec(offsetY = 4.dp, blurRadius = 16.dp, color = Color(0x59000000))
)
// Level 3 主色发光预设
val ShadowL3Primary = arrayOf(
    ShadowSpec(offsetY = 1.dp, blurRadius = 3.dp,  color = Color(0x335E5CE6)),
    ShadowSpec(offsetY = 4.dp, blurRadius = 16.dp, color = Color(0x4D5E5CE6))
)
```

---

## 7. 动画规范

所有动画遵循「快进慢出」（FastOutSlowIn），短促有力不拖沓。操作反馈 <= 150ms，状态切换 <= 300ms，主题切换 420ms。

### 7.1 动画参数速查表

| 场景 | 时长 | 曲线/参数 |
|------|------|----------|
| 卡片按压 scale→0.97 | 90ms | FastOutSlowIn |
| 卡片弹起 scale→1.0 | spring | dampingRatio=0.35, stiffness=700 |
| Tab 选中缩放 | 140ms | FastOutSlowIn |
| Tab 圆点出现（scale 0→1） | 160ms | FastOutSlowIn |
| 分类标签弹性选中 | spring | dampingRatio=0.40, stiffness=900 |
| Toggle 滑块位移 | 180ms | FastOutSlowIn |
| Toggle 轨道颜色 | 160ms | LinearEasing |
| 主题整体切换 | 420ms | LinearEasing |
| 颜色过渡（hover/focus） | 150ms | FastOutSlowIn |
| FAB 旋转 90° | 200ms | FastOutSlowIn |
| 保存按钮光泽扫过 | 600ms | Linear，hover 触发一次 |
| 页面进入（push） | 300ms | FastOutSlowIn |
| 页面退出（pop） | 250ms | FastOutSlowIn |
| 备注光标闪烁 | 1000ms | step(1, JumpEnd) |

### 7.2 通用按压缩放封装

```kotlin
@Composable
fun PressableScale(
    onClick: () -> Unit,
    pressScale: Float = 0.97f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressScale else 1f,
        animationSpec = if (pressed)
            tween(90, easing = FastOutSlowInEasing)
        else
            spring(dampingRatio = 0.35f, stiffness = 700f),
        label = "pressScale"
    )
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap = { onClick() }
                )
            }
    ) { content() }
}
```

### 7.3 iOS Toggle 动画

```kotlin
@Composable
fun IOSToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF30D158) else Color(0x4D78788C),
        animationSpec = tween(160), label = "trackColor"
    )
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 20f else 0f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "thumbOffset"
    )
    Box(
        Modifier.size(51.dp, 31.dp).clip(RoundedCornerShape(16.dp))
            .background(trackColor).clickable { onCheckedChange(!checked) }
    ) {
        Box(
            Modifier.padding(start = (2f + thumbOffset * 0.72f).dp, top = 2.dp)
                .size(27.dp).clip(CircleShape)
                .background(Color.White).shadow(2.dp, CircleShape)
        )
    }
}
```

### 7.4 主题切换颜色动画

```kotlin
@Composable
fun Color.animated(): Color = animateColorAsState(
    targetValue = this,
    animationSpec = tween(420, easing = LinearEasing),
    label = "themeColor"
).value

// 使用示例
val bg      = colorBackground.animated()
val surface = colorSurface.animated()
val label1  = colorLabel1.animated()
```

### 7.5 分类标签弹性选中

```kotlin
val scale by animateFloatAsState(
    targetValue = if (selected) 1.04f else 1f,
    animationSpec = spring(dampingRatio = 0.40f, stiffness = 900f),
    label = "tagScale"
)
Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
    // Tag content
}
```

---

## 8. 组件规范

本章逐一描述每个核心 UI 组件的尺寸、颜色、状态和行为规范。

---

### 8.1 TabBar（底部导航栏）

**形态**：浮动胶囊型，悬浮在内容之上，不占用底部 SafeArea。

| 属性 | 数值 |
|------|------|
| 宽度 | 父容器宽度 - 32dp（左右各 16dp 间距） |
| 最大宽度 | 340dp |
| 高度 | 68dp |
| 圆角 | 34dp（全圆胶囊） |
| 背景 | 毛玻璃：`colorTabBarBg` + `blur=20dp, saturate=1.8` |
| 边框 | 1dp，`colorSeparator` |
| 阴影 | Level 4 |
| 底部间距 | 28dp（距底部导航区域） |

**Tab 按钮规范**：

| 状态 | 图标尺寸 | 图标颜色 | 标签字重 | 标签颜色 |
|------|---------|---------|---------|---------|
| 选中 | 22dp | `colorTabActive` | W600 | `colorTabActive` |
| 未选中 | 20dp | `colorTabInactive` | W400 | `colorTabInactive` |

**圆点指示器**：4×4dp 圆形，颜色 `colorPrimary`，选中时 scale 0→1（animateFloatAsState，160ms）

**Add 按钮（中间）**：

| 属性 | 数值 |
|------|------|
| 尺寸 | 52×52dp |
| 形状 | 圆形（radiusCircle） |
| 背景 | 渐变（colorPrimaryGradStart → colorPrimaryGradEnd，140°） |
| 阴影 | Level 3（主色发光） |
| 图标 | Plus，24dp，白色 |
| 按压动画 | scale 0.94，spring 弹起 |

```kotlin
@Composable
fun FloatingTabBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    onAddClick: () -> Unit
) {
    val tabs = listOf(
        TabItem(Icons.Outlined.Home, Icons.Filled.Home, "首页"),
        TabItem(Icons.Outlined.Shield, Icons.Filled.Shield, "安全"),
        null, // Add button placeholder
        TabItem(Icons.Outlined.Cloud, Icons.Filled.Cloud, "加密"),
        TabItem(Icons.Outlined.Settings, Icons.Filled.Settings, "设置")
    )
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp)
            .padding(bottom = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth().height(68.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(colorTabBarBg)
                .border(1.dp, colorSeparator, RoundedCornerShape(34.dp))
                .multiShadow(*ShadowL4, cornerRadius = 34.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { i, tab ->
                if (tab == null) {
                    AddButton(onClick = onAddClick, Modifier.weight(1f))
                } else {
                    TabItem(
                        item = tab,
                        selected = selectedIndex == if (i < 2) i else i - 1,
                        onClick = { onTabSelected(if (i < 2) i else i - 1) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
```

---

### 8.2 PasswordListItem（密码列表项）

**用途**：首页「最近使用」列表和「全部密码」页面的通用列表项。

| 属性 | 数值 |
|------|------|
| 高度 | 68dp |
| 圆角 | 18dp（GroupCard 内时无单独圆角，共享父级 18dp） |
| 背景 | `colorSurface` |
| 阴影 | Level 1（独立使用时）/ 无（GroupCard 内使用时） |
| 水平内边距 | 14dp |
| 图标容器 | 42×42dp，圆角 13dp，品牌渐变背景 |
| 图标文字 | 17sp，Outfit W700，白色 |
| 名称 | 15sp，Inter W600，`colorLabel1`，letter-spacing=-0.15sp |
| 副文字 | 13sp，Inter W400，`colorLabel2` |
| 时间标签 | 11sp，Inter W500，`colorLabel3`（全部密码页显示） |
| 强弱标签 | 10sp，W700，强=绿色容器，弱=红色容器 |
| 右箭头 | ChevronRight，14dp，`colorLabel4` |
| 分隔线 | 0.5px，起点在图标右侧（x=56dp，即 14dp padding + 42dp 图标），颜色 `colorSeparator` |

**交互状态**：
- hover：整行背景变 `colorPrimaryLight`
- press：scale 0.97，90ms FastOutSlowIn
- release：spring(0.35, 700) 弹回

**品牌色规范**：图标背景使用品牌渐变色，不使用 `colorPrimary`。不知品牌色时使用 `colorPrimary` 渐变。

```kotlin
@Composable
fun PasswordListItem(
    item: PasswordItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PressableScale(onClick = onClick) {
        Row(
            modifier = modifier.fillMaxWidth().height(68.dp)
                .background(colorSurface)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Brand icon
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(item.gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.initial, style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = OutfitFamily, fontWeight = FontWeight.W700,
                    fontSize = 17.sp, color = Color.White
                ))
            }
            // Info
            Column(Modifier.weight(1f)) {
                Text(item.name, style = bodyLarge, color = colorLabel1,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.account, style = bodySmall, color = colorLabel2,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // Meta
            Column(horizontalAlignment = Alignment.End) {
                if (item.lastUsed != null)
                    Text(item.lastUsed, style = caption, color = colorLabel3)
                if (item.strengthBadge != null)
                    StrengthBadge(item.strengthBadge)
            }
            Icon(Icons.Outlined.ChevronRight, null,
                Modifier.size(14.dp), tint = colorLabel4)
        }
    }
}
```

---

### 8.3 SearchBar（搜索栏）

| 属性 | 数值 |
|------|------|
| 高度 | 48dp |
| 圆角 | 14dp |
| 背景 | `colorSurface` |
| 阴影 | Level 1 |
| 水平内边距 | 14dp |
| 图标与文字间距 | 10dp |
| 搜索图标 | `Icons.Outlined.Search`（SVG 矢量，勿用 emoji），17dp，`colorLabel3` |
| 占位符 | 15sp，W400，`colorLabel3` |
| 输入文字 | 15sp，W500，`colorLabel1` |

**Focus 状态**（动画 150ms）：
- border：1.5dp solid `colorPrimary`
- 额外光环：`box-shadow: 0 0 0 3px colorPrimaryLight`（Compose 中用 `drawBehind` 实现）

```kotlin
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "搜索密码、用户名...",
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (focused) colorPrimary else Color.Transparent,
        animationSpec = tween(150), label = "searchBorder"
    )
    Row(
        modifier = modifier.fillMaxWidth().height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colorSurface)
            .multiShadow(*ShadowL1Light, cornerRadius = 14.dp)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Outlined.Search, null, Modifier.size(17.dp), tint = colorLabel3)
        BasicTextField(
            value = query, onValueChange = onQueryChange,
            textStyle = bodyMedium.copy(color = colorLabel1),
            modifier = Modifier.weight(1f)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                if (query.isEmpty())
                    Text(placeholder, style = bodyMedium, color = colorLabel3)
                inner()
            }
        )
    }
}
```

---

### 8.4 SettingItem（设置列表项）

| 属性 | 数值 |
|------|------|
| 高度 | 56dp（含内边距） |
| 背景 | `colorSurface` |
| 阴影 | Level 1 |
| 水平内边距 | 16dp |
| 图标容器 | 36×36dp，圆角 10dp，彩色实色背景（见 2.3 功能色表） |
| 图标 | 18dp，白色 |
| 标签 | 15sp，Inter W600，`colorLabel1` |
| 尾部文字 | 14sp，Inter W500，`colorLabel2` |
| 右箭头 | ChevronRight，14dp，`colorLabel4` |
| 按压动画 | scale 0.97，90ms |

**分组规范**：多个 SettingItem 放在同一个 `GroupCard`（背景 `colorSurface`，圆角 18dp）内，行间用 0.5px `colorSeparator` 分隔，起始于图标右侧（x=50dp）。单个 SettingItem 不加边框，靠 GroupCard 的 Level 1 阴影浮起。

```kotlin
@Composable
fun SettingItem(
    icon: ImageVector,
    iconBgColor: Color,
    label: String,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    PressableScale(onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = Color.White)
            }
            Text(label, Modifier.weight(1f), style = bodyLarge, color = colorLabel1)
            if (trailingText != null)
                Text(trailingText, style = labelMedium, color = colorLabel2)
            Icon(Icons.Outlined.ChevronRight, null,
                Modifier.size(14.dp), tint = colorLabel4)
        }
    }
}
```

---

### 8.5 SettingToggleItem（开关设置项）

与 SettingItem 布局相同，右侧替换为 IOSToggle（见 7.3）。

| 属性 | 数值 |
|------|------|
| 轨道尺寸 | 51×31dp |
| 轨道圆角 | 16dp |
| 滑块尺寸 | 27×27dp |
| 滑块圆角 | 圆形 |
| 选中轨道色 | `#30D158`（iOS 绿，不跟随主题） |
| 未选中轨道色 | `rgba(120,120,128,0.30)` |
| 滑块颜色 | 纯白，带 2dp 阴影 |
| 位移动画 | 180ms FastOutSlowIn，offset 0→20dp |

---

### 8.6 StatCard（首页统计卡片）

首页并排两个，使用 Grid 2列布局。

**中性卡片**（如「12 个密码」）：

| 属性 | 数值 |
|------|------|
| 高度 | 150dp |
| 圆角 | 20dp |
| 背景 | `colorSurface` |
| 阴影 | Level 1 |
| 内边距 | 20dp |
| 图标容器 | 44×44dp，圆形，背景 `colorSurface2` |
| 图标 | 22dp，`colorPrimary` |
| 主文字 | 14sp，Inter W700，`colorLabel1`，letterSpacing=-0.2sp |
| 副文字 | 12sp，Inter W500，`colorLabel2` |

**强调卡片**（如「安全评分 87」）：

| 属性 | 数值 |
|------|------|
| 背景 | 渐变（`colorPrimaryGradStart` → `colorPrimaryGradEnd`，140°） |
| 阴影 | Level 3（主色发光） |
| 装饰圆 1 | 绝对定位右上角，120×120dp，白色 rgba(1.0, 0.08) |
| 装饰圆 2 | 左下角，80×80dp，白色 rgba(1.0, 0.05) |
| 图标容器 | 44×44dp，圆形，背景 `rgba(255,255,255,0.20)` |
| 图标 | 白色 |
| 主文字 | 白色 rgba(0.95) |
| 副文字 | 白色 rgba(0.72) |

---

### 8.7 CategoryTag（分类标签）

| 属性 | 选中 | 未选中 |
|------|------|--------|
| 高度 | 34dp | 34dp |
| 圆角 | 17dp（全圆） | 17dp（全圆） |
| 水平内边距 | 16dp | 16dp |
| 背景 | `colorPrimary` | `colorSurface` |
| 文字颜色 | 白色 | `colorLabel2` |
| 文字样式 | 13sp，W600 | 13sp，W600 |
| 阴影 | 0 2px 10px rgba(94,92,230,.28) | Level 1 |
| 缩放 | scale(1.04)，spring | scale(1.0) |
| 图标 | 12dp（可选，与文字同色） | — |

hover 未选中时：背景变 `colorPrimaryLight`，文字变 `colorPrimary`。

---

### 8.8 SecurityScoreCard（安全评分卡）

| 属性 | 数值 |
|------|------|
| 圆角 | 22dp |
| 背景 | `#1C1C1E`（固定深色，不随主题切换） |
| 内边距 | 22-24dp |
| 评分数字 | 64sp，Outfit W800，白色，lineHeight=64sp |
| 评分说明 | 13sp，Inter W400，`#8E8E93` |
| 评分等级 | 18sp，Inter W700，颜色随分数：<50 红，50-70 橙，>70 绿 |
| 进度条 | 高 4dp，圆角 2dp，满宽，颜色随分数同上 |
| 进度条背景 | `rgba(255,255,255,0.10)` |
| 装饰光圈 | 右上角 `radial-gradient(rgba(94,92,230,.15), transparent)` |

**分数对应颜色**：

| 分数范围 | 等级 | 颜色 |
|---------|------|------|
| 0-49 | 危险 | `#FF453A` |
| 50-69 | 一般 | `#FF9F0A` |
| 70-84 | 良好 | `#30D158` |
| 85-100 | 优秀 | `#30D158`（加星） |

---

### 8.9 GroupCard 内的 FieldRow（编辑页输入行）

编辑密码页使用 iOS 「分组 Cell」样式，多个字段共享一个 GroupCard。

| 属性 | 数值 |
|------|------|
| 行最小高度 | 54dp |
| 水平内边距 | 16dp |
| 图标 | 20dp，`colorLabel3`，与内容间距 14dp |
| 字段标签 | 11sp，Inter W700，`colorLabel3`，全大写，letterSpacing=0.3sp |
| 字段值 | 15sp，Inter W500，`colorLabel1` |
| 占位符 | 15sp，斜体，`colorLabel4` |
| 分隔线 | 0.5px `colorSeparator`，从图标右侧（x=56dp，即 16dp padding + 20dp 图标 + 20dp 间距）到行尾 |
| 操作按钮 | 30×30dp，圆角 9dp，背景 `colorSurface2` |
| 操作按钮图标 | 14dp，静态 `colorLabel2`，hover 变 `colorPrimary` + 背景变 `colorPrimaryLight` |

**密码字段专属**：
- 密码值显示为 12 个圆点（6dp，间距 4dp，颜色 `colorLabel1`，opacity 0.7）
- 右侧操作按钮：查看图标 + 复制图标（各 30×30dp）

**密码强度条**（附在密码行下方，不在行内）：

| 属性 | 数值 |
|------|------|
| 高度 | 4dp（含 padding-bottom 14dp） |
| 左偏移 | 50dp（对齐图标右侧） |
| 强度段数 | 4 段，段间隙 2dp |
| 颜色 | 段1红→段2橙→段3黄→段4绿，点亮N段表示强度 |
| 强度文字 | 11sp，W600，当前等级颜色 |
| 右侧详情 | 11sp，W500，`colorLabel3`（如「12 位 · 含大小写」） |

---

### 8.10 LockScreen（锁屏页）

| 区域 | 属性 |
|------|------|
| 整体背景 | `colorBackground` |
| 图标外圈 | 72×72dp 圆形，`colorPrimary` opacity 10% |
| 图标内圈 | 56×56dp 圆形，`colorPrimary` opacity 8% |
| 应用图标 | 36dp，`colorPrimary` |
| 欢迎标题 | 32sp，Outfit W700，`colorLabel1`，letterSpacing=-0.5sp |
| 副标题 | 15sp，Inter W400，`colorLabel2` |
| 密码输入框 | 同 InputField，高度 56dp，圆角 14dp |
| 解锁按钮 | 高 52dp，圆角 16dp，渐变背景，Level 3 阴影 |
| 指纹按钮 | 56×56dp 圆形，背景 `colorPrimaryLight`，图标 `colorPrimary` |
| 指纹标签 | 12sp，W500，`colorLabel3` |

---

### 8.11 NavBar（页面内导航栏）

用于编辑页、全部密码页等非主 Tab 页面。

| 属性 | 数值 |
|------|------|
| 高度 | 52dp |
| 返回按钮 | 34×34dp，圆角 12dp，背景 `colorSurface2` |
| 返回图标 | ChevronLeft（15px left-pointing），16dp，`colorLabel1` |
| 标题 | 居中，18sp，Outfit W700，`colorLabel1`，letterSpacing=-0.3sp |
| 右侧操作 | 「保存」文字：16sp，Inter W700，`colorPrimary` |
| 右侧计数 | 13sp，W600，`colorLabel3`，背景 `colorSurface2`，圆角 8dp，padding 5x10dp |
| 返回按钮 hover | scale(1.06)，background 加深 |

---

### 8.12 ActionButton（主/次操作按钮）

**主操作按钮**（如「保存密码」「立即备份」）：

| 属性 | 数值 |
|------|------|
| 高度 | 54dp |
| 圆角 | 16dp |
| 背景 | 渐变 `colorPrimaryGradient` |
| 阴影 | Level 3 |
| 图标 | 18dp，`rgba(255,255,255,0.90)` |
| 文字 | 16sp，Inter W700，白色，letterSpacing=-0.2sp |
| hover | 光泽扫过动画（600ms Linear） |
| press | scale(0.97)，90ms |

**次操作按钮**（如「从备份恢复」「生成随机密码」）：

| 属性 | 数值 |
|------|------|
| 高度 | 44-54dp |
| 圆角 | 14-16dp |
| 背景 | `colorSurface` 或 `colorPrimaryLight` |
| 边框 | 1.5dp `colorSeparator`（静态）/ `colorPrimary`（hover） |
| 文字颜色 | `colorPrimary` |
| 文字样式 | 14-16sp，Inter W600-700 |

**危险操作按钮**（如「删除此密码」）：

| 属性 | 数值 |
|------|------|
| 高度 | 44dp |
| 背景 | 透明 |
| hover 背景 | `rgba(255,59,48,0.06)` |
| 文字 | 14-15sp，Inter W600，`#FF3B30` |
| 图标 | 15-16dp，`#FF3B30` |
| 无圆角/边框 | — |

---

## 9. 页面布局规范

---

### 9.1 HomeContent（首页）

**整体结构**（从上到下）：

```
StatusBar（59dp）
├── 问候语 + 标题区（垂直排列）
├── SearchBar（48dp）
├── StatCard 双列网格（150dp）
├── CategoryTag 横向滚动行（34dp）
├── 「最近使用」标题行
└── PasswordListItem 列表（GroupCard 内）
TabBar（68dp，浮动）
```

**顶部问候区**：

| 元素 | 样式 |
|------|------|
| 问候语（如「下午好 👋」） | 14sp，Inter W500，`colorLabel2` |
| 「我的保险库」 | 32sp，Outfit W700，`colorLabel1`，letterSpacing=-0.8sp |
| 区域底部间距 | 20dp |

**「最近使用」标题行**：

| 元素 | 样式 |
|------|------|
| 左侧「最近使用」 | 18sp，Inter W700，`colorLabel1`，letterSpacing=-0.3sp |
| 右侧「查看全部」 | 14sp，Inter W600，`colorPrimary`，可点击 |

**密码列表**：独立 PasswordListItem，垂直间距 10dp，无 GroupCard 包裹（与全部密码页的 GroupCard 分组不同）。

**页面滚动**：`LazyColumn`，内容区水平 padding 20dp，顶部 padding 20dp。

---

### 9.2 SecurityContent（安全中心）

**整体结构**：

```
StatusBar
├── 页面大标题「安全中心」（34sp Outfit W700）
├── SecurityScoreCard（见 8.8）
├── 三列迷你统计卡（总数/弱密码/重复）
├── 「需要关注」Section + 问题列表
└── 「安全建议」Section + 建议卡片
```

**三列迷你统计卡**：

| 属性 | 数值 |
|------|------|
| 高度 | 约 100dp（自适应） |
| 圆角 | 16dp |
| 布局 | Grid 3列，间距 10dp |
| 「总数」卡 | 背景 `colorSurface2`，图标 `colorLabel2` |
| 「弱密码」卡 | 背景 `colorErrorContainer`，图标+数字 `colorError` |
| 「重复」卡 | 背景 `colorWarningContainer`，图标+数字 `colorWarning` |
| 数字 | 22sp，Outfit W700 |
| 标签 | 11sp，Inter W600，letterSpacing=0.2sp |

**问题列表项**（SecurityIssueItem）：

| 属性 | 数值 |
|------|------|
| 圆角 | 16dp（GroupCard 内） |
| 图标容器 | 40×40dp，圆角 12dp |
| 弱密码图标背景 | `colorErrorContainer` |
| 重复图标背景 | `colorWarningContainer` |
| 标题 | 15sp，Inter W600，`colorLabel1` |
| 描述 | 13sp，Inter W400，`colorLabel2` |
| 右箭头 | ChevronRight，14dp，`colorLabel4` |

**安全建议卡**：

| 属性 | 数值 |
|------|------|
| 背景 | `colorSuccessContainer` |
| 圆角 | 16dp |
| 图标 | 20dp，`colorSuccess` |
| 标题 | 14sp，Inter W600，绿色系文字 |
| 描述 | 13sp，Inter W400，绿色系文字（浅），行高 1.55 |

---

### 9.3 SettingsContent（设置页）

**整体结构**：

```
StatusBar
├── 页面大标题「设置」（34sp Outfit W700）
├── [保险库] Section
│   ├── ProfileCard（头像 + 名称 + 描述）
│   ├── 主密码（SettingItem）
│   └── 指纹解锁（SettingToggleItem）
├── [应用设置] Section
│   ├── 主题外观
│   ├── 语言
│   ├── 随机密码
│   ├── 声音反馈（Toggle）
│   └── 自动清除剪贴板（Toggle）
├── [数据管理] Section
│   ├── 导出密码
│   └── 导入密码
└── [更多] Section
    ├── 使用帮助
    ├── 隐私条款
    └── 关于
```

**ProfileCard**：

| 属性 | 数值 |
|------|------|
| 背景 | `colorSurface` |
| 圆角 | 16dp（GroupCard 内首项） |
| 头像 | 48×48dp，圆角 14dp，渐变背景 `colorPrimaryGradient` |
| 头像文字 | 首字母，20sp，Outfit W700，白色 |
| 主名称 | 15sp，Inter W600，`colorLabel1` |
| 副描述 | 13sp，Inter W400，`colorLabel2` |
| 右箭头 | ChevronRight，14dp，`colorLabel4` |

**Section 标题**：12sp，Inter W700，`colorLabel3`，全大写，letterSpacing=0.8sp，左 padding 4dp，距上方 Section 末尾 8dp，距下方 GroupCard 8dp。

---

### 9.4 LockScreen（锁屏）

**整体布局**：垂直居中，`Column(verticalArrangement = Arrangement.Center)`。

**从上到下间距**：

```
图标区域（双圆环 + 应用图标）
  ↓ 24dp
欢迎标题（32sp Outfit W700）
  ↓ 8dp
副标题（15sp Inter W400 colorLabel2）
  ↓ 40dp
密码输入框（InputField 变体，56dp）
  ↓ 16dp
解锁按钮（54dp 渐变）
  ↓ 24dp
指纹区域（56dp 圆形按钮 + 12sp 标签）
```

**双圆环图标**：

```kotlin
Box(contentAlignment = Alignment.Center) {
    // 外圈
    Box(Modifier.size(72.dp).clip(CircleShape)
        .background(colorPrimary.copy(alpha = 0.10f)))
    // 内圈
    Box(Modifier.size(56.dp).clip(CircleShape)
        .background(colorPrimary.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center) {
        Icon(appIcon, null, Modifier.size(36.dp), tint = colorPrimary)
    }
}
```

---

### 9.5 EditScreen（新增/编辑密码）

**整体结构**：

```
StatusBar
NavBar（返回 + 居中标题 + 右侧「保存」）
├── Hero 区（图标 + 更改图标链接）
├── [登录信息] GroupCard
│   ├── 用户名行（复制按钮）
│   ├── 密码行（查看 + 复制按钮）
│   └── 强度条（附在 GroupCard 底部）
├── 生成随机密码按钮（dashed border）
├── [附加信息] GroupCard
│   ├── 手机号行
│   └── 邮箱/网站行
├── [分类] CategoryTag 横向滚动
├── [备注] 多行卡片（min-height 80dp）
└── 底部操作区
    ├── 「保存密码」主按钮（54dp 渐变）
    └── 「删除此密码」危险按钮（仅编辑模式）
```

**编辑页图标 Hero**：

| 属性 | 数值 |
|------|------|
| 图标容器 | 80×80dp，圆角 22dp，品牌渐变背景 |
| 外光圈1 | 96×96dp 圆形，`radial-gradient(brand_color 0%, transparent 70%)`，opacity 8% |
| 外光圈2 | 90×90dp 圆形，opacity 5%（更柔和） |
| 编辑角标 | 28×28dp 圆形，背景 `colorSurface`，图标 `colorPrimary`，14dp |
| 服务名 | 22sp，Outfit W700，`colorLabel1` |
| 网站链接 | 13sp，Inter W500，`colorPrimary`，圆角 6dp hover 背景 |

**生成随机密码按钮**：

| 属性 | 数值 |
|------|------|
| 高度 | 44dp |
| 圆角 | 14dp |
| 背景 | `colorPrimaryLight` |
| 边框 | 1.5dp dashed `rgba(colorPrimary, 0.40)` |
| 图标 | 刷新图标，16dp，`colorPrimary` |
| 文字 | 14sp，Inter W700，`colorPrimary` |

---

### 9.6 AllPasswordsScreen（全部密码）

**整体结构**：

```
StatusBar
NavBar（返回 + 「全部密码」 + 右侧「N 项」计数气泡）
├── SearchBar（48dp）
├── 筛选行（CategoryChip 横向 + 右侧排序按钮）
├── 字母分组索引
│   ├── Section 标签（字母，12sp W700 colorLabel3 全大写）
│   └── GroupCard（同字母账户共用同一 GroupCard）
│       └── PasswordListItem × N（含 0.5px 分隔线）
└── 底部统计（「共 N 条记录」，13sp W500 colorLabel3）
FAB（56×56dp 圆角 18dp，右下角，距底 34dp 距右 24dp）
```

**字母分组规范**：

- Section 字母标签：12sp，Inter W700，`colorLabel3`，全大写，padding-top 4dp
- 同字母下所有密码合入一个 GroupCard（背景 `colorSurface`，圆角 18dp）
- 行间 0.5px 分隔线，起始于图标右侧

**筛选行排序按钮**：

| 属性 | 数值 |
|------|------|
| 尺寸 | 34×34dp |
| 圆角 | 11dp |
| 背景 | `colorSurface` |
| 阴影 | Level 1 |
| 图标 | 三横线递减，16dp，`colorLabel2` |

**FAB**：

| 属性 | 数值 |
|------|------|
| 尺寸 | 56×56dp |
| 圆角 | 18dp |
| 背景 | 渐变 `colorPrimaryGradient` |
| 阴影 | Level 3 |
| hover | 旋转 90°，scale(1.05)，200ms |
| 图标 | Plus，24dp，白色 |

---

## 10. 实施优先级

按依赖关系从基础到上层排列，必须严格按顺序实施，否则上层改动会因底层 Token 未就绪而反复返工。

### Phase 1 — 基础 Token（必须最先完成）

| 优先级 | 文件 | 改动内容 |
|--------|------|---------|
| P1.1 | `Color.kt` | 新增全套 Token（浅色+深色各 25+ 个） |
| P1.2 | `Type.kt` | 引入 Inter + Outfit，定义 15 级字阶 |
| P1.3 | `Theme.kt` | 接入新 Token，配置 MaterialTheme |
| P1.4 | `Dimens.kt` | 声明全套间距/圆角常量 |

### Phase 2 — 核心工具（阴影/动画工具函数）

| 优先级 | 内容 |
|--------|------|
| P2.1 | `ShadowUtils.kt`：`multiShadow` 工具函数 + 预设 |
| P2.2 | `AnimationUtils.kt`：`PressableScale`、`Color.animated()` |
| P2.3 | `IOSToggle.kt`：独立 Toggle 组件 |

### Phase 3 — 原子组件

| 优先级 | 组件 | 关键改动 |
|--------|------|---------|
| P3.1 | SearchBar | 去 emoji，矢量图标，Focus 光环 |
| P3.2 | CategoryTag | 弹性动画，hover 态 |
| P3.3 | PasswordListItem | 去 border，Level 1 阴影，品牌渐变图标 |
| P3.4 | SettingItem | 彩色图标容器（36dp），圆角 10dp |
| P3.5 | SettingToggleItem | IOSToggle 替换原生 Switch |

### Phase 4 — 页面级改造

| 优先级 | 页面 | 重点 |
|--------|------|------|
| P4.1 | HomeContent | 统计卡渐变，标题层次，标签弹性 |
| P4.2 | SecurityContent | 评分卡固定深色背景，三色统计 |
| P4.3 | SettingsContent | Section 标题规范，ProfileCard |
| P4.4 | LockScreen | 双圆环，渐变解锁按钮 |
| P4.5 | EditScreen | GroupCard 分组，强度条，图标 Hero |
| P4.6 | AllPasswordsScreen | 字母分组，强弱标签，FAB |

### Phase 5 — 细节收尾

| 优先级 | 内容 |
|--------|------|
| P5.1 | TabBar：圆点动画，毛玻璃效果 |
| P5.2 | 页面过渡：`slideInHorizontally + fadeIn` |
| P5.3 | 主题切换：`Color.animated()` 全局接入 |
| P5.4 | 深色模式微调：各容器透明度校验 |

---

## 11. Kotlin/Compose 代码片段

本章提供可直接复制使用的完整代码。

### 11.1 Color.kt（完整版）

```kotlin
// Color.kt
import androidx.compose.ui.graphics.Color

// ── 浅色模式 ──
val Primary           = Color(0xFF5E5CE6)
val PrimaryDark       = Color(0xFF4B49C8)
val PrimaryGradStart  = Color(0xFF5E5CE6)
val PrimaryGradEnd    = Color(0xFF7B79F7)
val PrimaryLight      = Color(0x145E5CE6) // 8% opacity
val Success           = Color(0xFF30D158)
val Error             = Color(0xFFFF3B30)
val Warning           = Color(0xFFFF9F0A)
val Blue              = Color(0xFF007AFF)
val Cyan              = Color(0xFF32ADE6)

val BackgroundLight   = Color(0xFFF2F2F7)
val SurfaceLight      = Color(0xFFFFFFFF)
val Surface2Light     = Color(0xFFF2F2F7)
val Label1Light       = Color(0xFF000000)
val Label2Light       = Color(0xFF6C6C70)
val Label3Light       = Color(0xFFAEAEB2)
val Label4Light       = Color(0xFFD1D1D6)
val SeparatorLight    = Color(0x173C3C43) // rgba(60,60,67,0.09)
val TabBarBgLight     = Color(0xEBFFFFFF) // 92% white
val TabActiveLight    = Color(0xFF5E5CE6)
val TabInactiveLight  = Color(0xFFAEAEB2)

val SuccessContLight  = Color(0x1A30D158) // 10%
val ErrorContLight    = Color(0x1AFF3B30)
val WarningContLight  = Color(0x1AFF9F0A)
val BlueContLight     = Color(0x1A007AFF)

// ── 深色模式 ──
val PrimaryDarkMode   = Color(0xFF6E6CE8)
val PrimaryGradStartD = Color(0xFF6E6CE8)
val PrimaryGradEndD   = Color(0xFF8E8CF9)
val PrimaryLightDark  = Color(0x1E6E6CE8) // 12% opacity
val ErrorDark         = Color(0xFFFF453A)
val SuccessDark       = Color(0xFF30D158) // 与浅色相同，但显式声明以便复制
val WarningDark       = Color(0xFFFF9F0A)
val BlueDark          = Color(0xFF0A84FF)
val CyanDark          = Color(0xFF64D2FF)

val BackgroundDark    = Color(0xFF000000)
val SurfaceDark       = Color(0xFF1C1C1E)
val Surface2Dark      = Color(0xFF2C2C2E)
val Label1Dark        = Color(0xFFFFFFFF)
val Label2Dark        = Color(0xFF8E8E93)
val Label3Dark        = Color(0xFF636366)
val Label4Dark        = Color(0xFF48484A)
val SeparatorDark     = Color(0x5D545458) // rgba(84,84,88,0.36)
val TabBarBgDark      = Color(0xF21C1C1E) // 95% dark
val TabActiveDark     = Color(0xFF6E6CE8)
val TabInactiveDark   = Color(0xFF636366)

val SuccessContDark   = Color(0x2630D158) // 15%
val ErrorContDark     = Color(0x26FF453A)
val WarningContDark   = Color(0x26FF9F0A)
val BlueContDark      = Color(0x26007AFF)

// ── 固定色（不随主题变化）──
val ScoreCardBg       = Color(0xFF1C1C1E)
val IconBgBlue        = Color(0xFF007AFF)
val IconBgGreen       = Color(0xFF30D158)
val IconBgOrange      = Color(0xFFFF9F0A)
val IconBgPurple      = Color(0xFF5E5CE6)
val IconBgCyan        = Color(0xFF32ADE6)
val IconBgRed         = Color(0xFFFF3B30)
val IconBgGray        = Color(0xFF8E8E93)
val IconBgLightGreen  = Color(0xFF34C759)
```

### 11.2 ThemeColors.kt（数据类 + 扩展）

```kotlin
data class AppColors(
    val primary: Color,
    val primaryGradStart: Color,
    val primaryGradEnd: Color,
    val primaryLight: Color,
    val success: Color,
    val error: Color,
    val warning: Color,
    val blue: Color,
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val label1: Color,
    val label2: Color,
    val label3: Color,
    val label4: Color,
    val separator: Color,
    val tabBarBg: Color,
    val tabActive: Color,
    val tabInactive: Color,
    val successContainer: Color,
    val errorContainer: Color,
    val warningContainer: Color,
    val blueContainer: Color,
    val isDark: Boolean
)

val LightColors = AppColors(
    primary = Primary, primaryGradStart = PrimaryGradStart,
    primaryGradEnd = PrimaryGradEnd, primaryLight = PrimaryLight,
    success = Success, error = Error, warning = Warning, blue = Blue,
    background = BackgroundLight, surface = SurfaceLight,
    surface2 = Surface2Light, label1 = Label1Light,
    label2 = Label2Light, label3 = Label3Light, label4 = Label4Light,
    separator = SeparatorLight, tabBarBg = TabBarBgLight,
    tabActive = TabActiveLight, tabInactive = TabInactiveLight,
    successContainer = SuccessContLight, errorContainer = ErrorContLight,
    warningContainer = WarningContLight, blueContainer = BlueContLight,
    isDark = false
)

val DarkColors = AppColors(
    primary = PrimaryDarkMode, primaryGradStart = PrimaryGradStartD,
    primaryGradEnd = PrimaryGradEndD, primaryLight = PrimaryLightDark,
    success = SuccessDark, error = ErrorDark, warning = WarningDark, blue = BlueDark,
    background = BackgroundDark, surface = SurfaceDark,
    surface2 = Surface2Dark, label1 = Label1Dark,
    label2 = Label2Dark, label3 = Label3Dark, label4 = Label4Dark,
    separator = SeparatorDark, tabBarBg = TabBarBgDark,
    tabActive = TabActiveDark, tabInactive = TabInactiveDark,
    successContainer = SuccessContDark, errorContainer = ErrorContDark,
    warningContainer = WarningContDark, blueContainer = BlueContDark,
    isDark = true
)

val LocalAppColors = staticCompositionLocalOf { LightColors }

@Composable
fun appColors() = LocalAppColors.current
```

### 11.3 PasswordListItem 完整实现

```kotlin
@Composable
fun PasswordListItem(
    name: String,
    account: String,
    initial: String,
    gradientColors: List<Color>,
    lastUsed: String? = null,
    isWeak: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = appColors()
    PressableScale(onClick = onClick, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().height(68.dp)
                .background(colors.surface)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(
                        gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, style = TextStyle(
                    fontFamily = OutfitFamily, fontWeight = FontWeight.W700,
                    fontSize = 17.sp, color = Color.White
                ))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.W600,
                    color = colors.label1, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-0.15).sp)
                Text(account, fontSize = 13.sp, fontWeight = FontWeight.W400,
                    color = colors.label2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (lastUsed != null)
                    Text(lastUsed, fontSize = 11.sp, fontWeight = FontWeight.W500,
                        color = colors.label3)
                if (isWeak)
                    Text("弱", fontSize = 10.sp, fontWeight = FontWeight.W700,
                        color = Error,
                        modifier = Modifier.background(
                            colors.errorContainer, RoundedCornerShape(4.dp)
                        ).padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                modifier = Modifier.size(14.dp), tint = colors.label4)
        }
    }
}
```

### 11.4 SettingItem 彩色图标实现

```kotlin
@Composable
fun SettingItem(
    icon: ImageVector,
    iconBgColor: Color,      // 使用 Color.kt 中定义的 IconBgXxx 常量
    label: String,
    trailingText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = appColors()
    PressableScale(onClick = onClick, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(iconBgColor),   // 固定彩色，不随主题变化
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = Color.White)
            }
            Text(label, Modifier.weight(1f), fontSize = 15.sp,
                fontWeight = FontWeight.W600, color = colors.label1)
            if (trailingText != null)
                Text(trailingText, fontSize = 14.sp, fontWeight = FontWeight.W500,
                    color = colors.label2)
            Icon(Icons.Outlined.ChevronRight, null,
                Modifier.size(14.dp), tint = colors.label4)
        }
    }
}

// 使用示例
SettingItem(
    icon = Icons.Outlined.Fingerprint,
    iconBgColor = IconBgGreen,   // #30D158
    label = "指纹解锁",
    onClick = { /* navigate */ }
)
```

### 11.5 GroupCard 通用封装

```kotlin
@Composable
fun GroupCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = appColors()
    val shadowLayers = if (colors.isDark) ShadowL1Dark else ShadowL1Light
    Column(
        modifier = modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .multiShadow(*shadowLayers, cornerRadius = 18.dp),
        content = content
    )
}

// GroupCard 内分隔线（在各行的 drawBehind 中绘制）
fun Modifier.groupDivider(
    startX: Dp = 50.dp,
    color: Color
): Modifier = drawBehind {
    val y = 0f
    val startPx = startX.toPx()
    drawLine(
        color = color,
        start = Offset(startPx, y),
        end = Offset(size.width, y),
        strokeWidth = 0.5.dp.toPx()
    )
}
```

### 11.6 渐变工具函数

```kotlin
// 品牌渐变 Brush（用于 PasswordListItem 图标）
fun brandGradient(vararg colors: Color): Brush =
    Brush.linearGradient(
        colors = colors.toList(),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

// 主色渐变（140°）
val primaryGradient: Brush
    @Composable get() {
        val c = appColors()
        return Brush.linearGradient(
            colors = listOf(c.primaryGradStart, c.primaryGradEnd),
            start = Offset(0f, Float.POSITIVE_INFINITY),
            end = Offset(Float.POSITIVE_INFINITY, 0f)
        )
    }

// 常用品牌色渐变预设
val NetflixGradient = brandGradient(Color(0xFFE50914), Color(0xFFB5070E))
val GoogleGradient  = brandGradient(Color(0xFF4285F4), Color(0xFF2B6DE0))
val WeChatGradient  = brandGradient(Color(0xFF07C160), Color(0xFF05A050))
val AliGradient     = brandGradient(Color(0xFFFF6A00), Color(0xFFE05A00))
val GithubGradient  = brandGradient(Color(0xFF333333), Color(0xFF111111))
```

### 11.7 页面进入/退出动画

```kotlin
// 在 NavHost 中配置
NavHost(
    navController = navController,
    startDestination = "home",
    enterTransition = {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(300))
    },
    exitTransition = {
        slideOutHorizontally(
            targetOffsetX = { -it / 3 },
            animationSpec = tween(250, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(200))
    },
    popEnterTransition = {
        slideInHorizontally(
            initialOffsetX = { -it / 3 },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(300))
    },
    popExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(250, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(200))
    }
) { /* composables */ }
```

---


### 11.8 Dimens.kt（完整版）

```kotlin
// Dimens.kt
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── 间距 ──
val Spacing2  = 2.dp
val Spacing4  = 4.dp
val Spacing6  = 6.dp
val Spacing8  = 8.dp
val Spacing10 = 10.dp
val Spacing12 = 12.dp
val Spacing14 = 14.dp
val Spacing16 = 16.dp
val Spacing20 = 20.dp
val Spacing24 = 24.dp
val Spacing28 = 28.dp
val Spacing32 = 32.dp
val Spacing48 = 48.dp

// ── 圆角 ──
val Radius4  = 4.dp
val Radius8  = 8.dp
val Radius10 = 10.dp
val Radius12 = 12.dp
val Radius13 = 13.dp
val Radius14 = 14.dp
val Radius16 = 16.dp
val Radius17 = 17.dp
val Radius18 = 18.dp
val Radius20 = 20.dp
val Radius22 = 22.dp
val Radius24 = 24.dp
val Radius26 = 26.dp
val Radius34 = 34.dp

// ── 组件固定尺寸 ──
val TabBarHeight        = 68.dp
val TabBarMaxWidth      = 340.dp
val TabBarBottom        = 28.dp
val AddButtonSize       = 52.dp
val PasswordItemHeight  = 68.dp
val SettingItemHeight   = 56.dp
val SearchBarHeight     = 48.dp
val StatCardHeight      = 150.dp
val CategoryTagHeight   = 34.dp
val ActionButtonHeight  = 54.dp
val InputFieldHeight    = 56.dp
val NavBarHeight        = 52.dp
val BackButtonSize      = 34.dp
val SettingIconSize     = 36.dp
val PasswordIconSize    = 42.dp
val EditHeroIconSize    = 80.dp
val LockIconOuterRing   = 72.dp
val LockIconInnerRing   = 56.dp
val FabSize             = 56.dp
val FabCorner           = 18.dp
```

### 11.9 阴影预设完整集（Level 2 / Level 4 / Level 5）

```kotlin
// 补充 Level 2 Elevated
val ShadowL2Light = arrayOf(
    ShadowSpec(offsetY = 2.dp, blurRadius = 8.dp,  color = Color(0x0F000000)),
    ShadowSpec(offsetY = 8.dp, blurRadius = 24.dp, color = Color(0x14000000))
)
val ShadowL2Dark = arrayOf(
    ShadowSpec(offsetY = 2.dp, blurRadius = 8.dp,  color = Color(0x66000000)),
    ShadowSpec(offsetY = 8.dp, blurRadius = 24.dp, color = Color(0x80000000))
)

// Level 4 TabBar
val ShadowL4Light = arrayOf(
    ShadowSpec(offsetY = (-1).dp, blurRadius = 0.dp, color = Color(0x0A000000)),
    ShadowSpec(offsetY = 8.dp,    blurRadius = 32.dp, color = Color(0x1A000000))
)
val ShadowL4Dark = arrayOf(
    ShadowSpec(offsetY = (-1).dp, blurRadius = 0.dp, color = Color(0x0FFFFFFF)),
    ShadowSpec(offsetY = 8.dp,    blurRadius = 32.dp, color = Color(0x80000000))
)

// Level 5 IconGlow（参数化品牌色）
fun shadowL5Glow(accentColor: Color) = arrayOf(
    ShadowSpec(offsetY = 4.dp, blurRadius = 12.dp, color = accentColor.copy(alpha = 0.30f)),
    ShadowSpec(offsetY = 8.dp, blurRadius = 28.dp, color = accentColor.copy(alpha = 0.18f))
)

// 使用示例
Modifier.multiShadow(*shadowL5Glow(Color(0xFFE50914)), cornerRadius = 22.dp) // Netflix红光
```

### 11.10 StrengthBadge 小组件

```kotlin
@Composable
fun StrengthBadge(isStrong: Boolean) {
    val colors = appColors()
    val text = if (isStrong) "强" else "弱"
    val textColor = if (isStrong) Success else Error
    val bgColor = if (isStrong) colors.successContainer else colors.errorContainer

    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.W700,
        color = textColor,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(Radius4))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
```


## 附录：快速参考卡

### A. 最常用颜色（直接复制使用）

```
浅色背景：#F2F2F7    深色背景：#000000
浅色卡片：#FFFFFF    深色卡片：#1C1C1E
主色：#5E5CE6        深色主色：#6E6CE8
成功：#30D158        错误：#FF3B30    警告：#FF9F0A
主文字（浅）：#000   主文字（深）：#FFF
次文字（浅）：#6C6C70  次文字（深）：#8E8E93
```

### B. 最常用圆角

```
列表项：18dp（GroupCard）/ 13dp（图标）
按钮：16dp（主）/ 14dp（次）
设置图标：10dp    卡片：18-22dp
TabBar：34dp      全圆：50%
```

### C. 字体速查

```
大标题：Outfit W700 34sp   导航标题：Outfit W700 18sp
列表主文字：Inter W600 15sp  列表副文字：Inter W400 13sp
按钮：Inter W700 16sp      Section标题：Inter W700 12sp 全大写
统计数字：Outfit W700 22sp  评分数字：Outfit W800 64sp
```

---

*文档版本：v1.0 · 生成时间：2026-06-25 · 适用范围：REPassCard Android（不含 CloudSyncScreen）*
