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
        Config::class,
        Link::class,
        Profile::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(
    Link.Type.Convertor::class,
    Config.Mode.Convertor::class,
)
abstract class XrayDatabase : RoomDatabase() {

    abstract fun configDao(): ConfigDao
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE links ADD COLUMN user_agent TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // create config table
                db.execSQL("""
                    CREATE TABLE configs (
                        id INTEGER PRIMARY KEY NOT NULL,
                        log TEXT NOT NULL,
                        dns TEXT NOT NULL,
                        inbounds TEXT NOT NULL,
                        outbounds TEXT NOT NULL,
                        routing TEXT NOT NULL,
                        log_mode INTEGER NOT NULL,
                        dns_mode INTEGER NOT NULL,
                        inbounds_mode INTEGER NOT NULL,
                        outbounds_mode INTEGER NOT NULL,
                        routing_mode INTEGER NOT NULL
                    )
                """)

                // insert the row
                db.execSQL("""
                    INSERT INTO configs (
                        log, dns, inbounds, outbounds, routing,
                        log_mode, dns_mode, inbounds_mode, outbounds_mode, routing_mode
                    ) VALUES (
                        '{}', '{}', '[]', '[]', '{}',
                        0, 0, 0, 0, 0
                    )
                """)
            }
        }

        @Volatile
        private var db: XrayDatabase? = null

        fun ref(context: Context): XrayDatabase {
            if (db == null) {
                synchronized(this) {
                    if (db == null) {
                        val migrations = arrayOf(
                            MIGRATION_1_2,
                            MIGRATION_2_3,
                            MIGRATION_3_4,
                        )
                        db = Room.databaseBuilder(
                            context.applicationContext,
                            XrayDatabase::class.java,
                            "xray"
                        ).addMigrations(*migrations).build()
                    }
                }
            }
            return db!!
        }
    }
}
