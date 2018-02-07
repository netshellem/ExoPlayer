package com.irdeto.drm;


import java.net.HttpURLConnection;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.BasicHttpParams;
/*import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.BasicHttpParams;

import android.os.Environment;*/
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;

import android.util.Log;
import android.content.Context;
import com.irdeto.drm.session.Session;
import com.irdeto.drm.utility.StringUtility;
import com.irdeto.drm.utility.Utility;

/*import com.ir.app.*;


import com.irdeto.drm.utility.StringUtility;
import com.irdeto.drm.utility.Utility;
import com.ir.app.*;*/
public class ChinaDrm {
    private static String TAG = ChinaDrm.class.getSimpleName();
    private String securePath;
    private DeviceInfo deviceInfo;

    private Session session;
    private Config config;


    // to enable JAC
    /*private static ApplicationManager m_AM = null;*/

    public ChinaDrm(Context context, String securePath, DeviceInfo deviceInfo) {
        this.securePath = securePath;
        this.deviceInfo = deviceInfo;
        this.config = Config.getInstance();

        Log.d(TAG, "chinadrm initIDs {");
/*        if (m_AM == null)
        {
            try
            {
                Log.d("JAC", (String)"To start ApplicationManager");
                m_AM = new ApplicationManager(ApplicationManager.DALVIKVM, context.getApplicationContext(),null,true);
                Log.d("JAC", (String)"ApplicationManager started");
            }
            catch (Exception e)
            {
                //throw new AndroidException(e.getMessage());
            }
        }*/
        native_initIDs();
        Log.d(TAG, "chinadrm initIDs }");
    }

    public int shutdown() {
        return native_shutdown();
    }
    public int startup(Context context) {
        return native_startup(securePath, deviceInfo.toJsonString(), context.getApplicationContext());
    }
    public boolean deleteLicense() {
        String licenseFileName = new StringBuffer(securePath).append("/").append("cdrm_license.dat").toString();
        return Utility.deleteFile(licenseFileName);
    }
    public void errorNotification(int severity, int errorCode, int additionalCode, String description) {

        Log.d(TAG, "severity["+severity+"] "
                +"errorCode["+ errorCode + "] "
                +"additionalCode["+ additionalCode + "] "
                +"errorNotification:" + description);

    }

    public int setForceQuit() {
        return native_setForceQuit();
    }

