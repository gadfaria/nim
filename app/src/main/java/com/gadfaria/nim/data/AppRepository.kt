package com.gadfaria.nim.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.Handler
import android.os.Process
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.text.Collator

/**
 * The device's app list, from [LauncherApps].
 *
 * LauncherApps instead of PackageManager.queryIntentActivities: it gives the
 * work profiles, reports installs/uninstalls through a callback and launches via
 * startMainActivity, which records the right origin (the launcher) for the
 * animation.
 *
 * None of this works without the <queries> block in the manifest — without it
 * the list comes back empty with no error at all.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val userManager =
        context.getSystemService(Context.USER_SERVICE) as UserManager

    /**
     * The profile the launcher itself runs in. Anything from another profile is
     * work — this is the public way to tell them apart, since
     * UserManager.isManagedProfile(id) isn't open API.
     */
    private val myUser = Process.myUserHandle()

    // Real alphabetical order: Collator puts "Câmera" next to "Camera" and not
    // after "Z", which is what a sort by character code would do.
    private val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }

    private val byLabel = Comparator<AppInfo> { a, b ->
        collator.compare(a.label, b.label).takeIf { it != 0 }
        // The same name in both profiles is the common case (Gmail, Drive,
        // Calendar): personal always comes first. Breaking ties by ComponentName
        // left the order between the two "Gmail" rows arbitrary.
            ?: a.isWork.compareTo(b.isWork).takeIf { it != 0 }
            ?: a.componentName.compareTo(b.componentName)
    }

    /**
     * Emits the current list and re-emits on every install, uninstall or
     * rename. A launcher that only reads it in onCreate shows ghost apps.
     */
    val apps: Flow<List<AppInfo>> = callbackFlow {
        // The callbacks only signal: the reload itself happens downstream, off
        // the main thread.
        fun changed() { trySend(Unit) }

        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = changed()
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = changed()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = changed()
            override fun onPackagesAvailable(
                packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean,
            ) = changed()
            override fun onPackagesUnavailable(
                packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean,
            ) = changed()
            // Fires when a shortcut is pinned (PinShortcutActivity accepted it),
            // removed or disabled by its owner app.
            override fun onShortcutsChanged(
                packageName: String, shortcuts: MutableList<ShortcutInfo>, user: UserHandle,
            ) = changed()
        }

        // Explicit Handler: the callback needs a Looper and this runs on a
        // dispatcher that has none.
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        changed()
        awaitClose { launcherApps.unregisterCallback(callback) }
    }
        // An app update fires several callbacks in a row: conflated, the burst
        // costs one reload instead of one per callback.
        .conflate()
        .map { load() }
        // Reading every profile and every label is hundreds of ms of binder
        // calls — on the main thread that's a frozen home on every return.
        .flowOn(Dispatchers.IO)

    private fun load(): List<AppInfo> =
        launcherApps.profiles
            .flatMap { user ->
                val serial = userManager.getSerialNumberForUser(user)
                // Can fail if the profile disappears or enters quiet mode between
                // the callback and the read.
                val activities = runCatching { launcherApps.getActivityList(null, user) }
                    .getOrDefault(emptyList())
                    .map { activity ->
                        AppInfo(
                            label = activity.label.toString(),
                            componentName = activity.componentName,
                            user = user,
                            userSerial = serial,
                            isWork = user != myUser,
                        )
                    }
                activities + loadPinnedShortcuts(user, serial)
            }
            .sortedWith(byLabel)

    /**
     * Shortcuts pinned to Nim, mixed with the apps in alphabetical order — the
     * user looks for "Vault" by name, without knowing (or needing to know)
     * whether it became a WebAPK or stayed a Chrome shortcut.
     *
     * Only the default launcher can read shortcuts; otherwise getShortcuts
     * throws SecurityException, and with the work profile locked,
     * IllegalStateException. Either way the drawer carries on with just the apps.
     */
    private fun loadPinnedShortcuts(user: UserHandle, serial: Long): List<AppInfo> {
        if (!launcherApps.hasShortcutHostPermission()) return emptyList()
        val query = LauncherApps.ShortcutQuery().setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED,
        )
        return runCatching { launcherApps.getShortcuts(query, user) }
            .getOrNull()
            .orEmpty()
            // Disabled = the owner app gave up on it (site removed, account gone).
            // Showing a name that opens nothing is worse than not showing it.
            .filter { it.isEnabled }
            .map { shortcut ->
                AppInfo(
                    label = shortcut.displayLabel(),
                    // Without an activity, a package-only ComponentName is enough:
                    // the app info screen only looks at the package.
                    componentName = shortcut.activity ?: ComponentName(shortcut.`package`, ""),
                    user = user,
                    userSerial = serial,
                    isWork = user != myUser,
                    shortcutId = shortcut.id,
                )
            }
    }

    /**
     * [sourceBounds] is where the opening animation starts from. Passing null
     * makes the app grow from the center of the screen instead of from the
     * tapped item.
     */
    fun launch(app: AppInfo, sourceBounds: Rect? = null) {
        runCatching {
            if (app.shortcutId != null) {
                launcherApps.startShortcut(
                    app.componentName.packageName, app.shortcutId, sourceBounds, null, app.user,
                )
            } else {
                launcherApps.startMainActivity(app.componentName, app.user, sourceBounds, null)
            }
        }
    }

    /** Opens the system details screen. Through LauncherApps to get the profile right. */
    fun openAppInfo(app: AppInfo, sourceBounds: Rect? = null) {
        runCatching {
            launcherApps.startAppDetailsActivity(app.componentName, app.user, sourceBounds, null)
        }
    }

    /**
     * Requests the uninstall. Android's dialog is what confirms — and what
     * refuses, for a system app; Nim deletes nothing on its own.
     */
    fun requestUninstall(app: AppInfo) {
        if (app.shortcutId != null) return unpin(app)
        runCatching {
            val intent = Intent(
                Intent.ACTION_DELETE,
                "package:${app.componentName.packageName}".toUri(),
            )
                .putExtra(Intent.EXTRA_USER, app.user)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
        }
    }

    /**
     * Unpinning a shortcut is its "uninstall": there's no app to remove, just
     * the shortcut to drop. The API has no "unpin this one": the launcher passes
     * the whole list that stays pinned for that package, minus the one leaving.
     */
    private fun unpin(app: AppInfo) {
        val packageName = app.componentName.packageName
        runCatching {
            val query = LauncherApps.ShortcutQuery()
                .setPackage(packageName)
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            val remaining = launcherApps.getShortcuts(query, app.user)
                .orEmpty()
                .map { it.id }
                .filter { it != app.shortcutId }
            launcherApps.pinShortcuts(packageName, remaining, app.user)
        }
    }
}

/** The name a shortcut is listed under: its short label, falling back to the long one and the id. */
fun ShortcutInfo.displayLabel(): String = (shortLabel ?: longLabel ?: id).toString()
