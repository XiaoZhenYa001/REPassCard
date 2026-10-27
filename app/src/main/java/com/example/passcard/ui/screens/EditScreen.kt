package com.example.passcard.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.view.Gravity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.passcard.ui.components.*
import com.example.passcard.ui.theme.*
import com.example.passcard.data.PasswordCategories
import com.example.passcard.util.ClipboardHelper
import com.example.passcard.util.LocalIconImage
import com.example.passcard.util.PasswordIconStorage
import com.example.passcard.util.PasswordIconType
import com.example.passcard.util.RandomPasswordGenerator
import com.example.passcard.util.RandomPasswordSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val ICON_PICKER_MIME_TYPES = arrayOf(
    "image/*",
    "image/svg+xml",
    "application/svg+xml",
    "text/xml",
    "application/xml",
    "*/*"
)

data class EditUiState(
    val id: String = "",
    val name: String = "",
    val username: String = "",
    val phone: String = "",
    val email: String = "",
    val password: String = "",
    val category: String = "",
    val note: String = "",
    val iconType: String = PasswordIconType.GENERATED,
    val iconValue: String = "",
    val isNew: Boolean = true
)

// EditUiState 的 Saver，用于 rememberSaveable 防止屏幕旋转数据丢失
val EditUiStateSaver = listSaver<EditUiState, Any>(
    save = { state ->
        listOf(
            state.id, state.name, state.username, state.phone, state.email,
            state.password, state.category, state.note, state.iconType, state.iconValue, state.isNew
        )
    },
    restore = { list ->
        EditUiState(
            id = list[0] as String,
            name = list[1] as String,
            username = list[2] as String,
            phone = list[3] as String,
            email = list[4] as String,
            password = list[5] as String,
            category = list[6] as String,
            note = list[7] as String,
            iconType = list[8] as String,
            iconValue = list[9] as String,
            isNew = list[10] as Boolean
        )
    }
)

