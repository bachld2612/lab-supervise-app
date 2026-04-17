package com.bachld.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Role model containing user role information.
 * 
 * Validates: Requirements 7.2, 7.5, 7.6
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Role {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("color")
    private String color;
    
    @JsonProperty("roleType")
    private Integer roleType;
    
    public Role() {
    }

    public Role(Long id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public Integer getRoleType() {
        return roleType;
    }
    
    public void setRoleType(Integer roleType) {
        this.roleType = roleType;
    }
}
