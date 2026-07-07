package com.easywiki.dto.request;

import jakarta.validation.constraints.Size;

public class ApplyJoinRequest {

    @Size(max = 500)
    private String reason;

    public ApplyJoinRequest() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
