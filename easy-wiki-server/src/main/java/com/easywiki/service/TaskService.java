package com.easywiki.service;

import com.easywiki.dto.event.NotificationEvent;
import com.easywiki.entity.Task;
import com.easywiki.entity.TaskLog;
import com.easywiki.enums.AssignmentStatus;
import com.easywiki.enums.NotificationEventType;
import com.easywiki.enums.TaskPriority;
import com.easywiki.enums.TaskStatus;
import com.easywiki.exception.BusinessException;
import com.easywiki.repository.GroupMemberRepository;
import com.easywiki.repository.TaskLogRepository;
import com.easywiki.repository.TaskRepository;
import com.easywiki.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskLogRepository taskLogRepository;
    private final GroupMembershipService membershipService;
    private final GroupMemberRepository memberRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository,
                       TaskLogRepository taskLogRepository,
                       GroupMembershipService membershipService,
                       GroupMemberRepository memberRepository,
                       NotificationService notificationService,
                       UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.taskLogRepository = taskLogRepository;
        this.membershipService = membershipService;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @Transactional
    public Task createTask(Long groupId, Long creatorId, String title, String description,
                           TaskPriority priority, Long assigneeId, LocalDateTime dueDate) {
        membershipService.requireMember(groupId, creatorId);

        Task task = new Task();
        task.setGroupId(groupId);
        task.setTitle(title);
        task.setDescription(description != null ? description : "");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(priority != null ? priority : TaskPriority.MEDIUM);
        task.setCreatorId(creatorId);
        task.setDueDate(dueDate);

        if (assigneeId != null) {
            requireGroupMember(groupId, assigneeId);
            task.setAssigneeId(assigneeId);
            task.setAssignmentStatus(AssignmentStatus.PENDING_ACCEPT);
        } else {
            task.setAssignmentStatus(AssignmentStatus.UNASSIGNED);
        }

        task = taskRepository.save(task);
        logAssignmentChange(task, creatorId, "CREATE", null, task.getAssignmentStatus());
        if (assigneeId != null) {
            notifyTaskAssigned(task, creatorId);
        }
        return task;
    }

    public List<Task> listByGroup(Long groupId, Long userId, TaskStatus status) {
        membershipService.requireMember(groupId, userId);
        if (status != null) {
            return taskRepository.findByGroupIdAndStatusOrderByCreatedAtDesc(groupId, status);
        }
        return taskRepository.findByGroupIdOrderByCreatedAtDesc(groupId);
    }

    public Task getTask(Long groupId, Long userId, Long taskId) {
        membershipService.requireMember(groupId, userId);
        return findTaskInGroup(groupId, taskId);
    }

    @Transactional
    public Task updateTask(Long groupId, Long userId, Long taskId, String title, String description,
                           TaskPriority priority, LocalDateTime dueDate) {
        membershipService.requireMember(groupId, userId);
        Task task = findTaskInGroup(groupId, taskId);
        task.setTitle(title);
        task.setDescription(description != null ? description : "");
        if (priority != null) {
            task.setPriority(priority);
        }
        task.setDueDate(dueDate);
        return taskRepository.save(task);
    }

    @Transactional
    public Task updateStatus(Long groupId, Long userId, Long taskId, TaskStatus newStatus) {
        membershipService.requireMember(groupId, userId);
        Task task = findTaskInGroup(groupId, taskId);
        TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);
        task = taskRepository.save(task);
        logStatusChange(task, userId, "UPDATE_STATUS", oldStatus, newStatus);
        return task;
    }

    @Transactional
    public void deleteTask(Long groupId, Long userId, Long taskId) {
        membershipService.requireMember(groupId, userId);
        findTaskInGroup(groupId, taskId);
        taskLogRepository.deleteAll(taskLogRepository.findByTaskIdOrderByCreatedAtAsc(taskId));
        taskRepository.deleteById(taskId);
    }

    @Transactional
    public Task assign(Long groupId, Long operatorId, Long taskId, Long assigneeId) {
        membershipService.requireMember(groupId, operatorId);
        requireGroupMember(groupId, assigneeId);
        Task task = findTaskInGroup(groupId, taskId);

        AssignmentStatus old = task.getAssignmentStatus();
        task.setAssigneeId(assigneeId);
        task.setAssignmentStatus(AssignmentStatus.PENDING_ACCEPT);
        task = taskRepository.save(task);
        logAssignmentChange(task, operatorId, "ASSIGN", old, AssignmentStatus.PENDING_ACCEPT);
        notifyTaskAssigned(task, operatorId);
        return task;
    }

    @Transactional
    public Task accept(Long groupId, Long userId, Long taskId) {
        membershipService.requireMember(groupId, userId);
        Task task = findTaskInGroup(groupId, taskId);
        if (task.getAssignmentStatus() != AssignmentStatus.PENDING_ACCEPT) {
            throw new BusinessException(400, "任务不在待接取状态");
        }
        if (!userId.equals(task.getAssigneeId())) {
            throw new BusinessException(403, "仅被指派人可确认接取");
        }

        AssignmentStatus old = task.getAssignmentStatus();
        task.setAssignmentStatus(AssignmentStatus.ACCEPTED);
        task = taskRepository.save(task);
        logAssignmentChange(task, userId, "ACCEPT", old, AssignmentStatus.ACCEPTED);
        notifyTaskAccepted(task, userId);
        return task;
    }

    @Transactional
    public Task reject(Long groupId, Long userId, Long taskId) {
        membershipService.requireMember(groupId, userId);
        Task task = findTaskInGroup(groupId, taskId);
        if (task.getAssignmentStatus() != AssignmentStatus.PENDING_ACCEPT) {
            throw new BusinessException(400, "任务不在待接取状态");
        }
        if (!userId.equals(task.getAssigneeId())) {
            throw new BusinessException(403, "仅被指派人可拒绝接取");
        }

        AssignmentStatus old = task.getAssignmentStatus();
        task.setAssigneeId(null);
        task.setAssignmentStatus(AssignmentStatus.UNASSIGNED);
        task = taskRepository.save(task);
        logAssignmentChange(task, userId, "REJECT", old, AssignmentStatus.UNASSIGNED);
        return task;
    }

    @Transactional
    public Task claim(Long groupId, Long userId, Long taskId) {
        membershipService.requireMember(groupId, userId);
        Task task = findTaskInGroup(groupId, taskId);
        if (task.getAssignmentStatus() != AssignmentStatus.UNASSIGNED) {
            throw new BusinessException(400, "任务已被指派，无法主动接取");
        }

        AssignmentStatus old = task.getAssignmentStatus();
        task.setAssigneeId(userId);
        task.setAssignmentStatus(AssignmentStatus.ACCEPTED);
        task = taskRepository.save(task);
        logAssignmentChange(task, userId, "CLAIM", old, AssignmentStatus.ACCEPTED);
        return task;
    }

    @Transactional
    public Task transfer(Long groupId, Long operatorId, Long taskId, Long newAssigneeId) {
        membershipService.requireMember(groupId, operatorId);
        requireGroupMember(groupId, newAssigneeId);
        Task task = findTaskInGroup(groupId, taskId);
        if (task.getAssignmentStatus() != AssignmentStatus.ACCEPTED) {
            throw new BusinessException(400, "仅已接取的任务可转派");
        }
        if (!operatorId.equals(task.getAssigneeId())) {
            throw new BusinessException(403, "仅当前执行人可转派");
        }

        AssignmentStatus old = task.getAssignmentStatus();
        task.setAssigneeId(newAssigneeId);
        task.setAssignmentStatus(AssignmentStatus.PENDING_ACCEPT);
        task = taskRepository.save(task);
        logAssignmentChange(task, operatorId, "TRANSFER", old, AssignmentStatus.PENDING_ACCEPT);
        return task;
    }

    private Task findTaskInGroup(Long groupId, Long taskId) {
        return taskRepository.findByIdAndGroupId(taskId, groupId)
                .orElseThrow(() -> new BusinessException(404, "任务不存在"));
    }

    private void requireGroupMember(Long groupId, Long userId) {
        if (!memberRepository.findByGroupIdAndUserId(groupId, userId).isPresent()) {
            throw new BusinessException(400, "被指派人不是小组成员");
        }
    }

    private void logAssignmentChange(Task task, Long operatorId, String action,
                                     AssignmentStatus from, AssignmentStatus to) {
        TaskLog log = new TaskLog();
        log.setTaskId(task.getId());
        log.setAction(action);
        log.setFromAssignment(from);
        log.setToAssignment(to);
        log.setOperatorId(operatorId);
        taskLogRepository.save(log);
    }

    private void logStatusChange(Task task, Long operatorId, String action,
                                 TaskStatus from, TaskStatus to) {
        TaskLog log = new TaskLog();
        log.setTaskId(task.getId());
        log.setAction(action);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperatorId(operatorId);
        taskLogRepository.save(log);
    }

    private void notifyTaskAssigned(Task task, Long operatorId) {
        if (task.getAssigneeId() == null) {
            return;
        }
        String operatorName = userRepository.findById(operatorId)
                .map(u -> u.getUsername())
                .orElse("成员");
        notificationService.publish(new NotificationEvent(
                task.getAssigneeId(),
                task.getGroupId(),
                NotificationEventType.TASK_ASSIGNED,
                "任务指派",
                operatorName + " 将任务「" + task.getTitle() + "」指派给你",
                null,
                "/groups/" + task.getGroupId() + "/tasks/" + task.getId()
        ));
    }

    private void notifyTaskAccepted(Task task, Long assigneeId) {
        String assigneeName = userRepository.findById(assigneeId)
                .map(u -> u.getUsername())
                .orElse("成员");
        notificationService.publish(new NotificationEvent(
                task.getCreatorId(),
                task.getGroupId(),
                NotificationEventType.TASK_ACCEPTED,
                "任务已接取",
                assigneeName + " 已接取任务「" + task.getTitle() + "」",
                null,
                "/groups/" + task.getGroupId() + "/tasks/" + task.getId()
        ));
    }
}
