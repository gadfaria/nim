package com.gadfaria.nim.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.gadfaria.nim.ui.theme.NimForegroundMuted

/**
 * Campo de busca da gaveta. BasicTextField em vez do TextField do Material
 * porque o Material traz caixa, borda e label — tudo que um launcher sem
 * ícones não quer.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onBackground,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Go,
            autoCorrectEnabled = false,
        ),
        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .padding(vertical = 14.dp),
        decorationBox = { field ->
            Box {
                if (query.isEmpty()) {
                    Text(
                        text = "buscar",
                        style = MaterialTheme.typography.titleMedium,
                        color = NimForegroundMuted,
                    )
                }
                field()
            }
        },
    )
}
