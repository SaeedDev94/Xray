package io.github.saeeddev94.xray.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

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
)
class Profile {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L

    @ColumnInfo(name = "link_id")
    var linkId: Long? = null

    @ColumnInfo(name = "index")
    var index: Int = -1

    @ColumnInfo(name = "name")
    var name: String = ""

    @ColumnInfo(name = "config")
    var config: String = ""
}
