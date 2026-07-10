package com.easywiki.ui.wiki

import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin

@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(context))
            .build()
    }
    val normalizedMarkdown = remember(markdown) { normalizeMarkdown(markdown) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                textSize = 16f
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, normalizedMarkdown)
        }
    )
}

/**
 * 规范化 Agent/LLM 返回的 Markdown，修复表格行被挤在同一行、字面量 \n 等问题。
 */
internal fun normalizeMarkdown(markdown: String): String {
    val withNewlines = markdown.replace("\\n", "\n")
    return expandCollapsedTableRows(withNewlines)
}

private fun expandCollapsedTableRows(markdown: String): String {
    val lines = markdown.lines()
    if (lines.none { it.contains('|') }) return markdown

    val result = mutableListOf<String>()
    var columnCount = 0
    var inTable = false

    for (line in lines) {
        val trimmed = line.trim()
        when {
            isTableSeparator(trimmed) -> {
                inTable = columnCount > 0
                result.add(line)
            }
            inTable && isTableRow(trimmed) -> {
                expandCollapsedTableLine(trimmed, columnCount).forEach { row ->
                    result.add(row)
                }
            }
            isTableRow(trimmed) && columnCount == 0 -> {
                columnCount = parseTableCells(trimmed).size
                result.add(line)
            }
            else -> {
                if (!isTableRow(trimmed)) {
                    inTable = false
                    columnCount = 0
                }
                result.add(line)
            }
        }
    }
    return result.joinToString("\n")
}

private fun isTableSeparator(line: String): Boolean {
    return line.startsWith("|") && line.contains('-')
}

private fun isTableRow(line: String): Boolean {
    return line.startsWith("|") && line.endsWith("|")
}

private fun parseTableCells(line: String): List<String> {
    return line.trim().trim('|').split('|').map { it.trim() }
}

private fun expandCollapsedTableLine(line: String, columnCount: Int): List<String> {
    val cells = parseTableCells(line)
    if (cells.size <= columnCount) return listOf(line)

    val rows = mutableListOf<List<String>>()
    var current = mutableListOf<String>()
    for (cell in cells) {
        when {
            cell.isEmpty() && current.size == columnCount -> {
                rows.add(current.toList())
                current = mutableListOf()
            }
            cell.isNotEmpty() -> {
                current.add(cell)
                if (current.size == columnCount) {
                    rows.add(current.toList())
                    current = mutableListOf()
                }
            }
        }
    }
    if (current.isNotEmpty()) {
        rows.add(current)
    }

    if (rows.size <= 1 && cells.size % columnCount == 0) {
        return cells.chunked(columnCount).map { formatTableRow(it) }
    }
    return if (rows.size > 1) rows.map { formatTableRow(it) } else listOf(line)
}

private fun formatTableRow(cells: List<String>): String {
    return "| ${cells.joinToString(" | ")} |"
}
