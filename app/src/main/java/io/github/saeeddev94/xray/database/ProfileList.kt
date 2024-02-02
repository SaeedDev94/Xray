package io.github.saeeddev94.xray.database

class ProfileList(
    var id: Long,
    var name: String,
) {
    companion object {
        fun fromProfile(value: Profile) = ProfileList(value.id, value.name)
    }
}
