package com.sdp4j.demo;

import com.sdp4j.sm4j.annotations.ForeignKey;
import com.sdp4j.sm4j.annotations.NotNull;
import com.sdp4j.sm4j.annotations.PrimaryKey;
import com.sdp4j.sm4j.annotations.Table;

@Table(name = "roles", mapToSnakeCase = true)
public class Role {

    @PrimaryKey
    private String id;

    @NotNull
    private String name;

    @ForeignKey(mapsTo = User.class)
    @NotNull
    private String userId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
