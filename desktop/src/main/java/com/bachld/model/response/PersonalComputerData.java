package com.bachld.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data model for personal computer information returned by API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonalComputerData {

    @JsonProperty("ipAddress")
    private String ipAddress;

    @JsonProperty("userId")
    private Integer userId;

    public PersonalComputerData() {
    }

    public PersonalComputerData(String ipAddress, Integer userId) {
        this.ipAddress = ipAddress;
        this.userId = userId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}
