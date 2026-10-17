package com.vknxworks.navictrl.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.auth.HttpFallbackNeeded
import com.vknxworks.navictrl.ui.components.ConfirmDialog
import com.vknxworks.navictrl.ui.components.PillButton
import com.vknxworks.navictrl.ui.components.friendlyMessage
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(onDone: () -> Unit, onCancel: (() -> Unit)?) {
    val context = LocalContext.current
    val c = context.container
    // The password can be shown in clear text here: keep this screen out of the recent-apps preview.
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.setRecentsScreenshotEnabled(false)
        onDispose { activity?.setRecentsScreenshotEnabled(true) }
    }
    val scope = rememberCoroutineScope()
    var address by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var offerHttp by remember { mutableStateOf<String?>(null) }
    val canSubmit = address.isNotBlank() && username.isNotBlank() && password.isNotEmpty() && !busy

    fun attempt(target: String) {
        busy = true
        error = null
        scope.launch {
            try {
                c.sessions.signIn(target, username, password)
                onDone()
            } catch (e: HttpFallbackNeeded) {
                offerHttp = e.httpUrl
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                error = e.friendlyMessage()
            } finally {
                busy = false
            }
        }
    }

    fun submit() {
        if (!canSubmit) return
        attempt(address)
    }

    offerHttp?.let { httpUrl ->
        ConfirmDialog(
            "Use an unencrypted connection?",
            "NaviCtrl couldn't connect to this server over HTTPS. It can try plain HTTP instead, " +
                "but then your password and music are sent unencrypted and anyone on the network can read them. " +
                "Only do this on a network you trust.",
            "Use HTTP",
            onDismiss = {
                offerHttp = null
                error = "Couldn't connect securely (HTTPS)"
            },
        ) {
            offerHttp = null
            address = httpUrl
            attempt(httpUrl)
        }
    }

    BackHandler(enabled = onCancel != null) { onCancel?.invoke() }

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.height(48.dp)) {
                if (onCancel != null) IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            }
            Text(if (onCancel == null) "NaviCtrl" else "Add a server", style = MaterialTheme.typography.displaySmall)
            Text(
                "Sign in to your Navidrome server with the address you'd open in a browser.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Field(
                address, { address = it },
                label = "Server address",
                placeholder = "music.example.com",
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next, autoCorrectEnabled = false),
                enabled = !busy,
            )
            if (address.trim().startsWith("http://", ignoreCase = true)) {
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceContainer).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.LockOpen, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Not encrypted (HTTP). Your password and music are sent in the clear. Fine on your home network, risky anywhere else.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Field(
                username, { username = it },
                label = "Username",
                keyboard = KeyboardOptions(imeAction = ImeAction.Next, capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                enabled = !busy,
            )
            Field(
                password, { password = it },
                label = "Password",
                visual = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                actions = KeyboardActions(onDone = { submit() }),
                enabled = !busy,
                trailing = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (showPassword) "Hide password" else "Show password")
                    }
                },
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            if (busy) {
                Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(28.dp)) }
            } else {
                PillButton("Sign in", onClick = ::submit, enabled = canSubmit, modifier = Modifier.fillMaxWidth())
            }
            Text(
                "Your password is stored encrypted on this phone and is only sent to your server.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    actions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    visual: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        singleLine = true,
        enabled = enabled,
        keyboardOptions = keyboard,
        keyboardActions = actions,
        visualTransformation = visual,
        trailingIcon = trailing,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
