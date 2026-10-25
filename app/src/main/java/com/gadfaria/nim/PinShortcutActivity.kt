package com.gadfaria.nim

import android.app.Activity
import android.content.pm.LauncherApps
import android.os.Bundle

/**
 * Atende os pedidos de "fixar atalho" (ShortcutManager.requestPinShortcut).
 *
 * O app que pede já mostrou o próprio diálogo de confirmação (o "Instalar" do
 * Chrome, por exemplo), então aqui não há UI: aceita e fecha. No caso de PWA,
 * aceitar já basta — o Chrome instala o WebAPK e ele aparece na gaveta pelo
 * LauncherApps.Callback do AppRepository, como qualquer outro app.
 *
 * Pedidos de widget (CONFIRM_PIN_APPWIDGET) não chegam aqui: o Nim não tem widgets.
 */
class PinShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val request = getSystemService(LauncherApps::class.java)?.getPinItemRequest(intent)
        if (request != null &&
            request.isValid &&
            request.requestType == LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT
        ) {
            // accept() pode lançar se o pedido expirar entre a checagem e a chamada.
            runCatching { request.accept() }
        }

        finish()
    }
}
