package com.easywiki.scheduler;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TaskReminderSchedulerTest {

    @Test
    void dueDateWithin24HourWindowIsDetected() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 7, 10, 0);
        LocalDateTime dueIn23h30m = now.plusHours(23).plusMinutes(30);

        assertThat(TaskReminderScheduler.isWithin24HourWindow(now, dueIn23h30m)).isTrue();
    }

    @Test
    void dueDateOutside24HourWindowIsNotDetected() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 7, 10, 0);
        LocalDateTime dueIn48h = now.plusHours(48);

        assertThat(TaskReminderScheduler.isWithin24HourWindow(now, dueIn48h)).isFalse();
    }

    @Test
    void dueDateAtWindowBoundaryIsDetected() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 7, 10, 0);
        LocalDateTime dueAt23h = now.plusHours(23);

        assertThat(TaskReminderScheduler.isWithin24HourWindow(now, dueAt23h)).isTrue();
    }
}
