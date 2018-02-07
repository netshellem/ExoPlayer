package com.irdeto.drm.session;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.Header;
import android.util.Log;

import com.irdeto.drm.URLHandler;
import com.irdeto.drm.utility.URLRequest;

@SuppressWarnings("deprecation")
public class UserService {
    public static final String TAG = "UserService";
    public static final String API_LOGIN_USER = "services/LoginUser";
    private static UserService INSTANCE = new UserService();
    private UserService() {}
    public static UserService getInstance() {
        return INSTANCE;
    }

    private Session session;
    public void login() {
        User user = session.getUser();
        String url = session.getUrl();
        String crmId = user.getCrmId();
        String userId = user.getUserId();
        String password = user.getPassword();

        String fullUrl = new StringBuffer().append(url).append(API_LOGIN_USER).append("?").append("CrmId=").append(crmId).toString();
        String body = new StringBuffer().append("UserId=").append(userId).append("&Password=").append(password).toString();

        Log.d(TAG, "login - url:" + fullUrl);
        Log.d(TAG, "login - body:" + body);
        URLRequest.userLogin(fullUrl, body, new URLHandler() {
            public void onComplete(int status, InputStream resp, Header[] headers) {
                try {
                    List<String> cookies = new ArrayList<String>();
                    for (Header header : headers) {
                        if (header.getName().equals("Set-Cookie")) {
                            cookies.add(header.getValue());
                        }
                    }

                    if (cookies.size() > 0) {
                        Log.d(TAG, "user login successfully");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void onException(Throwable e) {
                e.printStackTrace();
            }
        });
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public boolean isOnline() {
        return session == null ? false : session.isOnline();
    }


}
