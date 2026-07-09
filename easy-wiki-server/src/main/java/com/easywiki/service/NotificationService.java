package com.easywiki.service;

import com.easywiki.dto.event.NotificationEvent;
import com.easywiki.entity.Notification;
import com.easywiki.entity.UserDevice;
import com.easywiki.exception.BusinessException;
import com.easywiki.repository.NotificationRepository;
import com.easywiki.repository.UserDeviceRepository;
import com.easywiki.websocket.WsSessionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final WsSessionManager wsSessionManager;
    private final FcmPushService fcmPushService;

    public NotificationService(NotificationRepository notificationRepository,
                               UserDeviceRepository userDeviceRepository,
                               WsSessionManager wsSessionManager,
                               FcmPushService fcmPushService) {
        this.notificationRepository = notificationRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.wsSessionManager = wsSessionManager;
        this.fcmPushService = fcmPushService;
    }

    @Transactional
    public Notification publish(NotificationEvent event) {
        Notification notification = new Notification();
        notification.setUserId(event.getUserId());
        notification.setGroupId(event.getGroupId());
        notification.setType(event.getType());
        notification.setTitle(event.getTitle());
        notification.setBody(event.getBody());
        notification.setData(event.getData());
        notification.setTargetUrl(event.getTargetUrl());
        notification.setRead(false);
        notification = notificationRepository.save(notification);

        wsSessionManager.pushNotification(notification, event.getTargetUrl());

        if (!wsSessionManager.hasActiveSession(event.getUserId())) {
            Map<String, String> fcmData = new HashMap<>();
            fcmData.put("notificationId", String.valueOf(notification.getId()));
            fcmData.put("eventType", event.getType().name());
            if (event.getTargetUrl() != null) {
                fcmData.put("targetUrl", event.getTargetUrl());
            }
            if (event.getGroupId() != null) {
                fcmData.put("groupId", String.valueOf(event.getGroupId()));
            }
            fcmPushService.send(event.getUserId(), event.getTitle(), event.getBody(), fcmData);
        }

        return notification;
    }

    public List<Notification> listForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Notification markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BusinessException(404, "通知不存在"));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public UserDevice registerDevice(Long userId, String fcmToken, String platform) {
        UserDevice device = userDeviceRepository.findByUserIdAndFcmToken(userId, fcmToken)
                .orElseGet(UserDevice::new);
        device.setUserId(userId);
        device.setFcmToken(fcmToken);
        device.setPlatform(platform);
        return userDeviceRepository.save(device);
    }
}
