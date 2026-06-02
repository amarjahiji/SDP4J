package com.sdp4j.demo.models;

import com.sdp4j.sm4j.annotations.DefaultFalse;
import com.sdp4j.sm4j.annotations.NotNull;
import com.sdp4j.sm4j.annotations.PrimaryKey;
import com.sdp4j.sm4j.annotations.Table;

@Table(name = "users", mapToSnakeCase = true)
public class User {
    @PrimaryKey
    private String id;

    @NotNull
    private String firstName;

    @NotNull
    private String lastName;

    @DefaultFalse
    private Boolean isActive;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }
}
