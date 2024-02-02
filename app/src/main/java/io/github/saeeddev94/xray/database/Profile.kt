package io.github.saeeddev94.xray.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
class Profile {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L

    @ColumnInfo(name = "index")
    var index: Int = -1

    @ColumnInfo(name = "name")
    var name: String = ""

    @ColumnInfo(name = "config")
    var config: String = ""
}
