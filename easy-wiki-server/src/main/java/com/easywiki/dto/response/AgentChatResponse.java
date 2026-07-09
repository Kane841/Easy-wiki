package com.easywiki.dto.response;

import com.easywiki.enums.AgentIntent;

public class AgentChatResponse {

    private final String reply;
    private final AgentIntent intent;

    public AgentChatResponse(String reply, AgentIntent intent) {
        this.reply = reply;
        this.intent = intent;
    }

    public String getReply() {
        return reply;
    }

    public AgentIntent getIntent() {
        return intent;
    }
}
