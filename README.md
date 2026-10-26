# PassCard

PassCard 是一款面向 Android 的本地优先密码管理应用。它专注于把密码数据留在用户设备上，同时提供主密码、生物识别解锁、导入导出、加密备份和可控的云端对象存储同步能力。

PassCard is a local-first password manager for Android. It keeps password data under the user's control on the device, while providing a master password flow, biometric unlock, import/export, encrypted backups, and optional object-storage based cloud sync.

产品宗旨、页面职责与本轮改进见 [产品宗旨与改进记录](PRODUCT_REVIEW.md)。

## 功能特点

- 本地密码库：保存服务名称、用户名、手机号、邮箱、密码、分类和备注。
- 安全入口：支持主密码设置、锁屏解锁和 Android 生物识别能力。
- 密码健康：统计弱密码、重复密码并给出安全评分入口。
- 搜索体验：支持普通关键词排序，也支持 `/t 字段 关键词` 的字段检索语法。
- 数据迁移：支持 CSV/JSON 导入导出，并提供导入预览。
- 图标管理：支持为密码条目选择或导入自定义图标。
- 加密备份：支持 vault 格式的加密备份与恢复。
- 云端同步：支持用户自带 S3 兼容对象存储账号，云端只保存加密后的 vault 数据。
- 双语界面：应用内提供中文和 English 切换。

## Features

- Local vault for service name, username, phone, email, password, category, and notes.
- Master password, lock screen, and Android biometric unlock support.
- Password health overview for weak and reused passwords.
- Ranked keyword search plus `/t field keyword` field-specific search syntax.
- CSV/JSON import and export with import preview.
- Per-entry icon selection and custom icon import.
- Encrypted vault backup and restore.
- Optional S3-compatible object storage sync with encrypted cloud payloads only.
- Chinese and English app language support.

## 技术栈

- Kotlin
- Android Jetpack Compose
- Material 3
- Room + Paging
- SQLCipher
- Android Keystore / Biometric
- Gradle Kotlin DSL

## Tech Stack

- Kotlin
- Android Jetpack Compose
- Material 3
- Room + Paging
- SQLCipher
- Android Keystore / Biometric
- Gradle Kotlin DSL

## 构建与运行

环境建议：

- Android Studio
- JDK 17 或 Android Studio bundled JDK
- Android SDK 35

构建 Debug 包：

```powershell
.\gradlew.bat assembleDebug
```

或在类 Unix 终端中：

```bash
./gradlew assembleDebug
```

Debug APK 会生成在：

```text
app/build/outputs/apk/debug/
```

## Build and Run

Recommended environment:

- Android Studio
- JDK 17 or the Android Studio bundled JDK
- Android SDK 35

Build a debug APK:

```powershell
.\gradlew.bat assembleDebug
```

Or on a Unix-like shell:

```bash
./gradlew assembleDebug
```

The debug APK is generated under:

```text
app/build/outputs/apk/debug/
```

## 备份密码与指纹授权

首次上传时设置并确认一个独立备份密码，至少 15 个 Unicode 码点、最多 1024 个，不能全为空白。密码与确认内容逐字一致，空格、大小写和 Unicode 字符均原样使用。它与应用主密码分开，用于在本机派生备份加密密钥；云端只收到密文，不接收备份密码。请妥善保存该密码，换机、重装或本机生物识别密钥失效后，仍需它来恢复备份；忘记密码将无法解密。

可使用 Android 生物识别授权取用本机保存的备份密码，日常上传和恢复无需反复输入。指纹是本机授权方式，不是跨设备恢复密钥。手动输入仍可使用。后台自动备份需要另外授权保存一份由本设备 Android Keystore 保护、可供后台任务使用的备份密码；后台不会弹出生物识别。

本版本已移除 24 词生成、确认及输入流程，也不保留旧助记词加密格式的解密或迁移。

## Backup Password And Biometric Authorization

