package com.easywiki.agent;

import com.easywiki.entity.Task;
import com.easywiki.enums.AgentIntent;
import com.easywiki.enums.AssignmentStatus;
import com.easywiki.enums.TaskPriority;
import com.easywiki.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    @Test
    void taskOrganizeIntentInjectsTaskList() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("完成设计文档");
        task.setDescription("撰写 API 设计");
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setPriority(TaskPriority.HIGH);
        task.setAssignmentStatus(AssignmentStatus.ACCEPTED);
        task.setDueDate(LocalDateTime.of(2026, 7, 10, 18, 0));

        String prompt = PromptBuilder.buildSystemPrompt("研发组", AgentIntent.TASK_ORGANIZE, List.of(), List.of(task));

        assertThat(prompt).contains("研发组");
        assertThat(prompt).contains("完成设计文档");
        assertThat(prompt).contains("IN_PROGRESS");
        assertThat(prompt).contains("[1]");
    }

    @Test
    void wikiSummaryIntentInjectsPageContent() {
        var page = new com.easywiki.entity.WikiPage();
        page.setId(5L);
        page.setTitle("部署指南");
        page.setContent("使用 Docker Compose 部署服务");

        String prompt = PromptBuilder.buildSystemPrompt("运维组", AgentIntent.WIKI_SUMMARY, List.of(page), List.of());

        assertThat(prompt).contains("部署指南");
        assertThat(prompt).contains("Docker Compose");
    }

    @Test
    void generalIntentIncludesOverview() {
        String prompt = PromptBuilder.buildSystemPrompt("测试组", AgentIntent.GENERAL, List.of(), List.of());

        assertThat(prompt).contains("Wiki 页面数：0");
        assertThat(prompt).contains("任务数：0");
    }
}
