# REPassCard 云同步与加密备份设计方案

本文档记录 REPassCard 第一版云同步与加密备份功能的设计方案。目标是支持用户自带对象存储账号，在不依赖轻量后端的前提下，实现端到端加密备份与恢复。

当前版本本地数据库仍是明文；云同步设计是下一阶段重构；需要先完成本地 vault/密钥体系，再接云同步。

## 目标

- 支持手动“加密送至云端”。
- 支持手动“从云端获取”。
- 云端只保存密文，不保存任何明文密码。
- 用户使用自己的对象存储账号，例如腾讯云 COS、阿里云 OSS、S3 兼容存储。
- App 本地生成 Vault 加密密钥，用于加密密码库。
- 对象存储访问密钥只用于上传/下载密文文件，不参与密码库加密。
- 下载覆盖本地前，自动生成一份本地备份。
- 云端历史备份最多保留最近 7 份。
- 每份历史备份支持非敏感自动摘要，默认由 App 根据变更内容生成。

## 非目标

第一版暂不实现：

- 后端服务。
- 自动后台同步。
- 多设备实时同步。
- 删除记录的复杂合并。
- 云端明文导出。
- 设置界面使用帮助改造。

## 总体架构

```text
Android App
  |
  |-- VaultCrypto
  |     |-- 负责序列化密码库
  |     |-- 负责加密/解密 vault
  |     |-- 负责恢复密钥派生
  |
  |-- CloudSyncRepository
  |     |-- 组织上传、下载、备份、冲突处理
  |
  |-- CloudStorageClient
        |-- TencentCosClient
        |-- AliOssClient
        |-- S3CompatibleClient
```

云端对象存储只保存密文对象：

```text
vault/current/vault.enc
vault/current/manifest.json
vault/backups/20260508_214500.enc
vault/backups/20260508_220312.enc
```

## 密钥设计

### Vault 加密密钥

Vault 加密密钥用于加密和解密密码库。

设计要求：

- 在本地生成。
- 使用 24 个助记词形式展示给用户。
- 只显示一次。
- 用户必须自行保存。
- 用户丢失该密钥后，云端密文无法恢复。
- 云端永远不保存该密钥。

交互要求：

- 生成前明确提示：密钥丢失后无法恢复云端数据。
- 生成后要求用户确认已保存。
- 可要求用户输入指定位置的词，例如第 3、8、17 个词，以确认已经保存。
- 支持导入已有 Vault 加密密钥，用于换机恢复。
- 提供两种使用模式供用户权衡（Trade-off）：
  - 极致安全模式：每次同步都必须输入助记词，助记词和派生密钥只驻留内存，不落盘。
  - 便捷使用模式：在本地使用用户的指纹/面容解锁后，加密存储派生密钥；后续点击同步时只需通过生物识别验证即可释放密钥。
- 便捷使用模式的目标是提升云同步操作频率和使用意愿，但其安全边界低于极致安全模式。
- 无论选择哪种模式，用户都应清楚知道：助记词丢失将无法恢复云端密文。

缓存策略：

- 极致安全模式下，首次输入助记词验证通过后，派生的加密密钥缓存在 App 进程内存中。
- App 切到后台超过锁屏超时时间后清除内存缓存。
- App 进程被杀死后缓存自动消失。
- 不将助记词或派生密钥持久化到磁盘或 Android Keystore。
- 用户需要重新输入助记词的时机：App 重启、锁屏超时、手动锁定。
- 便捷使用模式下，可以使用 Android Keystore 保护一个由主助记词派生出的本地包裹密钥，并通过生物识别解锁该包裹密钥，再释放 vaultDataKey 或其等效解密能力。
- 便捷使用模式中，生物识别仅用于解锁本地加密存储的密钥材料，不应绕过 vault 的加密边界。

选择内存缓存而非持久化存储的理由：

