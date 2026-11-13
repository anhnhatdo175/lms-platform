package com.example.lms.course;

import com.fasterxml.jackson.annotation.JsonSetter;
import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.Size;

public class UpdateCategoryRequest {

    @Size(max = 255)
    private String name;

    private JsonNode description;

    private Boolean active;

    public String getName() {
        return name;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    public JsonNode getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(JsonNode description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    @JsonSetter("active")
    public void setActive(Boolean active) {
        this.active = active;
    }
}
