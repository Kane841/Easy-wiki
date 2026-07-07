package com.easywiki.service;

import com.easywiki.dto.request.RegisterRequest;
import com.easywiki.entity.User;
import com.easywiki.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {
    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;

    @Test
    void registerCreatesUserWithHashedPassword() {
        RegisterRequest req = new RegisterRequest("alice", "alice@test.com", "password123");
        authService.register(req);
        User user = userRepository.findByUsername("alice").orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo("password123");
        assertThat(user.getPasswordHash()).startsWith("$2a$");
    }
}
