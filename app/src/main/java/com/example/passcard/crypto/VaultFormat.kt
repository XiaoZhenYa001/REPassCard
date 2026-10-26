package com.example.passcard.crypto

import org.json.JSONArray
import org.json.JSONObject

data class VaultEncFile(
    val format: String,
    val version: Int,
    val kdf: KdfParameters,
    val wrappedDataKey: CipherBlob,
    val crypto: CipherBlob,
    val metadata: VaultMetadata
) {
    fun toJsonString(): String {
        return JSONObject().apply {
            put("format", format)
            put("version", version)
            put("kdf", kdf.toJson())
            put("wrappedDataKey", wrappedDataKey.toJson())
            put("crypto", crypto.toJson())
            put("metadata", metadata.toJson())
        }.toString()
    }

    companion object {
        fun fromJsonString(json: String): VaultEncFile {
            val root = JSONObject(json)
            return VaultEncFile(
                format = root.requiredString("format"),
                version = root.requiredInt("version"),
                kdf = KdfParameters.fromJson(root.getJSONObject("kdf")),
                wrappedDataKey = CipherBlob.fromJson(root.getJSONObject("wrappedDataKey")),
                crypto = CipherBlob.fromJson(root.getJSONObject("crypto")),
                metadata = VaultMetadata.fromJson(root.getJSONObject("metadata"))
            )
        }
    }
}

data class KdfParameters(
    val name: String,
    val salt: String,
    val params: KdfParameterDetail
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("salt", salt)
        put("params", params.toJson())
    }

    companion object {
        fun fromJson(json: JSONObject): KdfParameters {
            return KdfParameters(
                name = json.requiredString("name"),
                salt = json.requiredString("salt"),
                params = KdfParameterDetail.fromJson(json.getJSONObject("params"))
            )
        }
    }
}

data class KdfParameterDetail(
    val memory: Int,
    val iterations: Int,
    val parallelism: Int,
    val hashLength: Int,
    val outputEncoding: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("memory", memory)
        put("iterations", iterations)
        put("parallelism", parallelism)
        put("hashLength", hashLength)
        put("outputEncoding", outputEncoding)
    }

    companion object {
        fun fromJson(json: JSONObject): KdfParameterDetail {
            return KdfParameterDetail(
                memory = json.requiredInt("memory"),
                iterations = json.requiredInt("iterations"),
                parallelism = json.requiredInt("parallelism"),
                hashLength = json.requiredInt("hashLength"),
                outputEncoding = json.requiredString("outputEncoding")
            )
        }
    }
}

data class CipherBlob(
    val algorithm: String,
    val nonce: String,
    val ciphertext: String,
    val tag: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("algorithm", algorithm)
        put("nonce", nonce)
        put("ciphertext", ciphertext)
        put("tag", tag)
    }

    companion object {
        fun fromJson(json: JSONObject): CipherBlob {
            return CipherBlob(
                algorithm = json.requiredString("algorithm"),
                nonce = json.requiredString("nonce"),
                ciphertext = json.requiredString("ciphertext"),
                tag = json.requiredString("tag")
            )
        }
    }
}

data class VaultMetadata(
    val deviceId: String,
    val deviceName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val vaultRevision: Long,
    val itemCount: Int,
    val keyVersion: Int,
    val kdfVersion: Int,
    val formatVersion: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("deviceId", deviceId)
        put("deviceName", deviceName)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("vaultRevision", vaultRevision)
        put("itemCount", itemCount)
        put("keyVersion", keyVersion)
        put("kdfVersion", kdfVersion)
        put("formatVersion", formatVersion)
    }

    companion object {
        fun fromJson(json: JSONObject): VaultMetadata {
            return VaultMetadata(
                deviceId = json.requiredString("deviceId"),
                deviceName = json.requiredString("deviceName"),
                createdAt = json.requiredLong("createdAt"),
                updatedAt = json.requiredLong("updatedAt"),
                vaultRevision = json.requiredLong("vaultRevision"),
                itemCount = json.requiredInt("itemCount"),
                keyVersion = json.requiredInt("keyVersion"),
                kdfVersion = json.requiredInt("kdfVersion"),
                formatVersion = json.requiredInt("formatVersion")
            )
        }
    }
}

