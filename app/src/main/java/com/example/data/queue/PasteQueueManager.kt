package com.example.data.queue

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class QueuedItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val value: String,
    val sourcePhone: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class QueueSeparator(val title: String, val delimiter: String) {
    NEWLINE("Newline", "\n"),
    COMMA("Comma", ", "),
    TAB("Tab", "\t"),
    SPACE("Space", " "),
    BULLET("Bullet", "\n• "),
    DASH("Dash", " - "),
    CUSTOM("Custom", "");
}

object PasteQueueManager {
    private val _queue = MutableStateFlow<List<QueuedItem>>(emptyList())
    val queue: StateFlow<List<QueuedItem>> = _queue.asStateFlow()

    private val _selectedSeparator = MutableStateFlow(QueueSeparator.NEWLINE)
    val selectedSeparator: StateFlow<QueueSeparator> = _selectedSeparator.asStateFlow()

    private val _customSeparatorText = MutableStateFlow(" | ")
    val customSeparatorText: StateFlow<String> = _customSeparatorText.asStateFlow()

    fun setSeparator(separator: QueueSeparator) {
        _selectedSeparator.value = separator
    }

    fun setCustomSeparatorText(text: String) {
        _customSeparatorText.value = text
    }

    fun getEffectiveDelimiter(): String {
        return if (_selectedSeparator.value == QueueSeparator.CUSTOM) {
            _customSeparatorText.value
        } else {
            _selectedSeparator.value.delimiter
        }
    }

    @Synchronized
    fun add(label: String, value: String, sourcePhone: String = "") {
        val current = _queue.value.toMutableList()
        current.add(QueuedItem(label = label, value = value, sourcePhone = sourcePhone))
        _queue.value = current
    }

    @Synchronized
    fun addMultiple(items: List<QueuedItem>) {
        val current = _queue.value.toMutableList()
        current.addAll(items)
        _queue.value = current
    }

    @Synchronized
    fun popNext(): String? {
        val current = _queue.value.toMutableList()
        if (current.isEmpty()) return null
        val item = current.removeAt(0)
        _queue.value = current
        return item.value
    }

    @Synchronized
    fun drainAll(customDelimiter: String? = null): String {
        val current = _queue.value
        if (current.isEmpty()) return ""
        val delimiter = customDelimiter ?: getEffectiveDelimiter()
        val text = if (delimiter == "\n• ") {
            "• " + current.joinToString(delimiter) { it.value }
        } else {
            current.joinToString(delimiter) { it.value }
        }
        _queue.value = emptyList()
        return text
    }

    @Synchronized
    fun remove(id: String) {
        val current = _queue.value.toMutableList()
        current.removeAll { it.id == id }
        _queue.value = current
    }

    @Synchronized
    fun clear() {
        _queue.value = emptyList()
    }

    fun size(): Int = _queue.value.size
}
