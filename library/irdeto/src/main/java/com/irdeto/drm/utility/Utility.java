package com.irdeto.drm.utility;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.irdeto.drm.session.Session;
import com.irdeto.drm.session.User;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class Utility {

    public static Properties loadProperties(String fileName) {
        Properties prop = null;
        try {
            InputStream in = new FileInputStream(new File(fileName));
            prop = loadProperties(in);
        } catch (Exception e) {
            throw new IllegalArgumentException("can not load such properties");
        }

        return prop;
    }

    public static Properties loadProperties(InputStream in) {
        Properties prop = new Properties();
        try {
            prop.load(in);
        } catch (Exception e) {
            throw new IllegalArgumentException("can not load such properties");
        }

        return prop;
    }

    public static User getUser(Properties prop) {
        String crmId = prop.getProperty("rrm.crmId");
        String userId = prop.getProperty("rrm.userId");
        String password = prop.getProperty("rrm.password");
        return new User(crmId, userId, password);
    }

    public static Session getSession(Properties prop) {
        String url = prop.getProperty("rrm.url");
        String sessionId = prop.getProperty("rrm.sessionId");
        String ticket = prop.getProperty("rrm.ticket");
        User user = getUser(prop);
        return new Session(url, user, sessionId, ticket);
    }

    public static String getSecurePath(Context context) {
        return new StringBuilder("/data/data/").append(context.getPackageName()).append("/").toString();
    }

    public static String getConfigPath(Context context) {
        return new StringBuilder("/data/data/").append(context.getPackageName()).toString();
    }

    public static void copyConfigFiles(Context context) {
        String configPath = getConfigPath(context);
        copyConfigFiles(context, configPath);
    }

    public static void copyConfigFiles(Context context, String destPath) {
        String[] filePaths = null;
        try {
            filePaths = context.getAssets().list("data");
        } catch (Exception e) {

        }

        if (filePaths != null) {

            for(String path : filePaths) {
                String assetFilePath = new StringBuilder("data").append("/").append(path).toString();
                String destFilePath = new StringBuilder(destPath).append("/").append(path).toString();

                copyFile(context, assetFilePath, destFilePath);
            }
        }
    }

    public static void copySecureFile(Context context, String path, String destFilePath)
    {
       //File file = new File("asset:///acv.dat");
        AssetManager am = context.getAssets();
        InputStream fis = null;
        FileOutputStream outStream = null;
        try {
            fis = am.open(path);

            outStream = new FileOutputStream(destFilePath);

            int bytesRead = 0;
            byte[] buf = new byte[4096];
            while ((bytesRead = fis.read(buf)) != -1) {
                outStream.write(buf, 0, bytesRead);
            }
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(fis);
            close(outStream);
        }

    }

    public static void copyFile(Context context, String sourceFilePath, String destFilePath) {
        Log.d("utility", "copy file " + sourceFilePath + " to " + destFilePath);
        InputStream inStream = null;
        FileOutputStream outStream = null;
        try {
            inStream = context.getAssets().open(sourceFilePath);
            outStream = new FileOutputStream(destFilePath);

            int bytesRead = 0;
            byte[] buf = new byte[4096];
            while ((bytesRead = inStream.read(buf)) != -1) {
                outStream.write(buf, 0, bytesRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(inStream);
            close(outStream);
        }
    }

    public static void close(InputStream is) {
        if (is == null) return;
        try {is.close();} catch(Exception e) {e.printStackTrace();}
    }

    public static void close(OutputStream os) {
        if (os == null) return;
        try {os.close();} catch(Exception e) {e.printStackTrace();}
    }

    public static void copyConfig(Context context, String path, String file) {
        if ((context == null) || (path == null) || (file == null)) return;

        String assetFilePath = new StringBuilder("data").append("/").append(file).toString();
        String destFilePath = new StringBuilder(path).append("/").append(file).toString();
        copyFile(context, assetFilePath, destFilePath);
    }

    public static boolean deleteFile(String fileName) {
        boolean ret = false;
        try {
            ret = new File(fileName).delete();
        } catch (Exception e) {

        }

        return ret;
    }

    public static boolean writeData(String fileName, byte[] data) {
        OutputStream os = null;
        boolean ret = false;
        try {
            File file = new File(fileName);
            os = new FileOutputStream(file);
            os.write(data);
            ret = true;
        } catch (Exception e) {
        } finally {
            if (os != null) try {os.close();} catch (Exception ec) {;}
        }

        return ret;
    }


    /**
     * @return If could connect the network, return true; else return false.
     */
    public static boolean isOpenNetwork(Context context) {
        ConnectivityManager connManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();

        if ( networkInfo!= null ) {
            int networkType = networkInfo.getType();
            if(ConnectivityManager.TYPE_WIFI == networkType){
                /* TODO something with WIFI */
            }else if(ConnectivityManager.TYPE_MOBILE == networkType){
                /* TODO something with 4G/3G/2G */
            }
            return connManager.getActiveNetworkInfo().isAvailable();
        }

        return false;
    }
}

