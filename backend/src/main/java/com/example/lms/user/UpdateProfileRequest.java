package com.example.lms.user;

import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonSetter;
import tools.jackson.databind.JsonNode;

public class UpdateProfileRequest {

    @Size(max = 255)
    private String displayName;

    private JsonNode avatarResourceId;
    private boolean avatarResourceIdProvided;

    public String getDisplayName() {
        return displayName;
    }

    @JsonSetter("displayName")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public JsonNode getAvatarResourceId() {
        return avatarResourceId;
    }

    @JsonSetter("avatarResourceId")
    public void setAvatarResourceId(JsonNode avatarResourceId) {
        this.avatarResourceIdProvided = true;
        this.avatarResourceId = avatarResourceId;
    }

    public boolean isAvatarResourceIdProvided() {
        return avatarResourceIdProvided;
    }
}
