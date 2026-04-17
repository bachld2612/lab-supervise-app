package com.bachld.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response model for personal computer API endpoints.
 * Wraps status code and nullable PersonalComputerData.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonalComputerResponse {

    @JsonProperty("statusCode")
    private int statusCode;

    @JsonProperty("data")
    private PersonalComputerData data;

    public PersonalComputerResponse() {
    }

    public PersonalComputerResponse(int statusCode, PersonalComputerData data) {
        this.statusCode = statusCode;
        this.data = data;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public PersonalComputerData getData() {
        return data;
    }

    public void setData(PersonalComputerData data) {
        this.data = data;
    }
}
