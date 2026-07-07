package com.easywiki.dto.response;

import com.easywiki.entity.GroupJoinRequest;
import com.easywiki.enums.JoinRequestStatus;

import java.time.LocalDateTime;

public class JoinRequestResponse {

    private Long id;
    private Long groupId;
    private Long userId;
    private String reason;
    private JoinRequestStatus status;
    private LocalDateTime createdAt;

    public JoinRequestResponse() {
    }

    public static JoinRequestResponse from(GroupJoinRequest request) {
        JoinRequestResponse response = new JoinRequestResponse();
        response.setId(request.getId());
        response.setGroupId(request.getGroupId());
        response.setUserId(request.getUserId());
        response.setReason(request.getReason());
        response.setStatus(request.getStatus());
        response.setCreatedAt(request.getCreatedAt());
        return response;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public JoinRequestStatus getStatus() {
        return status;
    }

    public void setStatus(JoinRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
