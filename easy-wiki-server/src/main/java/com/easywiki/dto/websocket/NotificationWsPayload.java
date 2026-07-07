package com.easywiki.dto.websocket;

import com.easywiki.enums.NotificationEventType;

public class NotificationWsPayload {

    private Long id;
    private NotificationEventType eventType;
    private String content;
    private String targetUrl;
    private Long groupId;

    public NotificationWsPayload() {
    }

    public NotificationWsPayload(Long id, NotificationEventType eventType, String content,
                                 String targetUrl, Long groupId) {
        this.id = id;
        this.eventType = eventType;
        this.content = content;
        this.targetUrl = targetUrl;
        this.groupId = groupId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public NotificationEventType getEventType() {
        return eventType;
    }

    public void setEventType(NotificationEventType eventType) {
        this.eventType = eventType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
}
