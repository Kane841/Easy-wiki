package com.easywiki.service;

import com.easywiki.dto.request.RegisterRequest;
import com.easywiki.entity.ChatMessage;
import com.easywiki.enums.NotificationEventType;
import com.easywiki.repository.ChatMessageRepository;
import com.easywiki.repository.NotificationRepository;
import com.easywiki.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ChatServiceTest {

    @Autowired ChatService chatService;
    @Autowired ChatMessageRepository chatMessageRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired GroupService groupService;
    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;

    Long groupId;
    Long senderId;
    Long mentionedUserId;

    @BeforeEach
    void setup() {
        authService.register(new RegisterRequest("chatuser", "chat@test.com", "pass12345"));
        authService.register(new RegisterRequest("mentioned", "mentioned@test.com", "pass12345"));
        senderId = userRepository.findByUsername("chatuser").orElseThrow().getId();
        mentionedUserId = userRepository.findByUsername("mentioned").orElseThrow().getId();

        groupId = groupService.createGroup(senderId, "聊天组", "desc", true).getId();
        groupService.joinByInvite(groupService.createInvite(groupId, senderId).getToken(), mentionedUserId);
    }

    @Test
    void sendMessagePersistsContent() {
        ChatMessage message = chatService.sendMessage(groupId, senderId, "大家好");

        assertThat(message.getId()).isNotNull();
        ChatMessage saved = chatMessageRepository.findById(message.getId()).orElseThrow();
        assertThat(saved.getContent()).isEqualTo("大家好");
        assertThat(saved.getGroupId()).isEqualTo(groupId);
        assertThat(saved.getSenderId()).isEqualTo(senderId);
    }

    @Test
    void sendMessageParsesMentions() {
        ChatMessage message = chatService.sendMessage(groupId, senderId, "你好 @mentioned 请看一下");

        assertThat(chatService.parseMentions(groupId, message.getContent()))
                .containsExactly(mentionedUserId);

        var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(mentionedUserId);
        assertThat(notifications).isNotEmpty();
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationEventType.CHAT_MENTION);
    }

    @Test
    void sendMessageRejectsOverlongContent() {
        String longContent = "a".repeat(2001);
        assertThatThrownBy(() -> chatService.sendMessage(groupId, senderId, longContent))
                .isInstanceOf(com.easywiki.exception.BusinessException.class)
                .hasMessageContaining("2000");
    }

    @Test
    void listMessagesReturnsPagedResults() {
        chatService.sendMessage(groupId, senderId, "消息1");
        chatService.sendMessage(groupId, senderId, "消息2");

        var page = chatService.listMessages(groupId, senderId, 0, 50);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getContent()).isEqualTo("消息2");
    }
}
