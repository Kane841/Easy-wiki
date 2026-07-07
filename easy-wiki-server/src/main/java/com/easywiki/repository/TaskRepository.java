package com.easywiki.repository;

import com.easywiki.entity.Task;
import com.easywiki.enums.AssignmentStatus;
import com.easywiki.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    List<Task> findByGroupIdAndStatusOrderByCreatedAtDesc(Long groupId, TaskStatus status);

    Optional<Task> findByIdAndGroupId(Long id, Long groupId);

    List<Task> findByAssignmentStatus(AssignmentStatus assignmentStatus);

    @Query("SELECT t FROM Task t WHERE t.dueDate IS NOT NULL AND t.dueDate >= :start AND t.dueDate < :end AND t.status <> com.easywiki.enums.TaskStatus.DONE")
    List<Task> findDueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
