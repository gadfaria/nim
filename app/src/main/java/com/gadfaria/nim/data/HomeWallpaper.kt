package com.gadfaria.nim.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import com.gadfaria.nim.R

/**
 * Papel de parede da tela inicial com a cor de fundo do Nim.
 *
 * Existe por causa dos apps recentes: aquela tela é do Pixel Launcher, não do
 * Nim, e ela sempre desenha o papel de parede atrás dos cartões. Nenhum launcher
 * de terceiros consegue assumir os recentes, então o jeito de o fundo bater com
 * o Nim é o próprio papel de parede ter a cor dele.
 *
 * Só FLAG_SYSTEM: a tela de bloqueio é onde o papel de parede ainda aparece de
 * verdade, e trocá-la junto apagaria a escolha do usuário sem motivo.
 */
class HomeWallpaper(context: Context) {

    private val appContext = context.applicationContext
    private val wallpaperManager = WallpaperManager.getInstance(appContext)

    /**
     * Bloqueante — decodificar e gravar o papel de parede leva centenas de ms.
     * Chamar fora da main thread. Devolve false se o aparelho ou uma política
     * de administrador não deixar trocar.
     */
    fun applyNimBackground(): Boolean {
        if (!wallpaperManager.isWallpaperSupported || !wallpaperManager.isSetWallpaperAllowed) {
            return false
        }
        // Cor sólida não tem detalhe a perder na escala: um bitmap pequeno
        // esticado pelo sistema fica idêntico a um do tamanho da tela.
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
