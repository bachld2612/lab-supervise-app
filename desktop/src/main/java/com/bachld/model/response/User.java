package com.bachld.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User model containing authenticated user information.
 * 
 * Validates: Requirements 7.2, 7.5, 7.6
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("status")
    private Integer status;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("rawPassword")
    private String rawPassword;
    
    @JsonProperty("fullName")
    private String fullName;
    
    @JsonProperty("phone")
    private String phone;
    
    @JsonProperty("hometown")
    private String hometown;
    
    @JsonProperty("birthday")
    private String birthday;
    
    @JsonProperty("roleId")
    private Integer roleId;
    
    public User() {
    }

    public User(Long id, String email, String fullName, String phone) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getRawPassword() {
        return rawPassword;
    }
    
    public void setRawPassword(String rawPassword) {
        this.rawPassword = rawPassword;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getHometown() {
        return hometown;
    }
    
    public void setHometown(String hometown) {
        this.hometown = hometown;
    }
    
    public String getBirthday() {
        return birthday;
    }
    
    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }
    
    public Integer getRoleId() {
        return roleId;
    }
    
    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }
}
