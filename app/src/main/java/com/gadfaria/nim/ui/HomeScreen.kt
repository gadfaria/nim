package com.gadfaria.nim.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gadfaria.nim.R
import com.gadfaria.nim.data.AppInfo
import com.gadfaria.nim.ui.theme.Foreground
import com.gadfaria.nim.ui.theme.MutedForeground
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Clock driven by ACTION_TIME_TICK (the system sends it every minute).
 * Better than a delay loop: it doesn't wake the CPU for nothing and follows
 * time zone changes, manual time changes and day rollover for free.
 */
@Composable
private fun rememberCurrentTime(): State<LocalDateTime> {
    val context = LocalContext.current
    val time = remember { mutableStateOf(LocalDateTime.now()) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                time.value = LocalDateTime.now()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(
            context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        time.value = LocalDateTime.now()
        onDispose { context.unregisterReceiver(receiver) }
    }
    return time
}

@Composable
fun HomeScreen(
    favorites: List<AppInfo>,
    onLaunch: (AppInfo, Rect?) -> Unit,
    onOpenMenu: (AppInfo, Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Clock in the top third, favorites at the bottom: that's where the
        // thumb reaches without repositioning the hand.
        Spacer(Modifier.weight(1f))

        Column(Modifier.padding(horizontal = CardTextInset)) { Clock() }

        Spacer(Modifier.weight(2f))

        // No favorites, no card: an empty frame would be the only thing on screen.
        if (favorites.isNotEmpty()) {
            // Borderless and without press highlight: on the home the
            // favorites read as a plain list under their label.
            NimCard(bordered = false) {
                GroupLabel(stringResource(R.string.favorites_label))
                favorites.forEach { app ->
                    AppRow(
                        app = app,
                        // Everything here is a favorite by definition: the dot would be noise.
                        isFavorite = false,
                        onLaunch = onLaunch,
                        onOpenMenu = onOpenMenu,
                        highlightOnPress = false,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Signature()

        Spacer(Modifier.height(16.dp))
    }
}

/** Author credit, quiet like a sidebar footer: muted, with the nick a step brighter. */
@Composable
private fun Signature() {
    Text(
        text = buildAnnotatedString {
            append("powered by ")
            withStyle(SpanStyle(color = Foreground, fontWeight = FontWeight.Medium)) {
                append("gadfaria")
            }
        },
        style = MaterialTheme.typography.labelMedium,
        color = MutedForeground,
        modifier = Modifier.padding(horizontal = CardTextInset),
    )
}

@Composable
private fun Clock() {
    val now by rememberCurrentTime()
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale

    // 12h or 24h is the user's system preference, not the app's — hardcoding
    // "HH:mm" makes the launcher clock disagree with the status bar clock.
    val use24h = DateFormat.is24HourFormat(context)
    val hourFormat = remember(locale, use24h) {
        DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm", locale)
    }
    val dateFormat = remember(locale) { DateTimeFormatter.ofPattern("EEE, d MMM", locale) }

    Text(
        text = now.format(hourFormat),
        style = MaterialTheme.typography.displayLarge,
        color = Foreground,
    )
    Text(
        text = now.format(dateFormat),
        style = MaterialTheme.typography.bodyLarge,
        color = MutedForeground,
    )
}
