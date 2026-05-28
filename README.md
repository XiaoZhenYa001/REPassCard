# PassCard

PassCard 是一款面向 Android 的本地优先密码管理应用。它专注于把密码数据留在用户设备上，同时提供主密码、生物识别解锁、导入导出、加密备份和可控的云端对象存储同步能力。

PassCard is a local-first password manager for Android. It keeps password data under the user's control on the device, while providing a master password flow, biometric unlock, import/export, encrypted backups, and optional object-storage based cloud sync.

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

## 安全说明

- 不要把 `local.properties`、签名证书、keystore、真实 Access Key、Secret Key 或个人 vault 数据提交到仓库。
- 对象存储凭据只应配置在用户设备上，并建议使用权限受限的子账号。
- 云端同步设计目标是只上传加密后的 vault 数据，恢复助记词或恢复密钥应由用户自行妥善保存。
- 本仓库用于项目源码备份与维护，不包含发布签名材料。

## Security Notes

- Do not commit `local.properties`, signing certificates, keystores, real access keys, secret keys, or personal vault data.
- Object storage credentials should live only on the user's device. A least-privilege sub-account is recommended.
- Cloud sync is designed to upload encrypted vault payloads only. Recovery phrases or recovery keys must be stored safely by the user.
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
