package com.easywiki.dto.websocket;

import java.util.Map;

public class ChatMessageWsPayload {

    private Long groupId;
    private String content;

    public ChatMessageWsPayload() {
    }

    public ChatMessageWsPayload(Long groupId, String content) {
        this.groupId = groupId;
        this.content = content;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @SuppressWarnings("unchecked")
    public static ChatMessageWsPayload fromMap(Object payload) {
        if (payload instanceof ChatMessageWsPayload typed) {
            return typed;
        }
        if (payload instanceof Map<?, ?> map) {
            ChatMessageWsPayload result = new ChatMessageWsPayload();
            Object groupId = map.get("groupId");
            if (groupId instanceof Number number) {
                result.setGroupId(number.longValue());
            }
            Object content = map.get("content");
            if (content != null) {
                result.setContent(content.toString());
            }
            return result;
        }
        throw new IllegalArgumentException("Invalid CHAT_MESSAGE payload");
    }
}
