package com.gadfaria.nim.data

import android.content.ComponentName
import android.os.UserHandle
import java.text.Normalizer
import java.util.Locale

/**
 * A launchable app. No icon on purpose: the drawer is text only, so this is all
 * the launcher needs to load per app.
 *
 * [user] isn't decoration — with a work profile the same [componentName] shows
 * up twice, once per profile, and launching in the wrong profile opens the wrong
 * app.
 */
data class AppInfo(
    val label: String,
    val componentName: ComponentName,
    val user: UserHandle,
    val userSerial: Long,
    /**
     * App from a profile other than the launcher's — in practice, the work
     * profile. Without this the drawer lists "Gmail" twice, with the same name
     * and no visible difference.
     */
    val isWork: Boolean,
    /**
     * Set when the row isn't an app but a pinned shortcut — Chrome's "Add to
     * home screen", for example. Then [componentName] points to the shortcut's
     * owner app, which is what opens the app info.
     */
    val shortcutId: String? = null,
) {
    val isShortcut: Boolean get() = shortcutId != null

    /**
     * The app's identity across restarts — this is what goes to disk in the
     * favorites list.
     *
     * The serial comes from UserManager, not from `user.hashCode()`: hashCode has
     * no stability contract, and a favorite pointing to the wrong profile after
     * a reboot is a silent bug.
     */
    val key: String =
        if (shortcutId == null) "${componentName.flattenToShortString()}#$userSerial"
        // A shortcut is identified by package + id, not by activity: the same app
        // can pin several (one per site, in Chrome) and the activity may be missing.
        else "shortcut:${componentName.packageName}/$shortcutId#$userSerial"

    /**
     * Prepared once at load time, not on every keystroke. On a device with
     * hundreds of apps, normalizing and splitting the whole list into words per
     * typed character is the difference between instant and stuttering search.
     */
    val searchable: SearchableLabel = searchableOf(label)
}

/**
 * Lowercase and accent-free, so "cam" finds "Câmera" and "acoes" finds "Ações".
 * Locale.ROOT, not the device's: in Turkish "I" lowercases to a dotless "ı" and
 * "insta" would stop finding "Instagram".
 *
 * NFD splits the letter from the accent into two characters; then dropping the
 * diacritics (Unicode category Mn) leaves the base letter.
 */
fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase(Locale.ROOT)

private val DIACRITICS = "\\p{Mn}+".toRegex()
