package com.gadfaria.nim.data

import android.content.ComponentName
import android.os.UserHandle

/**
 * Um app lançável. Sem ícone de propósito: a gaveta é só texto, então isto é
 * tudo que o launcher precisa carregar por app.
 *
 * O [user] não é decoração — com perfil de trabalho o mesmo [componentName]
 * aparece duas vezes, uma por perfil, e lançar no perfil errado abre o app
 * errado.
 */
data class AppInfo(
    val label: String,
    val componentName: ComponentName,
    val user: UserHandle,
) {
    /** Estável entre recarregamentos; serve de chave nas listas do Compose. */
    val key: String = "${componentName.flattenToShortString()}@${user.hashCode()}"
}
