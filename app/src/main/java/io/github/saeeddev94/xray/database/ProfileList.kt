package io.github.saeeddev94.xray.database

class ProfileList(
    var id: Long,
    var index: Int,
    var name: String,
) {
    companion object {
        fun fromProfile(value: Profile) = ProfileList(value.id, value.index, value.name)
    }
}