- 端到端加密的核心前提是密钥不落盘。如果将派生密钥存入 Android Keystore，设备被完全控制时攻击者可以提取密钥，等同于降级为设备级加密。
- 内存缓存在一次会话中只需输入一次助记词，多次上传/下载操作时体验可接受。
- 与主密码锁屏机制配合：锁屏后清除缓存，解锁后如需同步操作再次输入助记词。
- 便捷使用模式将“安全性”与“可用性”做了权衡，适合更高频的同步场景，但不应作为默认唯一方案。

### 对象存储访问密钥

对象存储访问密钥用于访问腾讯云 COS、阿里云 OSS 或 S3 兼容对象存储。

设计要求：

- 只用于上传/下载密文文件。
- 不参与密码库加密。
- 推荐用户使用子账号/子用户 AccessKey。
- 不推荐用户填写主账号密钥。
- 保存到本机时必须使用 Android Keystore 加密保存。
- Keystore 只能保护“静态存储的凭证”，不能替代主 vault 密钥保护。

安全提示：

```text
推荐使用云厂商子账号/子用户密钥。
请勿填写主账号密钥。
建议只授予当前 Bucket 和指定路径的对象读写权限。
```

## 子账号说明

云厂商主账号权限通常很大，可能管理所有云资源、账单、服务器、数据库和对象存储。如果用户将主账号 `SecretId/SecretKey` 填入 App，一旦设备或本地配置泄露，风险范围会非常大。

子账号/子用户是为某个应用单独创建的低权限访问身份。建议只授予 REPassCard 所需的最小对象存储权限。

推荐权限范围：

```text
只能访问指定 bucket
只能访问指定路径前缀，例如 repasscard/
允许读取对象
允许上传对象
允许覆盖对象
允许列出指定路径下对象
允许删除指定路径前缀下的对象，例如 repasscard/ 下的旧备份
不允许管理账单
不允许创建服务器
不允许访问其他云服务
不允许删除 bucket 本身
```

即使子账号密钥泄露，攻击者最多只能访问 REPassCard 的密文备份文件，不能直接读取明文密码。

## 云存储平台

第一版设计为多平台，但业务层通过统一接口访问。

优先支持顺序：

1. S3 兼容接口抽象。
2. 腾讯云 COS。
3. 阿里云 OSS。

统一接口示例：

```kotlin
interface CloudStorageClient {
    suspend fun testConnection(): Result<Unit>
    suspend fun uploadObject(key: String, bytes: ByteArray): Result<CloudObjectMeta>
    suspend fun downloadObject(key: String): Result<ByteArray>
    suspend fun getObjectMeta(key: String): Result<CloudObjectMeta>
    suspend fun listObjects(prefix: String): Result<List<CloudObjectMeta>>
    suspend fun deleteObject(key: String): Result<Unit>
}
```

平台配置字段：

```text
platform: TencentCOS / AliOSS / S3Compatible
region
endpoint
bucket
objectPrefix
accessKeyId / secretId
accessKeySecret / secretKey
saveCredentialLocally
```

对象路径前缀规则：

- 默认值为 `repasscard/`。
- 普通用户无需修改。
- 高级设置中允许用户自定义。
- App 需在保存前自动规范化前缀格式，例如自动补齐末尾 `/`。
- 同一设备后续同步必须始终使用同一个前缀，否则视为不同云端空间。

## 云端文件格式

### vault.enc

`vault.enc` 是加密后的完整密码库。

建议格式：

