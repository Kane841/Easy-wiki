package com.easywiki.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateGroupRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    private boolean discoverable;

    public CreateGroupRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDiscoverable() {
        return discoverable;
    }

    public void setDiscoverable(boolean discoverable) {
        this.discoverable = discoverable;
    }
}
