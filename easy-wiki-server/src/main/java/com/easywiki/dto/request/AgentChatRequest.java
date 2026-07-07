package com.easywiki.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class AgentChatRequest {

    @NotBlank
    @Size(max = 4000)
    private String message;

    private List<ChatTurn> history = new ArrayList<>();

    public AgentChatRequest() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ChatTurn> getHistory() {
        return history;
    }

    public void setHistory(List<ChatTurn> history) {
        this.history = history != null ? history : new ArrayList<>();
    }

    public static class ChatTurn {
        private String role;
        private String content;

        public ChatTurn() {
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
