package com.easywiki.service;

import com.easywiki.agent.DeepSeekClient;
import com.easywiki.agent.LlmClient;
import com.easywiki.agent.PromptBuilder;
import com.easywiki.config.AgentProperties;
import com.easywiki.dto.request.AgentChatRequest;
import com.easywiki.dto.request.AgentTaskCreateRequest;
import com.easywiki.dto.response.AgentChatResponse;
import com.easywiki.entity.Group;
import com.easywiki.entity.Task;
import com.easywiki.entity.WikiPage;
import com.easywiki.enums.AgentIntent;
import com.easywiki.exception.BusinessException;
import com.easywiki.repository.GroupRepository;
import com.easywiki.repository.TaskRepository;
import com.easywiki.repository.WikiPageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AgentService {

    private final AgentProperties agentProperties;
    private final LlmClient llmClient;
    private final GroupMembershipService membershipService;
    private final GroupRepository groupRepository;
    private final TaskRepository taskRepository;
    private final WikiPageRepository wikiPageRepository;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    public AgentService(AgentProperties agentProperties,
                        LlmClient llmClient,
                        GroupMembershipService membershipService,
                        GroupRepository groupRepository,
                        TaskRepository taskRepository,
                        WikiPageRepository wikiPageRepository,
                        TaskService taskService,
                        ObjectMapper objectMapper) {
        this.agentProperties = agentProperties;
        this.llmClient = llmClient;
        this.membershipService = membershipService;
        this.groupRepository = groupRepository;
        this.taskRepository = taskRepository;
        this.wikiPageRepository = wikiPageRepository;
        this.taskService = taskService;
        this.objectMapper = objectMapper;
    }

    public AgentChatResponse chat(Long groupId, Long userId, String message, List<AgentChatRequest.ChatTurn> history) {
        requireEnabled();
        membershipService.requireMember(groupId, userId);

        AgentIntent intent = detectIntent(message);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(404, "小组不存在"));

        List<Task> tasks = taskRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
        List<WikiPage> wikiPages = loadWikiContext(groupId, message, intent);

        String systemPrompt = PromptBuilder.buildSystemPrompt(group.getName(), intent, wikiPages, tasks);

        List<LlmClient.ChatMessage> messages = new ArrayList<>();
        if (history != null) {
            for (AgentChatRequest.ChatTurn turn : history) {
                if (turn.getRole() != null && turn.getContent() != null) {
                    messages.add(new LlmClient.ChatMessage(turn.getRole(), turn.getContent()));
                }
            }
        }
        messages.add(new LlmClient.ChatMessage("user", message));

        String reply = llmClient.chat(systemPrompt, messages);
        reply = unwrapJsonContent(reply);
        return new AgentChatResponse(reply, intent);
    }

    @Transactional
    public List<Task> createTasksFromSuggestions(Long groupId, Long userId, AgentTaskCreateRequest request) {
        requireEnabled();
        membershipService.requireMember(groupId, userId);

        List<Task> created = new ArrayList<>();
        for (AgentTaskCreateRequest.TaskSuggestion suggestion : request.getTasks()) {
            Task task = taskService.createTask(groupId, userId, suggestion.getTitle(),
                    suggestion.getDescription(), suggestion.getPriority(), null, null);
            created.add(task);
        }
        return created;
    }

    AgentIntent detectIntent(String message) {
        if (message == null || message.isBlank()) {
            return AgentIntent.GENERAL;
        }
        String lower = message.toLowerCase(Locale.ROOT);

        if (containsAny(lower, "整理任务", "任务列表", "任务状态", "看板", "task list", "organize task")) {
            return AgentIntent.TASK_ORGANIZE;
        }
        if (containsAny(lower, "建议任务", "创建任务", "拆分任务", "生成任务", "task suggest", "create task")) {
            return AgentIntent.TASK_SUGGEST;
        }
        if (containsAny(lower, "摘要", "总结", "概括", "文档内容", "wiki", "summarize", "summary")) {
            return AgentIntent.WIKI_SUMMARY;
        }
        return AgentIntent.GENERAL;
    }

    private List<WikiPage> loadWikiContext(Long groupId, String message, AgentIntent intent) {
        if (intent == AgentIntent.WIKI_SUMMARY || intent == AgentIntent.TASK_SUGGEST) {
            List<WikiPage> matched = wikiPageRepository.searchByGroupIdAndKeyword(groupId, extractKeyword(message));
            if (!matched.isEmpty()) {
                return matched.stream().limit(5).toList();
            }
            return wikiPageRepository.findByGroupIdOrderBySortOrderAsc(groupId).stream().limit(3).toList();
        }
        return List.of();
    }

    private String extractKeyword(String message) {
        String trimmed = message.trim();
        if (trimmed.length() > 20) {
            return trimmed.substring(0, 20);
        }
        return trimmed;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void requireEnabled() {
        if (!agentProperties.isEnabled()) {
            throw new BusinessException(503, "Agent 助手未启用，请联系管理员配置 DEEPSEEK_API_KEY");
        }
    }

    /**
     * 某些 LLM 会返回 JSON 包装格式，如 {"type":"markdown","content":"..."}，
     * 此方法尝试提取其中的 content 字段，还原为纯 Markdown 文本。
     */
    String unwrapJsonContent(String reply) {
        if (reply == null || reply.isBlank()) {
            return reply;
        }
        String trimmed = reply.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                JsonNode node = objectMapper.readTree(trimmed);
                if (node.has("content") && node.get("content").isTextual()) {
                    return node.get("content").asText();
                }
            } catch (Exception ignored) {
                // not valid JSON, return as-is
            }
        }
        return reply;
    }
}
