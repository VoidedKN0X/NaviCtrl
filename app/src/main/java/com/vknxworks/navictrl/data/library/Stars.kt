package com.vknxworks.navictrl.data.library

import android.content.Context
import android.widget.Toast
import com.vknxworks.navictrl.data.api.StarKind
import com.vknxworks.navictrl.data.api.setStar
import com.vknxworks.navictrl.data.auth.SessionManager
import com.vknxworks.navictrl.ui.components.friendlyMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Favourites changed in this session. Lists loaded earlier still carry the server's old value,
 * so every heart reads through here and stays in step across screens.
 */
class StarStore(private val context: Context, private val sessions: SessionManager, private val scope: CoroutineScope) {
    private val _overrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val overrides: StateFlow<Map<String, Boolean>> = _overrides.asStateFlow()

    fun isStarred(id: String, fromServer: Boolean): Boolean = _overrides.value[id] ?: fromServer

    fun set(kind: StarKind, id: String, starred: Boolean) {
        val client = sessions.active.value?.client ?: return
        val before = _overrides.value[id]
        _overrides.update { it + (id to starred) }
        scope.launch {
            runCatching { client.setStar(kind, id, starred) }.onFailure { e ->
                _overrides.update { if (before == null) it - id else it + (id to before) }
                withContext(Dispatchers.Main) { Toast.makeText(context, e.friendlyMessage(), Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun clear() {
        _overrides.value = emptyMap()
    }
}
