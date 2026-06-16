package com.bachld.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClipboardEventPayload {

    @JsonProperty("applicationName")
    private String applicationName;

    @JsonProperty("action")
    private int action;

    @JsonProperty("clipboardTextEncrypted")
    private String clipboardTextEncrypted;

    @JsonProperty("clipboardKeyEncrypted")
    private String clipboardKeyEncrypted;

    @JsonProperty("clipboardIv")
    private String clipboardIv;

    public ClipboardEventPayload() {
    }

    public ClipboardEventPayload(String applicationName, int action, String clipboardTextEncrypted,
                                 String clipboardKeyEncrypted, String clipboardIv) {
        this.applicationName = applicationName;
        this.action = action;
        this.clipboardTextEncrypted = clipboardTextEncrypted;
        this.clipboardKeyEncrypted = clipboardKeyEncrypted;
        this.clipboardIv = clipboardIv;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public int getAction() {
        return action;
    }

    public String getClipboardTextEncrypted() {
        return clipboardTextEncrypted;
    }

    public String getClipboardKeyEncrypted() {
        return clipboardKeyEncrypted;
    }

    public String getClipboardIv() {
        return clipboardIv;
    }
}
