package com.easywiki.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private FirebaseApp firebaseApp;

    @PostConstruct
    void init() {
        String credentialsPath = System.getenv("FCM_CREDENTIALS");
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.info("FCM_CREDENTIALS not set; FCM push disabled");
            return;
        }

        Path path = Path.of(credentialsPath);
        if (!Files.isRegularFile(path)) {
            log.warn("FCM credentials file not found at {}: FCM push disabled", credentialsPath);
            return;
        }

        try (InputStream stream = new FileInputStream(path.toFile())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .build();

            if (!FirebaseApp.getApps().isEmpty()) {
                firebaseApp = FirebaseApp.getInstance();
            } else {
                firebaseApp = FirebaseApp.initializeApp(options);
            }
            log.info("Firebase initialized from {}", credentialsPath);
        } catch (IOException e) {
            log.warn("Failed to initialize Firebase from {}: {}", credentialsPath, e.getMessage());
        }
    }

    public FirebaseApp getFirebaseApp() {
        return firebaseApp;
    }

    public boolean isEnabled() {
        return firebaseApp != null;
    }
}
