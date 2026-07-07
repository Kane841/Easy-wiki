package com.easywiki.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FcmPushService {

    private static final Logger log = LoggerFactory.getLogger(FcmPushService.class);

    public void send(Long userId, String title, String body, Map<String, String> data) {
        log.debug("FCM stub: userId={}, title={}", userId, title);
    }
}
