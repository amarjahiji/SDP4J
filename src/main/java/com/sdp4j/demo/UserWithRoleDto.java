package com.sdp4j.demo;

public class UserWithRoleDto {

    private String id;
    private String firstName;
    private String lastName;
    private Boolean isActive;
    private String name;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Boolean getActive() { return isActive; }
    public void setActive(Boolean active) { isActive = active; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
