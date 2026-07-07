package com.easywiki.repository;

import com.easywiki.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByGroupIdOrderBySentAtDesc(Long groupId, Pageable pageable);
}
