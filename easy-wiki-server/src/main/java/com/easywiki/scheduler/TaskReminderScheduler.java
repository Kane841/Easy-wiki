package com.easywiki.scheduler;

import com.easywiki.dto.event.NotificationEvent;
import com.easywiki.entity.Task;
import com.easywiki.entity.TaskLog;
import com.easywiki.enums.AssignmentStatus;
import com.easywiki.enums.NotificationEventType;
import com.easywiki.repository.TaskLogRepository;
import com.easywiki.repository.TaskRepository;
import com.easywiki.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

@Component
public class TaskReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(TaskReminderScheduler.class);
    static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final TaskRepository taskRepository;
    private final TaskLogRepository taskLogRepository;
    private final NotificationService notificationService;

    public TaskReminderScheduler(TaskRepository taskRepository,
                                 TaskLogRepository taskLogRepository,
                                 NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.taskLogRepository = taskLogRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 * * * ?", zone = "Asia/Shanghai")
    @Transactional
    public void runHourlyJobs() {
        ZonedDateTime now = ZonedDateTime.now(SHANGHAI);
        sendDueReminders(now);
        if (now.getHour() == 9) {
            sendTodayDueReminders(now.toLocalDate().atStartOfDay());
        }
        expireAssignmentTimeouts(now.toLocalDateTime().minusDays(7));
    }

    void sendDueReminders(ZonedDateTime now) {
        LocalDateTime nowLocal = now.toLocalDateTime();
        LocalDateTime windowStart = nowLocal.plusHours(23);
        LocalDateTime windowEnd = nowLocal.plusHours(24);
        List<Task> tasks = taskRepository.findDueBetween(windowStart, windowEnd);
        for (Task task : tasks) {
            notifyDueReminder(task, "任务将在 24 小时内到期");
        }
        log.debug("Sent {} due-within-24h reminders", tasks.size());
    }

    void sendTodayDueReminders(LocalDateTime dayStart) {
        LocalDateTime dayEnd = dayStart.plusDays(1);
        List<Task> tasks = taskRepository.findDueBetween(dayStart, dayEnd);
        for (Task task : tasks) {
            notifyDueReminder(task, "任务今日到期，请及时处理");
        }
        log.debug("Sent {} due-today reminders at 9am", tasks.size());
    }

    void expireAssignmentTimeouts(LocalDateTime cutoff) {
        List<Task> pendingTasks = taskRepository.findByAssignmentStatus(AssignmentStatus.PENDING_ACCEPT);
        int expired = 0;
        for (Task task : pendingTasks) {
            LocalDateTime pendingSince = findPendingSince(task);
            if (pendingSince.isBefore(cutoff)) {
                resetToUnassigned(task);
                notificationService.publish(new NotificationEvent(
                        task.getCreatorId(),
                        task.getGroupId(),
                        NotificationEventType.TASK_ASSIGNMENT_TIMEOUT,
                        "指派超时",
                        "任务「" + task.getTitle() + "」指派已超时，已重置为未指派",
                        null,
                        "/groups/" + task.getGroupId() + "/tasks/" + task.getId()
                ));
                expired++;
            }
        }
        log.debug("Expired {} assignment timeouts", expired);
    }

    LocalDateTime findPendingSince(Task task) {
        return taskLogRepository.findByTaskIdOrderByCreatedAtAsc(task.getId()).stream()
                .filter(log -> log.getToAssignment() == AssignmentStatus.PENDING_ACCEPT)
                .map(TaskLog::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElse(task.getCreatedAt());
    }

    private void resetToUnassigned(Task task) {
        AssignmentStatus old = task.getAssignmentStatus();
        task.setAssigneeId(null);
        task.setAssignmentStatus(AssignmentStatus.UNASSIGNED);
        taskRepository.save(task);

        TaskLog logEntry = new TaskLog();
        logEntry.setTaskId(task.getId());
        logEntry.setAction("ASSIGNMENT_TIMEOUT");
        logEntry.setFromAssignment(old);
        logEntry.setToAssignment(AssignmentStatus.UNASSIGNED);
        logEntry.setOperatorId(task.getCreatorId());
        taskLogRepository.save(logEntry);
    }

    private void notifyDueReminder(Task task, String bodySuffix) {
        Long recipient = task.getAssigneeId() != null ? task.getAssigneeId() : task.getCreatorId();
        notificationService.publish(new NotificationEvent(
                recipient,
                task.getGroupId(),
                NotificationEventType.TASK_DUE_REMINDER,
                "任务到期提醒",
                "任务「" + task.getTitle() + "」" + bodySuffix,
                null,
                "/groups/" + task.getGroupId() + "/tasks/" + task.getId()
        ));
    }

    static boolean isWithin24HourWindow(LocalDateTime now, LocalDateTime dueDate) {
        LocalDateTime windowStart = now.plusHours(23);
        LocalDateTime windowEnd = now.plusHours(24);
        return !dueDate.isBefore(windowStart) && dueDate.isBefore(windowEnd);
    }
}
