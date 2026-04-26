package com.priyanshu.localpdf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.priyanshu.localpdf.models.ToolCategory

@Composable
fun BottomFilterBar(
    selectedCategory: ToolCategory?,
    onCategorySelected: (ToolCategory?) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(CutCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ToolCategory.values().forEach { category ->
                val icon = when (category) {
                    ToolCategory.MOST_USED -> Icons.Rounded.Star
                    ToolCategory.CONVERT_TO_PDF -> Icons.Rounded.NoteAdd
                    ToolCategory.CONVERT_FROM_PDF -> Icons.Rounded.FileDownload
                    ToolCategory.SECURITY -> Icons.Rounded.Lock
                    ToolCategory.PRO_FEATURES -> Icons.Rounded.AutoAwesome
                }
                IconButton(
                    onClick = { onCategorySelected(if (selectedCategory == category) null else category) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (selectedCategory == category) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (selectedCategory == category) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(icon, contentDescription = category.title)
                }
            }
        }
    }
}
