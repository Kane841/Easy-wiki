package com.easywiki.repository;

import com.easywiki.entity.Task;
import com.easywiki.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    List<Task> findByGroupIdAndStatusOrderByCreatedAtDesc(Long groupId, TaskStatus status);

    Optional<Task> findByIdAndGroupId(Long id, Long groupId);
}
