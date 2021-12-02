package com.talla.dvault.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.talla.dvault.database.dao.DVaultDao
import com.talla.dvault.utills.DataGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import androidx.room.migration.Migration
import com.talla.dvault.database.entities.*
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [User::class,AppLockModel::class,CategoriesModel::class, FolderTable::class,ItemModel::class],version = 1,exportSchema = false)
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
            var builder: Builder<VaultDatabase> = Room.databaseBuilder(context.applicationContext,VaultDatabase::class.java,"DVault.db")
                .addCallback(rdc).addMigrations(MIGRATION_1_2).fallbackToDestructiveMigration()
            val factory = SupportFactory(SQLiteDatabase.getBytes("DVault".toCharArray()))
            builder.openHelperFactory(factory)
            return builder.build()
        }

        //default callback
        var rdc: Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                // do something after database has been created
                Executors.newSingleThreadExecutor().execute {
                    INSTANCE?.let {
                        GlobalScope.launch(Dispatchers.Default) {
                            it.vaulDao().insertDefaultCatData(DataGenerator.getDefaultCatList())
                        }
                    }
                }
            }
            override fun onOpen(db: SupportSQLiteDatabase) {
                // do something every time database is open
            }
        }

        //migration
        val MIGRATION_1_2: Migration? = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("Create Table IF NOT EXISTS `CategoriesModel` (`catId` TEXT not null,`categoryName` TEXT not null,`totalItems` INTEGER not null,PRIMARY KEY(`catId`))")
            }
        }

    }

}