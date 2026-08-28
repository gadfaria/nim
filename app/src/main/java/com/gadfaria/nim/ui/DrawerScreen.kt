package com.gadfaria.nim.ui

import android.graphics.Rect
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.gadfaria.nim.data.AppInfo

@Composable
fun DrawerScreen(
    apps: List<AppInfo>,
    query: String,
    onQueryChange: (String) -> Unit,
    onLaunch: (AppInfo, Rect?) -> Unit,
    favoriteKeys: Set<String>,
    onOpenMenu: (AppInfo, Rect?) -> Unit,
    searchFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // O teclado cobre a lista sem isto — e é justamente a lista que o
            // usuário está olhando enquanto digita.
            .imePadding()
            .padding(horizontal = 28.dp),
    ) {
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            onSubmit = { apps.firstOrNull()?.let { onLaunch(it, null) } },
            focusRequester = searchFocusRequester,
        )

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(apps, key = { it.key }) { app ->
                AppRow(
                    app = app,
                    isFavorite = app.key in favoriteKeys,
                    onLaunch = onLaunch,
                    onOpenMenu = onOpenMenu,
                )
            }
        }
    }
}
