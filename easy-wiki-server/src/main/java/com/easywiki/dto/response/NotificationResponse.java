package com.easywiki.dto.response;

import com.easywiki.entity.Notification;
import com.easywiki.enums.NotificationEventType;

import java.time.LocalDateTime;

public class NotificationResponse {

    private Long id;
    private Long groupId;
    private NotificationEventType type;
    private String title;
    private String body;
    private String data;
    private String targetUrl;
    private boolean read;
    private LocalDateTime createdAt;

    public NotificationResponse() {
    }

    public static NotificationResponse from(Notification notification) {
        NotificationResponse resp = new NotificationResponse();
        resp.setId(notification.getId());
        resp.setGroupId(notification.getGroupId());
        resp.setType(notification.getType());
        resp.setTitle(notification.getTitle());
        resp.setBody(notification.getBody());
        resp.setData(notification.getData());
        resp.setTargetUrl(notification.getTargetUrl());
        resp.setRead(notification.isRead());
        resp.setCreatedAt(notification.getCreatedAt());
        return resp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public NotificationEventType getType() {
        return type;
    }

    public void setType(NotificationEventType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
