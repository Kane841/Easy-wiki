package com.easywiki.repository;

import com.easywiki.entity.TaskLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskLogRepository extends JpaRepository<TaskLog, Long> {

    List<TaskLog> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}
