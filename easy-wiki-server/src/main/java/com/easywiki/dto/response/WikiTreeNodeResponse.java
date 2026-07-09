package com.easywiki.dto.response;

import com.easywiki.entity.WikiPage;

import java.util.ArrayList;
import java.util.List;

public class WikiTreeNodeResponse {

    private Long id;
    private Long parentId;
    private String title;
    private int sortOrder;
    private List<WikiTreeNodeResponse> children = new ArrayList<>();

    public WikiTreeNodeResponse() {
    }

    public static WikiTreeNodeResponse from(WikiPage page) {
        WikiTreeNodeResponse node = new WikiTreeNodeResponse();
        node.setId(page.getId());
        node.setParentId(page.getParentId());
        node.setTitle(page.getTitle());
        node.setSortOrder(page.getSortOrder());
        return node;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<WikiTreeNodeResponse> getChildren() {
        return children;
    }

    public void setChildren(List<WikiTreeNodeResponse> children) {
        this.children = children;
    }
}
