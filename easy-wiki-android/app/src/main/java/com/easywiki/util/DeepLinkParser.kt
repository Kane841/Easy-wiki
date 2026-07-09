package com.easywiki.util

sealed class DeepLinkDestination {
    data class Workspace(val groupId: Long, val tab: WorkspaceTab? = null) : DeepLinkDestination()
    data class WikiPage(val groupId: Long, val pageId: Long) : DeepLinkDestination()
    data class Task(val groupId: Long, val taskId: Long) : DeepLinkDestination()
    data class Chat(val groupId: Long) : DeepLinkDestination()
}

object DeepLinkParser {

    private val wikiPattern = Regex("""/groups/(\d+)/wiki/pages/(\d+)""")
    private val taskPattern = Regex("""/groups/(\d+)/tasks/(\d+)""")
    private val chatPattern = Regex("""/groups/(\d+)/chat""")
    private val groupPattern = Regex("""/groups/(\d+)""")

    fun parse(targetUrl: String?): DeepLinkDestination? {
        if (targetUrl.isNullOrBlank()) return null
        wikiPattern.find(targetUrl)?.let { match ->
            return DeepLinkDestination.WikiPage(
                groupId = match.groupValues[1].toLong(),
                pageId = match.groupValues[2].toLong()
            )
        }
        taskPattern.find(targetUrl)?.let { match ->
            return DeepLinkDestination.Task(
                groupId = match.groupValues[1].toLong(),
                taskId = match.groupValues[2].toLong()
            )
        }
        chatPattern.find(targetUrl)?.let { match ->
            return DeepLinkDestination.Chat(groupId = match.groupValues[1].toLong())
        }
        groupPattern.find(targetUrl)?.let { match ->
            return DeepLinkDestination.Workspace(groupId = match.groupValues[1].toLong())
        }
        return null
    }
}
