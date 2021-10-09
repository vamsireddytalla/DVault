package com.talla.dvault.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.talla.dvault.database.dao.DVaultDao
import com.talla.dvault.database.entities.AppLockModel
import com.talla.dvault.database.entities.User
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [User::class,AppLockModel::class],version = 1,exportSchema = false)
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

        private fun createDatabase(context: Context) : VaultDatabase {
            var builder: Builder<VaultDatabase> =Room.databaseBuilder(context.applicationContext,VaultDatabase::class.java,"DVault.db")
//            val factory = SupportFactory(SQLiteDatabase.getBytes("PassPhrase".toCharArray()))
//            builder.openHelperFactory(factory)
            return builder.build()
        }

    }

}