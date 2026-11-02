package com.gadfaria.nim

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsControllerCompat
import com.gadfaria.nim.ui.NimApp
import com.gadfaria.nim.ui.theme.NimBackground
import com.gadfaria.nim.ui.theme.NimTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MainActivity : ComponentActivity() {

    private val viewModel: NimViewModel by viewModels()

    /** Emits when the user presses HOME while already in Nim. */
    private val homePressed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The background is always dark, so the bar icons are always light.
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        // On a home, the back button does nothing. Never finish(): the launcher
        // is what's left when everything else is closed — leaving it blacks out
        // the screen. The drawer's BackHandler is registered later and takes
        // priority over this one.
        onBackPressedDispatcher.addCallback(this) { /* intentional no-op */ }

        setContent {
            NimTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NimBackground)
                        .systemBarsPadding()
                ) {
                    NimApp(
                        viewModel = viewModel,
                        homePresses = homePressed.asSharedFlow(),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homePressed.tryEmit(Unit)
    }
}
