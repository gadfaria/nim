package com.gadfaria.nim.data

import android.content.ComponentName
import android.os.UserHandle
import java.text.Normalizer
import java.util.Locale

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

    /**
     * Calculado uma vez no carregamento, não a cada tecla. Num aparelho com
     * centenas de apps, normalizar a lista inteira por caractere digitado é a
     * diferença entre busca instantânea e busca engasgada.
     */
    val normalizedLabel: String = normalizeForSearch(label)
}

/**
 * Minúsculas e sem acento, para que "cam" ache "Câmera" e "acoes" ache "Ações".
 *
 * NFD separa a letra do acento em dois caracteres; aí basta descartar os
 * diacríticos (categoria Unicode Mn) e sobra a letra base.
 */
fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase(Locale.getDefault())

private val DIACRITICS = "\\p{Mn}+".toRegex()
