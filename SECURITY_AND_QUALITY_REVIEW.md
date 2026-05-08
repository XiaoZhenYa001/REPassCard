# REPassCard 项目问题分析与修复建议

本文档基于 2026-05-08 对当前项目的静态审查和本地构建验证整理，目的是为后续修复提供可直接执行的路线图。

验证结果：

- `.\gradlew.bat assembleDebug`：通过。
- `.\gradlew.bat test`：失败，原因是 JUnit 依赖被注释，但 `ExampleUnitTest.java` 仍引用 `org.junit`。

当前结论：

- 未发现典型的无限增长型内存泄漏。
- 作为密码管理器，当前存在多项严重安全和数据可靠性问题，优先级高于普通 UI/交互问题。

## P0 严重问题

### 1. 密码明文存储在 Room 数据库

相关位置：

- `app/src/main/java/com/example/passcard/data/PasswordEntity.kt`
- `app/src/main/java/com/example/passcard/data/AppDatabase.kt`
- `app/src/main/java/com/example/passcard/data/PasswordRepository.kt`
- `app/src/main/java/com/example/passcard/ui/screens/MainScreen.kt`

现状：

- `PasswordEntity.password` 是普通 `String` 字段。
- Room 数据库使用普通 SQLite 文件，未看到 SQLCipher、Jetpack Security Crypto 或字段级加密。
- About 页面中宣称支持 `AES-256 Local Encryption`，但实际代码没有实现对应加密能力。

影响：

- 设备被 root、备份文件泄露、调试提取、恶意软件读取应用私有目录、或物理访问攻击时，用户保存的所有密码可能被直接读取。
- UI 宣称与真实安全能力不一致，容易误导用户。

建议修复方案：

1. 引入真正的加密存储方案。
   - 推荐方案 A：使用 SQLCipher for Android，对整个 Room 数据库加密。
   - 推荐方案 B：保留普通 Room，但对敏感字段做字段级 AES-GCM 加密。
   - 对密码管理器来说，整体数据库加密更直接，字段级加密更灵活但更容易遗漏字段。

2. 使用主密码派生数据库密钥。
   - 不要直接把主密码作为加密 key。
   - 使用 Argon2id、scrypt 或 PBKDF2-HMAC-SHA256 派生密钥。
   - 每个用户/vault 必须有随机 salt。
   - KDF 参数应可版本化，便于未来升级。

3. 数据模型应区分明文 UI 模型和密文数据库模型。
   - 例如数据库实体中使用 `encryptedPassword`、`passwordNonce`、`passwordTag` 或统一密文字段。
   - Repository 层负责加解密，UI 层尽量不感知密文细节。

4. 迁移已有明文数据。
   - 首次打开新版本时要求用户输入/设置主密码。
   - 成功派生密钥后读取旧明文数据库，写入加密数据库或加密字段。
   - 迁移成功后删除旧明文字段或旧数据库。
   - 迁移必须可回滚，失败时不能丢数据。

5. 修正 About/安全中心文案。
   - 在真正实现加密前，不应展示 `AES-256 Local Encryption`。
   - 实现后再恢复文案，并说明加密范围。

验收标准：

- 数据库文件中不能直接搜索到用户密码明文。
- 主密码错误时无法解密 vault。
- 升级旧版本数据后密码仍完整。
- 添加自动化测试验证加密、解密、错误密码、迁移失败场景。

### 2. 主密码使用无盐 SHA-256 哈希

相关位置：

- `app/src/main/java/com/example/passcard/util/PreferencesManager.kt`

现状：

- `setMasterPassword()` 将主密码做一次 SHA-256 后存入 SharedPreferences。
- 没有 salt。
- 没有慢哈希/KDF。
- 主密码只用于验证 UI 解锁，不参与数据加密。

影响：

- 如果 SharedPreferences 泄露，攻击者可以用字典或 GPU 快速爆破主密码。
- 即使主密码强度较低，也没有 KDF 成本保护。
- 生物识别解锁只是绕过 UI 验证，不能保护真实数据密钥。

