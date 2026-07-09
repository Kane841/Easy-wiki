package com.easywiki.service;

import com.easywiki.dto.event.NotificationEvent;
import com.easywiki.dto.request.RegisterRequest;
import com.easywiki.enums.NotificationEventType;
import com.easywiki.repository.NotificationRepository;
import com.easywiki.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest {

    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notificationRepository;
    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired GroupService groupService;

    Long userId;
    Long groupId;

    @BeforeEach
    void setup() {
        authService.register(new RegisterRequest("notify_user", "notify@test.com", "pass12345"));
        userId = userRepository.findByUsername("notify_user").orElseThrow().getId();
        groupId = groupService.createGroup(userId, "通知组", "desc", true).getId();
    }

    @Test
    void publishCreatesDatabaseRecord() {
        NotificationEvent event = new NotificationEvent(
                userId, groupId, NotificationEventType.TASK_ASSIGNED,
                "新任务", "你有一个新任务", null, "/tasks/1");

        var notification = notificationService.publish(event);

        assertThat(notification.getId()).isNotNull();
        assertThat(notificationRepository.findById(notification.getId())).isPresent();
        assertThat(notification.getUserId()).isEqualTo(userId);
        assertThat(notification.getType()).isEqualTo(NotificationEventType.TASK_ASSIGNED);
        assertThat(notification.isRead()).isFalse();
    }

    @Test
    void markAsReadUpdatesRecord() {
        var notification = notificationService.publish(new NotificationEvent(
                userId, groupId, NotificationEventType.WIKI_UPDATED,
                "Wiki 更新", "文档已更新", null, "/wiki/1"));

        var updated = notificationService.markAsRead(userId, notification.getId());

        assertThat(updated.isRead()).isTrue();
    }

    @Test
    void registerDevicePersistsToken() {
        var device = notificationService.registerDevice(userId, "fcm-token-abc", "android");

        assertThat(device.getId()).isNotNull();
        assertThat(device.getFcmToken()).isEqualTo("fcm-token-abc");
        assertThat(device.getPlatform()).isEqualTo("android");
    }
}
