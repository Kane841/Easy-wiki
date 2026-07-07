package com.easywiki.dto.request;

import com.easywiki.enums.TaskPriority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class AgentTaskCreateRequest {

    @NotEmpty
    @Valid
    private List<TaskSuggestion> tasks = new ArrayList<>();

    public AgentTaskCreateRequest() {
    }

    public List<TaskSuggestion> getTasks() {
        return tasks;
    }

    public void setTasks(List<TaskSuggestion> tasks) {
        this.tasks = tasks;
    }

    public static class TaskSuggestion {

        @NotBlank
        @Size(max = 200)
        private String title;

        private String description;

        private TaskPriority priority;

        public TaskSuggestion() {
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public TaskPriority getPriority() {
            return priority;
        }

        public void setPriority(TaskPriority priority) {
            this.priority = priority;
        }
    }
}