建议修复方案：

1. 改为 KDF 派生。
   - 优先 Argon2id。
   - 如果 Android 依赖选择受限，可先使用 PBKDF2WithHmacSHA256，但迭代次数要足够高，并保存参数版本。

2. 保存以下元数据，而不是只保存一个 SHA-256 字符串。
   - `kdfAlgorithm`
   - `kdfVersion`
   - `salt`
   - `iterations` 或 Argon2 参数
   - `passwordVerifier`

3. 不只做登录校验，还要派生 vault key。
   - 主密码输入正确后，KDF 输出用于解密数据库密钥或直接解密 vault。
   - 推荐设计：随机生成 vault data key，用主密码派生 key 加密 data key。

4. 生物识别应保护密钥，而不是只保存一个布尔值。
   - 当前 `biometricEnabled` 是 SharedPreferences 中的布尔开关。
   - 更安全的方案是用 Android Keystore 创建需要用户认证的 key，用它加密 vault data key。
   - 生物识别通过后只释放/解密 data key，不应绕过加密体系。

验收标准：

- SharedPreferences 中不再出现简单 SHA-256 主密码哈希。
- 相同主密码在不同安装/不同用户下 verifier 不相同。
- 密码错误时无法获得 vault key。
- 生物识别关闭后无法继续使用旧的生物识别 key 解锁。

### 3. 冷启动数据库初始化存在竞态

相关位置：

- `app/src/main/java/com/example/passcard/PassCardApp.kt`
- `app/src/main/java/com/example/passcard/ui/MainViewModel.kt`

现状：

- `PassCardApp.onCreate()` 使用裸 `Thread` 异步初始化数据库。
- `MainViewModel.loadData()` 只调用一次 `PassCardApp.getDatabase()`。
- 如果 ViewModel 创建时数据库线程还没完成，`db == null` 后会设置 `initialized = true` 并直接返回。
- 后续 `repository` 可能永久为 null，新增、导入、删除操作会静默无效。

影响：

- 冷启动时可能出现空列表。
- 用户新增密码可能没有保存。
- 导入操作可能没有实际写入。
- 问题具有时序性，不容易稳定复现。

建议修复方案：

1. 去掉 `PassCardApp` 中裸 `Thread` 预初始化。
   - Room 的 `databaseBuilder(...).build()` 本身创建实例很轻。
   - 让 `AppDatabase.getInstance(applicationContext)` 在需要时同步返回单例。

2. ViewModel 中直接创建 repository。
   - 不要依赖一个可能为 null 的全局 `getDatabase()`。
   - 示例：

```kotlin
val db = AppDatabase.getInstance(application)
repository = PasswordRepository(db.passwordDao())
```

3. 如果确实需要异步初始化，暴露 `suspend fun getDatabase()` 或依赖注入容器。
   - ViewModel 应等待数据库可用，而不是失败后标记 initialized。

4. 所有写操作要处理 repository 未初始化状态。
   - 更好的方式是让 repository 非空。
   - 如果初始化失败，应向 UI 暴露错误状态，不应静默失败。

验收标准：

- 冷启动后密码列表稳定加载。
- 立即新增密码不会丢失。
- 通过单元测试或 instrumentation test 模拟数据库初始化慢的情况。

### 4. `fallbackToDestructiveMigration()` 可能清空用户密码

相关位置：

- `app/src/main/java/com/example/passcard/data/AppDatabase.kt`

现状：

- Room 配置了 `fallbackToDestructiveMigration()`。
- 数据库 `exportSchema = false`。

影响：

- 一旦数据库版本升级但没有 migration，Room 会删除旧数据库并重建。
- 对密码管理器来说，这等价于版本升级导致用户 vault 丢失。

建议修复方案：