```json
{
  "format": "repasscard-vault",
  "version": 1,
  "kdf": {
    "name": "argon2id",
    "salt": "base64-encoded-random-salt",
    "params": {
      "memory": 65536,
      "iterations": 3,
      "parallelism": 1,
      "hashLength": 32,
      "outputEncoding": "base64"
    }
  },
  "wrappedDataKey": {
    "algorithm": "AES-256-GCM",
    "nonce": "base64-encoded-nonce",
    "ciphertext": "base64-encoded-encrypted-vault-data-key",
    "tag": "base64-encoded-tag"
  },
  "crypto": {
    "algorithm": "AES-256-GCM",
    "nonce": "base64-encoded-nonce",
    "ciphertext": "base64-encoded-encrypted-vault-payload",
    "tag": "base64-encoded-tag"
  },
  "metadata": {
    "deviceId": "...",
    "deviceName": "...",
    "createdAt": 1778248992000,
    "updatedAt": 1778248992000,
    "vaultRevision": 42,
    "itemCount": 36,
    "keyVersion": 1,
    "kdfVersion": 1,
    "formatVersion": 1
  }
}
```

字段说明：

- `format`：用于识别文件类型。
- `version`：vault 文件结构版本，便于未来整体升级。
- `kdf.name`：当前使用的 KDF 算法名称，第一版优先 `argon2id`。
- `kdf.salt`：每个 vault 独立随机生成的盐值。
- `kdf.params`：KDF 参数，便于未来调整。
- `kdf.params.hashLength`：派生输出长度，建议 32 字节。
- `kdf.params.outputEncoding`：派生结果的编码方式，便于跨平台实现一致性。
- `wrappedDataKey`：使用 KEK 包裹的 vaultDataKey。
- `crypto`：使用 vaultDataKey 加密后的 vault payload。
- `metadata.keyVersion`：当前恢复密钥版本号。
- `metadata.kdfVersion`：KDF 参数版本号，用于兼容旧数据。
- `metadata.formatVersion`：文件格式版本号的镜像字段，便于实现层快速判断。


### manifest.json

`manifest.json` 保存非敏感同步元数据。

注意：manifest 中不能保存明文密码、账号、邮箱、手机号、备注等敏感字段。

备份摘要只使用 App 自动生成的非敏感摘要，例如“新增 N 条，删除 M 条，修改 K 条”。第一版不提供用户自由输入备注，避免用户不小心把账号、服务名、银行卡、手机号等敏感信息写入明文 manifest。

建议格式：

```json
{
  "format": "repasscard-manifest",
  "version": 1,
  "current": {
    "objectKey": "vault/current/vault.enc",
    "updatedAt": 1778248992000,
    "vaultRevision": 42,
    "etag": "...",
    "itemCount": 36,
    "deviceId": "...",
    "deviceName": "Pixel 8"
  },
  "backups": [
    {
      "backupId": "20260508_220312",
      "objectKey": "vault/backups/20260508_220312.enc",
      "createdAt": 1778248992000,
      "itemCount": 42,
      "summary": "新增 2 条，删除 1 条，修改 3 条",
      "isAutoGeneratedSummary": true,
      "keyVersion": 1
    }
  ]
}
```

## 加密方案

推荐：

- 内容加密：AES-256-GCM。
- 恢复密钥形式：24 个助记词。
- 密钥派生：Argon2id 优先；如果依赖受限，可暂用 PBKDF2WithHmacSHA256。
- 每次加密使用随机 nonce。
- 每个 vault 使用随机 salt。
- 密文格式必须带版本号，方便未来升级。

推荐密钥模型：

```text
24 个助记词 (256-bit 熵)
  -> Argon2id(salt, memory=64MB, iterations=3, parallelism=1)
  -> Key Encryption Key (KEK)
  -> KEK 加密/解密 vaultDataKey（保存为 wrappedDataKey）
  -> vaultDataKey 加密/解密密码库明文内容
```

说明：

- `vaultDataKey` 是首次创建 Vault 时随机生成的 256-bit 密钥。
- `vaultDataKey` 被 KEK 加密后以 `wrappedDataKey` 形式保存在 `vault.enc` 中。
- 每次加密密码库时使用 `vaultDataKey`，每次加密使用随机 nonce。
- 更换恢复密钥（助记词）时，只需用旧 KEK 解开 `vaultDataKey`，再用新 KEK 重新加密 `vaultDataKey`，无需重新加密全部密码数据。
- `keyVersion` 字段记录当前使用的密钥版本，方便识别历史备份对应的密钥。

