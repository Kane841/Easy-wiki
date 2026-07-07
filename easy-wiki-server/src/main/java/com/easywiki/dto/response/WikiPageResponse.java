package com.easywiki.dto.response;

import com.easywiki.entity.WikiPage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WikiPageResponse {

    private Long id;
    private Long parentId;
    private String title;
    private String content;
    private int sortOrder;
    private Integer version;
    private Long createdBy;
    private LocalDateTime updatedAt;

    public WikiPageResponse() {
    }

    public static WikiPageResponse from(WikiPage page) {
        WikiPageResponse resp = new WikiPageResponse();
        resp.setId(page.getId());
        resp.setParentId(page.getParentId());
        resp.setTitle(page.getTitle());
        resp.setContent(page.getContent());
        resp.setSortOrder(page.getSortOrder());
        resp.setVersion(page.getVersion());
        resp.setCreatedBy(page.getCreatedBy());
        resp.setUpdatedAt(page.getUpdatedAt());
        return resp;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
