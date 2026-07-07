package com.easywiki.model

data class WikiTreeNode(
    val id: Long,
    val parentId: Long?,
    val title: String,
    val sortOrder: Int,
    val children: List<WikiTreeNode> = emptyList()
)

data class WikiPage(
    val id: Long,
    val parentId: Long?,
    val title: String,
    val content: String?,
    val sortOrder: Int,
    val version: Int?,
    val createdBy: Long?,
    val updatedAt: String?
)

data class CreateWikiPageRequest(
    val parentId: Long? = null,
    val title: String,
    val content: String? = null
)

data class UpdateWikiPageRequest(
    val title: String,
    val content: String?,
    val version: Int
)
