package com.test.yanxiu.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.orhanobut.logger.LogLevel;
import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


/**
 * Created by cailei on 08/11/2016.
 */

public abstract class RequestBase {
    public static final Gson gson = new GsonBuilder().create();

    public static Gson getGson() {
        return gson;
    }

    protected static OkHttpClient client = null;

    protected static final Handler handler = new Handler(Looper.getMainLooper());

    protected transient Call call = null;

    public Call getCall() {
        return call;
    }

    public enum HttpType {
        GET,
        POST
    }

    protected abstract boolean shouldLog();

    protected HttpType httpType() {
        return HttpType.GET;
    }

    protected abstract String urlServer();

    protected abstract String urlPath();

    protected String fullUrl() throws NullPointerException, IllegalAccessException, IllegalArgumentException {
        String server = urlServer();
        String path = urlPath();

        if (server == null) {
            throw new NullPointerException();
        }

        server = omitSlash(server);
        path = omitSlash(path);

        if (!urlServer().substring(0, 4).equals("http")) {
            server = "http://" + urlServer();
        }

        String fullUrl = server;
        if (path != null) {
            fullUrl = fullUrl + "/" + path;
        }

        HttpUrl.Builder urlBuilder = HttpUrl.parse(fullUrl).newBuilder();

        Map<String, String> params = urlParams();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            try {
                urlBuilder.addEncodedQueryParameter(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                Log.e("e", e.getMessage().toString());
            }

        }
        fullUrl = urlBuilder.build().toString();

        return fullUrl;
    }

    public <T> UUID startRequest(final Class<T> clazz, final HttpCallback<T> callback) {
        UUID uuid = UUID.randomUUID();
        Request request = null;
        Logger.init("SK::").hideThreadInfo().methodCount(2).logLevel(LogLevel.FULL);
        try {
            request = generateRequest(uuid);

            Log.d("SK::  Request :", request.url().url().toString());
        } catch (Exception e) {

        }
        if (request == null) {
            callback.onFail(RequestBase.this, new Error("request start error"));
            return null;
        }
        client = SdkManager.initNetClient(client);
        call = client.newCall(request);
        final long start = System.currentTimeMillis();
        call.enqueue(new Callback() {
            @Override
            public void onFailure(final Call call, final IOException e) {
                if (call.isCanceled()) {
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFail(RequestBase.this, new Error("网络异常，请稍后重试"));
                    }
                });
            }

            @Override
            public void onResponse(final Call call, final Response response) throws IOException {
                try {
                    if (call.isCanceled()) {
                        return;
                    }
                } catch (Exception e) {
                    Log.e("error", e.getMessage());
                }

                final String retStr = response.body().string();


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!response.isSuccessful()) {
                            Log.d("time", (System.currentTimeMillis() - start) / 1000 + "");
                            Logger.d("Sk:: Response error  " + "code: " + response.code() + ",   message: " + response.message());
                            callback.onFail(RequestBase.this, new Error("服务器数据异常"));
                            return;
                        }
                        T ret;
                        try {
                            Logger.json(retStr);
                            ret = RequestBase.gson.fromJson(retStr, clazz);
                        } catch (Exception e) {
                            e.printStackTrace();
                            callback.onFail(RequestBase.this, new Error("服务器返回格式错误"));
                            return;
                        }
                        callback.onSuccess(RequestBase.this, ret);
                    }
                });

            }
        });

        return uuid;
    }

    public void cancelRequest() {
        if (call != null) {
            call.cancel();
        }
        call = null;
    }

    public static void cancelRequestWithUUID(UUID uuid) {
        for (Call call : client.dispatcher().queuedCalls()) {
            if (call.request().tag().equals(uuid))
                call.cancel();
        }
        for (Call call : client.dispatcher().runningCalls()) {
            if (call.request().tag().equals(uuid))
                call.cancel();
        }
    }

    // 去除Post Body中的参数后，剩余的应加入Url里的参数
    protected Map<String, String> urlParams() throws IllegalAccessException, IllegalArgumentException {
        String json = gson.toJson(this);
        Object o = gson.fromJson(json, this.getClass());
        Field[] fields = o.getClass().getFields();
        for (Field f : fields) {
            if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers())) {
                continue;
            }

            if (f.isAnnotationPresent(RequestParamType.class)) {
                RequestParamType annotation = (RequestParamType) f.getAnnotation(RequestParamType.class);
                RequestParamType.Type type = annotation.value();
                if (type == RequestParamType.Type.POST) {
                    f.set(o, null);
                }
            } else {
                if (httpType() == HttpType.GET) {
                }
                if (httpType() == HttpType.POST) {
                    f.set(o, null);
                }
            }
        }
        Map<String, String> params = new HashMap<>();
        String oJson = gson.toJson(o);
        params = gson.fromJson(oJson, params.getClass());

        return params;
    }

    // 应该加入Post Body中的参数
    protected Map<String, String> bodyParams() throws IllegalAccessException, IllegalArgumentException {
        String json = gson.toJson(this);
        Object o = gson.fromJson(json, this.getClass());
        Field[] fields = o.getClass().getFields();
        for (Field f : fields) {
            if (f.isAnnotationPresent(RequestParamType.class)) {
                RequestParamType annotation = (RequestParamType) f.getAnnotation(RequestParamType.class);
                RequestParamType.Type type = annotation.value();
                if (type == RequestParamType.Type.GET) {
                    f.set(o, null);
                }
            } else {
                if (httpType() == HttpType.GET) {
                    f.set(o, null);
                }
                if (httpType() == HttpType.POST) {
                }
            }
        }
        Map<String, String> params = new HashMap<>();
        String oJson = gson.toJson(o);
        params = gson.fromJson(oJson, params.getClass());

        return params;
    }

    private String omitSlash(String org) {
        if (org == null) {
            return null;
        }

        String ret = org;
        // 掐头
        String t = ret.substring(0, 1);

        if ("/".equals(ret.substring(0, 1))) {
            ret = ret.substring(1, ret.length());
        }
        // 去尾
        if ("/".equals(ret.substring(ret.length() - 1, ret.length()))) {
            ret = ret.substring(0, ret.length() - 1);
        }
        return ret;
    }

    protected void runOnUiThread(Runnable task) {
        handler.post(task);
    }

    protected Request generateRequest(UUID uuid) throws NullPointerException, IllegalAccessException, IllegalArgumentException {
        Request.Builder builder = new Request.Builder()
                .tag(uuid)
                .url(fullUrl());
        if (httpType() == HttpType.POST) {
            FormBody.Builder bodyBuilder = new FormBody.Builder();
            Map<String, String> params = bodyParams();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                bodyBuilder.add(entry.getKey(), entry.getValue());
            }
            builder.post(bodyBuilder.build());
        }

        Request request = builder.build();
        return request;
    }
}
