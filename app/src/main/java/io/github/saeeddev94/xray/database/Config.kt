package io.github.saeeddev94.xray.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "configs")
data class Config(
    @PrimaryKey
    @ColumnInfo(name = "id")
    var id: Int = 1,
    @ColumnInfo(name = "log")
    var log: String = "{}",
    @ColumnInfo(name = "dns")
    var dns: String = "{}",
    @ColumnInfo(name = "inbounds")
    var inbounds: String = "[]",
    @ColumnInfo(name = "outbounds")
    var outbounds: String = "[]",
    @ColumnInfo(name = "routing")
    var routing: String = "{}",
    @ColumnInfo(name = "log_mode")
    var logMode: Mode = Mode.Disable,
    @ColumnInfo(name = "dns_mode")
    var dnsMode: Mode = Mode.Disable,
    @ColumnInfo(name = "inbounds_mode")
    var inboundsMode: Mode = Mode.Disable,
    @ColumnInfo(name = "outbounds_mode")
    var outboundsMode: Mode = Mode.Disable,
    @ColumnInfo(name = "routing_mode")
    var routingMode: Mode = Mode.Disable,
) : Parcelable {
    enum class Mode(val value: Int) {
        Disable(0),
        Replace(1),
        Merge(2);

        class Convertor {
            @TypeConverter
            fun fromMode(mode: Mode): Int = mode.value

            @TypeConverter
            fun toMode(value: Int): Mode = entries[value]
        }
    }
}
