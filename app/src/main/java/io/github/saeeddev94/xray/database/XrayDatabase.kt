package io.github.saeeddev94.xray.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Profile::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class XrayDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var db: XrayDatabase? = null

        fun ref(context: Context): XrayDatabase {
            if (db != null) return db!!
            synchronized(this) {
                return Room.databaseBuilder(
                    context.applicationContext,
                    XrayDatabase::class.java,
                    "xray"
                ).build()
            }
        }
    }
}
