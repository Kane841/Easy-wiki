package com.easywiki.entity;

import com.easywiki.enums.AssignmentStatus;
import com.easywiki.enums.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_logs")
public class TaskLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(nullable = false, length = 50)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private TaskStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20)
    private TaskStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_assignment", length = 20)
    private AssignmentStatus fromAssignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_assignment", length = 20)
    private AssignmentStatus toAssignment;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TaskLog() {
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public TaskStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(TaskStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public TaskStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(TaskStatus toStatus) {
        this.toStatus = toStatus;
    }

    public AssignmentStatus getFromAssignment() {
        return fromAssignment;
    }

    public void setFromAssignment(AssignmentStatus fromAssignment) {
        this.fromAssignment = fromAssignment;
    }

    public AssignmentStatus getToAssignment() {
        return toAssignment;
    }

    public void setToAssignment(AssignmentStatus toAssignment) {
        this.toAssignment = toAssignment;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
