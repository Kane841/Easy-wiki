package com.easywiki.ui.navigation

object Routes {
    const val SERVER_CONFIG = "server_config"
    const val LOGIN = "login"
    const val GROUP_LIST = "group_list"
    const val WORKSPACE = "workspace/{groupId}"
    const val WIKI_DETAIL = "workspace/{groupId}/wiki/{pageId}"
    const val TASK_DETAIL = "workspace/{groupId}/tasks/{taskId}"
    const val PROFILE = "profile"
    const val MY_TASKS = "my_tasks"

    fun workspace(groupId: Long) = "workspace/$groupId"
    fun wikiDetail(groupId: Long, pageId: Long) = "workspace/$groupId/wiki/$pageId"
    fun taskDetail(groupId: Long, taskId: Long) = "workspace/$groupId/tasks/$taskId"
}
