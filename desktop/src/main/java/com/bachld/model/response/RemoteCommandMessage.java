package com.bachld.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RemoteCommandMessage {

    @JsonProperty("commandId")
    private String commandId;

    @JsonProperty("type")
    private String type;

    @JsonProperty("arguments")
    private Map<String, Object> arguments;

    public String getCommandId() { return commandId; }
    public void setCommandId(String commandId) { this.commandId = commandId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Map<String, Object> getArguments() { return arguments; }
    public void setArguments(Map<String, Object> arguments) { this.arguments = arguments; }
}
