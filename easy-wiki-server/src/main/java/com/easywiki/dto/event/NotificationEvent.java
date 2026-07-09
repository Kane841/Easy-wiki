package com.easywiki.dto.event;

import com.easywiki.enums.NotificationEventType;

public class NotificationEvent {

    private final Long userId;
    private final Long groupId;
    private final NotificationEventType type;
    private final String title;
    private final String body;
    private final String data;
    private final String targetUrl;

    public NotificationEvent(Long userId, Long groupId, NotificationEventType type,
                             String title, String body, String data, String targetUrl) {
        this.userId = userId;
        this.groupId = groupId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.data = data;
        this.targetUrl = targetUrl;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public NotificationEventType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getData() {
        return data;
    }

    public String getTargetUrl() {
        return targetUrl;
    }
}