@Composable
fun EditScreen(
    onBack: () -> Unit,
    onSave: suspend (PasswordItem) -> Unit,
    onDelete: suspend () -> Unit,
    modifier: Modifier = Modifier,
    password: PasswordItem? = null,
    currentLanguage: AppLanguage = AppLanguage.CHINESE,
    randomPasswordSpec: RandomPasswordSpec? = null,
    availableCategories: List<String> = emptyList(),
    loadAllPasswords: suspend () -> List<PasswordItem> = { emptyList() }
) {
    val themeColors = rememberThemeColors()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isZh = currentLanguage == AppLanguage.CHINESE
    val generationSpec = remember(randomPasswordSpec) {
        (randomPasswordSpec ?: RandomPasswordSpec()).normalized()
    }
    
    // 使用 rememberSaveable 防止屏幕旋转数据丢失
    val initialState = remember(password) {
        if (password != null) {
            EditUiState(
                id = password.id,
                name = password.name,
                username = password.username,
                phone = password.phone,
                email = password.email,
                password = password.password,
                category = password.category,
                note = password.note,
                iconType = password.iconType,
                iconValue = password.iconValue,
                isNew = false
            )
        } else {
            EditUiState()
        }
    }
    var uiState by rememberSaveable(password?.id, stateSaver = EditUiStateSaver) {
        mutableStateOf(initialState)
    }
    val categories = remember(availableCategories, uiState.category) {
        PasswordCategories.availableKeys(PasswordCategories.commonKeys + availableCategories + uiState.category)
            .filter { it.isNotEmpty() }
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf("") }
    fun requestBack() {
        if (isSaving) return
        if (uiState != initialState) showDiscardDialog = true else onBack()
    }
    BackHandler { requestBack() }

    val originalIconType = password?.iconType ?: PasswordIconType.GENERATED
    val originalIconValue = password?.iconValue.orEmpty()
    var showIconPicker by remember { mutableStateOf(false) }
    var pickerIconType by remember { mutableStateOf(uiState.iconType) }
    var pickerIconValue by remember { mutableStateOf(uiState.iconValue) }
    var deleteOldIconOnSave by rememberSaveable { mutableStateOf(true) }
    var localImages by remember { mutableStateOf<List<LocalIconImage>>(emptyList()) }
    var busyImageUri by remember { mutableStateOf<String?>(null) }
    var isImportingImage by remember { mutableStateOf(false) }

    fun showImageError(error: Throwable) {
        Toast.makeText(
            context,
            error.message ?: if (currentLanguage == AppLanguage.CHINESE) "图片处理失败" else "Image processing failed",
            Toast.LENGTH_LONG
        ).show()
    }

    fun copyPassword() {
        if (uiState.password.isEmpty()) return
        ClipboardHelper.copyToClipboard(
            context = context,
            text = uiState.password,
            label = "Password",
            showToast = false
        )
        Toast.makeText(
            context,
            if (currentLanguage == AppLanguage.CHINESE) "已复制密码" else "Password copied",
            Toast.LENGTH_SHORT
        ).apply {
            val topOffset = (context.resources.displayMetrics.density * 72).roundToInt()
            setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, topOffset)
        }.show()
    }

    fun hasImageReadPermission(): Boolean {
        return PasswordIconStorage.requiredReadPermissions().any { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun loadLocalImages() {
        scope.launch {
            val result = withContext(Dispatchers.IO) { PasswordIconStorage.listLocalImages(context) }
            result.onSuccess { localImages = it }
                .onFailure { showImageError(it) }
        }
    }

    val imagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            loadLocalImages()
        } else {
            val message = if (currentLanguage == AppLanguage.CHINESE) {
                "无法读取本地图片：请允许图片访问权限，或使用上传按钮选择图片。"
            } else {
                "Cannot read local images. Allow image access or use Upload to pick an image."
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun refreshLocalImages() {
        if (!hasImageReadPermission()) {
            imagePermissionLauncher.launch(PasswordIconStorage.requiredReadPermissions())
        } else {
            loadLocalImages()
        }
    }

    val uploadIconLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        scope.launch {
            isImportingImage = true
            val result = withContext(Dispatchers.IO) { PasswordIconStorage.importPickedIcon(context, uri) }
            result.onSuccess { saved ->
                pickerIconType = PasswordIconType.IMAGE
                pickerIconValue = saved.uriString
                refreshLocalImages()
                saved.warning?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
            }.onFailure { showImageError(it) }
            isImportingImage = false
        }
    }

    fun processLocalImage(image: LocalIconImage) {
        scope.launch {
            busyImageUri = image.uriString
            val result = withContext(Dispatchers.IO) { PasswordIconStorage.optimizeLibraryImage(context, image) }
            result.onSuccess { saved ->
                pickerIconType = PasswordIconType.IMAGE
                pickerIconValue = saved.uriString
                refreshLocalImages()
                saved.warning?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
            }.onFailure { showImageError(it) }
            busyImageUri = null
        }
    }

    fun shouldShowDeleteOldIconOption(): Boolean {
        return originalIconType == PasswordIconType.IMAGE &&
            originalIconValue.isNotBlank() &&
            (pickerIconType != originalIconType || pickerIconValue != originalIconValue)
    }

    suspend fun deleteOldIconIfNeeded() {
        if (!deleteOldIconOnSave) return
        if (originalIconType != PasswordIconType.IMAGE || originalIconValue.isBlank()) return
        if (uiState.iconType == originalIconType && uiState.iconValue == originalIconValue) return

        val referenceCount = runCatching {
            loadAllPasswords().count { item ->
                item.iconType == PasswordIconType.IMAGE && item.iconValue == originalIconValue
            }
        }.getOrDefault(Int.MAX_VALUE)

        // The edited record has already been saved with its new icon.
        if (referenceCount == 0) {
            val result = withContext(Dispatchers.IO) { PasswordIconStorage.deleteIcon(context, originalIconValue) }
            result.onFailure { showImageError(it) }
        }
    }

    fun buildSaveItem(): PasswordItem? {
        val isAllBlank = uiState.name.isBlank() &&
            uiState.username.isBlank() &&
            uiState.phone.isBlank() &&
            uiState.email.isBlank() &&
            uiState.password.isBlank() &&
            uiState.category.isBlank() &&
            uiState.note.isBlank()

        if (uiState.isNew && isAllBlank) {
            val message = if (currentLanguage == AppLanguage.CHINESE) {
                "请至少填写一项"
            } else {
                "Please fill at least one field"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            return null
        }

        val now = System.currentTimeMillis()
        return PasswordItem(
            id = uiState.id.ifEmpty { java.util.UUID.randomUUID().toString() },
            name = uiState.name,
            username = uiState.username,
            phone = uiState.phone,
            email = uiState.email,
            password = uiState.password,
            category = PasswordCategories.key(uiState.category),
            note = uiState.note,
            iconType = uiState.iconType,
            iconValue = uiState.iconValue,
            createdAt = password?.createdAt ?: now,
            updatedAt = now,
            revision = password?.revision ?: 0L,
            deviceId = password?.deviceId.orEmpty(),
            deletedAt = password?.deletedAt
        )
    }
    
    Column(
        modifier = modifier.fillMaxSize().background(themeColors.background).navigationBarsPadding().imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 58.dp)
                .padding(horizontal = Spacing20, vertical = Spacing4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing12)
        ) {
            PressableScale(onClick = { requestBack() }, enabled = !isSaving) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(Radius12))
                        .background(themeColors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = if (isZh) "返回" else "Back",
                        tint = themeColors.onBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Text(
                text = if (uiState.isNew) AppStrings.addPassword(currentLanguage) else AppStrings.editPassword(currentLanguage),
                style = MaterialTheme.typography.titleLarge,
                color = themeColors.onBackground,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            PressableScale(
                enabled = !isSaving,
                onClick = {
                    val item = buildSaveItem() ?: return@PressableScale
                    isSaving = true
                    scope.launch {
                        try {
                            onSave(item)
                            deleteOldIconIfNeeded()
                            onBack()
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            Toast.makeText(context, if (isZh) "保存失败，请重试。" else "Save failed. Please try again.", Toast.LENGTH_LONG).show()
                        } finally {
                            isSaving = false
                        }
                    }
                }
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(Radius12))
                        .background(themeColors.primaryLight)
                        .padding(horizontal = Spacing12, vertical = Spacing8),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSaving) { if (isZh) "保存中…" else "Saving…" } else AppStrings.save(currentLanguage),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.W700),
                        color = themeColors.primary
                    )
                }
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing20)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing16)
        ) {
            Spacer(modifier = Modifier.height(Spacing4))
            
            // 图标选择器
            LogoSelector(
                name = uiState.name,
                iconType = uiState.iconType,
                iconValue = uiState.iconValue,
                onChangeIcon = {
                    if (isSaving) return@LogoSelector
                    pickerIconType = uiState.iconType
                    pickerIconValue = uiState.iconValue
                    deleteOldIconOnSave = true
                    showIconPicker = true
                },
                changeIconText = AppStrings.changeIcon(currentLanguage)
            )
            
            Spacer(modifier = Modifier.height(Spacing4))
            
            // 名称输入
            InputField(
                enabled = !isSaving,
                label = AppStrings.name(currentLanguage),
                value = uiState.name,
                onValueChange = { uiState = uiState.copy(name = it) },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "服务名称" else "Service name"
            )
            
            // 用户名输入
            InputField(
                enabled = !isSaving,
                label = AppStrings.username(currentLanguage),
                value = uiState.username,
                onValueChange = { uiState = uiState.copy(username = it) },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "用户名或邮箱" else "Username or email"
            )
            
            // 手机号输入
            InputField(
                enabled = !isSaving,
                label = AppStrings.phone(currentLanguage),
                value = uiState.phone,
                onValueChange = { uiState = uiState.copy(phone = it) },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "手机号码" else "Phone number"
            )
            
            // 邮箱输入
            InputField(
                enabled = !isSaving,
                label = AppStrings.email(currentLanguage),
                value = uiState.email,
                onValueChange = { uiState = uiState.copy(email = it) },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "邮箱地址" else "Email address"
            )
            
            // 密码输入（带复制功能）
            InputField(
                enabled = !isSaving,
                copyDescription = if (isZh) "复制密码" else "Copy password",
                showDescription = if (isZh) "显示密码" else "Show password",
                hideDescription = if (isZh) "隐藏密码" else "Hide password",
                label = AppStrings.passwordLabel(currentLanguage),
                value = uiState.password,
                onValueChange = { uiState = uiState.copy(password = it) },
                isPassword = true,
                onCopy = if (uiState.password.isNotEmpty()) {
                    { copyPassword() }
                } else {
                    null
                },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "输入密码" else "Enter password"
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Spacing4)
            ) {
                FilledTonalButton(
                    enabled = !isSaving,
                    onClick = {
                        val generated = RandomPasswordGenerator.generate(generationSpec)
                        uiState = uiState.copy(password = generated)
                    },
                    shape = RoundedCornerShape(Radius14),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = themeColors.primaryLight,
                        contentColor = themeColors.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentLanguage == AppLanguage.CHINESE) "生成随机密码" else "Generate Password")
                }
                Text(
                    text = if (isZh) {
                        "使用设置中选定的字符 · ${generationSpec.length} 位"
                    } else {
                        "Uses your selected characters · ${generationSpec.length} characters"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = themeColors.onSurfaceVariant
                )
            }

            CategorySelector(
                label = AppStrings.category(currentLanguage),
                selectedCategory = PasswordCategories.key(uiState.category),
                categories = categories,
                onCategorySelected = { uiState = uiState.copy(category = it) },
                categoryLabel = { PasswordCategories.label(it, isZh) },
                enabled = !isSaving,
                onAddCategory = { newCategory = ""; showCategoryDialog = true },
                addCategoryText = if (isZh) "添加分类" else "Add category"
            )
            
            // 备注输入
            InputField(
                enabled = !isSaving,
                label = AppStrings.note(currentLanguage),
                value = uiState.note,
                onValueChange = { uiState = uiState.copy(note = it) },
                placeholder = if (currentLanguage == AppLanguage.CHINESE) "添加备注..." else "Add note...",
                isMultiline = true
            )
            
            // 删除按钮（仅编辑模式显示）
            if (!uiState.isNew) {
                Spacer(modifier = Modifier.height(Spacing12))
                DeleteButton(
                    text = AppStrings.deletePassword(currentLanguage),
                    onClick = { if (!isSaving) showDeleteDialog = true }
                )
            }
        }
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text(if (isZh) "添加分类" else "Add category") },
            text = { OutlinedTextField(value = newCategory, onValueChange = { newCategory = it }, singleLine = true,
                label = { Text(if (isZh) "分类名称" else "Category name") }) },
            confirmButton = { TextButton(enabled = newCategory.isNotBlank(), onClick = {
                uiState = uiState.copy(category = PasswordCategories.key(newCategory))
                showCategoryDialog = false
            }) { Text(if (isZh) "添加" else "Add") } },
            dismissButton = { TextButton(onClick = { showCategoryDialog = false }) { Text(if (isZh) "取消" else "Cancel") } }
        )
    }
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(if (isZh) "放弃未保存的修改？" else "Discard unsaved changes?") },
            text = { Text(if (isZh) "返回后，本次修改不会保存。" else "Your changes will be lost when you leave.") },
            confirmButton = { TextButton(onClick = { showDiscardDialog = false; onBack() }) {
                Text(if (isZh) "放弃修改" else "Discard", color = themeColors.error)
            } },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) {
                Text(if (isZh) "继续编辑" else "Keep editing")
            } }
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showDeleteDialog = false },
            title = { Text(if (isZh) "删除这条密码记录？" else "Delete this password?") },
            text = { Text(uiState.name.ifBlank { if (isZh) "未命名记录" else "Untitled item" }) },
            confirmButton = { TextButton(enabled = !isSaving, onClick = {
                isSaving = true
                scope.launch {
                    try {
                        onDelete()
                        showDeleteDialog = false
                        onBack()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        Toast.makeText(context, if (isZh) "删除失败，请重试。" else "Delete failed. Please try again.", Toast.LENGTH_LONG).show()
                    } finally {
                        isSaving = false
                    }
                }
            }) { Text(if (isZh) "删除" else "Delete", color = themeColors.error) } },
            dismissButton = { TextButton(enabled = !isSaving, onClick = { showDeleteDialog = false }) {
                Text(if (isZh) "取消" else "Cancel")
            } }
        )
    }

    if (showIconPicker) {
        PasswordIconPickerSheet(
            currentLanguage = currentLanguage,
            label = uiState.name,
            selectedIconType = pickerIconType,
            selectedIconValue = pickerIconValue,
            localImages = localImages,
            busyImageUri = busyImageUri,
            isImportingImage = isImportingImage,
            canDeleteOldImage = shouldShowDeleteOldIconOption(),
            deleteOldImage = deleteOldIconOnSave,
            onDeleteOldImageChange = { deleteOldIconOnSave = it },
            onSelectedIconChange = { type, value ->
                pickerIconType = type
                pickerIconValue = value
            },
            onRefreshLocalImages = { refreshLocalImages() },
            onLocalImageClick = { image -> processLocalImage(image) },
            onUploadClick = { uploadIconLauncher.launch(ICON_PICKER_MIME_TYPES) },
            onDismiss = { showIconPicker = false },
            onConfirm = {
                uiState = uiState.copy(iconType = pickerIconType, iconValue = pickerIconValue)
                showIconPicker = false
            }
        )
    }
}
