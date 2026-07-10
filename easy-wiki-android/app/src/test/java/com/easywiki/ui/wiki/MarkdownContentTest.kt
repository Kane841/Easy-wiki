package com.easywiki.ui.wiki

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownContentTest {

    @Test
    fun `expand collapsed table rows into separate lines`() {
        val input = """
            | ID | Name | Status |
            |----|------|--------|
            | 4 | task4 | TODO | | 3 | task3 | DONE |
        """.trimIndent()

        val result = normalizeMarkdown(input)

        assertTrue(result.contains("| 4 | task4 | TODO |"))
        assertTrue(result.contains("| 3 | task3 | DONE |"))
        assertTrue(!result.contains("| TODO | | 3 |"))
    }

    @Test
    fun `replace literal backslash-n with newline`() {
        val input = "line1\\nline2"
        assertEquals("line1\nline2", normalizeMarkdown(input))
    }
}
