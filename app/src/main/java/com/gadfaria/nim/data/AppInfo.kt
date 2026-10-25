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
    val userSerial: Long,
    /**
     * App de outro perfil que não o do launcher — na prática, o perfil de
     * trabalho. Sem isto a gaveta lista "Gmail" duas vezes, com o mesmo nome e
     * nenhuma diferença visível.
     */
    val isWork: Boolean,
    /**
     * Preenchido quando a linha não é um app, e sim um atalho fixado — o
     * "Adicionar à tela inicial" do Chrome, por exemplo. Aí [componentName]
     * aponta pro app dono do atalho, que é quem abre as informações.
     */
    val shortcutId: String? = null,
) {
    val isShortcut: Boolean get() = shortcutId != null

    /**
     * Identidade do app entre reinícios — é ela que vai pro disco na lista de
     * favoritos.
     *
     * O serial vem de UserManager, não de `user.hashCode()`: hashCode não tem
     * contrato de estabilidade, e um favorito que aponta pro perfil errado
     * depois de um reboot é um bug silencioso.
     */
    val key: String =
        if (shortcutId == null) "${componentName.flattenToShortString()}#$userSerial"
        // Atalho é identificado por pacote + id, não por activity: o mesmo app
        // pode fixar vários (um por site, no Chrome) e a activity pode faltar.
        else "shortcut:${componentName.packageName}/$shortcutId#$userSerial"

    /**
     * Preparado uma vez no carregamento, não a cada tecla. Num aparelho com
     * centenas de apps, normalizar e quebrar em palavras a lista inteira por
     * caractere digitado é a diferença entre busca instantânea e engasgada.
     */
    val searchable: SearchableLabel = searchableOf(label)
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
