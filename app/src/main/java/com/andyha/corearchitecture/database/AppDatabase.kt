package com.andyha.corearchitecture.database

import android.content.Context
import android.os.Build
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.andyha.coredata.storage.preference.AppSharedPreference
import com.andyha.coredata.storage.preference.dbEncryptionKey
import com.andyha.featureweatherkit.data.dao.LocationDetectedDao
import com.andyha.featureweatherkit.data.dao.WeatherDao
import com.andyha.featureweatherkit.data.model.LocationState.LocationDetected
import com.andyha.featureweatherkit.data.model.Weather
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteDatabaseHook
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom


const val VERSION = 1

@Database(
    version = VERSION,
    entities = [
        Weather::class,
        LocationDetected::class
    ],
    exportSchema = false
)
@TypeConverters(
    DateConverter::class,
    StringArrayConverter::class,
    StringToLongConverter::class,
)

abstract class AppDatabase : RoomDatabase() {

    fun clearAll() = clearAllTables()

    //define DAO get method here...
    abstract fun getWeatherDao(): WeatherDao
    abstract fun getLocationDetectedDao(): LocationDetectedDao

    companion object {
        private const val DATABASE_NAME = "Core-Arch-DB"

        fun getInstance(
            context: Context,
            prefs: AppSharedPreference,
            encrypted: Boolean = false,
            memorySecured: Boolean = false
        ): AppDatabase {
            val builder = Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()

            builder.apply {
                if (encrypted) {
                    val passPhrase = prefs.dbEncryptionKey ?: getEncryptionKey(prefs)
                    val factory = SupportFactory(passPhrase, object : SQLiteDatabaseHook {
                        override fun preKey(database: SQLiteDatabase?) = Unit

                        override fun postKey(database: SQLiteDatabase?) {
                            database?.rawExecSQL(
                                if (memorySecured) "PRAGMA cipher_memory_security = ON"
                                else "PRAGMA cipher_memory_security = OFF"
                            )
                        }
                    })
                    builder.openHelperFactory(factory)
                }
            }
            return builder.build()
        }

        private fun getEncryptionKey(prefs: AppSharedPreference): ByteArray {
            val passPhrase = ByteArray(32).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    SecureRandom.getInstanceStrong().nextBytes(this)
                } else {
                    SecureRandom().nextBytes(this)
                }
            }
            prefs.dbEncryptionKey = passPhrase
            return passPhrase
        }
    }
}