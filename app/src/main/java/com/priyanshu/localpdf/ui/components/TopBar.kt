package com.priyanshu.localpdf.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

import com.priyanshu.localpdf.engine.TaskProgressManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    onMenuClicked: () -> Unit
) {
    var isSearching by remember { mutableStateOf(false) }
    val taskState by TaskProgressManager.taskState.collectAsState()

    TopAppBar(
        title = {
            if (isSearching) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = MaterialTheme.typography.titleMedium.fontSize),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("Search tools...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        innerTextField()
                    }
                )
            } else {
                Text("LocalPDF Pro", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClicked) {
                Icon(Icons.Rounded.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            if (taskState.isActive) {
                Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = taskState.progress,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = {
                if (isSearching) {
                    onSearchChanged("")
                }
                isSearching = !isSearching
            }) {
                Icon(
                    imageVector = if (isSearching) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = "Search"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}
