package io.github.saeeddev94.xray

import io.github.saeeddev94.xray.database.ProfileList

interface ProfileClickListener {

    fun profileSelect(index: Int)
    fun profileEdit(index: Int, profile: ProfileList)
    fun profileDelete(index: Int, profile: ProfileList)

}
