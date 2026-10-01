package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.ClipboardItem
import com.example.data.model.PhoneField
import com.example.data.model.PhoneRecord
import com.example.data.model.PhoneWithFields
import com.example.data.tsv.CanonicalSpecs
import com.example.data.tsv.ParsedPhone
import com.example.data.tsv.TsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class DuplicateAction {
    UPDATE,
    REPLACE,
    ADD_AS_NEW,
    SKIP
}

data class DuplicateConflict(
    val incoming: ParsedPhone,
    val existing: PhoneRecord
)

class SpecRepository(private val database: AppDatabase) {
    private val phoneDao = database.phoneDao()
    private val clipboardDao = database.clipboardDao()

    val allPhones: Flow<List<PhoneRecord>> = phoneDao.getAllPhones()
    val allBrands: Flow<List<String>> = phoneDao.getAllBrands()
    val favoritePhones: Flow<List<PhoneRecord>> = phoneDao.getFavoritePhones()
    val recentPhones: Flow<List<PhoneRecord>> = phoneDao.getRecentPhones(30)
    val clipboardItems: Flow<List<ClipboardItem>> = clipboardDao.getAll()

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        val count = phoneDao.getPhoneCount()
        if (count == 0) {
            seedFlagships()
        }
    }

    suspend fun seedFlagships() = withContext(Dispatchers.IO) {
        val parsed = TsvParser.parse(CanonicalSpecs.SAMPLE_SEED_TSV)
        for (phone in parsed.phones) {
            insertNewPhoneWithFields(phone.record, phone.fields)
        }
    }

    fun getPhonesByBrand(brand: String): Flow<List<PhoneRecord>> {
        return phoneDao.getPhonesByBrand(brand)
    }

    fun searchPhones(query: String): Flow<List<PhoneRecord>> {
        return phoneDao.searchPhones(query)
    }

    fun searchFields(query: String): Flow<List<PhoneField>> {
        return phoneDao.searchFields(query)
    }

    fun getPhoneWithFields(phoneId: Long): Flow<PhoneWithFields?> {
        return phoneDao.getPhoneWithFields(phoneId)
    }

    suspend fun getPhoneWithFieldsSync(phoneId: Long): PhoneWithFields? = withContext(Dispatchers.IO) {
        phoneDao.getPhoneWithFieldsSync(phoneId)
    }

    suspend fun incrementPhoneUsage(phoneId: Long) = withContext(Dispatchers.IO) {
        phoneDao.incrementPhoneUsage(phoneId)
    }

    suspend fun togglePhoneFavorite(phoneId: Long, currentFav: Boolean) = withContext(Dispatchers.IO) {
        phoneDao.togglePhoneFavorite(phoneId, !currentFav)
    }

    suspend fun togglePhonePin(phoneId: Long, currentPin: Boolean) = withContext(Dispatchers.IO) {
        phoneDao.togglePhonePin(phoneId, !currentPin)
    }

    suspend fun incrementFieldUsage(fieldId: Long, phoneId: Long) = withContext(Dispatchers.IO) {
        phoneDao.incrementFieldUsage(fieldId)
        phoneDao.incrementPhoneUsage(phoneId)
    }

    suspend fun updateField(field: PhoneField) = withContext(Dispatchers.IO) {
        phoneDao.updateField(field)
    }

    suspend fun deletePhone(phoneId: Long) = withContext(Dispatchers.IO) {
        phoneDao.deletePhoneById(phoneId)
    }

    suspend fun clearDatabase() = withContext(Dispatchers.IO) {
        phoneDao.deleteAllPhones()
        clipboardDao.deleteAll()
    }

    suspend fun insertNewPhoneWithFields(record: PhoneRecord, fields: List<PhoneField>): Long = withContext(Dispatchers.IO) {
        val phoneId = phoneDao.insertPhone(record)
        val fieldsWithId = fields.map { it.copy(phoneId = phoneId) }
        phoneDao.insertFields(fieldsWithId)
        phoneId
    }

    suspend fun checkDuplicates(incomingPhones: List<ParsedPhone>): List<DuplicateConflict> = withContext(Dispatchers.IO) {
        val duplicates = mutableListOf<DuplicateConflict>()
        for (parsed in incomingPhones) {
            val existing = phoneDao.findExisting(
                brand = parsed.record.brand,
                model = parsed.record.model,
                fullName = parsed.record.fullName
            )
            if (existing != null) {
                duplicates.add(DuplicateConflict(incoming = parsed, existing = existing))
            }
        }
        duplicates
    }

    suspend fun applyDuplicateResolution(conflict: DuplicateConflict, action: DuplicateAction) = withContext(Dispatchers.IO) {
        when (action) {
            DuplicateAction.UPDATE -> {
                // Merge non-blank fields into existing phone
                val existingFull = phoneDao.getPhoneWithFieldsSync(conflict.existing.id) ?: return@withContext
                val existingFields = existingFull.fields.toMutableList()

                for (incomingField in conflict.incoming.fields) {
                    if (incomingField.fieldValue.isNotBlank()) {
                        val index = existingFields.indexOfFirst {
                            it.columnIndex == incomingField.columnIndex ||
                                    it.fieldName.equals(incomingField.fieldName, ignoreCase = true)
                        }
                        if (index >= 0) {
                            existingFields[index] = existingFields[index].copy(
                                fieldValue = incomingField.fieldValue,
                                category = incomingField.category
                            )
                        } else {
                            existingFields.add(incomingField.copy(phoneId = conflict.existing.id))
                        }
                    }
                }
                phoneDao.deleteFieldsByPhone(conflict.existing.id)
                phoneDao.insertFields(existingFields.map { it.copy(id = 0L, phoneId = conflict.existing.id) })
                phoneDao.updatePhone(conflict.existing.copy(updatedAt = System.currentTimeMillis()))
            }
            DuplicateAction.REPLACE -> {
                phoneDao.deleteFieldsByPhone(conflict.existing.id)
                val newFields = conflict.incoming.fields.map { it.copy(phoneId = conflict.existing.id) }
                phoneDao.insertFields(newFields)
                phoneDao.updatePhone(
                    conflict.existing.copy(
                        brand = conflict.incoming.record.brand,
                        model = conflict.incoming.record.model,
                        fullName = conflict.incoming.record.fullName,
                        originalTsv = conflict.incoming.record.originalTsv,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            DuplicateAction.ADD_AS_NEW -> {
                insertNewPhoneWithFields(conflict.incoming.record, conflict.incoming.fields)
            }
            DuplicateAction.SKIP -> {
                // Do nothing
            }
        }
    }

    // Clipboard operations
    suspend fun saveClipboardItem(content: String): Long = withContext(Dispatchers.IO) {
        val isTsv = TsvParser.isTsv(content)
        val detectedCount = if (isTsv) {
            TsvParser.parse(content).phones.size
        } else 0

        clipboardDao.insert(
            ClipboardItem(
                content = content,
                isTsv = isTsv,
                detectedPhonesCount = detectedCount
            )
        )
    }

    suspend fun deleteClipboardItem(item: ClipboardItem) = withContext(Dispatchers.IO) {
        clipboardDao.delete(item)
    }

    suspend fun clearClipboardHistory() = withContext(Dispatchers.IO) {
        clipboardDao.deleteAll()
    }

    suspend fun toggleClipboardFavorite(id: Long, currentFav: Boolean) = withContext(Dispatchers.IO) {
        clipboardDao.toggleFavorite(id, !currentFav)
    }

    suspend fun toggleClipboardPin(id: Long, currentPin: Boolean) = withContext(Dispatchers.IO) {
        clipboardDao.togglePin(id, !currentPin)
    }

    // Export formats
    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val phones = phoneDao.getAllPhonesWithFieldsSync()
        val jsonArray = JSONArray()
        for (item in phones) {
            val phoneObj = JSONObject()
            phoneObj.put("brand", item.phone.brand)
            phoneObj.put("model", item.phone.model)
            phoneObj.put("fullName", item.phone.fullName)
            phoneObj.put("originalTsv", item.phone.originalTsv)
            phoneObj.put("createdAt", item.phone.createdAt)
            phoneObj.put("usageCount", item.phone.usageCount)
            phoneObj.put("isFavorite", item.phone.isFavorite)

            val fieldsArray = JSONArray()
            for (f in item.fields) {
                val fObj = JSONObject()
                fObj.put("columnIndex", f.columnIndex)
                fObj.put("fieldName", f.fieldName)
                fObj.put("category", f.category)
                fObj.put("fieldValue", f.fieldValue)
                fieldsArray.put(fObj)
            }
            phoneObj.put("fields", fieldsArray)
            jsonArray.put(phoneObj)
        }
        jsonArray.toString(2)
    }

    suspend fun exportTsv(): String = withContext(Dispatchers.IO) {
        val phones = phoneDao.getAllPhonesWithFieldsSync()
        val headers = CanonicalSpecs.COLUMNS.map { it.name }.joinToString("\t")
        val rows = phones.map { p ->
            val fieldsMap = p.fields.associateBy { it.columnIndex }
            (0 until CanonicalSpecs.COLUMNS.size).joinToString("\t") { idx ->
                fieldsMap[idx]?.fieldValue.orEmpty().replace("\t", " ").replace("\n", " ")
            }
        }
        listOf(headers).plus(rows).joinToString("\n")
    }

    suspend fun exportCsv(): String = withContext(Dispatchers.IO) {
        val phones = phoneDao.getAllPhonesWithFieldsSync()
        val headers = CanonicalSpecs.COLUMNS.map { escapeCsv(it.name) }.joinToString(",")
        val rows = phones.map { p ->
            val fieldsMap = p.fields.associateBy { it.columnIndex }
            (0 until CanonicalSpecs.COLUMNS.size).joinToString(",") { idx ->
                escapeCsv(fieldsMap[idx]?.fieldValue.orEmpty())
            }
        }
        listOf(headers).plus(rows).joinToString("\n")
    }

    private fun escapeCsv(value: String): String {
        val containsSpecial = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (containsSpecial) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    suspend fun restoreFromJson(jsonString: String): Int = withContext(Dispatchers.IO) {
        val jsonArray = JSONArray(jsonString)
        var importedCount = 0
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val record = PhoneRecord(
                brand = obj.optString("brand", "Other"),
                model = obj.optString("model", "Unknown"),
                fullName = obj.optString("fullName", "Unknown Device"),
                originalTsv = obj.optString("originalTsv", ""),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                usageCount = obj.optInt("usageCount", 0),
                isFavorite = obj.optBoolean("isFavorite", false)
            )

            val fields = mutableListOf<PhoneField>()
            val fieldsArray = obj.optJSONArray("fields")
            if (fieldsArray != null) {
                for (j in 0 until fieldsArray.length()) {
                    val fObj = fieldsArray.getJSONObject(j)
                    fields.add(
                        PhoneField(
                            id = 0L,
                            phoneId = 0L,
                            columnIndex = fObj.optInt("columnIndex", j),
                            fieldName = fObj.optString("fieldName", "Column $j"),
                            category = fObj.optString("category", "OTHER"),
                            fieldValue = fObj.optString("fieldValue", "")
                        )
                    )
                }
            }

            insertNewPhoneWithFields(record, fields)
            importedCount++
        }
        importedCount
    }
}
