package io.github.saeeddev94.xray

interface ProfileTouchCallback {
    fun onItemMoved(fromPosition: Int, toPosition: Int): Boolean
    fun onItemMoveCompleted(startPosition: Int, endPosition: Int)
}
