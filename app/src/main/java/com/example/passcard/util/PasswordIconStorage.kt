package com.example.passcard.util

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.LruCache
import androidx.annotation.RequiresApi
import com.caverock.androidsvg.SVG
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object PasswordIconType {
    const val GENERATED = "generated"
    const val EMOJI = "emoji"
    const val IMAGE = "image"
}

data class LocalIconImage(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String
) {
    val uriString: String = uri.toString()
    val isOptimized: Boolean = name.startsWith("icon_") && name.endsWith(".webp", ignoreCase = true)
}

data class IconStorageResult(
    val uri: Uri,
    val fileName: String,
    val warning: String? = null
) {
    val uriString: String = uri.toString()
}

object PasswordIconStorage {
    private const val DIRECTORY_NAME = "PassCard/images"
    private const val RELATIVE_PATH = "Documents/$DIRECTORY_NAME/"
    private const val TARGET_SIZE = 384
    private const val SVG_RENDER_SIZE = 1024
    private const val SVG_OUTPUT_PADDING_RATIO = 0.08f
    private const val WEBP_MIME = "image/webp"
    private const val KEEP_FILE = ".passcard_images"
    private const val ICON_CACHE_KB = 4096

    private val iconCache = object : LruCache<String, Bitmap>(ICON_CACHE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return max(1, value.allocationByteCount / 1024)
        }
    }

    fun requiredReadPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun ensureImageFolder(context: Context): Result<Unit> {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ensureMediaStoreFolder(context)
            } else {
                legacyImageDirectory().mkdirs()
            }
        }
    }

    fun listLocalImages(context: Context): Result<List<LocalIconImage>> {
        return runCatching {
            ensureImageFolder(context).getOrThrow()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                listMediaStoreImages(context)
            } else {
                listLegacyImages()
            }
        }
    }

    fun importPickedIcon(context: Context, sourceUri: Uri): Result<IconStorageResult> {
        return createCompressedIcon(context, sourceUri, nextIconFileName())
    }

    fun optimizeLibraryImage(context: Context, image: LocalIconImage): Result<IconStorageResult> {
        if (image.isOptimized && image.sizeBytes in 1..512_000) {
            return Result.success(IconStorageResult(image.uri, image.name))
        }

        return createCompressedIcon(context, image.uri, nextIconFileName()).map { result ->
            val deleteResult = deleteUri(context, image.uri)
            if (deleteResult.isSuccess) {
                result
            } else {
                result.copy(
                    warning = "已使用优化后的图片，但原图片删除失败：${deleteResult.exceptionOrNull()?.message ?: "可能没有删除权限"}"
                )
            }
        }
    }

    fun deleteIcon(context: Context, iconValue: String): Result<Boolean> {
        if (iconValue.isBlank()) return Result.success(false)
        return deleteUri(context, Uri.parse(iconValue)).map { true }
    }

    fun decodeIconBitmap(context: Context, iconValue: String, maxSize: Int = TARGET_SIZE): Bitmap? {
        if (iconValue.isBlank()) return null
        getCachedIconBitmap(iconValue, maxSize)?.let { return it }
        return runCatching {
            decodeScaledBitmap(context, Uri.parse(iconValue), maxSize)
        }.getOrNull()?.also { bitmap ->
            iconCache.put(cacheKey(iconValue, maxSize), bitmap)
        }
    }

    fun getCachedIconBitmap(iconValue: String, maxSize: Int = TARGET_SIZE): Bitmap? {
        if (iconValue.isBlank()) return null
        return iconCache.get(cacheKey(iconValue, maxSize))
    }

    private fun createCompressedIcon(
        context: Context,
        sourceUri: Uri,
        fileName: String
    ): Result<IconStorageResult> {
        return runCatching {
            val bitmap = try {
                decodeScaledBitmap(context, sourceUri, TARGET_SIZE)
            } catch (error: OutOfMemoryError) {
                throw IllegalArgumentException("无法优化图片：图片尺寸过大，设备内存不足，请尝试较小的图片。")
            } ?: throw IllegalArgumentException("无法读取该图片：文件可能不是图片，或格式暂不受支持。")

            val bytes = compressBitmap(bitmap).getOrThrow()
            saveCompressedBytes(context, fileName, bytes).getOrThrow()
        }
    }

    private fun compressBitmap(bitmap: Bitmap): Result<ByteArray> {
        return runCatching {
            ByteArrayOutputStream().use { output ->
                val format = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && bitmap.hasAlpha() -> Bitmap.CompressFormat.WEBP_LOSSLESS
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Bitmap.CompressFormat.WEBP_LOSSY
                    else -> @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
                }
                val quality = if (bitmap.hasAlpha()) 100 else 88
                if (!bitmap.compress(format, quality, output)) {
                    throw IllegalArgumentException("无法优化图片：图片编码失败。")
                }
                output.toByteArray()
            }
        }
    }

    private fun saveCompressedBytes(
        context: Context,
        fileName: String,
        bytes: ByteArray
    ): Result<IconStorageResult> {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveMediaStoreBytes(context, fileName, bytes)
            } else {
                saveLegacyBytes(fileName, bytes)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveMediaStoreBytes(
        context: Context,
        fileName: String,
        bytes: ByteArray
    ): IconStorageResult {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, WEBP_MIME)
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: throw IllegalStateException("无法保存图片：无法在 Documents/PassCard/images 创建文件。")
        try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw IllegalStateException("无法保存图片：无法写入目标文件。")
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null
            )
            return IconStorageResult(uri, fileName)
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun saveLegacyBytes(fileName: String, bytes: ByteArray): IconStorageResult {
        val dir = legacyImageDirectory()
        if (!dir.exists() && !dir.mkdirs()) {
            throw IllegalStateException("无法保存图片：无法创建 ${dir.absolutePath}")
        }
        val file = File(dir, fileName)
        file.writeBytes(bytes)
        return IconStorageResult(Uri.fromFile(file), fileName)
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri, maxSize: Int): Bitmap? {
        if (isSvgFile(context, uri)) {
            return decodeSvgBitmap(context, uri, maxSize)
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openInputStream(context, uri).use { stream ->
            if (stream == null) throw IllegalArgumentException("无法读取该图片：文件不存在或没有读取权限。")
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IllegalArgumentException("无法读取该图片：格式不受支持或图片已损坏。")
        }

        val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxSize * 2)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = openInputStream(context, uri).use { stream ->
            if (stream == null) throw IllegalArgumentException("无法读取该图片：文件不存在或没有读取权限。")
            BitmapFactory.decodeStream(stream, null, options)
        } ?: throw IllegalArgumentException("无法读取该图片：解码失败，请尝试 jpg、png、webp、heic 等常见图片格式。")

        return centerCrop(decoded, maxSize)
    }

    private fun decodeSvgBitmap(context: Context, uri: Uri, maxSize: Int): Bitmap {
        val svg = openInputStream(context, uri).use { stream ->
            if (stream == null) {
                throw IllegalArgumentException("无法读取该 SVG：文件不存在或没有读取权限。")
            }
            SVG.getFromInputStream(stream)
        }
        val renderSize = max(SVG_RENDER_SIZE, maxSize * 3)
        val picture = svg.renderToPicture(renderSize, renderSize)
        val rendered = Bitmap.createBitmap(renderSize, renderSize, Bitmap.Config.ARGB_8888)
        Canvas(rendered).drawPicture(picture)
        return centerVisibleContent(rendered, maxSize, SVG_OUTPUT_PADDING_RATIO)
    }

    private fun centerCrop(source: Bitmap, size: Int): Bitmap {
        val edge = min(source.width, source.height)
        val left = (source.width - edge) / 2
        val top = (source.height - edge) / 2
        val square = Bitmap.createBitmap(source, left, top, edge, edge)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(
            square,
            null,
            android.graphics.Rect(0, 0, size, size),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        if (square != source) square.recycle()
        source.recycle()
        return output
    }

    private fun centerVisibleContent(source: Bitmap, size: Int, paddingRatio: Float): Bitmap {
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)

        var minX = source.width
        var minY = source.height
        var maxX = -1
        var maxY = -1

        pixels.forEachIndexed { index, pixel ->
            if ((pixel ushr 24) > 8) {
                val x = index % source.width
                val y = index / source.width
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }

        if (maxX < minX || maxY < minY) {
            return centerCrop(source, size)
        }

        val sourceRect = Rect(minX, minY, maxX + 1, maxY + 1)
        val contentWidth = sourceRect.width().coerceAtLeast(1)
        val contentHeight = sourceRect.height().coerceAtLeast(1)
        val padding = (size * paddingRatio).toInt()
        val targetSize = (size - padding * 2).coerceAtLeast(1)
        val scale = min(targetSize.toFloat() / contentWidth, targetSize.toFloat() / contentHeight)
        val destWidth = contentWidth * scale
        val destHeight = contentHeight * scale
        val destRect = RectF(
            (size - destWidth) / 2f,
            (size - destHeight) / 2f,
            (size + destWidth) / 2f,
            (size + destHeight) / 2f
        )
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(source, sourceRect, destRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        source.recycle()
        return output
    }

    private fun calculateInSampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        var halfWidth = width / 2
        var halfHeight = height / 2
        while (halfWidth / sample >= target && halfHeight / sample >= target) {
            sample *= 2
        }
        return max(1, sample)
    }

    private fun openInputStream(context: Context, uri: Uri): InputStream? {
        return if (uri.scheme == "file") {
            File(uri.path.orEmpty()).takeIf { it.exists() }?.let { FileInputStream(it) }
        } else {
            context.contentResolver.openInputStream(uri)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun ensureMediaStoreFolder(context: Context) {
        if (findKeepFile(context) != null) return
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, KEEP_FILE)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        runCatching {
            context.contentResolver.insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun findKeepFile(context: Context): Uri? {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?"
        resolver.query(collection, projection, selection, arrayOf(RELATIVE_PATH, KEEP_FILE), null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return ContentUris.withAppendedId(collection, cursor.getLong(0))
            }
        }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun listMediaStoreImages(context: Context): List<LocalIconImage> {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        val sort = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        val images = mutableListOf<LocalIconImage>()
        resolver.query(collection, projection, selection, arrayOf(RELATIVE_PATH), sort)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex).orEmpty()
                if (name.startsWith(".")) continue
                val mimeType = cursor.getString(mimeIndex).orEmpty()
                if (!isSupportedIconFile(name, mimeType)) continue
                val id = cursor.getLong(idIndex)
                images.add(
                    LocalIconImage(
                        uri = ContentUris.withAppendedId(collection, id),
                        name = name,
                        sizeBytes = cursor.getLong(sizeIndex),
                        mimeType = mimeType
                    )
                )
            }
        }
        return images
    }

    private fun listLegacyImages(): List<LocalIconImage> {
        val dir = legacyImageDirectory()
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            .orEmpty()
            .filter { it.isFile && isSupportedIconFile(it.name, "image/${it.extension.lowercase(Locale.ROOT)}") }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                LocalIconImage(
                    uri = Uri.fromFile(file),
                    name = file.name,
                    sizeBytes = file.length(),
                    mimeType = "image/${file.extension.lowercase(Locale.ROOT)}"
                )
            }
    }

    private fun isSupportedIconFile(name: String, mimeType: String): Boolean {
        val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase(Locale.ROOT)
        return extension in setOf("jpg", "jpeg", "png", "webp", "bmp", "heic", "heif", "svg") ||
            mimeType.startsWith("image/", ignoreCase = true) ||
            mimeType.equals("application/svg+xml", ignoreCase = true) ||
            mimeType.equals("text/xml", ignoreCase = true) ||
            mimeType.equals("application/xml", ignoreCase = true)
    }

    private fun isSvgFile(context: Context, uri: Uri): Boolean {
        val mimeType = runCatching { context.contentResolver.getType(uri).orEmpty() }.getOrDefault("")
        if (mimeType.contains("svg", ignoreCase = true)) return true

        val displayName = getDisplayName(context, uri)
        if (displayName.endsWith(".svg", ignoreCase = true)) return true
        if (uri.toString().substringBefore('?').endsWith(".svg", ignoreCase = true)) return true

        return runCatching {
            openInputStream(context, uri).use { stream ->
                if (stream == null) return@use false
                val probe = ByteArray(512)
                val read = stream.read(probe)
                if (read <= 0) return@use false
                String(probe, 0, read, Charsets.UTF_8).contains("<svg", ignoreCase = true)
            }
        }.getOrDefault(false)
    }

    private fun getDisplayName(context: Context, uri: Uri): String {
        if (uri.scheme == "file") return File(uri.path.orEmpty()).name
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0).orEmpty()
                }
            }
        }
        return uri.lastPathSegment.orEmpty()
    }

    private fun deleteUri(context: Context, uri: Uri): Result<Unit> {
        return runCatching {
            if (uri.scheme == "file") {
                val file = File(uri.path.orEmpty())
                if (file.exists() && !file.delete()) {
                    throw IllegalStateException("文件删除失败，请检查文件是否被占用或权限是否足够。")
                }
            } else {
                val deleted = context.contentResolver.delete(uri, null, null)
                if (deleted <= 0) {
                    throw IllegalStateException("文件删除失败，请检查 Documents/PassCard/images 的写入权限。")
                }
            }
        }
    }

    private fun nextIconFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return "icon_$stamp.webp"
    }

    private fun cacheKey(iconValue: String, maxSize: Int): String {
        return "$iconValue#$maxSize"
    }

    private fun legacyImageDirectory(): File {
        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), DIRECTORY_NAME)
    }
}
