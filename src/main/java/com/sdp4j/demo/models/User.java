package com.sdp4j.demo.models;

import com.sdp4j.sm4j.annotations.*;

import java.math.BigDecimal;

@Table(name = "users", mapToSnakeCase = true)
public class User {
    @PrimaryKey
    private String id;

    @NotNull
    private String firstName;

    @NotNull
    private String surname;

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

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }
}
