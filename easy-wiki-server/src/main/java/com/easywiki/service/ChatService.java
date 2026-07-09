package com.easywiki.service;

import com.easywiki.dto.event.NotificationEvent;
import com.easywiki.dto.response.ChatMessageResponse;
import com.easywiki.dto.websocket.WsMessage;
import com.easywiki.entity.ChatMessage;
import com.easywiki.entity.GroupMember;
import com.easywiki.enums.NotificationEventType;
import com.easywiki.exception.BusinessException;
import com.easywiki.repository.ChatMessageRepository;
import com.easywiki.repository.GroupMemberRepository;
import com.easywiki.repository.UserRepository;
import com.easywiki.websocket.WsSessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_]{2,50})");

    private final ChatMessageRepository chatMessageRepository;
    private final GroupMembershipService membershipService;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WsSessionManager wsSessionManager;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public ChatService(ChatMessageRepository chatMessageRepository,
                       GroupMembershipService membershipService,
                       GroupMemberRepository memberRepository,
                       UserRepository userRepository,
                       WsSessionManager wsSessionManager,
                       NotificationService notificationService,
                       ObjectMapper objectMapper) {
        this.chatMessageRepository = chatMessageRepository;
        this.membershipService = membershipService;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.wsSessionManager = wsSessionManager;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChatMessage sendMessage(Long groupId, Long senderId, String content) {
        membershipService.requireMember(groupId, senderId);
        if (content == null || content.isBlank()) {
            throw new BusinessException(400, "消息内容不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(400, "消息长度不能超过 " + MAX_CONTENT_LENGTH + " 字符");
        }

        List<Long> mentionIds = parseMentions(groupId, content);

        ChatMessage message = new ChatMessage();
        message.setGroupId(groupId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setMentions(serializeMentions(mentionIds));
        message = chatMessageRepository.save(message);

        ChatMessageResponse response = toResponse(message);
        WsMessage wsMessage = new WsMessage("CHAT_MESSAGE", response);
        List<Long> memberIds = memberRepository.findByGroupId(groupId).stream()
                .map(GroupMember::getUserId)
                .toList();
        wsSessionManager.broadcastToGroupMembers(memberIds, wsMessage);

        notifyMentions(groupId, senderId, content, mentionIds);

        return message;
    }

    public Page<ChatMessageResponse> listMessages(Long groupId, Long userId, int page, int size) {
        membershipService.requireMember(groupId, userId);
        int pageSize = size > 0 ? size : DEFAULT_PAGE_SIZE;
        Pageable pageable = PageRequest.of(Math.max(page, 0), pageSize);
        return chatMessageRepository.findByGroupIdOrderBySentAtDesc(groupId, pageable)
                .map(this::toResponse);
    }

    List<Long> parseMentions(Long groupId, String content) {
        Map<String, Long> usernameToUserId = new HashMap<>();
        for (GroupMember member : memberRepository.findByGroupId(groupId)) {
            userRepository.findById(member.getUserId()).ifPresent(user ->
                    usernameToUserId.put(user.getUsername().toLowerCase(), user.getId()));
        }

        Set<Long> mentionIds = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String username = matcher.group(1).toLowerCase();
            Long mentionedUserId = usernameToUserId.get(username);
            if (mentionedUserId != null) {
                mentionIds.add(mentionedUserId);
            }
        }
        return new ArrayList<>(mentionIds);
    }

    private void notifyMentions(Long groupId, Long senderId, String content, List<Long> mentionIds) {
        if (mentionIds.isEmpty()) {
            return;
        }
        String senderName = userRepository.findById(senderId)
                .map(u -> u.getUsername())
                .orElse("成员");
        for (Long mentionedUserId : mentionIds) {
            if (mentionedUserId.equals(senderId)) {
                continue;
            }
            notificationService.publish(new NotificationEvent(
                    mentionedUserId,
                    groupId,
                    NotificationEventType.CHAT_MENTION,
                    "群聊 @ 提及",
                    senderName + " 在群聊中提到了你：" + truncate(content, 100),
                    null,
                    "/groups/" + groupId + "/chat"
            ));
        }
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setGroupId(message.getGroupId());
        response.setSenderId(message.getSenderId());
        userRepository.findById(message.getSenderId())
                .ifPresent(user -> response.setSenderUsername(user.getUsername()));
        response.setContent(message.getContent());
        response.setMentions(deserializeMentions(message.getMentions()));
        response.setSentAt(message.getSentAt());
        return response;
    }

    private String serializeMentions(List<Long> mentionIds) {
        try {
            return objectMapper.writeValueAsString(mentionIds);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<Long> deserializeMentions(String mentionsJson) {
        if (mentionsJson == null || mentionsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(mentionsJson, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }
}
