package com.easywiki.controller;

import com.easywiki.dto.common.ApiResponse;
import com.easywiki.dto.request.RegisterDeviceRequest;
import com.easywiki.security.UserPrincipal;
import com.easywiki.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    private final NotificationService notificationService;

    public DeviceController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ApiResponse<Void> registerDevice(@Valid @RequestBody RegisterDeviceRequest req) {
        Long userId = currentUserId();
        notificationService.registerDevice(userId, req.getFcmToken(), req.getPlatform());
        return ApiResponse.ok(null);
    }

    private Long currentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.userId();
    }
}
