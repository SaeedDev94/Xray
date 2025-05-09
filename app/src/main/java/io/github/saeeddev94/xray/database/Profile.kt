package io.github.saeeddev94.xray.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "profiles",
    foreignKeys = [
        ForeignKey(
            entity = Link::class,
            parentColumns = ["id"],
            childColumns = ["link_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            name = "profiles_link_id_foreign",
            value = ["link_id"]
        ),
    ],
)
data class Profile(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L,
    @ColumnInfo(name = "link_id")
    var linkId: Long? = null,
    @ColumnInfo(name = "index")
    var index: Int = -1,
    @ColumnInfo(name = "name")
    var name: String = "",
    @ColumnInfo(name = "config")
    var config: String = "",
) : Parcelable
