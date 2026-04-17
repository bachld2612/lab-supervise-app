package com.bachld.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request model for updating personal computer IP address.
 */
public class PersonalComputerUpdateRequest {

    @JsonProperty("ipAddress")
    private String ipAddress;

    public PersonalComputerUpdateRequest() {
    }

    public PersonalComputerUpdateRequest(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
