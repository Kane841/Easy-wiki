package com.easywiki.dto.response;

import com.easywiki.entity.GroupInvite;

import java.time.LocalDateTime;

public class GroupInviteResponse {

    private String token;
    private LocalDateTime expiresAt;

    public GroupInviteResponse() {
    }

    public static GroupInviteResponse from(GroupInvite invite) {
        GroupInviteResponse response = new GroupInviteResponse();
        response.setToken(invite.getToken());
        response.setExpiresAt(invite.getExpiresAt());
        return response;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