    /**
     * Send request to License Server
     *
     * @param url
     * @param data
     */
    public String drmAgentRequest(String url, String data) {
        String response = "";
        byte[] nData = null;
        String fullUrl = url;
        HttpPost httpPost = null;
        HttpGet httpGet = null;

        try {
            if (data != null) {
                String sessionId="";
                String ticket="";
                String crmId="";
                if(session != null)
                {
                     sessionId = session.getSessionId();
                     ticket = session.getTicket();
                     crmId = session.getUser().getCrmId();
                }

                StringBuffer buffer = new StringBuffer(url);
                if (buffer.indexOf("?") > -1) {
                    buffer.append("&");
                } else {
                    buffer.append("?");
                }
                if (StringUtility.notEmpty(crmId)) buffer.append("CrmId=").append(crmId);
                if (StringUtility.notEmpty(crmId))
                    buffer.append("&").append("AccountId=").append(crmId);
                if (StringUtility.notEmpty(sessionId))
                    buffer.append("&").append("SessionId=").append(sessionId);
                if (StringUtility.notEmpty(ticket)) buffer.append("&").append("Ticket=").append(ticket);

                fullUrl = buffer.toString();
                httpPost = new HttpPost(fullUrl);
                List<String> cookies = null;
                if(session != null)
                    cookies = session.getCookies();
                if (cookies != null) {
                    for (String cookie : cookies) {
                        httpPost.addHeader("cookie", cookie);
                    }
                }
                httpPost.setEntity(new StringEntity(data));
                Log.d(TAG, "URL:" + fullUrl);
                Log.d(TAG, "Post Reqest:" + data);
            } else {
                httpGet = new HttpGet(fullUrl);
                Log.d(TAG, "URL:" + fullUrl);
                Log.d(TAG, "Get Request");
            }

            HttpResponse httpResp = null;
            HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, 10*1000);
            HttpConnectionParams.setSoTimeout(httpParameters, 5*1000);
            HttpClient httpClient = new DefaultHttpClient(httpParameters);

            httpResp = httpClient.execute((data == null) ? httpGet : httpPost);
            if (httpResp.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
                nData = EntityUtils.toByteArray(httpResp.getEntity());
                response = response + (new String(nData));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Response:" + response);
        return response;
    }
    /**
     * Send request to ChinaDRM Server
     *
     * @param url
     * @param data
     */
    public String drmRequest(String url, String data) {
        String response = "";
        byte[] nData = null;
        HttpPost httpPost = null;
        HttpGet httpGet = null;

        try {
            if (data != null) {
                httpPost = new HttpPost(url);
                httpPost.setEntity(new StringEntity(data));
                Log.d(TAG, "URL:" + url);
                Log.d(TAG, "Post Request:" + data);
            } else {
                httpGet = new HttpGet(url);
                Log.d(TAG, "URL:" + url);
                Log.d(TAG, "Get Request");
            }

            HttpResponse httpResp = null;
            HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, 10 * 1000);
            HttpConnectionParams.setSoTimeout(httpParameters, 5 * 1000);
            HttpClient httpClient = new DefaultHttpClient(httpParameters);

            httpResp = httpClient.execute((data == null) ? httpGet : httpPost);
            if (httpResp.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
                nData = EntityUtils.toByteArray(httpResp.getEntity());
                response = response + (new String(nData));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Response:" + response);
        return response;
    }

    public int acquireLicenseByUrl(Session session, String ecmData) {
        this.session = session;

        Log.d(TAG, "acquireLicense by URL: " + ecmData);
        // return native_acquireLicenseByUrl(ecmData, securePath);
        return native_acquireLicenseByUrl(ecmData, null);
    }
/*
@SuppressWarnings({ "deprecation", "unused" })
public class ChinaDrm {



    public int play(Session session, String filename, boolean isLocal) {
        String ecmData = null;
        String tsName = null;
        this.session = session;

        try {
            FileInputStream stream = new FileInputStream(filename);
            byte[] nData = new byte[stream.available()];
            stream.read(nData);
            stream.close();

            //extract #EXT-X-KEY from manifest
            if (nData != null) {
                String[]  lines = new String(nData).split("\n");
                for (int i=0; i< lines.length; i++) {
                    if (lines[i].startsWith("#EXT-X-KEY")) {
                        ecmData = lines[i].trim();
                    }
                    if (!lines[i].startsWith("#") && lines[i].endsWith(".ts")) {
                        tsName = filename.substring(0, filename.lastIndexOf("/")+1) + lines[i].trim();
                    }
                    if (ecmData != null && tsName!=null){
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "get ECM failed");
        }

        Log.d(TAG, "get ECM: " + ecmData + ", get TS: " + tsName);

        if (isLocal) {
            return native_play_local(ecmData, tsName);
        } else {
            return native_play(ecmData, tsName);
        }
    }

    public String playDash(Session session) {
        this.session = session;
        return native_play_dash();
    }



    public String queryInfo(Session session, String filename) {
        this.session = session;
        String ecmData = null;

        try{
            FileInputStream stream = new FileInputStream(filename);
            byte[] nData = new byte[stream.available()];
            stream.read(nData);
            stream.close();

            //extract #EXT-X-KEY from manifest
            if (nData != null) {
                String[]  lines = new String(nData).split("\n");
                for (int i=0; i< lines.length; i++) {
                    if (lines[i].startsWith("#EXT-X-KEY")) {
                        ecmData = lines[i].trim();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "get ECM failed");
        }

        Log.d(TAG, "queryInfo with ECM: " + ecmData);
        return native_queryinfo(ecmData);
    }

    public int setForceQuit() {
        return native_setForceQuit();
    }
*/
    static {
        try {
            System.loadLibrary("native-lib");
            System.loadLibrary("irdetodrm_ps");
        } catch (Exception e) {
           // Log.e(TAG, e.getMessage());
        }
    }

    public native int native_startup(String seucrePath, String deviceInfo, Context appCtx);

    public native int native_shutdown();

    public native int native_play(String ecmData, String dataFile);

    public native int native_play_local(String ecmData, String dataFile);

    public native void native_initIDs();

    public native String native_queryinfo(String ecmData);

    public native int native_acquireLicense(String ecmData, String dataPath);

    public native int native_acquireLicenseByUrl(String ecmData, String dataPath);

    public native byte[] native_decryptBuffer(byte[] data);

    public native int native_setForceQuit();

 /*   *//**
     * errorNotification
     *
     * @param severity
     * @param errorCode
     * @param additionalCode
     * @param description
     *//*
    public void errorNotification(int severity, int errorCode, int additionalCode, String description) {
        if (listener != null) {
            Log.d(TAG, "errorNotification:" + description);
            listener.onEvent(EventType.ERRORNOTIFICATION, description, "", "");
        }
    }

    *//**
     * Send request to License Server
     *
     * @param url
     * @param data
     *//*
    public String drmAgentRequest(String url, String data) {
        String response = "";
        byte[] nData = null;
        String fullUrl = url;
        HttpPost httpPost = null;
        HttpGet httpGet = null;

        try {
            if (data != null) {
                String sessionId = session.getSessionId();
                String ticket = session.getTicket();
                String crmId = session.getUser().getCrmId();

                StringBuffer buffer = new StringBuffer(url);
                if (buffer.indexOf("?") > -1) {
                    buffer.append("&");
                } else {
                    buffer.append("?");
                }
                if (StringUtility.notEmpty(crmId)) buffer.append("CrmId=").append(crmId);
                if (StringUtility.notEmpty(crmId))
                    buffer.append("&").append("AccountId=").append(crmId);
                if (StringUtility.notEmpty(sessionId))
                    buffer.append("&").append("SessionId=").append(sessionId);
                if (StringUtility.notEmpty(ticket)) buffer.append("&").append("Ticket=").append(ticket);

                fullUrl = buffer.toString();
                httpPost = new HttpPost(fullUrl);
                List<String> cookies = session.getCookies();
                if (cookies != null) {
                    for (String cookie : cookies) {
                        httpPost.addHeader("cookie", cookie);
                    }
                }
                httpPost.setEntity(new StringEntity(data));
                Log.d(TAG, "URL:" + fullUrl);
                Log.d(TAG, "Post Reqest:" + data);
            } else {
                httpGet = new HttpGet(fullUrl);
                Log.d(TAG, "URL:" + fullUrl);
                Log.d(TAG, "Get Request");
            }

            HttpResponse httpResp = null;
            HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, 10*1000);
            HttpConnectionParams.setSoTimeout(httpParameters, 5*1000);
            HttpClient httpClient = new DefaultHttpClient(httpParameters);

            httpResp = httpClient.execute((data == null) ? httpGet : httpPost);
            if (httpResp.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
                nData = EntityUtils.toByteArray(httpResp.getEntity());
                response = response + (new String(nData));
            }
        } catch (Exception e) {
        }

        Log.d(TAG, "Response:" + response);
        return response;
    }

    *//**
     * Send request to ChinaDRM Server
     *
     * @param url
     * @param data
     *//*
    public String drmRequest(String url, String data) {
        String response = "";
        byte[] nData = null;
        HttpPost httpPost = null;
        HttpGet httpGet = null;

        try {
            if (data != null) {
                httpPost = new HttpPost(url);
                httpPost.setEntity(new StringEntity(data));
                Log.d(TAG, "URL:" + url);
                Log.d(TAG, "Post Request:" + data);
            } else {
                httpGet = new HttpGet(url);
                Log.d(TAG, "URL:" + url);
                Log.d(TAG, "Get Request");
            }

            HttpResponse httpResp = null;
            HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, 10*1000);
            HttpConnectionParams.setSoTimeout(httpParameters, 5*1000);
            HttpClient httpClient = new DefaultHttpClient(httpParameters);

            httpResp = httpClient.execute((data == null) ? httpGet : httpPost);
            if (httpResp.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
                nData = EntityUtils.toByteArray(httpResp.getEntity());
                response = response + (new String(nData));
            }
        } catch (Exception e) {
        }

        Log.d(TAG, "Response:" + response);
        return response;
    }

*//*
  * Download & Go feature
  *//*

    *//*
      * download() -- download the main manifest, sub manifest and given numbers of TS to path
      *
      *  @param url    the url of main manifest
      *  @param path  the path to be store the downloaded manifest and ts
      *  @param number  the number of TS to be download
      *
      *  @retval the sub manifest which include "#EXT-X-KEY" info
      *//*
    public String download(String url, String path, int number) {
        byte[] nData = null;
        StringBuffer buffer = new StringBuffer(url);
        String fullUrl = buffer.toString();
        String mainName = null;
        String subName = null;
        String ecmData = null;
        String buf = null;

        // download main manifest to path/xxx.m3u8
        Log.d(TAG, "download to: " + path);
        mainName = path + fullUrl.substring(fullUrl.lastIndexOf("/")+1);
        nData = download2File(fullUrl, mainName);
        if (nData == null) {
            return null;
        }

        buf = new String(nData);
        if (!buf.startsWith("#EXTM3U")) {
            return null;   *//*NOT m3u8*//*
        }

        byte[] split = {0x0d, 0x0a};
        String[]  lines = new String(nData).split(new String(split));
        for (int i=0; i< lines.length; i++) {
            if (!lines[i].startsWith("#") && lines[i].endsWith(".m3u8")) {
                fullUrl = fullUrl.substring(0, fullUrl.lastIndexOf("/")+1) + lines[i].trim();
                subName = path + lines[i].trim();
                break;
            }
        }

        if (subName == null)
        {
            return null; *//* NOT Master m3u8 *//*
        }

        //download sub manifest and store to path/xxx/yyy.m3u8
        Log.d(TAG, "download sub manifest: " + subName);
        nData = download2File(fullUrl, subName);
        if (nData == null) {
            return null;
        }

        buf = new String(nData);
        if (!buf.startsWith("#EXTM3U")) {
            return null;   *//*NOT m3u8*//*
        }

        //download ts and store to path/xxx/zzz.ts
        fullUrl = fullUrl.substring(0, fullUrl.lastIndexOf("/")+1);
        int n = downloadTS(fullUrl, new String(nData), number, subName.substring(0, subName.lastIndexOf("/")+1));
        if (n != number) {
            return null;
        }

        return subName;
    }

    public int acquireLicense(Session session, String filename) {
        String ecmData = null;
        this.session = session;

        try {
            FileInputStream stream = new FileInputStream(filename);
            byte[] nData = new byte[stream.available()];
            stream.read(nData);
            stream.close();

            //extract #EXT-X-KEY from manifest
            if (nData != null) {
                String[]  lines = new String(nData).split("\n");
                for (int i=0; i< lines.length; i++) {
                    if (lines[i].startsWith("#EXT-X-KEY")) {
                        ecmData = lines[i].trim();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "get ECM failed");
        }

        Log.d(TAG, "acquireLicense with ECM: " + ecmData);
        return native_acquireLicense(ecmData, null);
    }

    public int acquireLicenseByUrl(Session session, String ecmData) {
        this.session = session;

        Log.d(TAG, "acquireLicense by URL: " + ecmData);

        return native_acquireLicenseByUrl(ecmData, null);
    }

    private  byte[] download2File(String url, String filename) {
        byte[] nData = null;

        try {
            HttpGet httpGet = new HttpGet(url);
            HttpResponse httpResp = null;
            HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, 10*1000);
            HttpConnectionParams.setSoTimeout(httpParameters, 10*1000);
            HttpClient httpClient = new DefaultHttpClient(httpParameters);

            httpResp = httpClient.execute(httpGet);
            if (httpResp.getStatusLine().getStatusCode() == HttpURLConnection.HTTP_OK) {
                nData = EntityUtils.toByteArray(httpResp.getEntity());
            }

            if (filename != null && nData != null) {
                File path = new File(filename.substring(0, filename.lastIndexOf("/")+1));
                if (!path.exists()) {
                    path.mkdir();
                }

                File file =new File(filename);
                if(!file.exists()){
                    file.createNewFile();
                }

                FileOutputStream stream = new FileOutputStream(file);
                stream.write(nData);
                stream.close();
            }

            Log.d(TAG, "download url: " + url + ", to file: " + filename);
        } catch (Exception e) {
            Log.d(TAG, "failed to download url: " + url + ", to file: " + filename);
        }
        return nData;
    }


    private int downloadTS(String url, String manifest, int number, String dataPath) {
        int count = 0;
        String[]  lines = manifest.split("\n");
        for (int i=0; i< lines.length; i++) {
            if (!lines[i].startsWith("#") && lines[i].endsWith(".ts")) {
                if (number>0) {
                    number--;
                    String tsName = dataPath + lines[i].trim();
                    String tsUrl = url + lines[i].trim();

                    Log.d(TAG, "download TS from: " + tsUrl + ", store to: " + tsName);
                    if (download2File(tsUrl, tsName) != null) {
                        count++;
                    }
                } else {
                    break;
                }
            }
        }

        return count;

    }*/
}
