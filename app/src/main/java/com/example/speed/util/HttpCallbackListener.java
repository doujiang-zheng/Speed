package com.example.speed.util;

/**
 * Created by 豆浆 on 2015-12-30.
 */
public interface HttpCallbackListener {
    void onFinish(String response);

    void onError(Exception e);
}
