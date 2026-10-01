package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SpecBoardApplication
import com.example.data.model.ClipboardItem
import com.example.data.model.PhoneField
import com.example.data.model.PhoneRecord
import com.example.data.model.PhoneWithFields
import com.example.data.queue.PasteQueueManager
import com.example.data.repository.DuplicateAction
import com.example.data.repository.DuplicateConflict
import com.example.data.tsv.CanonicalSpecs
import com.example.data.tsv.ParsedPhone
import com.example.data.tsv.TsvParseResult
import com.example.data.tsv.TsvParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SpecViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as SpecBoardApplication).repository

    // Search and Brand filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBrand = MutableStateFlow<String?>(null) // null means All
    val selectedBrand: StateFlow<String?> = _selectedBrand.asStateFlow()

    val allBrands: StateFlow<List<String>> = repository.allBrands.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Phones list reactive to search and brand filter
    val phonesList: StateFlow<List<PhoneRecord>> = combine(
        _searchQuery,
        _selectedBrand
    ) { query, brand ->
        Pair(query.trim(), brand)
    }.flatMapLatest { (query, brand) ->
        if (query.isNotBlank()) {
            repository.searchPhones(query)
        } else if (brand != null) {
            repository.getPhonesByBrand(brand)
        } else {
            repository.allPhones
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Selected phone for detail screen
    private val _selectedPhoneId = MutableStateFlow<Long?>(null)
    val selectedPhoneId: StateFlow<Long?> = _selectedPhoneId.asStateFlow()

    val selectedPhoneWithFields: StateFlow<PhoneWithFields?> = _selectedPhoneId.flatMapLatest { id ->
        if (id != null) {
            repository.getPhoneWithFields(id)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Clipboard items
    val clipboardItems: StateFlow<List<ClipboardItem>> = repository.clipboardItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Column mapping settings
    private val _brandColIndex = MutableStateFlow(0)
    val brandColIndex: StateFlow<Int> = _brandColIndex.asStateFlow()

    private val _modelColIndex = MutableStateFlow(1)
    val modelColIndex: StateFlow<Int> = _modelColIndex.asStateFlow()

    private val _fullNameColIndex = MutableStateFlow(2)
    val fullNameColIndex: StateFlow<Int> = _fullNameColIndex.asStateFlow()

    // Import Flow state
    private val _importParseResult = MutableStateFlow<TsvParseResult?>(null)
    val importParseResult: StateFlow<TsvParseResult?> = _importParseResult.asStateFlow()

    private val _pendingDuplicates = MutableStateFlow<List<DuplicateConflict>>(emptyList())
    val pendingDuplicates: StateFlow<List<DuplicateConflict>> = _pendingDuplicates.asStateFlow()

    // Notifications / Messages
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectBrand(brand: String?) {
        _selectedBrand.value = brand
    }

    fun openPhoneDetail(phoneId: Long) {
        _selectedPhoneId.value = phoneId
        viewModelScope.launch {
            repository.incrementPhoneUsage(phoneId)
        }
    }

    fun closePhoneDetail() {
        _selectedPhoneId.value = null
    }

    fun toggleFavorite(phoneId: Long, current: Boolean) {
        viewModelScope.launch {
            repository.togglePhoneFavorite(phoneId, current)
        }
    }

    fun togglePin(phoneId: Long, current: Boolean) {
        viewModelScope.launch {
            repository.togglePhonePin(phoneId, current)
        }
    }

    fun deletePhone(phoneId: Long) {
        viewModelScope.launch {
            repository.deletePhone(phoneId)
            if (_selectedPhoneId.value == phoneId) {
                _selectedPhoneId.value = null
            }
            _userMessage.emit("Phone deleted")
        }
    }

    fun incrementFieldUsage(field: PhoneField) {
        viewModelScope.launch {
            repository.incrementFieldUsage(field.id, field.phoneId)
        }
    }

    fun updateField(field: PhoneField) {
        viewModelScope.launch {
            repository.updateField(field)
            _userMessage.emit("Field updated")
        }
    }

    // Column Mapping
    fun updateColumnMapping(brandIdx: Int, modelIdx: Int, fullNameIdx: Int) {
        _brandColIndex.value = brandIdx
        _modelColIndex.value = modelIdx
        _fullNameColIndex.value = fullNameIdx
    }

    // Import handling
    fun previewTsvImport(tsvText: String): TsvParseResult {
        val result = TsvParser.parse(
            tsvText = tsvText,
            brandColIndex = _brandColIndex.value,
            modelColIndex = _modelColIndex.value,
            fullNameColIndex = _fullNameColIndex.value
        )
        _importParseResult.value = result
        return result
    }

    fun clearImportPreview() {
        _importParseResult.value = null
    }

    fun executeImport(onDuplicatesFound: (List<DuplicateConflict>) -> Unit) {
        val result = _importParseResult.value ?: return
        viewModelScope.launch {
            val nonDuplicates = mutableListOf<ParsedPhone>()
            val conflicts = mutableListOf<DuplicateConflict>()

            val allConflicts = repository.checkDuplicates(result.phones)
            val conflictSet = allConflicts.map { it.incoming.record.fullName.lowercase() }.toSet()

            for (parsed in result.phones) {
                val conflict = allConflicts.firstOrNull {
                    it.incoming.record.fullName.equals(parsed.record.fullName, ignoreCase = true) ||
                            (it.incoming.record.brand.equals(parsed.record.brand, ignoreCase = true) &&
                                    it.incoming.record.model.equals(parsed.record.model, ignoreCase = true))
                }
                if (conflict != null) {
                    conflicts.add(conflict)
                } else {
                    nonDuplicates.add(parsed)
                }
            }

            // Insert non-duplicates immediately
            for (item in nonDuplicates) {
                repository.insertNewPhoneWithFields(item.record, item.fields)
            }

            _importParseResult.value = null

            if (conflicts.isNotEmpty()) {
                _pendingDuplicates.value = conflicts
                onDuplicatesFound(conflicts)
            } else {
                _userMessage.emit("Imported ${nonDuplicates.size} phone(s) successfully!")
            }
        }
    }

    fun resolveNextDuplicate(action: DuplicateAction) {
        val list = _pendingDuplicates.value.toMutableList()
        if (list.isEmpty()) return
        val current = list.removeAt(0)
        _pendingDuplicates.value = list

        viewModelScope.launch {
            repository.applyDuplicateResolution(current, action)
            if (list.isEmpty()) {
                _userMessage.emit("Duplicate resolution complete!")
            }
        }
    }

    fun resolveAllRemainingDuplicates(action: DuplicateAction) {
        val list = _pendingDuplicates.value
        _pendingDuplicates.value = emptyList()

        viewModelScope.launch {
            for (conflict in list) {
                repository.applyDuplicateResolution(conflict, action)
            }
            _userMessage.emit("Applied $action to all remaining duplicates.")
        }
    }

    // Clipboard manager
    fun saveClipboard(text: String) {
        viewModelScope.launch {
            repository.saveClipboardItem(text)
            _userMessage.emit("Saved to clipboard history")
        }
    }

    fun deleteClipboardItem(item: ClipboardItem) {
        viewModelScope.launch {
            repository.deleteClipboardItem(item)
        }
    }

    fun clearClipboardHistory() {
        viewModelScope.launch {
            repository.clearClipboardHistory()
            _userMessage.emit("Clipboard history cleared")
        }
    }

    fun toggleClipboardFavorite(item: ClipboardItem) {
        viewModelScope.launch {
            repository.toggleClipboardFavorite(item.id, item.isFavorite)
        }
    }

    fun toggleClipboardPin(item: ClipboardItem) {
        viewModelScope.launch {
            repository.toggleClipboardPin(item.id, item.isPinned)
        }
    }

    // Database Actions
    fun seedSampleFlagships() {
        viewModelScope.launch {
            repository.seedFlagships()
            _userMessage.emit("Sample flagships loaded!")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearDatabase()
            PasteQueueManager.clear()
            _userMessage.emit("All database records cleared!")
        }
    }

    suspend fun exportJson(): String = repository.exportJson()
    suspend fun exportTsv(): String = repository.exportTsv()
    suspend fun exportCsv(): String = repository.exportCsv()

    fun restoreJson(jsonText: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val count = repository.restoreFromJson(jsonText)
                _userMessage.emit("Restored $count phone(s) from JSON")
                onComplete(count)
            } catch (e: Exception) {
                _userMessage.emit("Restore failed: ${e.message}")
            }
        }
    }
}
