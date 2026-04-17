package com.bachld.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Error response model for personal computer update API.
 * The data field contains a map of field name -> error message.
 * Example: {"ipAddress": "Địa chỉ IP không được phép bỏ trống"}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonalComputerErrorResponse {

    @JsonProperty("statusCode")
    private int statusCode;

    @JsonProperty("data")
    private Map<String, String> data;

    public PersonalComputerErrorResponse() {
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }

    /**
     * Returns the first error message from the data map.
     * @return the first error message, or a default message if none found
     */
    public String getFirstErrorMessage() {
        if (data != null && !data.isEmpty()) {
            return data.values().iterator().next();
        }
        return "Đã xảy ra lỗi không xác định.";
    }
}