## 上传流程：加密送至云端

1. 用户点击“加密送至云端”。
2. App 检查是否已生成或导入 Vault 加密密钥。
3. App 检查对象存储平台配置是否完整。
4. 如用户启用了本机保存云访问凭证，使用 Android Keystore 解密凭证。
5. 如用户未保存凭证，要求用户本次输入。
6. 上传前读取云端最新 `manifest.json`。
7. 比较本地记录的 `vaultRevision/etag` 与云端最新 `vaultRevision/etag`。
8. 如果不一致，说明云端已被其他设备更新，进入冲突处理流程，不直接覆盖。
9. 从本地数据库读取密码数据。
10. 序列化为 vault payload。
11. 使用 Vault 加密密钥加密 payload。
12. 生成非敏感备份摘要。
13. 上传新的历史备份到 `vault/backups/{timestamp}.enc`。
14. 上传当前版本到 `vault/current/vault.enc`。
15. 更新 `vault/current/manifest.json`（此步为提交点）。
16. 根据新 manifest 保留最近 7 份历史备份，清理 manifest 不再引用的旧备份对象。
17. 本地保存最近同步状态。

初始化同步流向规则：

- 新设备首次生成本地密钥后，如果本地密码库为空，不能直接把空库当作“最新版本”推送到云端。
- 当本地首次同步时，App 必须先读取云端 `manifest.json` 并判断云端是否已有数据。
- 如果云端已存在非空数据，而本地是空库，默认应判定为“从云端拉取/恢复”，而不是“用空库覆盖云端”。
- 只有在用户明确选择“使用本地空库覆盖云端”并二次确认后，才允许执行空库推送。
- 对于 `vaultRevision` 从 0 开始的情况，不能仅依赖 revision 数值判断是否可覆盖；必须结合云端是否存在 current manifest、当前条目数、首次初始化状态和显式用户选择一起判断。
- 如果本地与云端都为空，则可将首次上传视为初始化同步，但仍应保留完整确认流程。

原子性说明：

- `manifest.json` 是整个上传流程的提交点。只有 manifest 更新成功，本次上传才视为完成。
- 如果在 manifest 更新之前 App 崩溃或网络中断，云端状态仍然以旧 manifest 为准，不影响数据一致性。
- 清理旧备份必须发生在 manifest 更新成功之后。这样即使清理失败，也只是残留未引用的旧对象，不会导致 manifest 指向已删除的备份。
- 下次上传时检测到 manifest 与实际文件不一致，可以安全地重新上传覆盖或清理未引用对象。

网络错误与重试策略：

- 单次上传/下载请求失败后自动重试，最多 3 次，重试间隔指数退避（1s、2s、4s）。
- 超过重试次数后向用户报告具体错误（网络不可达、鉴权失败、Bucket 不存在等）。
- 多步骤操作中某一步失败后中止后续步骤，但不回滚已上传的数据文件（因为 manifest 未更新，不影响一致性）。
- 用户可以选择重试整个操作或取消。
- 上传/下载期间显示进度条，支持用户手动取消。

默认备份摘要规则：

```text
新增 N 条，删除 M 条，修改 K 条
```

如果无法计算变更内容，可退化为：

```text
手动备份，当前共 N 条
```

备注/摘要规则：

- 第一版只写入 App 自动生成的非敏感摘要。
- 不允许用户自由编辑写入 manifest 的备注，避免泄露服务名、账号、银行卡、手机号等敏感信息。
- 如未来确实需要用户自定义备注，应将备注加密后保存，不应明文写入 manifest。

## 下载流程：从云端获取