1. 移除 `fallbackToDestructiveMigration()`。
2. 将 `exportSchema` 改为 `true`。
3. 配置 schema 导出路径。
4. 每次修改数据库结构时提供明确 migration。
5. 增加 migration test。

示例方向：

```kotlin
@Database(
    entities = [PasswordEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase()
```

验收标准：

- 从旧版本升级到新版本不会删除用户数据。
- migration 测试覆盖至少一个版本升级路径。
- CI 中运行 migration 测试。

## P1 高优先级问题

### 5. 明文导出 CSV/JSON

相关位置：

- `app/src/main/java/com/example/passcard/ui/screens/MainScreen.kt`
- `app/src/main/java/com/example/passcard/util/CsvExporter.kt`
- `app/src/main/java/com/example/passcard/util/JsonExporter.kt`
- `app/src/main/res/xml/file_paths.xml`

现状：

- 导出时直接写入明文密码。
- 文件生成在 `cacheDir/exports`。
- 通过 FileProvider 分享给其他应用。
- 导出完成后没有清理缓存文件。
- UI 只有普通成功提示，没有足够强的安全确认。

影响：

- 用户可能把明文密码文件分享到不可信应用。
- 缓存目录中会残留明文导出文件，直到系统或应用清理。
- 如果其他组件或调试环境能访问缓存，风险增加。

建议修复方案：

1. 导出前增加高强度确认。
   - 明确说明：导出文件包含所有明文密码。
   - 要求用户再次输入主密码或通过生物识别确认。

2. 支持加密导出。
   - 默认导出加密 JSON。
   - 用户输入导出密码。
   - 使用 AES-GCM 加密导出内容。
   - 文件中保存 KDF 参数、salt、nonce、ciphertext。

3. 明文导出只能作为高级选项。
   - 明文导出入口应隐藏在二次确认后。
   - 文件名中可以包含 `plain-text` 警示。

4. 分享后清理缓存。
   - 如果使用 `ACTION_SEND`，无法准确知道目标应用何时读取完成。
   - 可在延迟一段时间后清理旧导出文件。
   - 每次导出前清理超过一定时间的旧文件。

5. 导入时识别加密导出格式。
   - 支持加密 JSON 导入。
   - 用户输入导出密码后解密再导入。

验收标准：

- 默认导出不产生明文密码文件。
- 明文导出必须经过明确确认。
- `cacheDir/exports` 不长期保留旧明文文件。

### 6. 剪贴板逻辑持有明文密码和 Context

相关位置：

- `app/src/main/java/com/example/passcard/util/ClipboardHelper.kt`
- `app/src/main/java/com/example/passcard/ui/components/PasswordListItem.kt`

现状：

- `ClipboardHelper` 是 object 单例。
- `lastCopiedText` 保存最近复制的明文密码。
- delayed `Runnable` 捕获传入的 `context`。
- 如果传入的是 Activity context，会在延迟期间持有 Activity。
- 默认 `clipboardClearEnabled = false`，复制后不自动清空。

影响：

- 明文密码会在应用内存中额外驻留一段时间。
- Activity 可能被 Handler/Runnable 短暂持有，形成轻量生命周期泄漏。
- 用户复制密码后，剪贴板可能长期保留敏感内容。

建议修复方案：

1. 只使用 `applicationContext`。
   - 在 `copyToClipboard()` 开头转换：

```kotlin
val appContext = context.applicationContext
```

2. 避免长期保存完整明文。
   - 可保存复制时的随机 token 或 hash，用于匹配时减少明文驻留。
   - 注意 hash 也不是完全无风险，但比单例保存明文更好。

3. 默认开启自动清理。
   - 将默认值改为 true。
   - 默认清理时间可以设为 30 秒或 60 秒。

4. 支持应用退出/锁屏时主动清理。
   - MainActivity `onStop()` 或自动锁定时调用清理方法。
   - 注意只清理仍匹配本应用复制内容的剪贴板，避免覆盖用户新复制内容。

