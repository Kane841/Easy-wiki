package com.easywiki.controller;

import com.easywiki.dto.common.ApiResponse;
import com.easywiki.dto.request.AgentChatRequest;
import com.easywiki.dto.request.AgentTaskCreateRequest;
import com.easywiki.dto.response.AgentChatResponse;
import com.easywiki.dto.response.TaskResponse;
import com.easywiki.security.UserPrincipal;
import com.easywiki.service.AgentService;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ApiResponse<AgentChatResponse> chat(@PathVariable Long groupId,
                                               @Valid @RequestBody AgentChatRequest req) {
        Long userId = currentUserId();
        AgentChatResponse response = agentService.chat(groupId, userId, req.getMessage(), req.getHistory());
        return ApiResponse.ok(response);
    }

    @PostMapping("/tasks/create")
    public ApiResponse<List<TaskResponse>> createTasks(@PathVariable Long groupId,
                                                       @Valid @RequestBody AgentTaskCreateRequest req) {
        Long userId = currentUserId();
        List<TaskResponse> tasks = agentService.createTasksFromSuggestions(groupId, userId, req).stream()
                .map(TaskResponse::from)
                .toList();
        return ApiResponse.ok(tasks);
    }

    private Long currentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.userId();
    }
}
