package com.irdeto.drm;

import java.io.InputStream;

import org.apache.http.Header;

@SuppressWarnings("deprecation")
public interface URLHandler {
    public void onComplete(int status, InputStream resp, Header[] headers);

    public void onException(Throwable e);
}
