package com.bachld.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PCInfoPayload {

    @JsonProperty("applicationName")
    private String applicationName;

    public PCInfoPayload() {
    }

    public PCInfoPayload(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
}
