package com.test.yanxiu.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class SdkManager {
    public static OkHttpClient initNetClient(OkHttpClient okHttpClient) {
        return okHttpClient = new OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS).build();
    }
}