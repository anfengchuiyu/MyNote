package com.example.medialearn.gles;

import android.content.Context;

import java.io.InputStream;

/**
 * Created by liuzhe
 * DateTime: 2021/1/30
 * Description:
 */
public class RawUtil {


    public static String getFromRaw(Context context, int rawResId) {
        String result = null;
        try {
            InputStream in = context.getResources().openRawResource(rawResId);
            int length = in.available();
            byte[] buffer = new byte[length];
            in.read(buffer);
            result = new String(buffer, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