1. 用户点击“从云端获取”。
2. App 检查对象存储平台配置是否完整。
3. 读取云端 `manifest.json`。
4. 对比本地更新时间和云端更新时间。
5. 向用户展示对比结果和推荐操作。
6. 如果用户选择覆盖本地，先自动生成一份本地备份。
7. 下载云端 `vault.enc`。
8. 要求用户输入或导入 Vault 加密密钥。
9. 解密并校验 vault 数据格式。
10. 写入本地数据库。
11. 更新本地同步状态。

失败与回滚说明：

- 如果读取 `manifest.json` 失败，直接中止，不修改本地数据。
- 如果自动生成本地备份失败，必须中止覆盖流程，避免在未备份情况下覆盖本地数据。
- 如果下载 `vault.enc` 失败，保留本地数据与本地备份，不执行写入。
- 如果解密或格式校验失败，保留本地数据与本地备份，不执行写入。
- 如果写入本地数据库失败，必须保留自动生成的本地备份，并向用户提示可从备份恢复。
- 如果在覆盖过程中 App 崩溃或被系统杀死，下次启动时应检测到未完成的恢复流程，并允许用户从最近本地备份继续恢复或回滚。

覆盖前提示示例：

```text
你正在从云端覆盖本地数据。
系统会先自动创建一份本地备份。
推荐选择更新时间较新的版本。
```

## 冲突处理

第一版由用户选择。

触发场景：

- 用户主动点击“从云端获取”。
- 用户点击“加密送至云端”时，App 发现云端 `vaultRevision/etag` 与本地上次同步记录不一致。
- 多设备使用同一对象存储路径时，其他设备已经先上传了新版本。

展示内容：

```text
本地数据：2026-05-08 21:30，36 条
云端数据：2026-05-08 22:15，38 条

推荐：使用云端数据，因为云端更新时间更新。
```

操作：

```text
使用云端覆盖本地
使用本地覆盖云端
尝试合并
取消
```

差异展示：

```text
云端特有：只存在于云端的记录
本机特有：只存在于本机的记录
双方都修改：云端和本机都存在但 updatedAt 或内容不同的记录
相同记录：两边一致，不需要操作
```

用户选择：

- 可以选择保留云端版本。
- 可以选择保留本机版本。
- 可以选择合并后上传为新的云端版本。
- 对双方都修改的记录，默认推荐 `updatedAt` 更新的一方，但允许用户逐条改选。
- 对云端特有和本机特有的记录，默认都保留。
- 用户确认后，App 生成合并后的本地数据，并再次加密上传为新的云端 current 版本。

推荐逻辑：

- 云端更新时间更新：推荐使用云端。
- 本地更新时间更新：推荐上传本地覆盖云端。
- 时间相同：推荐不操作。

合并规则第一版：

- 同一条密码按 `id` 匹配。
- 同一 `id` 的记录按 `updatedAt` 最新者胜出。
- 新增记录直接合并。
- 删除记录暂时不自动合并。

第一版合并的已知限制：

- 按 `updatedAt` 时间戳整条覆盖，不做字段级合并。如果两端同时修改了同一条记录的不同字段（如一端改了密码、一端改了备注），只保留 `updatedAt` 更新的一端，另一端的修改会丢失。
- 合并前向用户展示将被覆盖的记录列表，由用户确认后执行。
- 后续版本可考虑字段级合并或引入 CRDT。

后续可以增加 tombstone：

```kotlin
val deletedAt: Long? = null
```

这样才能正确处理跨设备删除同步。

## 本地备份

从云端覆盖本地前必须自动生成本地备份。

目的：

- 防止误覆盖。
- 防止云端密文被旧数据覆盖后无法找回。
- 给用户撤销空间。

本地备份可以保存在应用私有目录，并同样加密。

建议路径：

```text
files/local_backups/{timestamp}.enc
```

本地备份也应支持非敏感自动摘要：

```text
从云端覆盖前自动备份
```

本地备份清理规则：最多保留最近 10 份本地备份，超出后自动删除最旧的。

## UI 页面安排

入口：

```text
设置 -> 云同步与加密备份
```

页面结构：

