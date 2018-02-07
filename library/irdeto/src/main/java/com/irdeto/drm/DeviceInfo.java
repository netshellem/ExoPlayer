package com.irdeto.drm;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

public class DeviceInfo {
    public String DMDeviceModel = "";
    public String DMOS = "";
    public String DMOSVersion = "";

    public static DeviceInfo getInstance(Context context) {
        DeviceInfo device = new DeviceInfo();
        device.DMDeviceModel = Build.MODEL;
        device.DMOS = "Android Phone";
        device.DMOSVersion = Build.VERSION.RELEASE;

        return device;
    }

    public String getDMOs() {
        return new StringBuilder("Android ").toString();
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public String toJsonString() {
        return new StringBuilder("{")
                .append("\"DMDeviceModel\":\"").append(DMDeviceModel).append("\"")
                .append(",").append("\"DMOS\":\"").append(DMOS).append("\"")
                .append(",").append("\"DMOSVersion\":\"").append(DMOSVersion).append("\"")
                .append("}")
                .toString();
    }
}
