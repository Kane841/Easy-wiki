package com.easywiki.service;

import com.easywiki.config.FirebaseConfig;
import com.easywiki.entity.UserDevice;
import com.easywiki.repository.UserDeviceRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FcmPushService {

    private static final Logger log = LoggerFactory.getLogger(FcmPushService.class);

    private final UserDeviceRepository userDeviceRepository;
    private final FirebaseConfig firebaseConfig;

    public FcmPushService(UserDeviceRepository userDeviceRepository, FirebaseConfig firebaseConfig) {
        this.userDeviceRepository = userDeviceRepository;
        this.firebaseConfig = firebaseConfig;
    }

    public void send(Long userId, String title, String body, Map<String, String> data) {
        if (!firebaseConfig.isEnabled()) {
            log.debug("FCM not configured; skip push for userId={}", userId);
            return;
        }

        List<UserDevice> devices = userDeviceRepository.findByUserId(userId);
        if (devices.isEmpty()) {
            log.debug("No FCM tokens for userId={}", userId);
            return;
        }

        FirebaseMessaging messaging = FirebaseMessaging.getInstance(firebaseConfig.getFirebaseApp());
        for (UserDevice device : devices) {
            try {
                Message.Builder builder = Message.builder()
                        .setToken(device.getFcmToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build());
                if (data != null) {
                    builder.putAllData(data);
                }
                messaging.send(builder.build());
                log.debug("FCM sent to userId={}, platform={}", userId, device.getPlatform());
            } catch (FirebaseMessagingException e) {
                log.warn("FCM send failed for userId={}, token={}: {}",
                        userId, device.getFcmToken(), e.getMessage());
            }
        }
    }
}