```text
云同步与加密备份

1. Vault 加密密钥
   - 状态：未生成 / 已生成
   - 生成 Vault 加密密钥
   - 导入已有 Vault 加密密钥
   - 第一版不支持更换 Vault 加密密钥
   - 风险提示：密钥丢失后无法解密云端备份

2. 对象存储平台
   - 平台选择：腾讯云 COS / 阿里云 OSS / S3 兼容
   - Region
   - Endpoint
   - Bucket
   - 对象路径前缀

3. 访问凭证
   - AccessKeyId / SecretId
   - AccessKeySecret / SecretKey
   - 是否使用 Android Keystore 加密保存到本机
   - 测试连接

4. 同步操作
   - 加密送至云端
   - 从云端获取
   - 查看云端备份信息
   - 最近同步时间
   - 云端版本时间
   - 本地版本时间

5. 历史备份
   - 最近 7 份云端历史备份列表
   - 每条显示：备份时间、密码条目数、非敏感自动摘要、密钥版本
   - 操作：恢复此备份
   - 恢复历史备份流程：
     a. 用户选择某份历史备份，点击“恢复此备份”
     b. 展示确认对话框，显示备份时间、条目数和自动摘要
     c. 提示将覆盖本地数据，系统会先自动创建本地备份
     d. 用户确认后，自动生成本地备份
     e. 下载并解密所选历史备份
     f. 校验数据格式，写入本地数据库
     g. 提示恢复成功，显示恢复的条目数
```

注意：根据当前需求，暂不改造设置界面的使用帮助。

## 数据模型建议

### PasswordItem / PasswordEntity

为了支持合并和同步，建议增加：

```kotlin
val updatedAt: Long
val createdAt: Long
val revision: Long
val deviceId: String
```

后续支持删除合并时再增加：

```kotlin
val deletedAt: Long?
```

当前项目已有 `createdAt` 和 `updatedAt`，但转换到 UI 模型时没有完整保留，应在后续改造中统一。

### SyncState

建议本地保存同步状态：

```kotlin
data class SyncState(
    val platform: String,
    val bucket: String,
    val objectPrefix: String,
    val lastSyncAt: Long,
    val lastCloudUpdatedAt: Long,
    val lastVaultRevision: Long,
    val lastEtag: String?
)
```

### BackupMeta

```kotlin
data class BackupMeta(
    val backupId: String,
    val objectKey: String,
    val createdAt: Long,
    val itemCount: Int,
    val summary: String,
    val isAutoGeneratedSummary: Boolean,
    val keyVersion: Int
)
```

## 模块划分建议

```text
crypto/
  VaultCrypto.kt
  KeyDerivation.kt
  RecoveryPhrase.kt
  VaultFormat.kt

sync/
  CloudSyncRepository.kt
  CloudStorageClient.kt
  TencentCosClient.kt
  AliOssClient.kt
  S3CompatibleClient.kt
  SyncConflictResolver.kt
  SyncStateStore.kt
  BackupSummaryGenerator.kt

ui/screens/
  CloudSyncScreen.kt
```

## 实现顺序建议

1. 定义 vault 文件格式和 manifest 格式。
2. 实现 24 个助记词恢复密钥生成、确认、导入。
3. 实现 VaultCrypto，加密/解密本地 payload。
4. 实现本地 fake storage，用于先跑通上传、下载、备份、恢复流程。
5. 新增云同步页面 UI 骨架。
6. 实现 Android Keystore 加密保存对象存储访问密钥。
7. 实现 `CloudStorageClient` 统一接口。
8. 接入 S3 兼容实现。
9. 接入腾讯云 COS。
10. 接入阿里云 OSS。
11. 实现冲突比较和用户选择。
12. 实现覆盖本地前自动本地备份。
13. 实现最多保留 7 份云端备份。
14. 增加关键测试。

## 大文件与数据量边界

密码库序列化后的数据量需要有边界考虑：

