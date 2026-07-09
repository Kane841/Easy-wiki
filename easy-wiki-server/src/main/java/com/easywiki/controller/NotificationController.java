package com.easywiki.controller;

import com.easywiki.dto.common.ApiResponse;
import com.easywiki.dto.response.NotificationResponse;
import com.easywiki.security.UserPrincipal;
import com.easywiki.service.NotificationService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> listNotifications() {
        Long userId = currentUserId();
        List<NotificationResponse> notifications = notificationService.listForUser(userId).stream()
                .map(NotificationResponse::from)
                .toList();
        return ApiResponse.ok(notifications);
    }

    @PutMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markAsRead(@PathVariable Long id) {
        Long userId = currentUserId();
        var notification = notificationService.markAsRead(userId, id);
        return ApiResponse.ok(NotificationResponse.from(notification));
    }

    private Long currentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.userId();
    }
}
