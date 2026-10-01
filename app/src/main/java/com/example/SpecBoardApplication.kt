package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.repository.SpecRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SpecBoardApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var database: AppDatabase
        private set

    lateinit var repository: SpecRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        repository = SpecRepository(database)

        applicationScope.launch {
            repository.seedIfEmpty()
        }
    }

    companion object {
        lateinit var instance: SpecBoardApplication
            private set
    }
}
