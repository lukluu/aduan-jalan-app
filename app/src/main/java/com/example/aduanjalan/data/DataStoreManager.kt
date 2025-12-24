package com.example.aduanjalan.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.dataStore

    companion object {
        val TOKEN = stringPreferencesKey("TOKEN")
        val USER_NAME = stringPreferencesKey("USER_NAME")
    }

    fun getToken(): Flow<String> = dataStore.data.map { it[TOKEN] ?: "" }

    suspend fun saveToken(token: String) {
        dataStore.edit {
            it[TOKEN] = token
        }
    }

    suspend fun saveUserName(name: String) {
        dataStore.edit {
            it[USER_NAME] = name
        }
    }

    fun getUserName(): Flow<String> = dataStore.data.map { it[USER_NAME] ?: "Pengguna" }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
