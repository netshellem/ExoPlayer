package com.irdeto.drm.session;

public class User {
    private String crmId;
    private String userId;
    private String password;

    public User(String crmId, String userId, String password) {
        this.crmId = crmId;
        this.userId = userId;
        this.password = password;
    }

    public String getCrmId() {
        return crmId;
    }

    public String getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }
}


