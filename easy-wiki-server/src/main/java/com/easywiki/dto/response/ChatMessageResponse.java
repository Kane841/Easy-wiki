package com.easywiki.dto.response;

import com.easywiki.entity.ChatMessage;

import java.time.LocalDateTime;
import java.util.List;

public class ChatMessageResponse {

    private Long id;
    private Long groupId;
    private Long senderId;
    private String senderUsername;
    private String content;
    private List<Long> mentions;
    private LocalDateTime sentAt;

    public ChatMessageResponse() {
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

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<Long> getMentions() {
        return mentions;
    }

    public void setMentions(List<Long> mentions) {
        this.mentions = mentions;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
