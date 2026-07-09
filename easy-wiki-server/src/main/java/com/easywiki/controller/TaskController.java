package com.easywiki.controller;

import com.easywiki.dto.common.ApiResponse;
import com.easywiki.dto.request.AssignTaskRequest;
import com.easywiki.dto.request.CreateTaskRequest;
import com.easywiki.dto.request.UpdateTaskRequest;
import com.easywiki.dto.response.TaskResponse;
import com.easywiki.enums.TaskStatus;
import com.easywiki.security.UserPrincipal;
import com.easywiki.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public ApiResponse<List<TaskResponse>> listTasks(@PathVariable Long groupId,
                                                     @RequestParam(required = false) TaskStatus status) {
        Long userId = currentUserId();
        List<TaskResponse> tasks = taskService.listByGroup(groupId, userId, status).stream()
                .map(TaskResponse::from)
                .toList();
        return ApiResponse.ok(tasks);
    }

    @PostMapping
    public ApiResponse<TaskResponse> createTask(@PathVariable Long groupId,
                                                @Valid @RequestBody CreateTaskRequest req) {
        Long userId = currentUserId();
        var task = taskService.createTask(groupId, userId, req.getTitle(), req.getDescription(),
                req.getPriority(), req.getAssigneeId(), req.getDueDate());
        return ApiResponse.ok(TaskResponse.from(task));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskResponse> getTask(@PathVariable Long groupId, @PathVariable Long taskId) {
        Long userId = currentUserId();
        return ApiResponse.ok(TaskResponse.from(taskService.getTask(groupId, userId, taskId)));
    }

    @PutMapping("/{taskId}")
    public ApiResponse<TaskResponse> updateTask(@PathVariable Long groupId,
                                                @PathVariable Long taskId,
                                                @Valid @RequestBody UpdateTaskRequest req) {
        Long userId = currentUserId();
        var task = taskService.updateTask(groupId, userId, taskId, req.getTitle(), req.getDescription(),
                req.getPriority(), req.getDueDate());
        if (req.getStatus() != null) {
            task = taskService.updateStatus(groupId, userId, taskId, req.getStatus());
        }
        return ApiResponse.ok(TaskResponse.from(task));
    }

    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable Long groupId, @PathVariable Long taskId) {
        taskService.deleteTask(groupId, currentUserId(), taskId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{taskId}/assign")
    public ApiResponse<TaskResponse> assign(@PathVariable Long groupId,
                                            @PathVariable Long taskId,
                                            @Valid @RequestBody AssignTaskRequest req) {
        Long userId = currentUserId();
        var task = taskService.assign(groupId, userId, taskId, req.getAssigneeId());
        return ApiResponse.ok(TaskResponse.from(task));
    }

    @PostMapping("/{taskId}/accept")
    public ApiResponse<TaskResponse> accept(@PathVariable Long groupId, @PathVariable Long taskId) {
        Long userId = currentUserId();
        var task = taskService.accept(groupId, userId, taskId);
        return ApiResponse.ok(TaskResponse.from(task));
    }

    @PostMapping("/{taskId}/reject")
    public ApiResponse<TaskResponse> reject(@PathVariable Long groupId, @PathVariable Long taskId) {
        Long userId = currentUserId();
        var task = taskService.reject(groupId, userId, taskId);
        return ApiResponse.ok(TaskResponse.from(task));
    }

    @PostMapping("/{taskId}/claim")
    public ApiResponse<TaskResponse> claim(@PathVariable Long groupId, @PathVariable Long taskId) {
        Long userId = currentUserId();
        var task = taskService.claim(groupId, userId, taskId);
        return ApiResponse.ok(TaskResponse.from(task));
    }

    @PostMapping("/{taskId}/transfer")
    public ApiResponse<TaskResponse> transfer(@PathVariable Long groupId,
                                              @PathVariable Long taskId,
                                              @Valid @RequestBody AssignTaskRequest req) {
        Long userId = currentUserId();
        var task = taskService.transfer(groupId, userId, taskId, req.getAssigneeId());
        return ApiResponse.ok(TaskResponse.from(task));
    }

    @PostMapping("/{taskId}/give-up")
    public ApiResponse<TaskResponse> giveUp(@PathVariable Long groupId,
                                            @PathVariable Long taskId) {
        Long userId = currentUserId();
        var task = taskService.giveUp(groupId, userId, taskId);
        return ApiResponse.ok(TaskResponse.from(task));
    }

    private Long currentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        return principal.userId();
    }
}
