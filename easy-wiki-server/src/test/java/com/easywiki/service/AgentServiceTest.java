package com.easywiki.service;

import com.easywiki.agent.LlmClient;
import com.easywiki.dto.request.RegisterRequest;
import com.easywiki.enums.AgentIntent;
import com.easywiki.exception.BusinessException;
import com.easywiki.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AgentServiceTest {

    @Autowired AgentService agentService;

    @Test
    void detectIntentRecognizesTaskOrganize() {
        assertThat(agentService.detectIntent("帮我整理任务列表")).isEqualTo(AgentIntent.TASK_ORGANIZE);
    }

    @Test
    void detectIntentRecognizesWikiSummary() {
        assertThat(agentService.detectIntent("请摘要这篇文档")).isEqualTo(AgentIntent.WIKI_SUMMARY);
    }

    @Test
    void chatWhenDisabledThrows503() {
        assertThatThrownBy(() -> agentService.chat(1L, 1L, "hello", List.of()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(503);
    }

    @SpringBootTest
    @ActiveProfiles("test")
    @Transactional
    @Import(EnabledAgentTestConfig.class)
    @TestPropertySource(properties = "easywiki.agent.enabled=true")
    static class EnabledAgentTests {

        @Autowired AgentService agentService;
        @Autowired AuthService authService;
        @Autowired UserRepository userRepository;
        @Autowired GroupService groupService;
        @Autowired StubLlmClient stubLlmClient;

        Long groupId;
        Long userId;

        @BeforeEach
        void setup() {
            authService.register(new RegisterRequest("agent_user", "agent@test.com", "pass12345"));
            userId = userRepository.findByUsername("agent_user").orElseThrow().getId();
            groupId = groupService.createGroup(userId, "Agent组", "desc", true).getId();
            stubLlmClient.reply = "## 任务概览\n暂无任务";
            stubLlmClient.called.set(false);
        }

        @Test
        void chatWhenEnabledCallsLlmClient() {
            var response = agentService.chat(groupId, userId, "整理任务", List.of());

            assertThat(response.getReply()).contains("任务概览");
            assertThat(response.getIntent()).isEqualTo(AgentIntent.TASK_ORGANIZE);
            assertThat(stubLlmClient.called.get()).isTrue();
        }
    }

    @TestConfiguration
    static class EnabledAgentTestConfig {
        @Bean
        @Primary
        StubLlmClient stubLlmClient() {
            return new StubLlmClient();
        }
    }

    static class StubLlmClient implements LlmClient {
        final AtomicBoolean called = new AtomicBoolean(false);
        String reply = "";

        @Override
        public String chat(String systemPrompt, List<ChatMessage> messages) {
            called.set(true);
            return reply;
        }
    }
}