5. Android 13+ 可以使用敏感剪贴板标记。
   - 设置 `ClipDescription.EXTRA_IS_SENSITIVE`，减少系统预览暴露。

验收标准：

- delayed runnable 不持有 Activity context。
- 复制密码后到期会清理。
- 应用进入后台或锁定时能尽量清理本应用复制的敏感内容。

### 7. 应用后台后未自动重新上锁，且未禁止截图

相关位置：

- `app/src/main/java/com/example/passcard/MainActivity.kt`

现状：

- `isUnlocked` 只保存在 Compose `remember` 状态中。
- 没有看到 `onPause`、`onStop` 后自动锁定。
- 没有设置 `WindowManager.LayoutParams.FLAG_SECURE`。

影响：

- 用户切到后台后，回到应用可能仍然保持解锁。
- 最近任务预览或截图可能暴露密码列表、账号信息。

建议修复方案：

1. 增加自动锁定策略。
   - 进入后台立即锁定，或超过 N 秒锁定。
   - 设置页可提供选项：立即、30 秒、1 分钟、5 分钟。

2. 将解锁状态放入可控的 session manager。
   - 避免散落在 Activity 的局部 Compose 状态。
   - 记录 `lastBackgroundAt`。

3. 启用 `FLAG_SECURE`。
   - 对密码管理器建议默认开启。
   - 如果需要用户可配置，也应默认开启。

示例方向：

```kotlin
window.setFlags(
    WindowManager.LayoutParams.FLAG_SECURE,
    WindowManager.LayoutParams.FLAG_SECURE
)
```

验收标准：

- 应用进入后台后按策略重新锁定。
- 最近任务缩略图不显示敏感内容。
- 系统截图被阻止或显示空白。

### 8. 默认插入示例账号和示例密码

相关位置：

- `app/src/main/java/com/example/passcard/ui/MainViewModel.kt`

现状：

- 数据库为空时自动插入 Google、Netflix、Facebook 等示例数据。
- 示例数据中包含看似真实的邮箱和密码。

影响：

- 用户可能误以为是真实数据或产品内置账号。
- 会污染真实 vault。
- 安全扫描或导出时会把示例密码也导出。

建议修复方案：

1. 正式构建中移除自动插入示例数据。
2. 如果需要演示数据，只在 debug build 或预览模式启用。
3. 首次使用时显示空状态，引导用户添加第一条密码。

验收标准：

- 新安装正式版本数据库为空。
- 用户不操作时不会自动生成任何密码条目。

## P2 中优先级问题

### 9. 安全中心数据硬编码

相关位置：

- `app/src/main/java/com/example/passcard/ui/screens/MainScreen.kt`
- `app/src/main/java/com/example/passcard/ui/screens/SecurityScreen.kt`

现状：

- 安全分数固定为 85。
- 密码总数固定为 142。
- 弱密码固定为 3。
- 重复使用固定为 12。
- compromised 文案也是假数据。

影响：

- 安全中心无法反映用户真实风险。
- 可能让用户误以为密码状态良好。

建议修复方案：

1. 使用真实密码列表计算指标。
   - 总数：`passwords.size`
   - 弱密码：长度短、只含数字、常见弱密码、缺少复杂度等。
   - 重复密码：按密码值或加密前的安全比较统计。
   - 空密码/空账号：提示补全。

2. 谨慎处理泄露检测。
   - 如果使用 Have I Been Pwned k-anonymity API，要只发送 SHA-1 前缀，不能上传明文密码。
   - 如果没有实现泄露检测，不要显示“已发现泄露”。

3. 安全分数要可解释。
   - 分数由哪些项扣分，应在 UI 中能对应到具体建议。

验收标准：

- 新增/修改/删除密码后安全中心数据同步变化。
- 没有实现的检测项不显示假结果。

### 10. 测试配置损坏

相关位置：

