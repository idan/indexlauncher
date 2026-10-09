package app.indexlauncher.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.indexlauncher.R
import app.indexlauncher.model.AppEntry
import app.indexlauncher.model.IconShape
import app.indexlauncher.model.searchApps
import kotlinx.coroutines.flow.drop

@Composable
fun SearchScreen(
    apps: List<AppEntry>,
    favoriteIds: List<String>,
    catalogVersion: Int,
    iconShape: IconShape,
    loadIcon: suspend (AppEntry, Int, IconShape) -> Bitmap?,
    onToggleFavorite: (String) -> Unit,
    onAppClick: (AppEntry) -> Unit,
    onDismiss: () -> Unit,
    active: Boolean = true,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var openRowId by remember { mutableStateOf<String?>(null) }
    val results = remember(apps, query) { searchApps(apps, query) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(active) {
        if (active) {
            focusRequester.requestFocus()
            keyboard?.show()
        } else {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }
    LaunchedEffect(Unit) {
        // Don't reset a restored search scroll position just because the activity was recreated.
        snapshotFlow { query }.drop(1).collect {
            listState.scrollToItem(0)
            openRowId = null
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                placeholder = { Text(stringResource(R.string.search_apps)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            )
            IconButton(onClick = {
                keyboard?.hide()
                onDismiss()
            }) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.close_search))
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        ) {
            if (results.isEmpty()) {
                item(key = "no-results") {
                    Text(
                        stringResource(R.string.no_search_results),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            items(results, key = { it.id }) { app ->
                AppRow(
                    app = app,
                    catalogVersion = catalogVersion,
                    iconShape = iconShape,
                    loadIcon = loadIcon,
                    isFavorite = app.id in favoriteIds,
                    showLabelShadow = false,
                    isOpen = openRowId == app.id,
                    onReveal = { openRowId = it },
                    onToggleFavorite = onToggleFavorite,
                    onClick = onAppClick,
                )
            }
        }
    }
}
