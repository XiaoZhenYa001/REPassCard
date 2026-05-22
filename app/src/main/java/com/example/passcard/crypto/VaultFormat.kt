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
                format = root.getString("format"),
                version = root.getInt("version"),
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
                name = json.getString("name"),
                salt = json.getString("salt"),
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
                memory = json.getInt("memory"),
                iterations = json.getInt("iterations"),
                parallelism = json.getInt("parallelism"),
                hashLength = json.optInt("hashLength", 32),
                outputEncoding = json.optString("outputEncoding", "base64")
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
                algorithm = json.getString("algorithm"),
                nonce = json.getString("nonce"),
                ciphertext = json.getString("ciphertext"),
                tag = json.getString("tag")
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
                deviceId = json.optString("deviceId", ""),
                deviceName = json.optString("deviceName", ""),
                createdAt = json.optLong("createdAt", 0L),
                updatedAt = json.optLong("updatedAt", 0L),
                vaultRevision = json.optLong("vaultRevision", 0L),
                itemCount = json.optInt("itemCount", 0),
                keyVersion = json.optInt("keyVersion", 1),
                kdfVersion = json.optInt("kdfVersion", 1),
                formatVersion = json.optInt("formatVersion", 1)
            )
        }
    }
}

data class VaultPayload(
    val items: List<VaultPasswordRecord>
) {
    fun toJsonString(): String = JSONObject().apply {
        put("items", JSONArray().apply {
            items.forEach { put(it.toJson()) }
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
            return VaultPayload(items)
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
    val deletedAt: Long? = null
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
                deletedAt = deleted
            )
        }
    }
}
