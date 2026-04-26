package com.priyanshu.localpdf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.ui.unit.dp
import com.priyanshu.localpdf.models.ToolCategory
import com.priyanshu.localpdf.models.allPdfTools
import com.priyanshu.localpdf.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onToolClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ToolCategory?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val filteredTools = remember(searchQuery, selectedCategory) {
        allPdfTools.filter { tool ->
            val matchesCategory = selectedCategory == null || tool.category == selectedCategory
            val matchesSearch = tool.title.contains(searchQuery, ignoreCase = true) ||
                    tool.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.background,
                drawerShape = androidx.compose.foundation.shape.RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "PDF Toolkit",
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                NavigationDrawerItem(
                    label = { Text("All Tools", fontWeight = FontWeight.SemiBold) },
                    selected = selectedCategory == null,
                    onClick = {
                        selectedCategory = null
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.surface,
                        unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )

                val expandedCategories = remember { mutableStateListOf<ToolCategory>() }

                ToolCategory.values().forEach { category ->
                    val isExpanded = expandedCategories.contains(category)

                    Column {
                        NavigationDrawerItem(
                            label = { Text(category.title, fontWeight = FontWeight.SemiBold) },
                            selected = false,
                            onClick = {
                                if (isExpanded) expandedCategories.remove(category)
                                else expandedCategories.add(category)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            badge = {
                                Icon(
                                    if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )

                        androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
                            Column {
                                allPdfTools.filter { it.category == category }.forEach { tool ->
                                    NavigationDrawerItem(
                                        label = { Text(tool.title) },
                                        selected = false,
                                        onClick = {
                                            onToolClick(tool.id)
                                            scope.launch { drawerState.close() }
                                        },
                                        icon = { Icon(tool.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                        modifier = Modifier.padding(start = 32.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
                                        colors = NavigationDrawerItemDefaults.colors(
                                            unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedIconColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    searchQuery = searchQuery,
                    onSearchChanged = { searchQuery = it },
                    onMenuClicked = { scope.launch { drawerState.open() } }
                )
            },
            bottomBar = {
                BottomFilterBar(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ToolGrid(
                    tools = filteredTools,
                    onToolClick = { onToolClick(it.id) },
                    header = { StatsBar() } // Inject StatsBar so it scrolls inline and inherits width padding!
                )
            }
        }
    }
}