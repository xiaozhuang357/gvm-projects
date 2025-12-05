package com._404.wms.databases.userdata;

import com.google.gson.JsonObject;
import java.sql.Timestamp;

// 用户基本信息类
public class UserInfo {
    private int uid;
    private String name;
    private String email;
    private int level;

    @Override
    public String toString() {
        return String.format("User{uid=%d, name='%s', email='%s',level='%d'}", uid, name, email, level);
    }

    public JsonObject toJsonObject() {
        JsonObject response = new JsonObject();
        response.addProperty("level", this.level);
        response.addProperty("uid", this.uid);
        response.addProperty("name", this.name);
        response.addProperty("email", this.email);
        return response;
    }

    // Getters and Setters
    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}