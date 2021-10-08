package com.talla.dvault.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class UserPreferences(val context: Context)
{

    companion object{
       val Context.dataStore:DataStore<Preferences> by preferencesDataStore("settings.db")
        val USER_NAME="USER_NAME"
        val EMAIL="EMAIL"
        val USER_IMAGE="USER_IMAGE"
        val NIGHT_MODE="NIGHT_MODE"
        val FIRST_TIME="FIRST_TIME"
    }

    suspend fun storeStringData(key: String, value: String) {
        val dataStoreKey = stringPreferencesKey(key)
        context.dataStore.edit { prefernce ->
            prefernce[dataStoreKey] = value
        }
    }

    fun getData(key: String): Flow<String> {
        val dataStoreKey = stringPreferencesKey(key);
        var returnVal=context.dataStore.data.catch{
            if(it is IOException)
            {
                emit(emptyPreferences())
            }
            else{
                throw it
            }
        }.map { preference ->
            val result = preference[dataStoreKey] ?: "No Data Found"
            result
        }
        return returnVal
    }

    suspend fun saveBooleanData(key:String,value:Boolean)
    {
        val dataStoreKey=booleanPreferencesKey(key)
        context.dataStore.edit { pref ->
             pref[dataStoreKey]=value
        }
    }

    fun getBooleanData(key: String): Flow<Boolean>
    {
        val dataStoreKey = booleanPreferencesKey(key);
        var returnVal=context.dataStore.data.map { preference ->
            val result = preference[dataStoreKey] ?: false
            result
        }
        return returnVal
    }


}