Set and confirm a separate backup password of 15–1024 Unicode code points for your first upload; it cannot contain only whitespace. Confirmation must match exactly; whitespace, case and Unicode text are preserved. It is independent of the app's master password and derives the backup encryption key locally. The cloud receives ciphertext, never the backup password. Keep the password safely: a new device, reinstall, or invalidated biometric key requires it to restore the backup. A forgotten password cannot decrypt the backup.

Android biometrics can authorize the saved password on this device for subsequent uploads and restores. Biometrics provide local authorization, not a portable recovery key; manual password entry remains available. Unattended automatic backup requires separate consent to save a backup password protected by this device's Android Keystore. Background tasks cannot show a biometric prompt.

The 24-word generation, confirmation and entry flows have been removed. This version does not decrypt or migrate backups encrypted with the former mnemonic format.

## 云端备份兼容性与导入规则

云端上传先保存独立的加密备份，再原子创建下一序号的不可变提交记录。多个设备争抢同一提交序号时只有一个能成功；失败方停止并重新比较云端，不自动跳号重试，也不覆盖其他设备的版本。读取、权限、分页、提交损坏或版本冲突会停止操作；不会把错误当成空云端。初次接入已有数据的设备，需要先保留本地记录并比较或恢复云端，建立同步版本后才能上传。

