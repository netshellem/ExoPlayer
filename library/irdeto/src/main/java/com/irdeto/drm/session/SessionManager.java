package com.irdeto.drm.session;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class SessionManager {

    private Map<String, Session> sessionCache = new HashMap<String, Session>();

    private static SessionManager INSTANCE = new SessionManager();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public Session getSession(String userId, String url) {
        Session session = sessionCache.get(userId);
        if (session == null || session.isExpired()) {
            session = getSessionFromService(userId, url);
            if (session != null) {
                sessionCache.put(userId, session);
            }
        }

        return session;
    }

    private Session getSessionFromService(String userId, String url) {
        byte[] nData = null;
        Session session = null;

        StringBuffer urlStr = new StringBuffer();
        urlStr.append(url);
        if (url.endsWith("/")) {
            urlStr.append("Gateway/GetSession?");
        } else {
            urlStr.append("/Gateway/GetSession?");
        }
        urlStr.append("UserId=").append(userId);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) (new URL(urlStr.toString())).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            InputStream in = new BufferedInputStream(conn.getInputStream());
            nData = readStream(in);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) conn.disconnect();
        }

        session = parse(nData);
        return session;
    }

    private Session parse(byte[] nData) {
        if (nData == null) {
            return null;
        }

        Session session = null;
        String str = new String(nData);
        String[] params = str.split("&");
        if (params.length > 1) {
            session = new Session();
            for (String param : params) {
                String[] pairs = param.split("=");
                if (pairs.length == 2) {
                    if (pairs[0].equals("SessionId")) {
                        session.setSessionId(pairs[1]);
                    } else if (pairs[0].equals("Ticket")) {
                        session.setTicket(pairs[1]);
                    }
                }
            }
        }

        return session;
    }

    private byte[] readStream(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;

        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }

        byte[] data = out.toByteArray();
        out.close();
        in.close();

        return data;
    }

}