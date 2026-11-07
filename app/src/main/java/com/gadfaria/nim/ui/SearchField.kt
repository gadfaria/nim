package com.gadfaria.nim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gadfaria.nim.ui.theme.Foreground
import com.gadfaria.nim.ui.theme.InputBorder
import com.gadfaria.nim.ui.theme.MutedForeground
import com.gadfaria.nim.ui.theme.RadiusLg
import com.gadfaria.nim.ui.theme.Ring

/**
 * The drawer's search field, shaped like shadcn's Input: rounded-lg, 1dp input
 * border over bg-input/30; when focused the border turns into the ring color.
 * BasicTextField instead of Material's TextField, which brings its own label
 * and indicator line.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val style = MaterialTheme.typography.titleMedium

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = style.copy(color = Foreground),
        cursorBrush = SolidColor(Foreground),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Go,
            autoCorrectEnabled = false,
        ),
        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { field ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (focused) Ring else InputBorder, RadiusLg)
                    .background(InputBorder.copy(alpha = 0.045f), RadiusLg)
                    .padding(horizontal = ItemPadding, vertical = 10.dp),
            ) {
                if (query.isEmpty()) {
                    Text(text = "buscar", style = style, color = MutedForeground)
                }
                field()
            }
        },
    )
}
