package com.easywiki.agent;

import com.easywiki.entity.Task;
import com.easywiki.entity.WikiPage;
import com.easywiki.enums.AgentIntent;

import java.util.List;

public final class PromptBuilder {

    private PromptBuilder() {
    }

    public static String buildSystemPrompt(String groupName, AgentIntent intent,
                                           List<WikiPage> wikiPages, List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 Easy-wiki 团队助手，服务于小组「").append(groupName).append("」。");
        sb.append("请用 Markdown 格式回复，简洁专业。\n\n");

        switch (intent) {
            case TASK_ORGANIZE -> {
                sb.append("用户希望整理或了解任务情况。当前小组任务列表：\n");
                appendTasks(sb, tasks);
            }
            case WIKI_SUMMARY -> {
                sb.append("用户希望了解或摘要 Wiki 文档。相关文档：\n");
                appendWikiPages(sb, wikiPages);
            }
            case TASK_SUGGEST -> {
                sb.append("用户希望根据文档内容建议可执行任务。参考文档：\n");
                appendWikiPages(sb, wikiPages);
                sb.append("\n如需输出任务建议，请在回复末尾附加 JSON 代码块，格式：\n");
                sb.append("```json\n{\"tasks\":[{\"title\":\"...\",\"description\":\"...\",\"priority\":\"MEDIUM\"}]}\n```\n");
            }
            default -> {
                sb.append("小组概况：\n");
                sb.append("- Wiki 页面数：").append(wikiPages.size()).append("\n");
                sb.append("- 任务数：").append(tasks.size()).append("\n");
                if (!tasks.isEmpty()) {
                    sb.append("\n近期任务：\n");
                    appendTasks(sb, tasks.stream().limit(10).toList());
                }
            }
        }

        return sb.toString();
    }

    private static void appendTasks(StringBuilder sb, List<Task> tasks) {
        if (tasks.isEmpty()) {
            sb.append("（暂无任务）\n");
            return;
        }
        for (Task task : tasks) {
            sb.append("- [").append(task.getId()).append("] ")
                    .append(task.getTitle())
                    .append(" | 状态: ").append(task.getStatus())
                    .append(" | 优先级: ").append(task.getPriority())
                    .append(" | 指派: ").append(task.getAssignmentStatus());
            if (task.getDueDate() != null) {
                sb.append(" | 截止: ").append(task.getDueDate());
            }
            sb.append("\n");
            if (task.getDescription() != null && !task.getDescription().isBlank()) {
                sb.append("  描述: ").append(truncate(task.getDescription(), 200)).append("\n");
            }
        }
    }

    private static void appendWikiPages(StringBuilder sb, List<WikiPage> pages) {
        if (pages.isEmpty()) {
            sb.append("（暂无 Wiki 页面）\n");
            return;
        }
        for (WikiPage page : pages) {
            sb.append("### ").append(page.getTitle()).append(" (id=").append(page.getId()).append(")\n");
            sb.append(truncate(page.getContent(), 1500)).append("\n\n");
        }
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }
}