根据标准 Endpoint 域名自动识别腾讯 COS、阿里云 OSS 和其他 S3 兼容服务：COS 使用 `x-cos-forbid-overwrite: true`；OSS 推荐使用官方 S3 兼容 Endpoint（如 `https://s3.oss-cn-hangzhou.aliyuncs.com`），使用 `x-oss-forbid-overwrite: true`，标准 S3 使用 `If-None-Match: *`。腾讯/阿里的专用防覆盖参数受桶版本控制影响，使用此方案的桶须未启用版本控制；阿里云 OSS 已开启后暂停版本控制的桶同样无法使用此防覆盖参数；是否可安全使用以实际探测为准。自定义域名默认按通用 S3 处理。无需向应用额外提供厂商选择项，也不把 S3 兼容等同于所有接口完全相同。相关接口见[腾讯 COS 文档](https://cloud.tencent.com/document/product/436/7749)与[阿里云 OSS 文档](https://www.alibabacloud.com/help/en/oss/developer-reference/putobject)。

“测试连接”和每次上传都会在独立临时对象上验证创建成功、重复创建被拒绝，以及读回内容和 ETag 一致；不再要求可更新的 `If-Match` 接口。临时探测不使用真实保险库对象，只清理本次拥有的探测对象。失败提示区分权限、网络、HTTP、ETag 和防覆盖失效，腾讯/阿里会提示检查版本控制；不回显服务端正文、URL 或凭据。连接测试通过表示本次读写及上传保护均已验证。

提交记录采用版本 3，位于 `vault/commits/`，加密正文仍采用备份密码格式版本 2。小型提交记录永久保留以防止迟到的上传复用旧序号，请勿为该目录配置自动删除规则；恢复列表只展示最新提交引用的最近 7 份备份正文。完整分页用于发现最新提交，分页失败停止操作。只有成功发布者清理已知历史中超出 7 份的正文；中断、超时或冲突留下的未提交对象不会被自动删除，以免影响另一台设备或已成功但响应丢失的上传。

旧固定路径清单保持只读兼容；旧助记词加密正文仍不解密。使用同一目录的所有设备应升级到本版本：旧客户端写固定清单无法与新提交日志协调，不支持新旧客户端混写。更新后无需重建桶或删除本机密码数据。腾讯/阿里、S3 的协议回归使用本机 HTTP 服务及测试凭据，真实服务账号仍需实际验收。

CSV 文件识别到表头后严格按列名导入，缺失字段为空。密码保留原始空格、制表符与引号内换行；无表头文件继续按“服务、用户名、手机号、邮箱、密码、备注、分类”的顺序解析。CSV / JSON 允许空密码记录，并支持本应用导出记录的内容导回；只有各内容字段完全一致的记录才跳过，邮箱、手机号、密码或备注不同的记录保留。CSV / JSON 导出包含图标属性，本地图片引用只在当前安装有效。加密备份另外打包自定义图片内容，支持跨设备恢复。

## Cloud Backup Compatibility and CSV Rules

Uploads save an independent encrypted vault, then atomically create the next immutable commit sequence. Concurrent writers compete for the same sequence; the loser stops without overwriting the winner or automatically retrying at another sequence. Read, pagination, malformed-commit and checkpoint failures stop the operation. A new device must compare or restore existing cloud data before uploading.

Standard endpoint domains select Tencent COS (`x-cos-forbid-overwrite: true`), Alibaba OSS (`x-oss-forbid-overwrite: true`) or generic S3 (`If-None-Match: *`) automatically. For OSS, use the documented S3-compatible endpoint, such as `https://s3.oss-cn-hangzhou.aliyuncs.com`. COS and OSS require a bucket whose versioning does not disable their create-only protection (suspended OSS versioning also disables it); the live capability check is authoritative. Custom domains default to generic S3. Both Test connection and uploads probe isolated temporary objects for successful creation, rejection of duplicate creation, and consistent content/ETags. Matching-version updates are no longer required. Diagnostics never echo server bodies, URLs or credentials.

Version 3 commit records live under `vault/commits/`; encrypted payloads remain backup-password format version 2. Keep the small commit records permanently, including in bucket lifecycle policies, to fence delayed writers. The current commit references at most 7 recoverable vaults; only a successful publisher may clean up known, expired history payloads. Uncommitted or ambiguously published objects remain untouched. Complete, validated pagination discovers the latest commit. Older fixed manifests remain read compatible, but every writer sharing a prefix must upgrade: old manifest writers cannot coordinate with the new commit log. Former mnemonic-encrypted payloads remain unsupported. Provider behavior has local HTTP regression coverage, not verification against real cloud accounts.

CSV headers map only the columns they name; missing fields stay empty. Password whitespace and quoted line endings are preserved. Headerless files retain the existing positional format. CSV and JSON accept records without passwords and skip only records whose content fields are identical. CSV and JSON export icon attributes with installation-local image references. Encrypted backups also include custom image bytes for cross-device restoration.

## 图标与历史备份恢复

“备份 → 历史备份”可查看本机保护副本和云端已提交备份。本机保留最近 10 份，云端清单保留最近 7 份；本机历史无需联网或配置云端。列表只读取文件属性、清单与对象头，选中并确认恢复后才读取加密正文。旧备份不一定提供记录数与修订信息。

恢复会先加密保存当前本机记录，等待数据库替换成功，再更新状态。历史版本只恢复到本机，不修改云端；成功后清除同步检查点，后台自动上传不能将历史内容直接覆盖云端。后续上传需重新比较当前云端并建立同步版本。

加密备份保存生成图标属性与自定义图片二进制内容。相同图片按摘要复用，恢复时校验大小与摘要，并保存到应用私有目录。仅打包本应用图标库中的可信图片；单图上限 2 MiB、全部图标上限 16 MiB、最多 1024 张。无法读取或超过限制时明确停止备份，避免默默遗漏图标。对于可解密的备份，原本未保存的图片无法凭空恢复；缺失时使用生成图标并显示提示。

## Icons and Backup History

Open Backup → Backup history to select a local protection copy or a committed cloud backup. Up to 10 local copies and 7 committed cloud versions are retained. Local history works offline. Listing reads file attributes, manifests and object headers; encrypted payloads are read after selection and confirmation. Metadata unavailable in older backups is left unspecified.

A restore first saves an encrypted copy of the current local vault, awaits the database replacement, then updates its state. Historical restoration changes only this device and clears the sync checkpoint so automatic uploads cannot overwrite the cloud with old content. Compare the current cloud vault again before resuming uploads.

Encrypted backups preserve generated-icon attributes and custom image bytes. Images are deduplicated and validated by digest, then restored into app-private storage. Only images in PassCard's trusted icon library are included, with limits of 2 MiB per image, 16 MiB in total and 1024 images. Unreadable or oversized images stop the backup explicitly. In supported backups, missing image content uses a generated fallback with a warning; omitted image bytes cannot be recovered from the backup.

## 加载与密码健康

首页、安全中心及问题列表区分加载、成功空结果和读取失败；失败可以重试。数据库初始化失败也提供重试入口。统计、弱密码原因和重复组来自同一份数据库快照，并在后台计算。

健康检查使用本地启发式规则，提示长度不足 15 个字符、常见密码、重复模式及连续字符；不要求固定大小写或符号组合，不查询泄露库，也不承诺破解时间。空字符串不参与评分或重复检查；非空的纯空白密码保留原文并提示较弱。重复判断逐字比较，不修剪密码。随机密码使用系统密码学随机源；新安装默认长度为 16，已有设置保持不变。

## Loading and Password Health

Home, security and issue lists distinguish loading, a successful empty result and a failed read with retry. Database initialization failures also offer retry. Statistics, weak-password reasons and reuse groups are calculated in the background from one database snapshot.

Local heuristics flag passwords shorter than 15 code points, familiar passwords, repetitions and predictable sequences. They do not enforce character-class combinations, query breach databases or promise a cracking-time estimate. Empty strings are excluded from scoring and reuse checks; nonempty whitespace-only passwords are preserved and flagged. Reuse compares exact passwords. Password generation uses a cryptographic random source. New installations default to 16 characters; existing settings are preserved.

## 安全说明

- 不要把 `local.properties`、签名证书、keystore、真实 Access Key、Secret Key 或个人 vault 数据提交到仓库。
- 对象存储凭据只应配置在用户设备上，并建议使用权限受限的子账号。
- 移除主密码需要输入当前密码并明确确认；验证成功才关闭主密码及生物识别入口。
- 剪贴板按复制标记与到期时间清理，不在清理任务中保存密码正文。后台到期后会在应用重新取得输入焦点时补清理；应用进程终止后无法保证清理。Android 在无输入焦点时限制读取剪贴板描述，清空接口也不提供原子条件写入，不能承诺后台精准定时或完全消除跨应用竞争。[Android 官方 API 说明](https://developer.android.com/reference/android/content/ClipboardManager#getPrimaryClipDescription())。
- 云端同步只上传加密后的 vault 数据；用户应妥善保存独立备份密码。指纹授权无法代替换机恢复所需的密码。
- 本仓库用于项目源码备份与维护，不包含发布签名材料。

## Security Notes

- Do not commit `local.properties`, signing certificates, keystores, real access keys, secret keys, or personal vault data.
- Object storage credentials should live only on the user's device. A least-privilege sub-account is recommended.
- Master-password removal requires verification and explicit confirmation; successful removal also disables biometric unlock.
- Clipboard cleanup tracks only a copy token and deadline. Overdue cleanup resumes when the app regains input focus. Process termination can prevent cleanup, and the platform does not provide atomic conditional clearing. See the [Android clipboard API](https://developer.android.com/reference/android/content/ClipboardManager#getPrimaryClipDescription()).
- Cloud sync uploads encrypted vault payloads only. Keep the separate backup password safely; biometric authorization cannot replace it on a new device.
- This repository is for source backup and maintenance, and does not include release signing materials.

## 项目结构

```text
app/src/main/java/com/example/passcard/
├── crypto/      # Vault encryption, backup format, and recovery models
├── data/        # Room database, repositories, search, security stats
├── sync/        # Cloud storage and sync repositories
├── ui/          # Compose screens, components, theme
└── util/        # Import/export, preferences, icon storage, helpers
```

## Repository Layout

```text
app/src/main/java/com/example/passcard/
├── crypto/      # Vault encryption, backup format, and recovery models
├── data/        # Room database, repositories, search, security stats
├── sync/        # Cloud storage and sync repositories
├── ui/          # Compose screens, components, theme
└── util/        # Import/export, preferences, icon storage, helpers
```

## 维护状态

当前版本号：`0.81`

该项目仍在持续开发中。提交公开仓库前，请优先确认敏感配置、签名文件、构建产物和个人数据没有进入 Git 跟踪。

## Maintenance Status

Current version: `0.81`

This project is still under active development. Before publishing changes, verify that sensitive configuration, signing files, build outputs, and personal data are not tracked by Git.
