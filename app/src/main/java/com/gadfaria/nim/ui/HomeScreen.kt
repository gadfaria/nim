package com.gadfaria.nim.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.gadfaria.nim.ui.theme.NimForegroundMuted
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Relógio movido por ACTION_TIME_TICK (o sistema emite a cada minuto).
 * Melhor que um loop com delay: não acorda a CPU à toa e acompanha
 * mudança de fuso, de hora manual e virada de dia de graça.
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
fun HomeScreen(modifier: Modifier = Modifier) {
    val now by rememberCurrentTime()
    val context = LocalContext.current
    val locale = Locale.getDefault()

    // 12h ou 24h é preferência do usuário no sistema, não do app — fixar "HH:mm"
    // faz o relógio do launcher discordar do relógio da barra de status.
    val use24h = DateFormat.is24HourFormat(context)
    val hourFormat = remember(locale, use24h) {
        DateTimeFormatter.ofPattern(if (use24h) "HH:mm" else "h:mm", locale)
    }
    val dateFormat = remember(locale) { DateTimeFormatter.ofPattern("EEE, d MMM", locale) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = now.format(hourFormat),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = now.format(dateFormat),
            style = MaterialTheme.typography.bodyLarge,
            color = NimForegroundMuted,
        )
    }
}
