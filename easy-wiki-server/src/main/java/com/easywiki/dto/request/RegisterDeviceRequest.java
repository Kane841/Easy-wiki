package com.easywiki.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterDeviceRequest {

    @NotBlank
    @Size(max = 512)
    private String fcmToken;

    @NotBlank
    @Size(max = 20)
    private String platform;

    public RegisterDeviceRequest() {
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
