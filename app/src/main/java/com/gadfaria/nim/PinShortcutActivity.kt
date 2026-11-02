package com.gadfaria.nim

import android.app.Activity
import android.content.pm.LauncherApps
import android.os.Bundle

/**
 * Handles "pin shortcut" requests (ShortcutManager.requestPinShortcut).
 *
 * The requesting app has already shown its own confirmation dialog (Chrome's
 * "Install", for example), so there's no UI here: accept and close. For a PWA,
 * accepting is enough — Chrome installs the WebAPK and it shows up in the drawer
 * through AppRepository's LauncherApps.Callback, like any other app.
 *
 * Widget requests (CONFIRM_PIN_APPWIDGET) don't arrive here: Nim has no widgets.
 */
class PinShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val request = getSystemService(LauncherApps::class.java)?.getPinItemRequest(intent)
        if (request != null &&
            request.isValid &&
            request.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT
        ) {
            // accept() can throw if the request expires between the check and the call.
            runCatching { request.accept() }
        }

        finish()
    }
}
