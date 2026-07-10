package com.easywiki.dto.response;

import com.easywiki.entity.Task;
import com.easywiki.enums.AssignmentStatus;
import com.easywiki.enums.TaskPriority;
import com.easywiki.enums.TaskStatus;

import java.time.LocalDateTime;

public class TaskResponse {

    private Long id;
    private Long groupId;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Long assigneeId;
    private AssignmentStatus assignmentStatus;
    private Long creatorId;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;
    private String groupName;

    public TaskResponse() {
    }

    public static TaskResponse from(Task task) {
        TaskResponse resp = new TaskResponse();
        resp.setId(task.getId());
        resp.setGroupId(task.getGroupId());
        resp.setTitle(task.getTitle());
        resp.setDescription(task.getDescription());
        resp.setStatus(task.getStatus());
        resp.setPriority(task.getPriority());
        resp.setAssigneeId(task.getAssigneeId());
        resp.setAssignmentStatus(task.getAssignmentStatus());
        resp.setCreatorId(task.getCreatorId());
        resp.setDueDate(task.getDueDate());
        resp.setCreatedAt(task.getCreatedAt());
        return resp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public AssignmentStatus getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(AssignmentStatus assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
