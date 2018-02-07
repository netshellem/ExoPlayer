package com.irdeto.drm.utility;

/**
 * Created by Syliu on 25-01-2018.
 */

public class StringUtility {
    public static boolean isEmpty(String str) {
        return (str == null || str.equals("")) ? true : false;
    }

    public static boolean notEmpty(String str) {
        return (str == null || str.equals("")) ? false : true;
    }
}
