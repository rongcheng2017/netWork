package com.test.yanxiu.network;

/**
 * Created by cailei on 09/11/2016.
 */

public interface HttpCallback<T> {
    void onSuccess(RequestBase request, T ret);
    void onFail(RequestBase request, Error error);

}
