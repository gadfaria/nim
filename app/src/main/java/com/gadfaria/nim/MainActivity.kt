package com.gadfaria.nim

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import com.gadfaria.nim.ui.HomeScreen
import com.gadfaria.nim.ui.theme.NimBackground
import com.gadfaria.nim.ui.theme.NimTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MainActivity : ComponentActivity() {

    /**
     * Emite quando o usuário aperta HOME já estando no Nim. A partir do M2 isto
     * fecha a gaveta e devolve o scroll ao topo; hoje só existe pra fiação
     * ficar pronta.
     */
    private val homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Fundo é sempre escuro, então os ícones das barras são sempre claros.
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        // Numa home, o botão voltar não faz nada. Nunca finish(): o launcher
        // é o que sobra quando tudo mais é fechado — sair dele deixa a tela preta.
        onBackPressedDispatcher.addCallback(this) { /* no-op proposital */ }

        setContent {
            NimTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NimBackground)
                        .systemBarsPadding()
                ) {
                    HomeScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homePressed.tryEmit(Unit)
    }

    fun homePresses() = homePressed.asSharedFlow()
}
