package com.talla.dvault.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.talla.dvault.database.dao.DVaultDao
import com.talla.dvault.database.entities.User

@Database(entities = [User::class],version = 1,exportSchema = false)
abstract class VaultDatabase: RoomDatabase()
{
    abstract fun vaulDao():DVaultDao

    companion object {
        @Volatile
        private var INSTANCE:VaultDatabase?=null
        private val LOCK=Any()

        operator fun invoke(context: Context) = INSTANCE ?: synchronized(LOCK){
            INSTANCE ?: createDatabase(context).also { INSTANCE = it }
        }

        private fun createDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext,VaultDatabase::class.java,"DVault.db").build()

    }

}