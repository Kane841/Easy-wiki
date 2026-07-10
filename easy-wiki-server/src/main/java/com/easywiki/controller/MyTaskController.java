package com.easywiki.controller;

import com.easywiki.dto.common.ApiResponse;
import com.easywiki.dto.response.TaskResponse;
import com.easywiki.entity.Group;
import com.easywiki.entity.Task;
import com.easywiki.enums.TaskStatus;
import com.easywiki.repository.GroupRepository;
import com.easywiki.security.UserPrincipal;
import com.easywiki.service.TaskService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tasks")
public class MyTaskController {

    private final TaskService taskService;
    private final GroupRepository groupRepository;

    public MyTaskController(TaskService taskService, GroupRepository groupRepository) {
        this.taskService = taskService;
        this.groupRepository = groupRepository;
    }

    @GetMapping("/my")
    public ApiResponse<List<TaskResponse>> listMyTasks(@RequestParam(required = false) TaskStatus status) {
        Long userId = currentUserId();
        List<Task> tasks = taskService.listMyTasks(userId, status);

        // Batch load group names
        List<Long> groupIds = tasks.stream().map(Task::getGroupId).distinct().toList();
        Map<Long, String> groupNameMap = groupRepository.findAllById(groupIds).stream()
                .collect(Collectors.toMap(Group::getId, Group::getName));

        List<TaskResponse> responses = tasks.stream()
                .map(task -> {
                    TaskResponse resp = TaskResponse.from(task);
                    resp.setGroupName(groupNameMap.getOrDefault(task.getGroupId(), ""));
                    return resp;
                })
                .toList();
        return ApiResponse.ok(responses);
    }

    private Long currentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.userId();
    }
}
