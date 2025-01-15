package io.github.saeeddev94.xray.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Link::class,
        Profile::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Link.Type.Convertor::class)
abstract class XrayDatabase : RoomDatabase() {

    abstract fun linkDao(): LinkDao
    abstract fun profileDao(): ProfileDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // create links table
                db.execSQL("""
                    CREATE TABLE links (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        address TEXT NOT NULL,
                        type INTEGER NOT NULL,
                        is_active INTEGER NOT NULL
                    )
                """)

                // add link_id to profiles table
                db.execSQL("ALTER TABLE profiles ADD COLUMN link_id INTEGER")

                // create profiles_new table similar to profiles but with new column (link_id)
                db.execSQL("""
                    CREATE TABLE profiles_new (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        link_id INTEGER,
                        "index" INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        config TEXT NOT NULL,
                        FOREIGN KEY (link_id) REFERENCES links(id) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                """)

                // create index for link_id
                db.execSQL("CREATE INDEX profiles_link_id_foreign ON profiles_new(link_id)")

                // importing data from profile to profiles_new
                db.execSQL("""
                   INSERT INTO profiles_new (id, link_id, "index", name, config)
                   SELECT id, link_id, "index", name, config FROM profiles
                """)

                // drop profiles table
                db.execSQL("DROP TABLE profiles")

                // rename profiles_new to profiles
                db.execSQL("ALTER TABLE profiles_new RENAME TO profiles")
            }
        }

        @Volatile
        private var db: XrayDatabase? = null

        fun ref(context: Context): XrayDatabase {
            if (db == null) {
                synchronized(this) {
                    if (db == null) {
                        db = Room.databaseBuilder(
                            context.applicationContext,
                            XrayDatabase::class.java,
                            "xray"
                        ).addMigrations(MIGRATION_1_2).build()
                    }
                }
            }
            return db!!
        }
    }
}
