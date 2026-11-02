package com.gadfaria.nim.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import com.gadfaria.nim.R

/**
 * Home screen wallpaper in Nim's background color.
 *
 * It exists because of recents: that screen belongs to Pixel Launcher, not Nim,
 * and it always draws the wallpaper behind the cards. No third-party launcher
 * can take over recents, so the way to make the background match Nim is for the
 * wallpaper itself to have its color.
 *
 * FLAG_SYSTEM only: the lock screen is where the wallpaper still really shows,
 * and changing it too would wipe the user's choice for no reason.
 */
class HomeWallpaper(context: Context) {

    private val appContext = context.applicationContext
    private val wallpaperManager = WallpaperManager.getInstance(appContext)

    /**
     * Blocking — decoding and writing the wallpaper takes hundreds of ms. Call it
     * off the main thread. Returns false if the device or an admin policy
     * doesn't allow changing it.
     */
    fun applyNimBackground(): Boolean {
        if (!wallpaperManager.isWallpaperSupported || !wallpaperManager.isSetWallpaperAllowed) {
            return false
        }
        // A solid color has no detail to lose when scaled: a small bitmap
        // stretched by the system looks identical to a screen-sized one.
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888).apply {
            eraseColor(appContext.getColor(R.color.nim_background))
        }
        return runCatching {
            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
        }.isSuccess
    }

    private companion object {
        const val SIZE = 64
    }
}
