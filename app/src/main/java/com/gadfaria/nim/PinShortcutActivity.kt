package com.gadfaria.nim

import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gadfaria.nim.data.displayLabel
import com.gadfaria.nim.ui.PinShortcutPrompt
import com.gadfaria.nim.ui.theme.NimTheme

/**
 * Handles "pin shortcut" requests (ShortcutManager.requestPinShortcut).
 *
 * Always asks first. Chrome shows its own "Install" dialog, but nothing forces
 * an app to: confirming is the launcher's job. Accepting silently would let any
 * app drop a shortcut named after another app into the drawer, where a
 * text-only list can't tell them apart.
 *
 * For a PWA, accepting is enough — Chrome installs the WebAPK and it shows up in
 * the drawer through AppRepository's LauncherApps.Callback, like any other app.
 *
 * Widget requests (CONFIRM_PIN_APPWIDGET) don't arrive here: Nim has no widgets.
 */
class PinShortcutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launcherApps = getSystemService(LauncherApps::class.java)
        // The activity is exported, so the intent can be anything: reading a
        // malformed extra throws.
        val request = runCatching { launcherApps?.getPinItemRequest(intent) }.getOrNull()
        val shortcut = request
            ?.takeIf {
                it.isValid && it.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT
            }
            ?.shortcutInfo

        if (launcherApps == null || request == null || shortcut == null) {
            finish()
            return
        }

        val label = shortcut.displayLabel()
        val requester = requesterLabel(launcherApps, shortcut)

        enableEdgeToEdge()
        setContent {
            NimTheme {
                PinShortcutPrompt(
                    label = label,
                    requester = requester,
                    onAccept = {
                        // accept() can throw if the request expired while the prompt was open.
                        runCatching { request.accept() }
                        finish()
                    },
                    onCancel = ::finish,
                )
            }
        }
    }

    /** The requesting app's name, or its package when the name can't be read. */
    private fun requesterLabel(launcherApps: LauncherApps, shortcut: ShortcutInfo): String =
        runCatching {
            launcherApps.getApplicationInfo(shortcut.`package`, 0, shortcut.userHandle)
                ?.let { packageManager.getApplicationLabel(it).toString() }
        }.getOrNull() ?: shortcut.`package`
}
