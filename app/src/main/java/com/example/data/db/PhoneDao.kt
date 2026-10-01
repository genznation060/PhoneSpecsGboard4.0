package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.PhoneField
import com.example.data.model.PhoneRecord
import com.example.data.model.PhoneWithFields
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneDao {
    @Query("SELECT * FROM phones ORDER BY isPinned DESC, isFavorite DESC, updatedAt DESC")
    fun getAllPhones(): Flow<List<PhoneRecord>>

    @Query("SELECT * FROM phones WHERE brand = :brand ORDER BY isPinned DESC, isFavorite DESC, updatedAt DESC")
    fun getPhonesByBrand(brand: String): Flow<List<PhoneRecord>>

    @Query("SELECT DISTINCT brand FROM phones ORDER BY brand ASC")
    fun getAllBrands(): Flow<List<String>>

    @Transaction
    @Query("SELECT * FROM phones WHERE id = :phoneId")
    fun getPhoneWithFields(phoneId: Long): Flow<PhoneWithFields?>

    @Transaction
    @Query("SELECT * FROM phones WHERE id = :phoneId")
    suspend fun getPhoneWithFieldsSync(phoneId: Long): PhoneWithFields?

    @Transaction
    @Query("SELECT * FROM phones ORDER BY isPinned DESC, isFavorite DESC, updatedAt DESC")
    suspend fun getAllPhonesWithFieldsSync(): List<PhoneWithFields>

    @Query("""
        SELECT * FROM phones
        WHERE fullName LIKE '%' || :query || '%'
           OR brand LIKE '%' || :query || '%'
           OR model LIKE '%' || :query || '%'
        ORDER BY isPinned DESC, isFavorite DESC, updatedAt DESC
    """)
    fun searchPhones(query: String): Flow<List<PhoneRecord>>

    @Query("SELECT * FROM phones WHERE isFavorite = 1 ORDER BY isPinned DESC, updatedAt DESC")
    fun getFavoritePhones(): Flow<List<PhoneRecord>>

    @Query("SELECT * FROM phones ORDER BY usageCount DESC, updatedAt DESC LIMIT :limit")
    fun getRecentPhones(limit: Int = 20): Flow<List<PhoneRecord>>

    @Query("""
        SELECT * FROM phones
        WHERE LOWER(brand) = LOWER(:brand) AND LOWER(model) = LOWER(:model)
           OR LOWER(fullName) = LOWER(:fullName)
        LIMIT 1
    """)
    suspend fun findExisting(brand: String, model: String, fullName: String): PhoneRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhone(phone: PhoneRecord): Long

    @Update
    suspend fun updatePhone(phone: PhoneRecord)

    @Delete
    suspend fun deletePhone(phone: PhoneRecord)

    @Query("DELETE FROM phones WHERE id = :id")
    suspend fun deletePhoneById(id: Long)

    @Query("DELETE FROM phones")
    suspend fun deleteAllPhones()

    @Query("UPDATE phones SET usageCount = usageCount + 1, updatedAt = :timestamp WHERE id = :phoneId")
    suspend fun incrementPhoneUsage(phoneId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE phones SET isFavorite = :isFav WHERE id = :phoneId")
    suspend fun togglePhoneFavorite(phoneId: Long, isFav: Boolean)

    @Query("UPDATE phones SET isPinned = :isPinned WHERE id = :phoneId")
    suspend fun togglePhonePin(phoneId: Long, isPinned: Boolean)

    // Fields
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFields(fields: List<PhoneField>)

    @Query("DELETE FROM phone_fields WHERE phoneId = :phoneId")
    suspend fun deleteFieldsByPhone(phoneId: Long)

    @Update
    suspend fun updateField(field: PhoneField)

    @Query("UPDATE phone_fields SET usageCount = usageCount + 1 WHERE id = :fieldId")
    suspend fun incrementFieldUsage(fieldId: Long)

    @Query("UPDATE phone_fields SET isFavorite = :isFav WHERE id = :fieldId")
    suspend fun toggleFieldFavorite(fieldId: Long, isFav: Boolean)

    @Query("""
        SELECT * FROM phone_fields
        WHERE fieldName LIKE '%' || :query || '%'
           OR fieldValue LIKE '%' || :query || '%'
        ORDER BY usageCount DESC LIMIT 50
    """)
    fun searchFields(query: String): Flow<List<PhoneField>>

    @Query("SELECT COUNT(*) FROM phones")
    suspend fun getPhoneCount(): Int
}
