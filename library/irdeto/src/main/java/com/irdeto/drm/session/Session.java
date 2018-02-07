package com.irdeto.drm.session;

import java.util.List;

import com.irdeto.drm.utility.StringUtility;

public class Session {
    private String url;
    private User user;
    private List<String> cookies;
    private String sessionId;
    private String ticket;

    public Session() {
        this(null, null, null, null);
    }

    public Session(String url, User user, String sessionId, String ticket) {
        this.url = url;
        this.user = user;
        this.sessionId = sessionId;
        this.ticket = ticket;
    }

    public String getUrl() {
        return url;
    }

    public User getUser() {
        return user;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isOnline() {
        if (StringUtility.notEmpty(sessionId) && StringUtility.notEmpty(ticket)) return true;
        if ((cookies != null) && (cookies.size() > 0)) return true;

        return false;
    }

    public List<String> getCookies() {
        return cookies;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTicket() {
        return ticket;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setTicket(String ticket) {
        this.ticket = ticket;
    }

    public void setCookies(List<String> cookies) {
        this.cookies = cookies;
    }

    public boolean isExpired() {
        return true;
    }

}
