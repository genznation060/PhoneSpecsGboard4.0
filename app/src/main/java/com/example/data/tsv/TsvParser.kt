package com.example.data.tsv

import com.example.data.model.PhoneField
import com.example.data.model.PhoneRecord
import com.example.data.model.SpecCategory

data class ParsedPhone(
    val record: PhoneRecord,
    val fields: List<PhoneField>
)

data class TsvParseResult(
    val phones: List<ParsedPhone>,
    val rowCount: Int,
    val columnCount: Int,
    val hasHeader: Boolean,
    val detectedHeaders: List<String>
)

object TsvParser {
    fun isTsv(text: String): Boolean {
        if (!text.contains('\t')) return false
        val lines = text.split("\r\n", "\n", "\r").filter { it.isNotBlank() }
        return lines.any { it.contains('\t') }
    }

    fun parse(
        tsvText: String,
        brandColIndex: Int = 0,
        modelColIndex: Int = 1,
        fullNameColIndex: Int = 2
    ): TsvParseResult {
        val lines = tsvText.split("\r\n", "\n", "\r").filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return TsvParseResult(emptyList(), 0, 0, false, emptyList())
        }

        val firstRowCells = lines[0].split("\t", limit = -1).map { it.trim() }
        val hasHeader = detectHeader(firstRowCells)

        val headerList: List<String>
        val dataRows: List<String>

        if (hasHeader) {
            headerList = firstRowCells
            dataRows = lines.drop(1)
        } else {
            headerList = CanonicalSpecs.COLUMNS.map { it.name }
            dataRows = lines
        }

        var maxCols = firstRowCells.size
        val parsedPhones = mutableListOf<ParsedPhone>()

        for (row in dataRows) {
            val cells = row.split("\t", limit = -1).map { it.trim() }
            if (cells.all { it.isEmpty() }) continue
            if (cells.size > maxCols) {
                maxCols = cells.size
            }

            var brand = cells.getOrNull(brandColIndex).orEmpty()
            var model = cells.getOrNull(modelColIndex).orEmpty()
            var fullName = cells.getOrNull(fullNameColIndex).orEmpty()

            // Heuristic resolution if brand is blank
            if (brand.isBlank()) {
                val searchTarget = (fullName + " " + model + " " + cells.joinToString(" ")).lowercase()
                val foundBrand = CanonicalSpecs.KNOWN_BRANDS.firstOrNull {
                    searchTarget.contains(it.lowercase())
                }
                if (foundBrand != null) {
                    brand = foundBrand
                }
            }

            // If model is blank, try stripping brand prefix from full name
            if (model.isBlank() && fullName.isNotBlank()) {
                if (brand.isNotBlank() && fullName.startsWith(brand, ignoreCase = true)) {
                    model = fullName.substring(brand.length).trim()
                } else {
                    model = fullName
                }
            }

            // If full name is blank, construct from brand + model
            if (fullName.isBlank()) {
                fullName = if (brand.isNotBlank() && model.isNotBlank()) {
                    if (model.startsWith(brand, ignoreCase = true)) model else "$brand $model"
                } else if (model.isNotBlank()) {
                    model
                } else if (brand.isNotBlank()) {
                    brand
                } else {
                    "Unknown Device"
                }
            }

            if (brand.isBlank()) {
                brand = "Other"
            }
            if (model.isBlank()) {
                model = fullName
            }

            val phoneRecord = PhoneRecord(
                id = 0L,
                brand = brand,
                model = model,
                fullName = fullName,
                originalTsv = row,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val fields = mutableListOf<PhoneField>()
            for (colIdx in cells.indices) {
                val rawValue = cells[colIdx]
                val fieldName = headerList.getOrNull(colIdx) ?: "Column $colIdx"
                val category = if (colIdx < CanonicalSpecs.COLUMNS.size) {
                    CanonicalSpecs.COLUMNS[colIdx].category
                } else {
                    SpecCategory.guessCategory(fieldName)
                }

                fields.add(
                    PhoneField(
                        id = 0L,
                        phoneId = 0L,
                        columnIndex = colIdx,
                        fieldName = fieldName,
                        category = category.name,
                        fieldValue = rawValue
                    )
                )
            }

            parsedPhones.add(ParsedPhone(phoneRecord, fields))
        }

        return TsvParseResult(
            phones = parsedPhones,
            rowCount = parsedPhones.size,
            columnCount = maxCols,
            hasHeader = hasHeader,
            detectedHeaders = headerList
        )
    }

    private fun detectHeader(cells: List<String>): Boolean {
        if (cells.isEmpty()) return false
        val firstCell = cells[0].lowercase()
        if (firstCell in listOf("brand", "phone", "manufacturer", "device", "make")) {
            return true
        }

        val headerKeywords = setOf(
            "brand", "model", "name", "price", "display", "screen", "chipset", "cpu", "gpu",
            "ram", "storage", "camera", "battery", "dimensions", "weight", "os", "charging"
        )

        var matchedCount = 0
        for (cell in cells) {
            val lower = cell.lowercase()
            if (headerKeywords.any { lower.contains(it) }) {
                matchedCount++
            }
        }
        return matchedCount >= 3
    }
}