data class VaultPayload(
    val items: List<VaultPasswordRecord>,
    val icons: List<VaultIconAsset> = emptyList()
) {
    fun toJsonString(): String = JSONObject().apply {
        put("items", JSONArray().apply {
            items.forEach { put(it.toJson()) }
        })
        put("icons", JSONArray().apply {
            icons.forEach { icon ->
                put(JSONObject().apply {
                    put("id", icon.id)
                    put("data", icon.contentBase64)
                })
            }
        })
    }.toString()

    companion object {
        fun fromJsonString(json: String): VaultPayload {
            val root = JSONObject(json)
            val array = root.getJSONArray("items")
            val items = buildList {
                for (i in 0 until array.length()) {
                    add(VaultPasswordRecord.fromJson(array.getJSONObject(i)))
                }
            }
            val iconArray = if (root.has("icons")) root.getJSONArray("icons") else JSONArray()
            require(iconArray.length() <= VaultIconArchive.MAX_ICONS) { "备份图标数量超出支持范围。" }
            val icons = (0 until iconArray.length()).map { index ->
                val icon = iconArray.getJSONObject(index)
                val id = icon.get("id")
                val data = icon.get("data")
                require(id is String && data is String) { "备份图标内容无效。" }
                require(data.length <= VaultIconArchive.MAX_ENCODED_ICON_LENGTH) { "备份图标超出支持的大小。" }
                VaultIconAsset(id, data)
            }
            return VaultPayload(items, icons)
        }
    }
}

data class VaultPasswordRecord(
    val id: String,
    val name: String,
    val username: String,
    val phone: String,
    val email: String,
    val password: String,
    val category: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long = 0L,
    val deviceId: String = "",
    val deletedAt: Long? = null,
    val iconType: String = "generated",
    val iconValue: String = "",
    val iconAssetId: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("username", username)
        put("phone", phone)
        put("email", email)
        put("password", password)
        put("category", category)
        put("note", note)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("revision", revision)
        put("deviceId", deviceId)
        put("iconType", iconType)
        put("iconValue", iconValue)
        if (iconAssetId != null) put("iconAssetId", iconAssetId)
        if (deletedAt != null) put("deletedAt", deletedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): VaultPasswordRecord {
            val deleted = if (json.has("deletedAt") && !json.isNull("deletedAt")) {
                json.getLong("deletedAt")
            } else null
            return VaultPasswordRecord(
                id = json.getString("id"),
                name = json.optString("name", ""),
                username = json.optString("username", ""),
                phone = json.optString("phone", ""),
                email = json.optString("email", ""),
                password = json.optString("password", ""),
                category = json.optString("category", ""),
                note = json.optString("note", ""),
                createdAt = json.optLong("createdAt", 0L),
                updatedAt = json.optLong("updatedAt", 0L),
                revision = json.optLong("revision", 0L),
                deviceId = json.optString("deviceId", ""),
                deletedAt = deleted,
                iconType = json.optString("iconType", "generated"),
                iconValue = json.optString("iconValue", ""),
                iconAssetId = if (json.has("iconAssetId") && !json.isNull("iconAssetId")) json.getString("iconAssetId") else null
            )
        }
    }
}

/** JSONObject's numeric getters coerce strings and fractional values, which is unsuitable for version/KDF fields. */
private fun JSONObject.requiredInt(name: String): Int {
    val value = requiredLong(name)
    require(value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "备份数字字段超出范围：$name" }
    return value.toInt()
}

private fun JSONObject.requiredLong(name: String): Long {
    val value = get(name)
    require(value is Int || value is Long) { "备份数字字段无效：$name" }
    return (value as Number).toLong()
}

private fun JSONObject.requiredString(name: String): String {
    val value = get(name)
    require(value is String) { "备份文本字段无效：$name" }
    return value
}
