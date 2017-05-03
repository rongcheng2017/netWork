package com.test.yanxiu.network;


import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class SdkManager {
    public static OkHttpClient initNetClient(OkHttpClient okHttpClient) {
        return okHttpClient = new OkHttpClient.Builder()
                .readTimeout(100, TimeUnit.SECONDS).writeTimeout(100, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS).build();
    }
}