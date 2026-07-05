package com.mute.shutter.data

import android.content.Context

class SessionPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isPaired: Boolean
        get() = prefs.getBoolean(KEY_PAIRED, false)
        set(value) = prefs.edit().putBoolean(KEY_PAIRED, value).apply()

    var lastConnectPort: Int
        get() = prefs.getInt(KEY_CONNECT_PORT, 0)
        set(value) = prefs.edit().putInt(KEY_CONNECT_PORT, value).apply()

    var lastHost: String?
        get() = prefs.getString(KEY_HOST, null)
        set(value) = prefs.edit().putString(KEY_HOST, value).apply()

    var lastMuteValue: String?
        get() = prefs.getString(KEY_MUTE_VALUE, null)
        set(value) = prefs.edit().putString(KEY_MUTE_VALUE, value).apply()

    var watcherRunning: Boolean
        get() = prefs.getBoolean(KEY_WATCHER_RUNNING, false)
        set(value) = prefs.edit().putBoolean(KEY_WATCHER_RUNNING, value).apply()

    fun clearPairing() {
        prefs.edit()
            .remove(KEY_PAIRED)
            .remove(KEY_CONNECT_PORT)
            .remove(KEY_HOST)
            .remove(KEY_MUTE_VALUE)
            .remove(KEY_WATCHER_RUNNING)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "mute_session"
        private const val KEY_PAIRED = "is_paired"
        private const val KEY_CONNECT_PORT = "last_connect_port"
        private const val KEY_HOST = "last_host"
        private const val KEY_MUTE_VALUE = "last_mute_value"
        private const val KEY_WATCHER_RUNNING = "watcher_running"
    }
}
