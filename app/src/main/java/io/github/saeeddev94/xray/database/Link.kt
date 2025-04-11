package io.github.saeeddev94.xray.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "links")
data class Link(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L,
    @ColumnInfo(name = "name")
    var name: String = "",
    @ColumnInfo(name = "address")
    var address: String = "",
    @ColumnInfo(name = "type")
    var type: Type = Type.Json,
    @ColumnInfo(name = "is_active")
    var isActive: Boolean = false,
    @ColumnInfo(name = "user_agent")
    var userAgent: String? = null,
) {
    enum class Type(val value: Int) {
        Json(0),
        Subscription(1);

        class Convertor {
            @TypeConverter
            fun fromType(type: Type): Int = type.value

            @TypeConverter
            fun toType(value: Int): Type = entries[value]
        }
    }
}
