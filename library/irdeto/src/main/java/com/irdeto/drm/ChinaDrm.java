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
        return native_acquireLicenseByUrl(ecmData, null);
    }

    public void createSession(String ecmData) {
        native_createsession(ecmData);
    }

    public  int acquireLicense(String ecmData, String dataPath){
        return native_acquireLicense(ecmData, dataPath);
    }

    public void destorySession() {
        native_destorysession();
    }

    public byte[] decryptBuffer( byte[] data,  String chinaDrmLine, boolean isFinal){
        return  native_decryptBuffer(  data,  chinaDrmLine, isFinal);
    }
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

    public native byte[] native_decryptBuffer( byte[] data,  String chinaDrmLine, boolean isFinal);

    public native void native_createsession( String ecmData);

    public native void native_destorysession();

    public native int native_setForceQuit();


}