- 预估：1000 条密码记录，每条平均 500 字节，序列化 payload 约 500KB，加密后约 500KB~600KB。
- 5000 条以上记录预估 payload 可达 2.5MB~3MB 左右。
- 对象存储单次 PUT 上传限制通常为 5GB，正常使用场景下不会触及。
- 考虑到移动网络环境，payload 超过 5MB 时建议在 UI 上提示用户数据量较大，建议在 Wi-Fi 环境下操作。
- 第一版不实现分片上传。如果未来密码库数据量显著增长，可以考虑 multipart upload。
- 序列化时使用紧凑 JSON 格式，不包含格式化缩进和多余空格。

## Vault 加密密钥更换策略

第一版不提供“更换 Vault 加密密钥”功能，保持一个 Vault 对应一个 24 词助记词。

理由：

- 对小白用户来说，多个助记词会显著增加理解和保存成本。
- 如果新备份使用新助记词、旧备份仍使用旧助记词，用户需要长期保存多个助记词，容易恢复失败。
- 第一版的重点是把本地加密、手动同步、冲突处理和恢复流程做稳。

第一版行为：

- 用户首次生成或导入助记词后，该助记词就是当前 Vault 的唯一恢复密钥。
- 云端 current 和最近 7 份云端历史备份都应使用同一个助记词体系。
- 本地自动备份也应使用同一个助记词体系。
- 如果用户认为助记词已经泄露，第一版建议创建新 Vault 并重新上传，而不是在原 Vault 内直接换密钥。

后续版本如果要支持更换助记词，推荐采用“迁移全部保留备份”的策略：

1. 用户输入旧助记词并验证通过。
2. App 解密 current 和最近 7 份云端历史备份。
3. 生成新的 24 词助记词，并要求用户确认保存。
4. 使用新助记词重新加密 current 和最近 7 份云端历史备份。
5. 更新 manifest 中的 `keyVersion`。
6. 迁移成功后，当前云端保留范围内的备份只需要新助记词恢复。

注意：如果用户在其他地方保存了更老的备份文件，那些备份仍然需要旧助记词才能恢复。

## 关键测试场景

- 生成 24 个助记词。
- 用户确认助记词位置。
- 相同助记词可以解密同一份 vault。
- 错误助记词无法解密 vault。
- 上传前生成的密文不包含明文密码。
- 下载覆盖前一定生成本地备份。
- 云端备份超过 7 份后会删除更旧备份。
- 合并时同一 `id` 使用 `updatedAt` 更新的记录。
- 对象存储凭证保存后，本地文件中不能看到明文 SecretKey。
- 测试连接失败时 UI 给出明确错误。
- 上传过程中网络中断后，manifest 未更新，云端数据保持一致。
- 上传前检测到云端 `vaultRevision/etag` 已变化时，不直接覆盖，进入冲突处理。
- current 和最近 7 份云端历史备份均可由同一个助记词解密。
- 恢复历史备份前自动生成本地备份。
- 大数据量（5000+ 条记录）下加密和上传正常完成。
- 本地备份超过 10 份后自动删除最旧的备份。

## 风险与注意事项

- Vault 加密密钥丢失后无法恢复云端数据，这是端到端加密的必要代价，必须在 UI 中明确说明。
- 对象存储访问密钥必须建议用户使用子账号/子用户。
- App 不能把云厂商主账号密钥写入代码。
- manifest 不能包含敏感字段。
- 日志中不能打印 SecretKey、恢复密钥、明文密码、密文解密后的 payload。
- 第一版手动同步比自动同步更可控，应先把安全和恢复流程做稳。

## 导出安全策略

- 默认只允许加密导出。
- 明文导出必须二次确认，并要求主密码或生物识别验证。
- 明文导出文件名必须明显标注为明文，例如包含 `plain-text` 或 `unencrypted`。
- 导出完成后应自动清理缓存文件，避免旧导出文件长期残留在本地。
