package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.dialogs.DuplicateResolutionDialog
import com.example.ui.dialogs.ImportTsvDialog
import com.example.ui.screens.ClipboardScreen
import com.example.ui.screens.KeyboardSandboxScreen
import com.example.ui.screens.PhoneDetailScreen
import com.example.ui.screens.PhonesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.SpecBoardTheme
import com.example.ui.viewmodel.SpecViewModel

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    PHONES("Phones", Icons.Filled.PhoneAndroid, Icons.Outlined.PhoneAndroid),
    CLIPBOARD("Clipboard", Icons.Filled.Assignment, Icons.Outlined.Assignment),
    KEYBOARD("Keyboard", Icons.Filled.Keyboard, Icons.Outlined.Keyboard),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    private val viewModel: SpecViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SpecBoardTheme(darkTheme = true) {
                val snackbarHostState = remember { SnackbarHostState() }
                var selectedTabIndex by remember { mutableIntStateOf(0) }
                val currentTab = MainTab.entries[selectedTabIndex]

                val phones by viewModel.phonesList.collectAsState()
                val allBrands by viewModel.allBrands.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val selectedBrand by viewModel.selectedBrand.collectAsState()
                val selectedPhoneId by viewModel.selectedPhoneId.collectAsState()
                val selectedPhoneWithFields by viewModel.selectedPhoneWithFields.collectAsState()

                val clipboardItems by viewModel.clipboardItems.collectAsState()

                val brandColIdx by viewModel.brandColIndex.collectAsState()
                val modelColIdx by viewModel.modelColIndex.collectAsState()
                val fullNameColIdx by viewModel.fullNameColIndex.collectAsState()

                val pendingDuplicates by viewModel.pendingDuplicates.collectAsState()

                var showImportDialog by remember { mutableStateOf(false) }
                var importInitialText by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    viewModel.userMessage.collect { msg ->
                        snackbarHostState.showSnackbar(msg)
                    }
                }

                // If phone detail is open, show PhoneDetailScreen
                if (selectedPhoneId != null) {
                    PhoneDetailScreen(
                        phoneWithFields = selectedPhoneWithFields,
                        onBack = { viewModel.closePhoneDetail() },
                        onToggleFavorite = { id, current -> viewModel.toggleFavorite(id, current) },
                        onTogglePin = { id, current -> viewModel.togglePin(id, current) },
                        onFieldUsed = { field -> viewModel.incrementFieldUsage(field) },
                        onUpdateField = { field -> viewModel.updateField(field) }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = when (currentTab) {
                                            MainTab.PHONES -> "SpecBoard"
                                            MainTab.CLIPBOARD -> "Spec Clipboard"
                                            MainTab.KEYBOARD -> "IME Sandbox"
                                            MainTab.SETTINGS -> "Settings"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                )
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 4.dp
                            ) {
                                MainTab.entries.forEachIndexed { index, tab ->
                                    val isSelected = selectedTabIndex == index
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { selectedTabIndex = index },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.title,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.title,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = PrimaryCyan,
                                            selectedTextColor = PrimaryCyan,
                                            indicatorColor = PrimaryCyan.copy(alpha = 0.15f)
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                MainTab.PHONES -> {
                                    PhonesScreen(
                                        phones = phones,
                                        allBrands = allBrands,
                                        searchQuery = searchQuery,
                                        selectedBrand = selectedBrand,
                                        onSearchChange = { viewModel.setSearchQuery(it) },
                                        onBrandSelect = { viewModel.selectBrand(it) },
                                        onPhoneClick = { viewModel.openPhoneDetail(it) },
                                        onToggleFavorite = { id, fav -> viewModel.toggleFavorite(id, fav) },
                                        onTogglePin = { id, pin -> viewModel.togglePin(id, pin) },
                                        onDeletePhone = { viewModel.deletePhone(it) },
                                        onOpenImport = {
                                            importInitialText = ""
                                            showImportDialog = true
                                        },
                                        onLoadSamples = { viewModel.seedSampleFlagships() }
                                    )
                                }

                                MainTab.CLIPBOARD -> {
                                    ClipboardScreen(
                                        items = clipboardItems,
                                        onSaveToHistory = { viewModel.saveClipboard(it) },
                                        onDelete = { viewModel.deleteClipboardItem(it) },
                                        onToggleFavorite = { viewModel.toggleClipboardFavorite(it) },
                                        onTogglePin = { viewModel.toggleClipboardPin(it) },
                                        onClearAll = { viewModel.clearClipboardHistory() },
                                        onOpenImportWithText = { text ->
                                            importInitialText = text
                                            showImportDialog = true
                                        }
                                    )
                                }

                                MainTab.KEYBOARD -> {
                                    KeyboardSandboxScreen(
                                        repository = (application as SpecBoardApplication).repository
                                    )
                                }

                                MainTab.SETTINGS -> {
                                    SettingsScreen(
                                        brandColIdx = brandColIdx,
                                        modelColIdx = modelColIdx,
                                        fullNameColIdx = fullNameColIdx,
                                        onUpdateColumnMapping = { b, m, f -> viewModel.updateColumnMapping(b, m, f) },
                                        onLoadSamples = { viewModel.seedSampleFlagships() },
                                        onClearAllData = { viewModel.clearAllData() },
                                        onExportJson = { viewModel.exportJson() },
                                        onExportTsv = { viewModel.exportTsv() },
                                        onExportCsv = { viewModel.exportCsv() },
                                        onRestoreJson = { json, cb -> viewModel.restoreJson(json, cb) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Import TSV Dialog
                if (showImportDialog) {
                    ImportTsvDialog(
                        initialText = importInitialText,
                        brandColIdx = brandColIdx,
                        modelColIdx = modelColIdx,
                        fullNameColIdx = fullNameColIdx,
                        onDismiss = {
                            showImportDialog = false
                            viewModel.clearImportPreview()
                        },
                        onConfirmImport = { tsvText ->
                            viewModel.previewTsvImport(tsvText)
                            viewModel.executeImport {
                                // duplicates found handled by pendingDuplicates state
                            }
                            showImportDialog = false
                        }
                    )
                }

                // Duplicate Resolution Dialog
                if (pendingDuplicates.isNotEmpty()) {
                    val currentConflict = pendingDuplicates.first()
                    DuplicateResolutionDialog(
                        conflict = currentConflict,
                        totalRemaining = pendingDuplicates.size,
                        onAction = { action -> viewModel.resolveNextDuplicate(action) },
                        onApplyAllRemaining = { action -> viewModel.resolveAllRemainingDuplicates(action) },
                        onDismiss = { viewModel.resolveNextDuplicate(com.example.data.repository.DuplicateAction.SKIP) }
                    )
                }
            }
        }
    }
}
