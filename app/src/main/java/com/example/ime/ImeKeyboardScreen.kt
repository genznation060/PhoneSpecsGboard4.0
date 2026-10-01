package com.example.ime

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PhoneField
import com.example.data.model.PhoneRecord
import com.example.data.model.PhoneWithFields
import com.example.data.model.SpecCategory
import com.example.data.queue.PasteQueueManager
import com.example.data.queue.QueueSeparator
import com.example.data.queue.QueuedItem
import com.example.data.repository.SpecRepository
import com.example.ui.theme.ImeBarSurface
import com.example.ui.theme.ImeKeyBackground
import com.example.ui.theme.ImeKeySecondary
import com.example.ui.theme.ImeKeyText
import com.example.ui.theme.ImeKeyboardSurface
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.PrimaryCyanDark
import kotlinx.coroutines.launch

enum class ImeMode {
    BRANDS,
    PHONES,
    SPEC_CATEGORIES,
    QUEUE,
    FAVORITES,
    RECENT,
    SEARCH,
    KEYPAD
}

@Composable
fun ImeKeyboardScreen(
    repository: SpecRepository,
    onInsertText: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onSwitchIme: () -> Unit,
    onHideKeyboard: () -> Unit,
    modifier: Modifier? = null
) {
    val rootMod = modifier ?: Modifier
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentMode by remember { mutableStateOf(ImeMode.BRANDS) }
    var selectedBrand by remember { mutableStateOf<String?>(null) }
    var selectedPhone by remember { mutableStateOf<PhoneRecord?>(null) }
    var selectedCategory by remember { mutableStateOf(SpecCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val allBrands by repository.allBrands.collectAsState(initial = emptyList())
    val allPhones by repository.allPhones.collectAsState(initial = emptyList())
    val favoritePhones by repository.favoritePhones.collectAsState(initial = emptyList())
    val recentPhones by repository.recentPhones.collectAsState(initial = emptyList())
    val queueItems by PasteQueueManager.queue.collectAsState()
    val selectedSeparator by PasteQueueManager.selectedSeparator.collectAsState()

    var phoneWithFields by remember { mutableStateOf<PhoneWithFields?>(null) }

    LaunchedEffect(selectedPhone) {
        val p = selectedPhone
        phoneWithFields = if (p != null) {
            repository.getPhoneWithFieldsSync(p.id)
        } else null
    }

    fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(20)
                }
            }
        } catch (_: Exception) {}
    }

    fun insert(text: String) {
        vibrate()
        onInsertText(text)
    }

    Box(
        modifier = rootMod
            .fillMaxWidth()
            .height(320.dp)
            .background(ImeKeyboardSurface)
            .testTag("ime_keyboard_container")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(ImeBarSurface)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left breadcrumbs or Back
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (currentMode != ImeMode.BRANDS && currentMode != ImeMode.KEYPAD) {
                        IconButton(
                            onClick = {
                                when (currentMode) {
                                    ImeMode.SPEC_CATEGORIES -> {
                                        currentMode = ImeMode.PHONES
                                    }
                                    ImeMode.PHONES -> {
                                        currentMode = ImeMode.BRANDS
                                        selectedBrand = null
                                    }
                                    else -> {
                                        currentMode = ImeMode.BRANDS
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp).testTag("ime_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = PrimaryCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Breadcrumb trail
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BRANDS",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedBrand == null && currentMode == ImeMode.BRANDS) PrimaryCyan else ImeKeySecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    currentMode = ImeMode.BRANDS
                                    selectedBrand = null
                                    selectedPhone = null
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )

                        if (selectedBrand != null) {
                            Text("›", color = ImeKeySecondary, fontSize = 12.sp)
                            Text(
                                text = selectedBrand.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedPhone == null && currentMode == ImeMode.PHONES) PrimaryCyan else ImeKeySecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        currentMode = ImeMode.PHONES
                                        selectedPhone = null
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        if (selectedPhone != null) {
                            Text("›", color = ImeKeySecondary, fontSize = 12.sp)
                            Text(
                                text = selectedPhone?.model.orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryCyan,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Right quick mode buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { currentMode = ImeMode.SEARCH },
                        modifier = Modifier.size(34.dp).testTag("ime_search_mode_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (currentMode == ImeMode.SEARCH) PrimaryCyan else ImeKeySecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { currentMode = ImeMode.FAVORITES },
                        modifier = Modifier.size(34.dp).testTag("ime_fav_mode_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorites",
                            tint = if (currentMode == ImeMode.FAVORITES) Color(0xFFEF4444) else ImeKeySecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { currentMode = ImeMode.RECENT },
                        modifier = Modifier.size(34.dp).testTag("ime_recent_mode_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Recent",
                            tint = if (currentMode == ImeMode.RECENT) PrimaryCyan else ImeKeySecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Queue button with badge
                    IconButton(
                        onClick = { currentMode = ImeMode.QUEUE },
                        modifier = Modifier.size(34.dp).testTag("ime_queue_mode_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (queueItems.isNotEmpty()) {
                                    Badge(containerColor = PrimaryCyan) {
                                        Text("${queueItems.size}", fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAddCheck,
                                contentDescription = "Queue",
                                tint = if (currentMode == ImeMode.QUEUE) PrimaryCyan else ImeKeySecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            currentMode = if (currentMode == ImeMode.KEYPAD) ImeMode.BRANDS else ImeMode.KEYPAD
                        },
                        modifier = Modifier.size(34.dp).testTag("ime_keypad_mode_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Keypad",
                            tint = if (currentMode == ImeMode.KEYPAD) PrimaryCyan else ImeKeySecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // MIDDLE CONTENT (Height adjusted for top and bottom bar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(ImeKeyboardSurface)
            ) {
                if (allPhones.isEmpty() && currentMode != ImeMode.KEYPAD) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No smartphone records yet.\nImport TSV in main app or load sample flagships.",
                            color = ImeKeySecondary,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    when (currentMode) {
                        ImeMode.BRANDS -> {
                            ImeBrandsView(
                                brands = allBrands,
                                onSelectBrand = { brand ->
                                    selectedBrand = brand
                                    currentMode = ImeMode.PHONES
                                }
                            )
                        }
                        ImeMode.PHONES -> {
                            val brandPhones = remember(allPhones, selectedBrand) {
                                if (selectedBrand != null) {
                                    allPhones.filter { it.brand.equals(selectedBrand, ignoreCase = true) }
                                } else allPhones
                            }
                            ImePhonesView(
                                phones = brandPhones,
                                onSelectPhone = { phone ->
                                    selectedPhone = phone
                                    currentMode = ImeMode.SPEC_CATEGORIES
                                },
                                onInsertFullName = { fullName ->
                                    insert(fullName)
                                }
                            )
                        }
                        ImeMode.SPEC_CATEGORIES -> {
                            ImeSpecCategoriesView(
                                phoneWithFields = phoneWithFields,
                                selectedCategory = selectedCategory,
                                onSelectCategory = { selectedCategory = it },
                                onInsertField = { field ->
                                    insert(field.fieldValue)
                                    scope.launch {
                                        repository.incrementFieldUsage(field.id, field.phoneId)
                                    }
                                },
                                onAddToQueue = { field ->
                                    PasteQueueManager.add(
                                        label = field.fieldName,
                                        value = field.fieldValue,
                                        sourcePhone = selectedPhone?.fullName.orEmpty()
                                    )
                                    vibrate()
                                    scope.launch {
                                        repository.incrementFieldUsage(field.id, field.phoneId)
                                    }
                                }
                            )
                        }
                        ImeMode.QUEUE -> {
                            ImeQueueView(
                                items = queueItems,
                                selectedSeparator = selectedSeparator,
                                onSelectSeparator = { PasteQueueManager.setSeparator(it) },
                                onInsertNext = {
                                    val next = PasteQueueManager.popNext()
                                    if (next != null) insert(next)
                                },
                                onInsertAll = {
                                    val text = PasteQueueManager.drainAll()
                                    if (text.isNotBlank()) insert(text)
                                },
                                onRemoveItem = { PasteQueueManager.remove(it) },
                                onClearAll = { PasteQueueManager.clear() }
                            )
                        }
                        ImeMode.FAVORITES -> {
                            ImePhonesView(
                                phones = favoritePhones,
                                onSelectPhone = { phone ->
                                    selectedPhone = phone
                                    currentMode = ImeMode.SPEC_CATEGORIES
                                },
                                onInsertFullName = { insert(it) }
                            )
                        }
                        ImeMode.RECENT -> {
                            ImePhonesView(
                                phones = recentPhones,
                                onSelectPhone = { phone ->
                                    selectedPhone = phone
                                    currentMode = ImeMode.SPEC_CATEGORIES
                                },
                                onInsertFullName = { insert(it) }
                            )
                        }
                        ImeMode.SEARCH -> {
                            ImeSearchView(
                                repository = repository,
                                onInsert = { insert(it) },
                                onQueue = { label, value, phone ->
                                    PasteQueueManager.add(label, value, phone)
                                    vibrate()
                                }
                            )
                        }
                        ImeMode.KEYPAD -> {
                            ImeKeypadView(
                                onChar = { insert(it) },
                                onBackspace = onBackspace,
                                onEnter = onEnter,
                                onSpace = onSpace
                            )
                        }
                    }
                }
            }

            // BOTTOM TOOLBAR (Always Visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(ImeBarSurface)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Switch IME Globe
                Surface(
                    color = ImeKeyBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { onSwitchIme() }
                        .testTag("ime_globe_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Switch Input Method",
                            tint = ImeKeyText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Phones/Brands toggle
                Surface(
                    color = if (currentMode == ImeMode.BRANDS || currentMode == ImeMode.PHONES) PrimaryCyanDark else ImeKeyBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable {
                            currentMode = ImeMode.BRANDS
                            selectedBrand = null
                            selectedPhone = null
                        }
                        .testTag("ime_brands_shortcut_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Brands",
                            tint = ImeKeyText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Wide Space Bar
                Surface(
                    color = ImeKeyBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clickable {
                            vibrate()
                            onSpace()
                        }
                        .testTag("ime_space_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "SPACE",
                            style = MaterialTheme.typography.labelSmall,
                            color = ImeKeySecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Backspace Key
                Surface(
                    color = ImeKeyBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable {
                            vibrate()
                            onBackspace()
                        }
                        .testTag("ime_backspace_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = ImeKeyText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Enter Key (Cyan)
                Surface(
                    color = PrimaryCyan,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable {
                            vibrate()
                            onEnter()
                        }
                        .testTag("ime_enter_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                            contentDescription = "Enter",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Hide Keyboard Key
                Surface(
                    color = ImeKeyBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { onHideKeyboard() }
                        .testTag("ime_hide_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardHide,
                            contentDescription = "Hide Keyboard",
                            tint = ImeKeySecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// 1. Brands View
@Composable
private fun ImeBrandsView(
    brands: List<String>,
    onSelectBrand: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 85.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize().testTag("ime_brands_grid")
    ) {
        items(brands) { brand ->
            Surface(
                color = ImeBarSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable { onSelectBrand(brand) }
                    .testTag("ime_brand_item_$brand")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = PrimaryCyanDark,
                        shape = CircleShape,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = brand.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                    Text(
                        text = brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = ImeKeyText,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// 2. Phones View
@Composable
private fun ImePhonesView(
    phones: List<PhoneRecord>,
    onSelectPhone: (PhoneRecord) -> Unit,
    onInsertFullName: (String) -> Unit
) {
    if (phones.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No phones found", color = ImeKeySecondary, fontSize = 12.sp)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize().testTag("ime_phones_list")
    ) {
        items(phones, key = { it.id }) { phone ->
            Surface(
                color = ImeBarSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPhone(phone) }
                    .testTag("ime_phone_row_${phone.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = phone.fullName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = ImeKeyText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${phone.brand} • ${phone.model}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ImeKeySecondary
                        )
                    }

                    // Direct Full Name Paste Chip
                    Surface(
                        color = PrimaryCyan.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable { onInsertFullName(phone.fullName) }
                            .testTag("ime_paste_name_${phone.id}")
                    ) {
                        Text(
                            text = "Paste Name",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// 3. Spec Categories & Fields View
@Composable
private fun ImeSpecCategoriesView(
    phoneWithFields: PhoneWithFields?,
    selectedCategory: SpecCategory,
    onSelectCategory: (SpecCategory) -> Unit,
    onInsertField: (PhoneField) -> Unit,
    onAddToQueue: (PhoneField) -> Unit
) {
    if (phoneWithFields == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading phone specs...", color = ImeKeySecondary, fontSize = 12.sp)
        }
        return
    }

    val fields = phoneWithFields.fields
    val filtered = remember(fields, selectedCategory) {
        if (selectedCategory == SpecCategory.ALL) {
            fields.sortedBy { it.columnIndex }
        } else {
            fields.filter {
                it.category.equals(selectedCategory.name, ignoreCase = true) ||
                        it.category.equals(selectedCategory.title, ignoreCase = true)
            }.sortedBy { it.columnIndex }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Category Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(ImeBarSurface.copy(alpha = 0.5f))
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(SpecCategory.entries.toTypedArray()) { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    color = if (isSelected) cat.color else ImeKeyBackground,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .clickable { onSelectCategory(cat) }
                        .testTag("ime_cat_${cat.name}")
                ) {
                    Text(
                        text = cat.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color.White else ImeKeyText,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Specs List
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize().testTag("ime_specs_list")
        ) {
            items(filtered, key = { it.id }) { field ->
                val cat = SpecCategory.fromString(field.category)
                Surface(
                    color = ImeBarSurface,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInsertField(field) }
                        .testTag("ime_field_${field.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    color = cat.color,
                                    shape = CircleShape,
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Text(
                                    text = field.fieldName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ImeKeySecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = field.fieldValue.ifBlank { "—" },
                                style = MaterialTheme.typography.bodySmall,
                                color = ImeKeyText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Queue button
                        IconButton(
                            onClick = { onAddToQueue(field) },
                            modifier = Modifier.size(28.dp).testTag("ime_queue_add_${field.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add to queue",
                                tint = PrimaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 4. Queue View
@Composable
private fun ImeQueueView(
    items: List<QueuedItem>,
    selectedSeparator: QueueSeparator,
    onSelectSeparator: (QueueSeparator) -> Unit,
    onInsertNext: () -> Unit,
    onInsertAll: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Separator selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Separator:",
                style = MaterialTheme.typography.labelSmall,
                color = ImeKeySecondary
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                QueueSeparator.entries.forEach { sep ->
                    Surface(
                        color = if (selectedSeparator == sep) PrimaryCyan else ImeKeyBackground,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { onSelectSeparator(sep) }
                    ) {
                        Text(
                            text = sep.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedSeparator == sep) Color(0xFF0F172A) else ImeKeyText,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Action buttons: Insert Next, Insert All, Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onInsertNext,
                enabled = items.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .testTag("ime_queue_insert_next"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
            ) {
                Text("Insert Next (${items.size})", fontSize = 11.sp, color = Color(0xFF0F172A))
            }

            Button(
                onClick = onInsertAll,
                enabled = items.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .testTag("ime_queue_insert_all"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Insert All", fontSize = 11.sp, color = Color.White)
            }

            if (items.isNotEmpty()) {
                Surface(
                    color = Color(0xFFEF4444).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .size(34.dp)
                        .clickable { onClearAll() }
                        .testTag("ime_queue_clear")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // List of queued items
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Queue is empty. Tap '+' on any spec to add it here.",
                    color = ImeKeySecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    Surface(
                        color = ImeBarSurface,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryCyan
                                )
                                Text(
                                    text = item.value,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ImeKeyText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onRemoveItem(item.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = ImeKeySecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 5. Search View
@Composable
private fun ImeSearchView(
    repository: SpecRepository,
    onInsert: (String) -> Unit,
    onQueue: (String, String, String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results by repository.searchFields(query).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search fields & values...", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("ime_search_field"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ImeBarSurface,
                unfocusedContainerColor = ImeBarSurface,
                focusedBorderColor = PrimaryCyan,
                unfocusedBorderColor = ImeKeyBackground,
                focusedTextColor = ImeKeyText,
                unfocusedTextColor = ImeKeyText
            ),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = ImeKeySecondary)
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(results, key = { it.id }) { field ->
                Surface(
                    color = ImeBarSurface,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onInsert(field.fieldValue) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = field.fieldName,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryCyan
                            )
                            Text(
                                text = field.fieldValue,
                                style = MaterialTheme.typography.bodySmall,
                                color = ImeKeyText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { onQueue(field.fieldName, field.fieldValue, "") },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Queue",
                                tint = PrimaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 6. Compact QWERTY Keypad
@Composable
private fun ImeKeypadView(
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit
) {
    var isNumbers by remember { mutableStateOf(false) }
    var isShifted by remember { mutableStateOf(false) }

    val row1 = if (isNumbers) listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    else listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")

    val row2 = if (isNumbers) listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/")
    else listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")

    val row3 = if (isNumbers) listOf("*", "\"", "'", ":", ";", "!", "?")
    else listOf("z", "x", "c", "v", "b", "n", "m")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            row1.forEach { char ->
                val display = if (isShifted) char.uppercase() else char
                KeyButton(text = display, modifier = Modifier.weight(1f)) { onChar(display) }
            }
        }

        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Spacer(modifier = Modifier.weight(0.5f))
            row2.forEach { char ->
                val display = if (isShifted) char.uppercase() else char
                KeyButton(text = display, modifier = Modifier.weight(1f)) { onChar(display) }
            }
            Spacer(modifier = Modifier.weight(0.5f))
        }

        // Row 3 (Shift, letters, 123)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Surface(
                color = if (isShifted) PrimaryCyan else ImeKeyBackground,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(38.dp)
                    .clickable { isShifted = !isShifted }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "⇧",
                        fontWeight = FontWeight.Bold,
                        color = if (isShifted) Color(0xFF0F172A) else ImeKeyText
                    )
                }
            }

            row3.forEach { char ->
                val display = if (isShifted) char.uppercase() else char
                KeyButton(text = display, modifier = Modifier.weight(1f)) { onChar(display) }
            }

            Surface(
                color = if (isNumbers) PrimaryCyan else ImeKeyBackground,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(38.dp)
                    .clickable { isNumbers = !isNumbers }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isNumbers) "ABC" else "?123",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isNumbers) Color(0xFF0F172A) else ImeKeyText
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    text: String,
    modifier: Modifier? = null,
    onClick: () -> Unit
) {
    val buttonMod = modifier ?: Modifier
    Surface(
        color = ImeKeyBackground,
        shape = RoundedCornerShape(6.dp),
        modifier = buttonMod
            .height(38.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = ImeKeyText,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}
