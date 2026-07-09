package com.easywiki.service;

import com.easywiki.dto.request.RegisterRequest;
import com.easywiki.entity.Task;
import com.easywiki.enums.AssignmentStatus;
import com.easywiki.enums.TaskStatus;
import com.easywiki.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TaskServiceTest {

    @Autowired TaskService taskService;
    @Autowired GroupService groupService;
    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;

    Long groupId;
    Long creatorId;
    Long assigneeId;
    Long otherMemberId;

    @BeforeEach
    void setup() {
        authService.register(new RegisterRequest("creator", "creator@test.com", "pass12345"));
        authService.register(new RegisterRequest("assignee", "assignee@test.com", "pass12345"));
        authService.register(new RegisterRequest("member2", "member2@test.com", "pass12345"));

        creatorId = userRepository.findByUsername("creator").orElseThrow().getId();
        assigneeId = userRepository.findByUsername("assignee").orElseThrow().getId();
        otherMemberId = userRepository.findByUsername("member2").orElseThrow().getId();

        groupId = groupService.createGroup(creatorId, "任务组", "desc", true).getId();
        groupService.joinByInvite(groupService.createInvite(groupId, creatorId).getToken(), assigneeId);
        groupService.joinByInvite(groupService.createInvite(groupId, creatorId).getToken(), otherMemberId);
    }

    @Test
    void createWithoutAssigneeIsUnassigned() {
        Task task = taskService.createTask(groupId, creatorId, "任务A", "desc", null, null, null);
        assertThat(task.getAssignmentStatus()).isEqualTo(AssignmentStatus.UNASSIGNED);
        assertThat(task.getAssigneeId()).isNull();
    }

    @Test
    void createWithAssigneeIsPendingAccept() {
        Task task = taskService.createTask(groupId, creatorId, "任务B", "desc", null, assigneeId, null);
        assertThat(task.getAssignmentStatus()).isEqualTo(AssignmentStatus.PENDING_ACCEPT);
        assertThat(task.getAssigneeId()).isEqualTo(assigneeId);
    }

    @Test
    void assignSetsPendingAccept() {
        Task task = taskService.createTask(groupId, creatorId, "任务C", "desc", null, null, null);
        Task assigned = taskService.assign(groupId, creatorId, task.getId(), assigneeId);
        assertThat(assigned.getAssignmentStatus()).isEqualTo(AssignmentStatus.PENDING_ACCEPT);
        assertThat(assigned.getAssigneeId()).isEqualTo(assigneeId);
    }

    @Test
    void acceptSetsAccepted() {
        Task task = taskService.createTask(groupId, creatorId, "任务D", "desc", null, assigneeId, null);
        Task accepted = taskService.accept(groupId, assigneeId, task.getId());
        assertThat(accepted.getAssignmentStatus()).isEqualTo(AssignmentStatus.ACCEPTED);
    }

    @Test
    void rejectReturnsToUnassigned() {
        Task task = taskService.createTask(groupId, creatorId, "任务E", "desc", null, assigneeId, null);
        Task rejected = taskService.reject(groupId, assigneeId, task.getId());
        assertThat(rejected.getAssignmentStatus()).isEqualTo(AssignmentStatus.UNASSIGNED);
        assertThat(rejected.getAssigneeId()).isNull();
    }

    @Test
    void claimUnassignedTaskSetsAccepted() {
        Task task = taskService.createTask(groupId, creatorId, "任务F", "desc", null, null, null);
        Task claimed = taskService.claim(groupId, assigneeId, task.getId());
        assertThat(claimed.getAssignmentStatus()).isEqualTo(AssignmentStatus.ACCEPTED);
        assertThat(claimed.getAssigneeId()).isEqualTo(assigneeId);
    }

    @Test
    void transferSetsNewAssigneePendingAccept() {
        Task task = taskService.createTask(groupId, creatorId, "任务G", "desc", null, assigneeId, null);
        taskService.accept(groupId, assigneeId, task.getId());
        Task transferred = taskService.transfer(groupId, assigneeId, task.getId(), otherMemberId);
        assertThat(transferred.getAssignmentStatus()).isEqualTo(AssignmentStatus.PENDING_ACCEPT);
        assertThat(transferred.getAssigneeId()).isEqualTo(otherMemberId);
    }

    @Test
    void updateStatusChangesTaskStatus() {
        Task task = taskService.createTask(groupId, creatorId, "任务H", "desc", null, null, null);
        Task updated = taskService.updateStatus(groupId, creatorId, task.getId(), TaskStatus.IN_PROGRESS);
        assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void acceptByNonAssigneeThrows() {
        Task task = taskService.createTask(groupId, creatorId, "任务I", "desc", null, assigneeId, null);
        assertThatThrownBy(() -> taskService.accept(groupId, otherMemberId, task.getId()))
                .isInstanceOf(com.easywiki.exception.BusinessException.class);
    }
}
