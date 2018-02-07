package com.irdeto.drm.utility;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.irdeto.drm.URLHandler;

@SuppressWarnings({ "deprecation"})
public class URLRequest {
    public static final String POST = "post";
    public static final String GET = "get";

    public static void syncRequest(String url, Header[] headers, HttpEntity entity, URLHandler handler) {
        HttpPost httpPost = new HttpPost(url);
        if (headers != null) {
            for (Header header : headers) {
                httpPost.addHeader(header);
            }
        }

        HttpResponse httpResp = null;
        HttpClient httpClient = new DefaultHttpClient();
        try {
            httpPost.setEntity(entity);
            httpResp = httpClient.execute(httpPost);
            handler.onComplete(httpResp.getStatusLine().getStatusCode(), httpResp.getEntity().getContent(), httpResp.getAllHeaders());
        } catch (Exception e) {
            handler.onException(e);
        }
    }

    public static void userLogin(String url, String message, URLHandler handler) {
        try {
            syncRequest(url, null, new StringEntity(message), handler);
        } catch (Exception e) {
            handler.onException(e);
        }
    }

}
