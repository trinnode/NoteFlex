package com.noteflex.overlay

object AuthState {
    private val unlockedTabs = mutableSetOf<String>()

    fun unlock(tabId: String) {
        unlockedTabs.add(tabId)
    }

    fun isUnlocked(tabId: String): Boolean = unlockedTabs.contains(tabId)

    fun lock(tabId: String) {
        unlockedTabs.remove(tabId)
    }

    fun lockAll() {
        unlockedTabs.clear()
    }
}
