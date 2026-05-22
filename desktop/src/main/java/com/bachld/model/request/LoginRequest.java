package com.bachld.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request model for login API endpoint.
 * Contains user credentials and device type information.
 * 
 * Validates: Requirements 7.2, 7.5, 7.6
 */
public class LoginRequest {
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("password")
    private String password;
    
    @JsonProperty("device")
    private String device;

    @JsonProperty("wifiSsid")
    private String wifiSsid;

    public LoginRequest() {
    }

    public LoginRequest(String email, String password, String device, String wifiSsid) {
        this.email = email;
        this.password = password;
        this.device = device;
        this.wifiSsid = wifiSsid;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getdevice() {
        return device;
    }

    public void setdevice(String device) {
        this.device = device;
    }

    public String getWifiSsid() {
        return wifiSsid;
    }

    public void setWifiSsid(String wifiSsid) {
        this.wifiSsid = wifiSsid;
    }
}
