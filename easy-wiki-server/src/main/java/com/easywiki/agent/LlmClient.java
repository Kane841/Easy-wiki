package com.easywiki.agent;

import java.util.List;

public interface LlmClient {

    String chat(String systemPrompt, List<ChatMessage> messages);

    record ChatMessage(String role, String content) {
    }
}
