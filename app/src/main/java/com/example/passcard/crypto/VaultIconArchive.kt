package com.example.passcard.crypto

import java.io.InputStream
import java.util.Base64

/** Icon data is serialized only inside the encrypted vault payload. */
data class VaultIconAsset(val id: String, val contentBase64: String)

interface VaultIconStorage {
    fun openLocalIcon(reference: String): InputStream?
    fun storeRestoredIcon(bytes: ByteArray): String
}

data class RestoredVaultIcons(val records: List<VaultPasswordRecord>, val warnings: List<String>)

class VaultIconArchive(private val storage: VaultIconStorage?) {
    fun prepareExport(records: List<VaultPasswordRecord>): VaultPayload {
        val assets = linkedMapOf<String, VaultIconAsset>()
        val references = mutableMapOf<String, String>()
        var totalBytes = 0L
        val prepared = records.map { record ->
            if (record.iconType != "image") return@map record.copy(iconAssetId = null)
            val assetId = references.getOrPut(record.iconValue) {
                val bytes = try {
                    val stream = storage?.openLocalIcon(record.iconValue)
                        ?: throw IllegalArgumentException("图标不存在或没有读取权限。")
                    stream.use { readBoundedIcon(it) }
                } catch (error: Exception) {
                    throw IllegalArgumentException("无法备份「${record.name}」的图片图标：${error.message ?: "读取失败"} 请重新选择或优化图标后再备份。", error)
                }
                val hash = iconContentHash(bytes)
                if (!assets.containsKey(hash)) {
                    totalBytes += bytes.size
                    require(totalBytes <= MAX_TOTAL_ICON_BYTES) { "备份图标总大小不能超过 16 MiB，请先优化图标。" }
                    require(assets.size < MAX_ICONS) { "备份图标数量超出支持范围。" }
                    assets[hash] = VaultIconAsset(hash, Base64.getEncoder().encodeToString(bytes))
                }
                hash
            }
            // A URI belongs to one device; the portable payload refers only to embedded content.
            record.copy(iconValue = "", iconAssetId = assetId)
        }
        return VaultPayload(prepared, assets.values.toList())
    }

    fun prepareRestore(payload: VaultPayload): RestoredVaultIcons {
        require(payload.icons.size <= MAX_ICONS) { "备份图标数量超出支持范围。" }
        val decoded = linkedMapOf<String, ByteArray>()
        var totalBytes = 0L
        // Validate every asset and reference before writing any files.
        payload.icons.forEach { asset ->
            require(asset.id.matches(ASSET_ID)) { "备份图标标识无效。" }
            require(!decoded.containsKey(asset.id)) { "备份包含重复的图标标识。" }
            require(asset.contentBase64.length <= MAX_ENCODED_ICON_LENGTH) { "备份图标超出支持的大小。" }
            val bytes = try {
                Base64.getDecoder().decode(asset.contentBase64)
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("备份图标编码无效。", error)
            }
            require(bytes.isNotEmpty() && bytes.size <= MAX_ICON_BYTES) { "备份图标大小无效。" }
            totalBytes += bytes.size
            require(totalBytes <= MAX_TOTAL_ICON_BYTES) { "备份图标总大小超出支持范围。" }
            require(iconContentHash(bytes) == asset.id) { "备份图标内容与标识不一致。" }
            decoded[asset.id] = bytes
        }
        payload.items.forEach { record ->
            record.iconAssetId?.let { assetId ->
                require(record.iconType == "image" && decoded.containsKey(assetId)) { "备份缺少记录所需的图片图标。" }
            }
        }
        val restoredReferences = mutableMapOf<String, String>()
        val legacyReferences = mutableMapOf<String, Boolean>()
        val warnings = mutableListOf<String>()
        val restored = payload.items.map { record ->
            if (record.iconType != "image") return@map record
            val assetId = record.iconAssetId
            if (assetId != null) {
                val newReference = restoredReferences.getOrPut(assetId) {
                    requireNotNull(storage) { "无法保存备份图标，请在应用内恢复备份。" }
                        .storeRestoredIcon(decoded.getValue(assetId))
                }
                record.copy(iconValue = newReference, iconAssetId = null)
            } else {
                // Old backups may have only a device-specific URI. Never follow a remote or arbitrary file URI.
                val accessible = legacyReferences.getOrPut(record.iconValue) {
                    runCatching {
                        storage?.openLocalIcon(record.iconValue)?.use { readBoundedIcon(it) } != null
                    }.getOrDefault(false)
                }
                if (accessible) record else {
                    warnings += "旧备份未包含图片内容，无法恢复「${record.name}」的图标，已使用默认图标。"
                    record.copy(iconType = "generated", iconValue = "", iconAssetId = null)
                }
            }
        }
        return RestoredVaultIcons(restored, warnings)
    }

    companion object {
        const val MAX_ICON_BYTES = 2 * 1024 * 1024
        const val MAX_TOTAL_ICON_BYTES = 16 * 1024 * 1024
        const val MAX_ICONS = 1024
        const val MAX_ENCODED_ICON_LENGTH = ((MAX_ICON_BYTES + 2) / 3) * 4
        private val ASSET_ID = Regex("[0-9a-f]{64}")
    }
}