- `app/build.gradle.kts`
- `app/src/test/java/com/example/passcard/ExampleUnitTest.java`
- `app/src/test/java/com/example/passcard/ExampleUnitTest.kt`

现状：

- `testImplementation(libs.junit)` 被注释。
- Java 测试文件仍使用 JUnit。
- `.\gradlew.bat test` 会失败。
- `tasks.register<Delete>("testClasses") { enabled = false }` 不能真正解决测试依赖问题。

影响：

- CI 或本地测试无法作为质量门禁。
- 后续修复加密、迁移、导入导出时缺少回归保护。

建议修复方案：

1. 恢复测试依赖。
   - 取消注释 `testImplementation(libs.junit)`。
   - 如需 AndroidX 测试，也恢复对应依赖。

2. 删除重复/占位测试。
   - 保留 Kotlin 或 Java 其中一种。
   - 删除无意义 placeholder。

3. 增加关键测试。
   - 主密码 KDF。
   - 加密/解密。
   - 错误密码不能解密。
   - Room migration。
   - CSV/JSON import parser。
   - 导出清理逻辑。

验收标准：

- `.\gradlew.bat test` 通过。
- 至少覆盖核心安全和数据迁移逻辑。

### 11. Release 未开启混淆/压缩

相关位置：

- `app/build.gradle.kts`

现状：

- release 中 `isMinifyEnabled = false`。

影响：

- 代码更容易被反编译分析。
- 包体更大。
- 对密码管理器来说，虽然混淆不能替代安全设计，但应作为基础加固措施。

建议修复方案：

1. release 开启 minify。
2. 配置 R8/ProGuard 规则。
3. 确认 Room、Compose、Biometric、加密库规则不被误删。

验收标准：

- release 构建通过。
- 核心功能在混淆后正常运行。

### 12. 文本编码/乱码问题

相关位置：

- 多个 Kotlin 文件和 UI 文案中存在明显乱码。

现状：

- 文件中大量中文注释和字符串显示为乱码。
- 例如 `娆㈣繋鍥炴潵`、`瀵嗙爜` 等。

影响：

- 中文 UI 文案不可用。
- 后续维护困难。
- 可能导致字符串匹配、导入表头识别等逻辑失效。

建议修复方案：

1. 统一项目编码为 UTF-8。
2. 将中文 UI 文案移入 `strings.xml`。
3. 修复 `AppStrings` 和各屏幕硬编码文案。
4. CSV/JSON 导入表头匹配应使用正确中文集合。

验收标准：

- 中文界面显示正常。
- 源码文件在 IDE 和 Gradle 编译中均按 UTF-8 处理。
- 中文 CSV 表头能正确识别。

## 建议修复顺序

1. 修复数据库初始化竞态，确保数据读写稳定。
2. 移除示例数据自动插入，避免污染真实 vault。
3. 恢复测试配置，让 `.\gradlew.bat test` 可运行。
4. 设计并实现主密码 KDF 与 vault key 管理。
5. 实现数据库或字段级加密，并迁移旧明文数据。
6. 移除 `fallbackToDestructiveMigration()`，建立 Room migration 机制。
7. 修复剪贴板清理、后台自动锁定、`FLAG_SECURE`。
8. 改造导入导出，默认使用加密导出。
9. 将安全中心改为真实数据计算。
10. 修复中文乱码和资源管理。
11. release 开启 R8 混淆并做回归测试。

## 最小可行修复里程碑

如果时间有限，建议先完成以下最小安全闭环：

1. 数据库初始化不再可能返回永久 null。
2. 删除默认示例密码。
3. 主密码改为 PBKDF2/Argon2id + salt。
4. 密码字段 AES-GCM 加密入库。
5. 进入后台自动锁定，并启用 `FLAG_SECURE`。
6. 导出前增加强确认，导出后清理缓存文件。
7. 恢复 `.\gradlew.bat test`。

完成这些后，项目才比较接近一个可继续迭代的密码管理器基础版本。

