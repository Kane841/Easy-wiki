package com.easywiki.ui.wiki

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.easywiki.ui.common.EmptyState
import com.easywiki.ui.common.ShimmerListSkeleton
import com.easywiki.viewmodel.WikiTreeUiState

@Composable
fun WikiTreeScreen(
    uiState: WikiTreeUiState,
    onRefresh: () -> Unit,
    onPageClick: (Long) -> Unit,
    onCreatePage: (String, String?) -> Unit,
    onShowCreateDialog: () -> Unit,
    onHideCreateDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var newPageTitle by remember { mutableStateOf("") }
    var newPageContent by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (uiState.tree.isEmpty() && !uiState.isLoading) {
            onRefresh()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.flatNodes.isEmpty() -> {
                ShimmerListSkeleton(itemCount = 5, lineCount = 1)
            }
            uiState.flatNodes.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.MenuBook,
                    title = "暂无 Wiki 页面",
                    subtitle = "点击右下角 + 创建第一个页面"
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.flatNodes, key = { it.id }) { node ->
                        Text(
                            text = node.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPageClick(node.id) }
                                .padding(start = (node.depth * 16 + 8).dp, top = 12.dp, bottom = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

        // Create Page Button
        FloatingActionButton(
            onClick = onShowCreateDialog,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "创建页面")
        }

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }

        // Create Dialog
        if (uiState.showCreateDialog) {
            AlertDialog(
                onDismissRequest = onHideCreateDialog,
                title = { Text("创建 Wiki 页面") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPageTitle,
                            onValueChange = { newPageTitle = it },
                            label = { Text("标题") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPageContent,
                            onValueChange = { newPageContent = it },
                            label = { Text("内容（可选）") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onCreatePage(newPageTitle, newPageContent.takeIf { it.isNotBlank() })
                            newPageTitle = ""
                            newPageContent = ""
                        }
                    ) {
                        Text("创建")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onHideCreateDialog) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
